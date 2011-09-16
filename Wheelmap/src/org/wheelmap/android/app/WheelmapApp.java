package org.wheelmap.android.app;

import org.wheelmap.android.manager.MapFileManager;
import org.wheelmap.android.manager.MyLocationManager;
import org.wheelmap.android.manager.SupportManager;

import android.app.Application;

public class WheelmapApp extends Application {
	private MyLocationManager mLocationManager;
	private SupportManager mSupportManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mLocationManager = MyLocationManager.initOnce( this );
		// MapFileManager.get( this ).updateLocal();
		mSupportManager = SupportManager.initOnce( this );
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		mLocationManager.clear();
	}
}