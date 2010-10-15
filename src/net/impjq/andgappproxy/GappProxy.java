package net.impjq.andgappproxy;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

public class GappProxy extends Activity implements OnClickListener {
	public static final String LOGTAG = GappProxy.class.getSimpleName();

	static final String SETTINGS_FETCH_SERVER_URL = "gappproxy_fetch_server_url";
	static final String SETTINGS_RUNNING_AT_LOCAL_PORT = "gappproxy_local_port";

	static final String mDefaultFetchServerUrl = "http://pjqgapp.appspot.com/fetch.py";
	static final String mDefaultLocalPort = "5865";

	private String mFetchServerUrl;
	private String mLocalPort;

	private Button mStartProxyButton;
	private Button mStopProxyButton;

	private EditText mGappProxyServerURLEditText;
	private EditText mGappProxyLocalServerPort;	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		init();
		restoreSettings();
	}

	void init() {
		mStartProxyButton = (Button) findViewById(R.id.startProxyButton);
		mStopProxyButton = (Button) findViewById(R.id.stopProxyButton);

		mStartProxyButton.setOnClickListener(this);
		mStopProxyButton.setOnClickListener(this);

		mGappProxyServerURLEditText = (EditText) findViewById(R.id.gappProxy_fetchserver);
		mGappProxyLocalServerPort = (EditText) findViewById(R.id.gappProxy_localport);

		Intent serviceIntent = new Intent(this, ProxyService.class);

		this.bindService(serviceIntent, mServiceConnection,
				Service.BIND_AUTO_CREATE);

	}

	ServiceConnection mServiceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			Utils.log(LOGTAG, "onServiceConnected");
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub

		}
	};

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		this.unbindService(mServiceConnection);
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub

		int key = v.getId();

		switch (key) {
		case R.id.startProxyButton:
			Utils.log(LOGTAG, "onClick startProxyButton");
			// First need to save the settings.
			saveSettings();
			Intent startProxyIntent = new Intent(this, ProxyService.class);
			startProxyIntent.setAction(ProxyService.ACTION_START_SERVER);
			startService(startProxyIntent);
			showNotification();

			break;

		case R.id.stopProxyButton:
			Utils.log(LOGTAG, "onClick stopProxyButton");
			Intent stopProxyIntent = new Intent(this, ProxyService.class);
			stopProxyIntent.setAction(ProxyService.ACTION_STOP_SERVER);
			startService(stopProxyIntent);
			dismissNotification();
			break;

		default:
			break;
		}
	}

	int notificationId = 1;

	void showNotification() {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, GappProxy.class), 0);
		Notification n = new Notification(R.drawable.icon,
				"GappProxy @local,PORT=" + mLocalPort, System
						.currentTimeMillis());
		n.setLatestEventInfo(this, LOGTAG, "Running GappProxy @local,PORT="
				+ mLocalPort, contentIntent);
		n.vibrate = new long[] { 100, 250, 100, 500 };
		nm.notify(notificationId, n);
	}

	void dismissNotification() {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(notificationId);
	}

	private void saveSettings() {
		SharedPreferences preference = PreferenceManager
				.getDefaultSharedPreferences(this);

		Editor editor = preference.edit();

		// Save fetch server url
		String serverUrl = mGappProxyServerURLEditText.getText().toString();

		if (null != serverUrl) {
			editor.putString(SETTINGS_FETCH_SERVER_URL, serverUrl);
			mFetchServerUrl = serverUrl;
		}

		// Save local port.
		String port = mGappProxyLocalServerPort.getText().toString();

		if (null != port) {
			editor.putString(SETTINGS_RUNNING_AT_LOCAL_PORT, port);
			mLocalPort = port;
		}

		editor.commit();
	}

	/**
	 * Read the settings from SharedPreferences.
	 */
	private void readSettings() {
		SharedPreferences preference = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Get fetch server url
		mFetchServerUrl = preference.getString(SETTINGS_FETCH_SERVER_URL,
				mDefaultFetchServerUrl);

		// Get local port
		mLocalPort = preference.getString(SETTINGS_RUNNING_AT_LOCAL_PORT,
				mDefaultLocalPort);
	}

	/**
	 * Restore the Settings when launch this application.
	 */
	private void restoreSettings() {
		readSettings();
		if (null != mGappProxyServerURLEditText) {
			mGappProxyServerURLEditText.setText(mFetchServerUrl);
		}

		if (null != mGappProxyLocalServerPort) {
			mGappProxyLocalServerPort.setText(mLocalPort);
		}
	}

	private void setProxy() {
		System.getProperties().setProperty("http.proxyHost", "10.85.40.153");
		System.getProperties().setProperty("http.proxyPort", "8000");

	}
}