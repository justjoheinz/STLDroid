package softwarepoets.stldroid;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import com.lamerman.FileDialog;

public class STLDroid extends Activity {
	/** Called when the activity is first created. */
	
	SharedPreferences preferences;
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				String result = data.getExtras().getString(
						FileDialog.RESULT_PATH);
				Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(result), STLDroid.this, STLView.class);
				startActivity(intent);
				finish();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		preferences = PreferenceManager.getDefaultSharedPreferences(STLDroid.this);
		String path = preferences.getString("path", "/sdcard");
		Intent fileIntent = new Intent(STLDroid.this, FileDialog.class);
		fileIntent.putExtra(FileDialog.START_PATH, path);
		startActivityForResult(fileIntent, 1);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		super.onKeyDown(keyCode, event);
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			Intent intent = new Intent(STLDroid.this, Preferences.class);
			startActivity(intent);
			return true;
		}
		return false;
	}

}