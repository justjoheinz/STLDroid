package softwarepoets.stldroid;

import java.util.List;

import roboguice.application.RoboApplication;

import com.google.inject.Module;

public class STLDroidApplication extends RoboApplication {
	
	protected void addApplicationModules(List<Module> modules) {
        modules.add(new STLDroidModule());
    }

}
