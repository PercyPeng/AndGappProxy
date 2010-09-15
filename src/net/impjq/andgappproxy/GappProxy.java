package net.impjq.andgappproxy;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class GappProxy extends Activity implements OnClickListener {
	public static final String LOGTAG = GappProxy.class.getSimpleName();

	Button mStartProxyButton;
	Button mStopProxyButton;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		init();
	}

	void init() {
		mStartProxyButton = (Button) findViewById(R.id.startProxyButton);
		mStopProxyButton = (Button) findViewById(R.id.stopProxyButton);

		mStartProxyButton.setOnClickListener(this);
		mStopProxyButton.setOnClickListener(this);

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

	public void onClick(View v) {
		// TODO Auto-generated method stub

		int key = v.getId();

		switch (key) {
		case R.id.startProxyButton:
			Utils.log(LOGTAG, "onClick startProxyButton");
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
				"GappProxy @local,PORT=" + Conf.PORT, System
						.currentTimeMillis());
		n.setLatestEventInfo(this, LOGTAG, "Running GappProxy @local,PORT="
				+ Conf.PORT, contentIntent);
		n.vibrate = new long[] { 100, 250, 100, 500 };
		nm.notify(notificationId, n);
	}

	void dismissNotification() {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(notificationId);
	}
}