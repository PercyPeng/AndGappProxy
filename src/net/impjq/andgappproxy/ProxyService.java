package net.impjq.andgappproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class ProxyService extends Service {
	public static final String LOGTAG = ProxyService.class.getSimpleName();
	public static final String ACTION_START_SERVER = "start_server";
	public static final String ACTION_STOP_SERVER = "stop_server";

	boolean mIsServerRunning;
	ServerSocket serverSocket;

	private String mFetchServerUrl;
	private String mLocalPort;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mIsServerRunning = false;		
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		Utils.log(LOGTAG, "onStart,mIsServerRunning=" + mIsServerRunning);
		readSettings();

		if (null == intent) {
			return;
		}

		String action = "";
		action = intent.getAction();
		Utils.log(LOGTAG, "onStart,action==" + action);
		if (action.equals(ACTION_START_SERVER)) {
			if (!mIsServerRunning) {
				startServer(mFetchServerUrl,Integer.parseInt(mLocalPort));
			}
		}

		if (action.equals(ACTION_STOP_SERVER)) {
			try {
				if (null != serverSocket) {
					serverSocket.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			stopSelf();
			mIsServerRunning = false;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Read the settings from SharedPreferences.
	 */
	private void readSettings() {
		SharedPreferences preference = PreferenceManager
				.getDefaultSharedPreferences(this);

		// Get fetch server url
		mFetchServerUrl = preference.getString(
				GappProxy.SETTINGS_FETCH_SERVER_URL,
				GappProxy.mDefaultFetchServerUrl);

		// Get local port
		mLocalPort = preference.getString(
				GappProxy.SETTINGS_RUNNING_AT_LOCAL_PORT,
				GappProxy.mDefaultLocalPort);
	}

	public void startServer(final String fetchServerUrl, final int port) {
		Utils.log(LOGTAG, "startServer:port=" + port);

		new Thread(new Runnable() {

			public void run() {
				// TODO Auto-generated method stub
				try {
					serverSocket = new ServerSocket(port);
					mIsServerRunning = true;
					while (true) {
						Utils.log(LOGTAG, "waiting for connect,port="+port);
						Socket client = serverSocket.accept();
						new ProxyServerWorkThread(client,fetchServerUrl).start();
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();

	}
}
