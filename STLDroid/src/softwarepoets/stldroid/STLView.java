package softwarepoets.stldroid;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import processing.core.PApplet;
import toxi.color.TColor;
import toxi.geom.AABB;
import toxi.geom.Vec3D;
import toxi.geom.mesh.Mesh3D;
import toxi.geom.mesh.STLReader;
import toxi.geom.mesh.STLReader.STLReaderCallback;
import toxi.geom.mesh.TriangleMesh;
import toxi.processing.ToxiclibsSupport;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class STLView extends PApplet implements ParseResultListener {

	class AsciiParser extends AsyncTask<String, String, Mesh3D> {
		private ProgressDialog pd;
		private String fileName;

		private WeakReference<ParseResultListener> listener;

		@Override
		protected Mesh3D doInBackground(String... params) {
			STLAsciiReader reader = new STLAsciiReader();
			Mesh3D parsedMesh = null;
			fileName = params[0];
			try {
				publishProgress("Parsing as ASCII");
				parsedMesh = reader.load(params[0]);
			} catch (FileNotFoundException e) {
				Log.e(TAG, e.getMessage());
			} catch (IllegalStateException e) {
				Log.e(TAG, "Not an Ascii STL File: " + e.getMessage());
			}
			return parsedMesh;
		}

		@Override
		protected void onPostExecute(Mesh3D result) {
			pd.dismiss();
			listener.get().postResult(result);
		}

		@Override
		protected void onPreExecute() {
			pd = ProgressDialog.show(STLView.this, null, "Parsing as ASCII");
		}

		@Override
		protected void onProgressUpdate(String... values) {
			pd.setMessage(values[0]);
		}

		public void setParseResultHandler(ParseResultListener listener) {
			this.listener = new WeakReference<ParseResultListener>(listener);
		}

	}

	class BinaryParser extends AsyncTask<String, Integer, Mesh3D> implements
			STLReaderCallback {
		private ProgressDialog pd;

		private WeakReference<ParseResultListener> listener;

		@Override
		protected Mesh3D doInBackground(String... params) {
			STLReader reader = new STLReader();
			reader.setSTLReaderCallBack(this);
			Mesh3D parsedMesh = null;
			// publishProgress("Parsing as ASCII");
			try {
				InputStream is = new FileInputStream(new File(params[0]));
				parsedMesh = reader.loadBinary(is, "stl", 0x4000,
						TriangleMesh.class);
			} catch (FileNotFoundException e) {
				parsedMesh = null;
			}

			return parsedMesh;
		}

		@Override
		protected void onPostExecute(Mesh3D result) {
			pd.dismiss();
			result.center(Vec3D.ZERO);
			mesh = result;
			listener.get().postResult(result);
		}

		@Override
		protected void onPreExecute() {
			pd = new ProgressDialog(STLView.this);
			pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			pd.setMessage("Parsing binary STL");
			pd.show();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (pd.getMax() != values[0])
				pd.setMax(values[0]);
			pd.setProgress(values[1]);
		}

		@Override
		public void report(int totalfaces, int currentFace) {
			publishProgress(totalfaces, currentFace);
		}

		public void setParseResultHandler(ParseResultListener listener) {
			this.listener = new WeakReference<ParseResultListener>(listener);
		}

	}

	class Download extends AsyncTask<Uri, Void, Uri> {

		private ProgressDialog pd;

		@Override
		protected Uri doInBackground(Uri... address) {
			OutputStream out = null;
			URLConnection conn = null;
			File cacheDir = getCacheDir();
			InputStream in = null;
			try {
				// Get the URL
				URL url = new URL(address[0].toString());
				File outputFile = new File(cacheDir,
						address[0].getLastPathSegment());
				FileOutputStream fos = new FileOutputStream(outputFile);
				// Open an output stream to the destination file on our local
				// filesystem
				out = new BufferedOutputStream(fos);
				conn = url.openConnection();

				in = conn.getInputStream();

				// Get the data
				byte[] buffer = new byte[1024];
				int numRead;
				while ((numRead = in.read(buffer)) != -1) {
					out.write(buffer, 0, numRead);
				}
				return Uri.parse(outputFile.toURI().toString());
			} catch (Exception exception) {
				Log.e("DOWNLOAD", exception.getMessage());
			} finally {
				try {
					if (in != null) {
						in.close();
					}
					if (out != null) {
						out.close();
					}
				} catch (IOException ioe) {
					// Shouldn't happen, maybe add some logging here if you are
					// not
					// fooling around ;)
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Uri result) {
			pd.dismiss();
		}

		@Override
		protected void onPreExecute() {
			pd = ProgressDialog.show(STLView.this, null, "Downloading");
		}
	}

	private static final String TAG = "STLVIEW";

	public static final int GROW = 0;
	public static final int SHRINK = 1;

	public static final int TOUCH_INTERVAL = 0;

	public static final float MIN_SCALE = 1f;
	public static final float MAX_SCALE = 4f;
	public static final float ZOOM = 1.5f;

	private float rotateX;
	private Mesh3D mesh;
	private ToxiclibsSupport toxic;

	private float brightness = 200;

	private Download downloader;

	private float rotateY;
	private Uri fileUri;

	private SharedPreferences preferences;

	float scale = 1.0f;

	float xCur, yCur, xPre, yPre, xSec, ySec, distDelta, distCur, distPre = -1;
	int mWidth, mHeight, mTouchSlop;
	long mLastGestureTime;

	private boolean showBuild;

	private float xBuild;

	private float yBuild;

	private float zBuild;

	private AABB bBox;

	@Override
	public void destroy() {
		mesh = null;
		Log.i(TAG, "STLView destroy called.");
		super.destroy();
	}

	@Override
	public void draw() {
		background(0);
		translate(screenWidth / 2, screenHeight / 2);
		scale(scale);
		initlights();
		rotateX(rotateX);
		rotateY(rotateY);
		if (showBuild) {
			toxic.chooseStrokeFill(false, TColor.newRGBA(1f, 0, 0, 0.01f),
					TColor.newRGBA(0.7f, 0, 0, 0.1f));
			toxic.box(bBox);
		}

		if (mesh != null) {
			toxic.chooseStrokeFill(false, TColor.newGray(0.8f),
					TColor.newGray(0.8f));
			toxic.mesh(mesh, false);
		}
	}

	private void getPrefs() {
		preferences = PreferenceManager
				.getDefaultSharedPreferences(STLView.this);
		brightness = Float.parseFloat(preferences
				.getString("brightness", "200"));
		showBuild = preferences.getBoolean("showBuildPlatform", false);
		xBuild = Float.parseFloat(preferences.getString("xBuild", "100"));
		yBuild = Float.parseFloat(preferences.getString("yBuild", "100"));
		zBuild = Float.parseFloat(preferences.getString("zBuild", "100"));
	}

	private void initlights() {
		directionalLight(brightness, brightness, brightness, 0, 0, 1);
		directionalLight(brightness, brightness, brightness, 1, 1, 0);
		directionalLight(brightness, brightness, brightness, -1, 1, -10);
	}

	@Override
	public void keyPressed() {
		if (key == CODED && keyCode == KeyEvent.KEYCODE_MENU) {
			Intent intent = new Intent(STLView.this, Preferences.class);
			startActivity(intent);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fileUri = getIntent().getData();
		Log.i(TAG, "onCreate started: " + fileUri.toString());
		if ("http".equals(fileUri.getScheme())) {
			downloader = new Download();
			downloader.execute(fileUri);
		}
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "STLView onDestroy called.");
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "STLView onPause called.");
		super.onPause();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "STLView onResume called.");
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i(TAG, "STLView onStart() called");
		getPrefs();
		bBox = new AABB(new Vec3D(0, 0, 0), new Vec3D(xBuild / 2, yBuild / 2,
				zBuild / 2));
		if (downloader != null) {
			try {
				fileUri = downloader.get();
			} catch (InterruptedException e) {
				Log.e(TAG, e.getMessage());
			} catch (ExecutionException e) {
				Log.e(TAG, e.getMessage());
			} finally {
				downloader = null;
			}
		}
		if (fileUri.getLastPathSegment().toLowerCase().endsWith(".stl")) {
			if (mesh == null) {
				Log.i(TAG, "STL File");
				AsciiParser parser = new AsciiParser();
				parser.setParseResultHandler(this);
				parser.execute(fileUri.getPath());
			}
		}
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "STLView onStop called.");
		super.onStop();
		mesh = null;
	}

	@Override
	public void pause() {
		Log.i(TAG, "STLView pause called.");
		super.pause();
	}

	@Override
	public synchronized void postResult(Mesh3D parsedMesh) {
		if (parsedMesh == null) {
			BinaryParser binParser = new BinaryParser();
			binParser.setParseResultHandler(this);
			binParser.execute(fileUri.getPath());
			return;
		} else {
			parsedMesh.center(Vec3D.ZERO);
			mesh = parsedMesh;
		}

	}

	@Override
	public void resume() {
		Log.i(TAG, "STLView resume called.");
		// TODO Auto-generated method stub
		super.resume();
	}

	@Override
	public void setup() {
		toxic = new ToxiclibsSupport(this);
	}

	@Override
	public int sketchHeight() {
		return screenHeight;
	}

	@Override
	public String sketchRenderer() {
		return P3D;
	}

	@Override
	public int sketchWidth() {
		return screenWidth;
	}

	@Override
	public void start() {
		Log.i(TAG, "STLView start called.");
		super.start();
	}

	@Override
	public void stop() {
		Log.i(TAG, "STLView stop called.");
		super.stop();
	}

	@Override
	public boolean surfaceTouchEvent(MotionEvent event) {
		int action = event.getAction() & MotionEvent.ACTION_MASK, p_count = event
				.getPointerCount();

		switch (action) {
		case MotionEvent.ACTION_MOVE:

			long now = android.os.SystemClock.uptimeMillis();
			// point 1 coords
			xCur = event.getX(0);
			yCur = event.getY(0);
			if (p_count == 1 && now - mLastGestureTime > 200) {
				rotateY = map(xCur, 0, screenWidth, TWO_PI, 0);
				rotateX = map(yCur, 0, screenHeight, TWO_PI, 0);
			}
			if (p_count > 1) {
				// point 2 coords
				xSec = event.getX(1);
				ySec = event.getY(1);

				// distance between
				distCur = (float) Math.sqrt(Math.pow(xSec - xCur, 2)
						+ Math.pow(ySec - yCur, 2));
				distDelta = distPre > -1 ? distCur - distPre : 0;
				float scale = this.scale;
				if (Math.abs(distDelta) > mTouchSlop) {
					mLastGestureTime = 0;

					int mode = distDelta > 0 ? GROW : (distCur == distPre ? 2
							: SHRINK);

					switch (mode) {
					case GROW: // grow
						this.scale = scale + (distDelta * 0.01f);
						break;
					case SHRINK: // shrink
						this.scale = scale + (distDelta * 0.01f);
						break;
					}
					scale = (scale <= 0 ? scale = 0 : scale);

					mLastGestureTime = now;
					xPre = xCur;
					yPre = yCur;
					distPre = distCur;
					return true;
				}
			}

			xPre = xCur;
			yPre = yCur;
			distPre = distCur;
			break;
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_UP:
			distPre = -1;
			mLastGestureTime = android.os.SystemClock.uptimeMillis();
			break;
		}

		return super.surfaceTouchEvent(event);
	}
}
