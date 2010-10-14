package net.impjq.andgappproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public class ProxyServerWorkThread extends Thread {
	public static final String LOGTAG = ProxyServerWorkThread.class
			.getSimpleName();
	Socket mClienSocket;

	SSLSocket mSSLSocket;

	String mGappProxyURL;

	public static final String REQUEST_URL = "Request_URL";
	public static final String REQUEST_METHORD = "Request_METHORD";

	public ProxyServerWorkThread(Socket clientSocket, String fetchServerUrl) {
		// TODO Auto-generated constructor stub
		mClienSocket = clientSocket;
		mGappProxyURL = fetchServerUrl;
	}

	@Override
	public void run() {
		if (null == mClienSocket) {
			Utils.log(LOGTAG, "The socket is invalid");
			return;
		}

		Utils.log(LOGTAG, "A Client Connected:");
		String host = mClienSocket.getInetAddress().getHostAddress();
		String hostName = mClienSocket.getInetAddress().getHostName();
		Utils.log(LOGTAG, "Client Host:" + host);
		Utils.log(LOGTAG, "Client HostName:" + hostName);

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					mClienSocket.getInputStream()));
			OutputStream os = mClienSocket.getOutputStream();
			PrintWriter pw = new PrintWriter(os, true);

			String line = null;
			StringBuffer stringBuffer = new StringBuffer();
			HashMap<String, String> hashMap = new HashMap<String, String>();

			int startGetPostData = 0;
			while (true) {
				line = br.readLine();

				Utils.log(LOGTAG, "Received:" + line);
				stringBuffer.append(line + '\n');

				if (line == null) {
					break;
				}
				Utils.log(LOGTAG, "Received:length=" + line.length());

				if (!hashMap.containsKey("METHORD")) {
					if (line.startsWith("GET")) {
						hashMap.put(REQUEST_METHORD, "GET");
						String getValue = line.split(" ")[1];
						hashMap.put(REQUEST_URL, getValue);
						hashMap = handleGET(br, hashMap, line);
						break;
					}

					if (line.startsWith("POST")) {
						Utils.log(LOGTAG, "It is post methord");
						hashMap.put(REQUEST_METHORD, "POST");
						String getValue = line.split(" ")[1];
						hashMap.put(REQUEST_URL, getValue);
						hashMap = handlePOST(br, hashMap, line);
						break;
					}

					if (line.startsWith("CONNECT")) {
						Utils.log(LOGTAG, "It is CONNECT methord");
						hashMap.put(REQUEST_METHORD, "CONNECT");
						String getValue = line.split(" ")[1];
						hashMap.put(REQUEST_URL, getValue);
						break;
					}
				}
				if (line.length() == 0) {
					if (null != hashMap.get(REQUEST_METHORD)) {
						if (hashMap.get(REQUEST_METHORD).equals("POST")) {
							Utils.log(LOGTAG, "get post data br.readLine");
							line = br.readLine();
							Utils.log(LOGTAG, "get post data line=" + line);

							int length = 0;
							if (hashMap.containsKey("Content-Length")) {
								length = Integer.valueOf(hashMap.get(
										"Content-Length").replace(" ", ""));
								Utils.log(LOGTAG, "Content-Length=" + length);
							}

							if (null == line) {
								line = br.readLine();
							}
							Utils.log(LOGTAG, "get post data line=" + line);

							Utils.log(LOGTAG, "get post data line=" + line);

							String postData = line;
							while (null != line) {
								postData += line + '\n';
								if (length == postData.length()) {
									line = null;
									break;
								}
								line = br.readLine();
							}

							hashMap.put("POST_DATA", postData);
							Utils.log(LOGTAG, "post data=" + postData);

							break;
						} else {
							break;
						}
					} else {
						break;
					}
				}

				if (startGetPostData == 0 && null != line) {
					int index = line.indexOf(":");
					if (index < 0) {
						continue;
					}

					// Utils.log(LOGTAG, "index="+index+" of "+line.length());
					String key = line.substring(0, index);
					String value = line.substring(index + 1, line.length());
					hashMap.put(key, value);
				}
			}

			List<NameValuePair> nvp = createParams(hashMap);

			HttpResponse httpResponse = doHttpPost(mGappProxyURL, nvp);

			Utils.log(LOGTAG,
					"**************print response********************");
			Utils.log(LOGTAG, "statusLine=" + httpResponse.getStatusLine());

			httpResponse.getEntity().writeTo(os);

			os.close();
			pw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	List<NameValuePair> createParams(HashMap<String, String> hashmap) {
		Utils.log(LOGTAG,
				"************************printHeader*********************");
		String headers = "";
		List<NameValuePair> nvp = new ArrayList<NameValuePair>();
		nvp.add(new BasicNameValuePair("method", hashmap.get(REQUEST_METHORD)));
		nvp.add(new BasicNameValuePair("encoded_path", encode(hashmap
				.get(REQUEST_URL))));

		hashmap.remove(REQUEST_METHORD);
		hashmap.remove(REQUEST_URL);
		Iterator<String> iterator = hashmap.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			String value = (String) hashmap.get(key);
			Utils.log(LOGTAG, key + "=" + value);
			headers += key + ":" + value;
		}

		Utils.log(LOGTAG, "headers=" + headers);

		nvp.add(new BasicNameValuePair("headers", headers));
		nvp.add(new BasicNameValuePair("postdata", hashmap.get("POST_DATA")));
		nvp.add(new BasicNameValuePair("version", Conf.VERSION));

		return nvp;
	}

	HttpResponse doHttpPost(String url, List<NameValuePair> nvp) {
		Utils.log(LOGTAG, "doHttpPost");
		DefaultHttpClient httpClient = new DefaultHttpClient();

		HttpPost httpPost = new HttpPost(url);
		HttpResponse response = null;
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(nvp, HTTP.UTF_8));
			httpPost.addHeader("Accept-Encoding", "identity, *;q=0");
			httpPost.addHeader("Connection", "close");

			response = httpClient.execute(httpPost);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// httpClient.getConnectionManager().shutdown();
		return response;
	}

	void connectToProxyServer(String url, List<NameValuePair> nvp) {
		try {
			URLConnection uc = new URL(url).openConnection();
			InputStream is = uc.getInputStream();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	void writeResultToClient(PrintWriter pw, String result) {
		// pw.println("HTTP/1.1 200 OK\r\n");
		// pw.println("\r\n");
		try {
			result.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pw.write(result);
	}

	void writeResult(PrintWriter pw) {
		pw.println("HTTP/1.1 200 OK\r\n");
		pw.println("\r\n");
		// pw.write("Request received.");
		pw
				.println("<html>  <head>  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /> <title>GAppProxy已经在工作了</title>  </head><body>Say Hello</body></html> ");
		pw.flush();
	}

	String getMethord(StringBuffer stringBuffer) {
		if (null == stringBuffer) {
			return null;
		}

		return null;
	}

	public static String encode(String str) {
		Utils.log(LOGTAG, "str=" + str);

		if (null == str) {
			return null;
		}

		Base64 base64 = new Base64();
		String encodedStr = "";
		try {
			byte[] bytes = base64.encode(str.getBytes("UTF-8"));
			encodedStr = new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Utils.log(LOGTAG, "encoded str=" + encodedStr);

		return encodedStr;
	}

	HashMap<String, String> handleGET(BufferedReader br,
			HashMap<String, String> hashMap, String firstline) {
		Utils.log(LOGTAG, "handlePOST");
		String line = firstline;
		int length = 0;

		while (true) {
			try {
				line = br.readLine();
				length = line.length();
				if (0 == length) {
					break;
				}

				if (null != line) {
					int index = line.indexOf(":");
					if (index < 0) {
						continue;
					}

					// Utils.log(LOGTAG, "index="+index+" of "+line.length());
					String key = line.substring(0, index);
					String value = line.substring(index + 1, line.length());
					hashMap.put(key, value);
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return hashMap;
	}

	HashMap<String, String> handlePOST(BufferedReader br,
			HashMap<String, String> hashMap, String firstline) {
		Utils.log(LOGTAG, "handlePOST");
		String line = firstline;
		int length = 0;
		while (true) {
			try {
				line = br.readLine();
				length = line.length();

				Utils.log(LOGTAG, "line=" + line + ",length=" + length);

				if (null == line) {
					Utils.log(LOGTAG, "line==null");
					break;
				}

				if (0 == length) {
					int contentLength = Integer.valueOf(hashMap.get(
							"Content-Length").replace(" ", ""));
					int readLength = 0;
					String postData = "";
					Utils.log(LOGTAG, "Get Post data,contentLength="
							+ contentLength);
					while (readLength < contentLength) {
						line = br.readLine() + '\n';
						readLength += line.length();
						postData += line;
						Utils.log(LOGTAG, "line=" + line + ",readLength="
								+ readLength);
					}

					hashMap.put("POST_DATA", postData);
					Utils.log(LOGTAG, "post data=" + postData);
					break;
				}

				int index = line.indexOf(":");
				if (index < 0) {
					continue;
				}

				// Utils.log(LOGTAG, "index="+index+" of "+line.length());
				String key = line.substring(0, index);
				String value = line.substring(index + 1, line.length());
				hashMap.put(key, value);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return hashMap;

	}

	void handleCONNECT() {
		Utils.log(LOGTAG, "handlePOST");

		SSLContext sslContext = null;
		try {
			sslContext = SSLContext.getInstance("SSLv3");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// sslContext.init(kmf.getKeyManagers(),null,null);

		SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

		SocketFactory SS = SSLSocketFactory.getDefault();

		try {
			SSLServerSocket s = (SSLServerSocket) factory
					.createServerSocket(9999);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
