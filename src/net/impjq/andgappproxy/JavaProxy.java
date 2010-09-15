package net.impjq.andgappproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class JavaProxy {
	public static final String LOGTAG=JavaProxy.class.getSimpleName();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		startServer(Conf.PORT);

	}
	
    public static void startServer(int port) {
        Utils.log(LOGTAG, "startServer:port=" + port);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
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
}
