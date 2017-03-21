package com.egoshi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;

public class SplashActivity extends Activity {

	private static final String TAG = "SplashActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Window win = getWindow();
		win.requestFeature(Window.FEATURE_NO_TITLE);
		//win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.splash);
		
		Handler handlerSplash = new Handler(){
			public void handleMessage(Message msg){
				Intent intent = new Intent(SplashActivity.this,DeviceScanActivity.class);
				startActivity(intent);
				finish();
			}
		};
		handlerSplash.sendEmptyMessageDelayed(0, 1500);
	}
	
	/*
	@Override	
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.splash, menu);
		return true;
	}
	*/

}
