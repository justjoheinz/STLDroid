package softwarepoets.stldroid;

import roboguice.activity.RoboPreferenceActivity;
import android.os.Bundle;

public class Preferences extends RoboPreferenceActivity {
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

	}

}
