/**
 * ProxyCache.java - Simple caching proxy
 *
 * $Id: ProxyCache.java,v 1.3 2004/02/16 15:22:00 kangasha Exp $
 *
 */

import java.net.*;
import java.io.*;
import java.util.*;

public class ProxyCache {
	/** Port for the proxy */
	private static int port;
	/** Socket for client connections */
	private static ServerSocket socket;

	private static Map<String, String> cache = new Hashtable<String, String>();
	//캐시정보를 저장하기 위한 hashtable MAP
	
	/** Create the ProxyCache object and the socket */
	public static void init(int p) {
		port = p;
		try {
			socket = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("Error creating socket: " + e);
			System.exit(-1);
		}
	}

	public static void handle(Socket client) {
		Socket server = null;
		HttpRequest request = null;
		HttpResponse response = null;
		boolean needCaching = false;
		/*
		 * Process request. If there are any exceptions, then simply return and
		 * end this request. This unfortunately means the client will hang for a
		 * while, until it timeouts.
		 */

		/* Read request */
		try {
			
			BufferedReader fromClient = new BufferedReader(
					new InputStreamReader(client.getInputStream()));
			System.out.println("-------------------start---------------------");
			request = new HttpRequest(fromClient);

		} catch (IOException e) {
			System.out.println("Error reading request from client: " + e);
			drawEndLine();
			return;
		}
		/* Send request to server */
		try {
			/* Open socket and write request to socket */
			server = new Socket(request.getHost(), request.getPort());	
			DataOutputStream toServer = new DataOutputStream(server.getOutputStream());

			if ((cache.get(request.URI)) == null) {
				needCaching = true;
				toServer.writeBytes(request.toString());
			}//캐쉬에 없으면 요청을 보낸다.  
			System.out.println("\nRequest with Header:" + request.toString());
		} catch (UnknownHostException e) {
			System.out.println("Unknown host: " + request.getHost());
			System.out.println(e);
			drawEndLine();
			return;
		} catch (IOException e) {
			System.out.println("Error writing request to server: " + e);
			drawEndLine();
			return;
		}
		/* Read response and forward it to client */
		try {
			byte[] cache = ProxyCache.uncaching(request.URI);
			
			if (cache.length==0 && needCaching) {
				DataInputStream fromServer = new DataInputStream(server.getInputStream());
				response = new HttpResponse(fromServer);
				DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
				
				/* Write response to client. First headers, then body */
				toClient.writeBytes(response.toString());
				toClient.write(response.body);
				ProxyCache.caching(request, response); //캐쉬에 저장.
				System.out.println("Response with Header: " + response.toString());
				drawEndLine();
				
				client.close();
				server.close();
			}
			
			else{
				DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
				toClient.write(cache);
				client.close();
				server.close(); 
			}
		} catch (IOException e) {
			System.out.println("Error writing response to client: " + e);
			drawEndLine();
		}
	}

	/** Read command line arguments and start proxy */
	public static void main(String args[]) {
		int myPort = 0;

		File cachedir = new File("cache/");
		if (!cachedir.exists()) {
			cachedir.mkdir();
		} // 캐쉬 경로가 없으면 생성.

		try {
			myPort = Integer.parseInt(args[0]);
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Need port number as argument");
			System.exit(-1);
		} catch (NumberFormatException e) {
			System.out.println("Please give port number as integer.");
			System.exit(-1);
		}

		init(myPort);

		/**
		 * Main loop. Listen for incoming connections and spawn a new thread for
		 * handling them
		 */
		Socket client = null;

		while (true) {
			try {
				client = socket.accept();
				handle(client);

			} catch (IOException e) {
				System.out.println("Error reading request from client: " + e);
				/*
				 * Definitely cannot continue processing this request, so skip
				 * to next iteration of while loop.
				 */
				continue;
			}
		}

	}
	
	
	public synchronized static void caching(HttpRequest request, HttpResponse response) throws IOException {
		File cacheFile;
		DataOutputStream fileStream;

		cacheFile = new File("cache/", "cache_" + System.currentTimeMillis());
		fileStream = new DataOutputStream(new FileOutputStream(cacheFile));
		fileStream.writeBytes(response.toString()); /* headers */
		fileStream.write(response.body); /* body */
		fileStream.close();
		cache.put(request.URI, cacheFile.getAbsolutePath());
	}

	public synchronized static byte[] uncaching(String uri) throws IOException {
		File filecached;
		FileInputStream fileStream;
		String hashfile;
		byte[] byteCached;
		if ((hashfile = cache.get(uri)) != null) {
			filecached = new File(hashfile);
			fileStream = new FileInputStream(filecached);
			byteCached = new byte[(int) filecached.length()];
			fileStream.read(byteCached);
			System.out.println("Hit on cache : " + uri + "\n");
			drawEndLine();
			fileStream.close();
			return byteCached;
		} else {
			System.out.println("No hit on cache : " + uri);
			return byteCached = new byte[0];
		}
	
	}
	public static void drawEndLine(){
		System.out.println("--------------------end----------------------\n");
	}
}
