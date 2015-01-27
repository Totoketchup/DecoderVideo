package com.example.decoderudpclient;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DecodeActivity extends ActionBarActivity implements SurfaceHolder.Callback {

	private PlayerThread mPlayer = null;
	private PlayerAudioThread mAudioPlayer = null;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_decode);
		
		SurfaceView sv = new SurfaceView(this); // Create a SurfaceView
		sv.getHolder().addCallback(this);
		setContentView(sv);
	}

	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mPlayer == null) {
			mPlayer = new PlayerThread(holder.getSurface());
			mPlayer.start();
		}
		if ( mAudioPlayer == null){
			mAudioPlayer = new PlayerAudioThread();
			mAudioPlayer.start();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.decode, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mPlayer != null) {
			mPlayer.interrupt();
		}		
	}
}
