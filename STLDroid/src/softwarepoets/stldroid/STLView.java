package softwarepoets.stldroid;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

import processing.core.PApplet;
import processing.core.PShape;
import processing.core.PShape3D;
import toxi.color.TColor;
import toxi.geom.mesh.Mesh3D;
import toxi.processing.ToxiclibsSupport;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

public class STLView extends PApplet {

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
		protected void onCancelled() {
			super.onCancelled();
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

	private PShape stl;
	private float rotateX;
	private Mesh3D mesh;
	private PShape shape;
	private ToxiclibsSupport toxic;

	private Download downloader;

	private float rotateY;
	private Uri fileUri;

	float scale = 1.0f;

	float xCur, yCur, xPre, yPre, xSec, ySec, distDelta, distCur, distPre = -1;
	int mWidth, mHeight, mTouchSlop;
	long mLastGestureTime;

	@Override
	public void draw() {
		background(0);
		initlights();
		translate(screenWidth / 2, screenHeight / 2);
		rotateX(rotateX);
		rotateY(rotateY);
		scale(scale);
		if (mesh != null) {
		toxic.chooseStrokeFill(false, TColor.newGray(150), TColor.newGray(150));
		toxic.mesh(mesh, false);
		}
		else {
			shape(shape);
		}
	}

	private void initlights() {
		directionalLight(200, 200, 200, 0, 0, 1);
		directionalLight(200, 200, 200, 1, 1, 0);
		directionalLight(200, 200, 200, -1, 1, -10);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fileUri = getIntent().getData();
		Log.i(TAG, fileUri.toString());
		if ("http".equals(fileUri.getScheme())) {
			downloader = new Download();
			downloader.execute(fileUri);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
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
		Log.i(TAG, "filename: " + fileUri.getPath());
		Log.i(TAG, "filename: " + fileUri.getPath());
		if (fileUri.getLastPathSegment().toLowerCase().endsWith(".stl")) {
			try {
				Log.i(TAG, "STL File");
				mesh = new STLAsciiReader().load(fileUri.getPath());
			} catch (FileNotFoundException e) {
				Log.e(TAG, e.getMessage());
			} catch (IllegalStateException e) {
				Toast.makeText(STLView.this, "Not an ASCII STL file",
						Toast.LENGTH_LONG).show();
				finish();
			}
		}
		if (fileUri.getLastPathSegment().toLowerCase().endsWith(".obj")) {
			Log.i(TAG, "obj File");
			mesh = null;
			shape = loadShape(fileUri.getPath());
		}
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
		return OPENGL;
	}

	@Override
	public int sketchWidth() {
		return screenWidth;
	}

	@Override
	public boolean surfaceTouchEvent(MotionEvent event) {
		Log.i(TAG, "surfachtouchevent");
		int action = event.getAction() & MotionEvent.ACTION_MASK, p_count = event
				.getPointerCount();

		switch (action) {
		case MotionEvent.ACTION_MOVE:
			Log.i(TAG, "move");
			// point 1 coords
			xCur = event.getX(0);
			yCur = event.getY(0);
			if (p_count == 1) {
				rotateY = map(xCur, 0, screenWidth, TWO_PI, 0);
				rotateX = map(yCur, 0, screenHeight, TWO_PI, 0);
			}
			if (p_count > 1) {
				Log.i(TAG, "move pcount >2");
				// point 2 coords
				xSec = event.getX(1);
				ySec = event.getY(1);

				// distance between
				distCur = (float) Math.sqrt(Math.pow(xSec - xCur, 2)
						+ Math.pow(ySec - yCur, 2));
				distDelta = distPre > -1 ? distCur - distPre : 0;
				Log.i(TAG, "distDelta: " + distDelta);
				float scale = this.scale;
				long now = android.os.SystemClock.uptimeMillis();
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
			Log.i(TAG, "updown");
			distPre = -1;
			mLastGestureTime = android.os.SystemClock.uptimeMillis();
			break;
		}

		return super.surfaceTouchEvent(event);
	}
}
