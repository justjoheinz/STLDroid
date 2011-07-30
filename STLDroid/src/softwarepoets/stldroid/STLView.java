package softwarepoets.stldroid;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import processing.core.PApplet;
import processing.core.PShape3D;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

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
			downloadRequired = false;
		}

		@Override
		protected void onPreExecute() {
			pd = ProgressDialog.show(STLView.this, null, "Downloading");
			downloadRequired = true;
		}

	}

	private PShape3D stl;
	private float rotateX;

	private float rotateY;
	private boolean downloadRequired;

	@Override
	public void draw() {
		background(0);
		translate(screenWidth / 2, screenHeight / 2);
		rotateX(rotateX);
		rotateY(rotateY);
		box(40, 40, 40);
	}

	@Override
	public void mouseDragged() {
		rotateX = map(mouseX, 0, screenWidth, 0, TWO_PI);
		rotateY = map(mouseY, 0, screenHeight, 0, TWO_PI);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Uri filename = getIntent().getData();
		/**
		if ("http".equals(filename.getScheme())) {
			new Download().execute(filename);
		}
		if (".obj".equalsIgnoreCase(filename.getLastPathSegment())) {
			stl = new PShape3D(STLView.this, filename.toString(), null);
		}
		*/
	}

	@Override
	public void setup() {
		// stl = loadShape(filename)
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

}
