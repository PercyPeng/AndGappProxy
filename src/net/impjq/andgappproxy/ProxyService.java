package net.impjq.andgappproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ProxyService extends Service {
	public static final String LOGTAG = ProxyService.class.getSimpleName();
	public static final String ACTION_START_SERVER = "start_server";
	public static final String ACTION_STOP_SERVER = "stop_server";

	boolean mIsServerRunning;
	ServerSocket serverSocket;

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

		if (null == intent) {
			return;
		}

		String action = "";
		action = intent.getAction();
		Utils.log(LOGTAG, "onStart,action==" + action);
		if (action.equals(ACTION_START_SERVER)) {
			if (!mIsServerRunning) {
				startServer(Conf.PORT);
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
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public void startServer(final int port) {
		Utils.log(LOGTAG, "startServer:port=" + port);

		new Thread(new Runnable() {

			public void run() {
				// TODO Auto-generated method stub
				try {
					serverSocket = new ServerSocket(port);
					mIsServerRunning = true;
					while (true) {
						Utils.log(LOGTAG, "waiting for connect...");
						Socket client = serverSocket.accept();
						new ProxyServerWorkThread(client).start();
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}).start();

	}
}
