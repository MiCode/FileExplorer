/*
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.swiftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.micode.fileexplorer.FTPServerService;
import net.micode.fileexplorer.R;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


public class ProxyConnector extends Thread {
	public static final int IN_BUF_SIZE = 2048;
	public static final String ENCODING = "UTF-8";
	public static final int RESPONSE_WAIT_MS = 10000;
	public static final int QUEUE_WAIT_MS = 20000;
	public static final long UPDATE_USAGE_BYTES = 5000000; 
	public static final String PREFERRED_SERVER = "preferred_server"; //preferences
	public static final int CONNECT_TIMEOUT = 5000;
	
	private FTPServerService ftpServerService;
	private MyLog myLog = new MyLog(getClass().getName());
	private JSONObject response = null;
	private Thread responseWaiter = null;
	private Queue<Thread> queuedRequestThreads = new LinkedList<Thread>();
	private Socket commandSocket = null;
	private OutputStream out = null;
	private String hostname = null;
	private InputStream inputStream = null;
	private long proxyUsage = 0;
	private State proxyState = State.DISCONNECTED;
	private String prefix;
	private String proxyMessage = null;
	
	public enum State {CONNECTING, CONNECTED, FAILED, UNREACHABLE, DISCONNECTED};
	
	//QuotaStats cachedQuotaStats = null; // quotas have been canceled for now
	
	static final String USAGE_PREFS_NAME = "proxy_usage_data";
	
	/* We establish a so-called "command session" to the proxy. New connections
	 * will be handled by creating addition control and data connections to the
	 * proxy. See proxy_protocol.txt and proxy_architecture.pdf for an
	 * explanation of how proxying works. Hint: it's complicated.
	 */ 
	
	public ProxyConnector(FTPServerService ftpServerService) {
		this.ftpServerService = ftpServerService;
		this.proxyUsage = getPersistedProxyUsage();
		setProxyState(State.DISCONNECTED);
		Globals.setProxyConnector(this);
	}
	
	public void run() {
		myLog.i("In ProxyConnector.run()");
		setProxyState(State.CONNECTING);
		try {
			String candidateProxies[] = getProxyList();
			for(String candidateHostname : candidateProxies) {
				hostname = candidateHostname;
				commandSocket = newAuthedSocket(hostname, Defaults.REMOTE_PROXY_PORT);
				if(commandSocket == null) {
					continue;
				}
				commandSocket.setSoTimeout(0); // 0 == forever
				//commandSocket.setKeepAlive(true);
				// Now that we have authenticated, we want to start the command session so we can
				// be notified of pending control sessions.
				JSONObject request = makeJsonRequest("start_command_session");
				response = sendRequest(commandSocket, request);
				if(response == null) {
					myLog.i("Couldn't create proxy command session");
					continue; // try next server
				}
				if(!response.has("prefix")) {
					myLog.l(Log.INFO, "start_command_session didn't receive a prefix in response");
					continue; // try next server
				}
				prefix = response.getString("prefix");
				response = null;  // Indicate that response is free for other use
				myLog.l(Log.INFO, "Got prefix of: " + prefix);
				break; // breaking with commandSocket != null indicates success
			}
			if(commandSocket == null) {
				myLog.l(Log.INFO, "No proxies accepted connection, failing.");
				setProxyState(State.UNREACHABLE);
				return;
			}
			setProxyState(State.CONNECTED);
			preferServer(hostname);
			inputStream = commandSocket.getInputStream();
			out = commandSocket.getOutputStream();
			int numBytes;
			byte[] bytes = new byte[IN_BUF_SIZE];
			//spawnQuotaRequester().start();
			while(true) {
				myLog.d("to proxy read()");
				numBytes = inputStream.read(bytes);
				incrementProxyUsage(numBytes);
				myLog.d("from proxy read()");
				JSONObject incomingJson = null;
				if(numBytes > 0) {
					String responseString = new String(bytes, ENCODING);
					incomingJson = new JSONObject(responseString);
					if(incomingJson.has("action")) {
						// If the incoming JSON object has an "action" field, then it is a
						// request, and not a response
						incomingCommand(incomingJson);
					} else {
						// If the incoming JSON object does not have an "action" field, then
						// it is a response to a request we sent earlier.
						// If there's an object waiting for a response, then that object
						// will be referenced by responseWaiter.
						if(responseWaiter != null) {
							if(response != null) {
								myLog.l(Log.INFO, "Overwriting existing cmd session response");
							}
							response = incomingJson;
							responseWaiter.interrupt();
						} else {
							myLog.l(Log.INFO, "Response received but no responseWaiter");
						}
					}
				} else if(numBytes  == 0) {
					myLog.d("Command socket read 0 bytes, looping");
				} else { // numBytes < 0
					myLog.l(Log.DEBUG, "Command socket end of stream, exiting");
					if(proxyState != State.DISCONNECTED) {
						// Set state to FAILED unless this was an intentional
						// socket closure.
						setProxyState(State.FAILED);
					}
					break;
				}
			}
			myLog.l(Log.INFO, "ProxyConnector thread quitting cleanly");
		} catch (IOException e) {
			myLog.l(Log.INFO, "IOException in command session: " + e);
			setProxyState(State.FAILED);
		} catch (JSONException e) {
			myLog.l(Log.INFO, "Commmand socket JSONException: " + e);
			setProxyState(State.FAILED);
		} catch (Exception e) {
			myLog.l(Log.INFO, "Other exception in ProxyConnector: " + e);
			setProxyState(State.FAILED);
		} finally {
			Globals.setProxyConnector(null);
			hostname = null;
			myLog.d("ProxyConnector.run() returning");
			persistProxyUsage();
		}
	}
	
	// This function is used to spawn a new Thread that will make a request over the
	// command thread. Since the main ProxyConnector thread handles the input
	// request/response de-multiplexing, it cannot also make a request using the
	// sendCmdSocketRequest, since sendCmdSocketRequest will block waiting for
	// a response, but the same thread is expected to deliver the response.
	// The short story is, if the main ProxyConnector command session thread wants to
	// make a request, the easiest way is to spawn a new thread and have it call
	// sendCmdSocketRequest in the same way as any other thread. 
	//private Thread spawnQuotaRequester() {
	//	return new Thread() {
	//		public void run() {
	//			getQuotaStats(false);
	//		}
	//	};
	//}
	
	/**
	 * Since we want devices to generally stick with the same proxy server,
	 * and we may want to explicitly redirect some devices to other servers,
	 * we have this mechanism to store a "preferred server" on the device. 
	 */
	private void preferServer(String hostname) {
		SharedPreferences prefs = Globals.getContext()
			.getSharedPreferences(PREFERRED_SERVER, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PREFERRED_SERVER, hostname);
		editor.commit();
	}
	
	private String[] getProxyList() {
		SharedPreferences prefs = Globals.getContext()
			.getSharedPreferences(PREFERRED_SERVER, 0);
		String preferred = prefs.getString(PREFERRED_SERVER, null);

		String[] allProxies;
		
		if(Defaults.release) {
			allProxies = new String[] {
				"c1.swiftp.org",
				"c2.swiftp.org",
				"c3.swiftp.org",
				"c4.swiftp.org",
				"c5.swiftp.org",
				"c6.swiftp.org",
				"c7.swiftp.org",
				"c8.swiftp.org",
				"c9.swiftp.org"};
		} else {
			//allProxies = new String[] {
			//	"cdev.swiftp.org"
			//};
			allProxies = new String[] {
					"c1.swiftp.org",
					"c2.swiftp.org",
					"c3.swiftp.org",
					"c4.swiftp.org",
					"c5.swiftp.org",
					"c6.swiftp.org",
					"c7.swiftp.org",
					"c8.swiftp.org",
					"c9.swiftp.org"};
		}
		
		// We should randomly permute the server list in order to spread
		// load between servers. Collections offers a shuffle() function 
		// that does this, so we'll convert to List and back to String[].
		List<String> proxyList = Arrays.asList(allProxies);
		Collections.shuffle(proxyList);
		allProxies = proxyList.toArray(new String[] {}); // arg used for type
		
		// Return preferred server first, followed by all others
		if(preferred == null) {
			return allProxies;
		} else {
			return Util.concatStrArrays(
					new String[] {preferred}, allProxies); 
		}
	}
	
	
	
	private boolean checkAndPrintJsonError(JSONObject json) throws JSONException {
		if(json.has("error_code")) {
			// The returned JSON object will have a field called "errorCode"
			// if there was a problem executing our request.
			StringBuilder s = new StringBuilder(
					"Error in JSON response, code: ");
			s.append(json.getString("error_code"));
			if(json.has("error_string")) {
				s.append(", string: ");
				s.append(json.getString("error_string"));
			}
			myLog.l(Log.INFO, s.toString());
			
			// Obsolete: there's no authentication anymore
			// Dev code to enable frequent database wipes. If we fail to login,
			// remove our stored account info, causing a create_account action
			// next time.
			//if(!Defaults.release) {
			//	if(json.getInt("error_code") == 11) {
			//		myLog.l(Log.DEBUG, "Dev: removing secret due to login failure");
			//		removeSecret();
			//	}
			//}
			return true;
		}
		return false;
	}
	
	/**
	 * Reads our persistent storage, looking for a stored proxy authentication secret.
	 * @return The secret, if present, or null.
	 */
	//Obsolete, there's no authentication anymore
	/*private String retrieveSecret() {
		SharedPreferences settings = Globals.getContext().
			getSharedPreferences(Defaults.getSettingsName(),
			Defaults.getSettingsMode());
		return settings.getString("proxySecret", null);
	}*/
	
	//Obsolete, there's no authentication anymore
	/*private void storeSecret(String secret) {
		SharedPreferences settings = Globals.getContext().
			getSharedPreferences(Defaults.getSettingsName(),
			Defaults.getSettingsMode());
		Editor editor = settings.edit();
		editor.putString("proxySecret", secret);
		editor.commit();
	}*/
	
	//Obsolete, there's no authentication anymore
	/*private void removeSecret() {
		SharedPreferences settings = Globals.getContext().
			getSharedPreferences(Defaults.getSettingsName(),
					Defaults.getSettingsMode());
		Editor editor = settings.edit();
		editor.remove("proxySecret");
		editor.commit();
	}*/
	
	private void incomingCommand(JSONObject json) {
		try {
			String action = json.getString("action");
			if(action.equals("control_connection_waiting")) {
				startControlSession(json.getInt("port"));
			} else if(action.equals("prefer_server")) {
				String host = json.getString("host"); // throws JSONException, fine
				preferServer(host);
				myLog.i("New preferred server: " + host);
			} else if(action.equals("message")) {
				proxyMessage = json.getString("text");
				myLog.i("Got news from proxy server: \"" + proxyMessage + "\"");
				FTPServerService.updateClients(); // UI update to show message
			} else if(action.equals("noop")) {
				myLog.d("Proxy noop");
			} else {
				myLog.l(Log.INFO, "Unsupported incoming action: " + action);
			}
			// If we're starting a control session register with ftpServerService
		} catch (JSONException e){
			myLog.l(Log.INFO, "JSONException in proxy incomingCommand");
		}
	}
	
	private void startControlSession(int port) {
		Socket socket;
		myLog.d("Starting new proxy FTP control session");
		socket = newAuthedSocket(hostname, port);
		if(socket == null) {
			myLog.i("startControlSession got null authed socket");
			return;
		}
		ProxyDataSocketFactory dataSocketFactory = new ProxyDataSocketFactory();
		SessionThread thread = new SessionThread(socket, dataSocketFactory,
				SessionThread.Source.PROXY);
		thread.start();
		ftpServerService.registerSessionThread(thread);
	}
	
	/**
	 * Connects an outgoing socket to the proxy and authenticates, creating an account
	 * if necessary.
	 */
	private Socket newAuthedSocket(String hostname, int port) {
		if(hostname == null) {
			myLog.i("newAuthedSocket can't connect to null host");
			return null;
		}
		JSONObject json = new JSONObject();
		//String secret = retrieveSecret();
		Socket socket;
		OutputStream out = null;
		InputStream in = null;
		
		try {
			myLog.d("Opening proxy connection to " + hostname + ":" + port);
			socket = new Socket();
			socket.connect(new InetSocketAddress(hostname, port), CONNECT_TIMEOUT);
			json.put("android_id", Util.getAndroidId());
			json.put("swiftp_version", Util.getVersion());
			json.put("action", "login");
			out = socket.getOutputStream();
			in = socket.getInputStream();
			int numBytes;
			
			out.write(json.toString().getBytes(ENCODING));
			myLog.l(Log.DEBUG, "Sent login request");
			// Read and parse the server's response
			byte[] bytes = new byte[IN_BUF_SIZE];
			// Here we assume that the server's response will all be contained in
			// a single read, which may be unsafe for large responses
			numBytes = in.read(bytes);
			if(numBytes == -1) {
				myLog.l(Log.INFO, "Proxy socket closed while waiting for auth response");
				return null;
			} else if (numBytes == 0) {
				myLog.l(Log.INFO, "Short network read waiting for auth, quitting");
				return null;
			}
			json = new JSONObject(new String(bytes, 0, numBytes, ENCODING));
			if(checkAndPrintJsonError(json)) {
				return null;
			}
			myLog.d("newAuthedSocket successful");
			return socket;
		} catch(Exception e) {
			myLog.i("Exception during proxy connection or authentication: " + e);
			return null;
		}
	}
	
	public void quit() {
		setProxyState(State.DISCONNECTED);
		try {
			sendRequest(commandSocket, makeJsonRequest("finished")); // ignore reply
			
			if(inputStream != null) {
				myLog.d("quit() closing proxy inputStream");
				inputStream.close();
			} else {
				myLog.d("quit() won't close null inputStream");
			}
			if(commandSocket != null) {
				myLog.d("quit() closing proxy socket");
				commandSocket.close();
			} else {
				myLog.d("quit() won't close null socket");
			}
		}  
		catch (IOException e) {} 
		catch(JSONException e) {}
		persistProxyUsage();
		Globals.setProxyConnector(null);
	}
	
	private JSONObject sendCmdSocketRequest(JSONObject json) {
		try {
			boolean queued;
			synchronized(this) {
				if(responseWaiter == null) {
					responseWaiter = Thread.currentThread();
					queued = false;
					myLog.d("sendCmdSocketRequest proceeding without queue");
				} else if (!responseWaiter.isAlive()) {
					// This code should never run. It is meant to recover from a situation
					// where there is a thread that sent a proxy request but died before
					// starting the subsequent request. If this is the case, the correct
					// behavior is to run the next queued thread in the queue, or if the
					// queue is empty, to perform our own request. 
					myLog.l(Log.INFO, "Won't wait on dead responseWaiter");
					if(queuedRequestThreads.size() == 0) {
						responseWaiter = Thread.currentThread();
						queued = false;
					} else {
						queuedRequestThreads.add(Thread.currentThread());
						queuedRequestThreads.remove().interrupt(); // start queued thread
						queued = true;
					}
				} else {
					myLog.d("sendCmdSocketRequest queueing thread");
					queuedRequestThreads.add(Thread.currentThread());
					queued = true;
				}
			}
			// If a different thread has sent a request and is waiting for a response,
			// then the current thread will be in a queue waiting for an interrupt
			if(queued) {
				// The current thread must wait until we are popped off the waiting queue
				// and receive an interrupt()
				boolean interrupted = false;
				try {
					myLog.d("Queued cmd session request thread sleeping...");
					Thread.sleep(QUEUE_WAIT_MS);
				} catch (InterruptedException e) {
					myLog.l(Log.DEBUG, "Proxy request popped and ready");
					interrupted = true;
				}
				if(!interrupted) {
					myLog.l(Log.INFO, "Timed out waiting on proxy queue");
					return null;
				}
			}
			// We have been popped from the wait queue if necessary, and now it's time
			// to send the request.
			try {
				responseWaiter = Thread.currentThread();
				byte[] outboundData = Util.jsonToByteArray(json);
				try {
					out.write(outboundData);
				} catch(IOException e) {
					myLog.l(Log.INFO, "IOException sending proxy request");
					return null;
				}
				// Wait RESPONSE_WAIT_MS for a response from the proxy
				boolean interrupted = false;
				try {
					// Wait for the main ProxyConnector thread to interrupt us, meaning
					// that a response has been received.
					myLog.d("Cmd session request sleeping until response");
					Thread.sleep(RESPONSE_WAIT_MS);
				} catch (InterruptedException e) {
					myLog.d("Cmd session response received");
					interrupted = true;
				}
				if(!interrupted) {
					myLog.l(Log.INFO, "Proxy request timed out");
					return null;
				}
				// At this point, the main ProxyConnector thread will have stored
				// our response in "JSONObject response".
				myLog.d("Cmd session response was: " + response);
				return response;
			} 
			finally {
				// Make sure that when this request finishes, the next thread on the
				// queue gets started.
				synchronized(this) {
					if(queuedRequestThreads.size() != 0) {
						queuedRequestThreads.remove().interrupt();
					}
				}
			}
		} catch (JSONException e) {
			myLog.l(Log.INFO, "JSONException in sendRequest: " + e);
			return null;
		}
	}

	public JSONObject sendRequest(InputStream in, OutputStream out, JSONObject request) 
	throws JSONException {
		try {
			out.write(Util.jsonToByteArray(request));
			byte[] bytes = new byte[IN_BUF_SIZE];
			int numBytes = in.read(bytes);
			if(numBytes < 1) {
				myLog.i("Proxy sendRequest short read on response");
				return null;
			}
			JSONObject response = Util.byteArrayToJson(bytes);
			if(response == null) {
				myLog.i("Null response to sendRequest");
			}
			if(checkAndPrintJsonError(response)) {
				myLog.i("Error response to sendRequest");
				return null;
			}
			return response;
		} catch (IOException e) {
			myLog.i("IOException in proxy sendRequest: " + e);
			return null;
		}
	}
	
	public JSONObject sendRequest(Socket socket, JSONObject request) 
	throws JSONException {
		 try {
			 if(socket == null) {
				 // The server is probably shutting down
				 myLog.i("null socket in ProxyConnector.sendRequest()");
				 return null;
			 } else {
				 return sendRequest(socket.getInputStream(), 
						 socket.getOutputStream(), 
						 request);
			 }
		 } catch (IOException e) {
			 myLog.i("IOException in proxy sendRequest wrapper: " + e);
			 return null;
		 }
	}
	
	public ProxyDataSocketInfo pasvListen() {
		try {
			// connect to proxy and authenticate
			myLog.d("Sending data_pasv_listen to proxy");
			Socket socket = newAuthedSocket(this.hostname, Defaults.REMOTE_PROXY_PORT);
			if(socket == null) {
				myLog.i("pasvListen got null socket");
				return null;
			}
			JSONObject request = makeJsonRequest("data_pasv_listen");
			
			JSONObject response = sendRequest(socket, request);
			if(response == null) {
				return null;
			}
			int port = response.getInt("port");
			return new ProxyDataSocketInfo(socket, port);
		} catch(JSONException e) {
			myLog.l(Log.INFO, "JSONException in pasvListen");
			return null;
		}
	}
	
	public Socket dataPortConnect(InetAddress clientAddr, int clientPort) {
		/** 
		 * This function is called by a ProxyDataSocketFactory when it's time to
		 * transfer some data in PORT mode (not PASV mode). We send a 
		 * data_port_connect request to the proxy, containing the IP and port
		 * of the FTP client to which a connection should be made.
		 */
		try {
			myLog.d("Sending data_port_connect to proxy");
			Socket socket = newAuthedSocket(this.hostname, Defaults.REMOTE_PROXY_PORT);
			if(socket == null) {
				myLog.i("dataPortConnect got null socket");
				return null;
			}
			JSONObject request =  makeJsonRequest("data_port_connect");
			request.put("address", clientAddr.getHostAddress());
			request.put("port", clientPort);
			JSONObject response = sendRequest(socket, request);
			if(response == null) {
				return null; // logged elsewhere
			}
			return socket;
		} catch (JSONException e) {
			myLog.i("JSONException in dataPortConnect");
			return null;
		}
	}
	
	/**
	 * Given a socket returned from pasvListen(), send a data_pasv_accept request
	 * over the socket to the proxy, which should result in a socket that is ready
	 * for data transfer with the FTP client. Of course, this will only work if the
	 * FTP client connects to the proxy like it's supposed to. The client will have
	 * already been told to connect by the response to its PASV command. 
	 * 
	 * This should only be called from the onTransfer method of ProxyDataSocketFactory.
	 *  
	 * @param socket A socket previously returned from ProxyConnector.pasvListen()
	 * @return true if the accept operation completed OK, otherwise false
	 */
	
	public boolean pasvAccept(Socket socket) {
		try {
			JSONObject request = makeJsonRequest("data_pasv_accept");
			JSONObject response = sendRequest(socket, request);
			if(response == null) {
				return false;  // error is logged elsewhere
			}
			if(checkAndPrintJsonError(response)) {
				myLog.i("Error response to data_pasv_accept");
				return false;
			}
			// The proxy's response will be an empty JSON object on success
			myLog.d("Proxy data_pasv_accept successful");
			return true;
		} catch (JSONException e) {
			myLog.i("JSONException in pasvAccept: " + e);
			return false;
		}
	}
	
	public InetAddress getProxyIp() {
		if(this.isAlive()) {
			if(commandSocket.isConnected()) {
				return commandSocket.getInetAddress();
			}
		}
		return null;
	}
	
	private JSONObject makeJsonRequest(String action) throws JSONException {
		JSONObject json = new JSONObject();
		json.put("action", action);
		return json;
	}
	
	/* Quotas have been canceled for now	  
	  public QuotaStats getQuotaStats(boolean canUseCached) {
		if(canUseCached) {
			if(cachedQuotaStats != null) {
				myLog.d("Returning cachedQuotaStats");
				return cachedQuotaStats;
			} else {
				myLog.d("Would return cached quota stats but none retrieved");
			}
		}
		// If there's no cached quota stats, or if the called wants fresh stats,
		// make a JSON request to the proxy, assuming the command session is open.
		try {
			JSONObject response = sendCmdSocketRequest(makeJsonRequest("check_quota"));
			int used, quota;
			if(response == null) {
				myLog.w("check_quota got null response");
				return null;
			}
			used = response.getInt("used");
			quota = response.getInt("quota");
			myLog.d("Got quota response of " + used + "/" + quota);
			cachedQuotaStats = new QuotaStats(used, quota) ;
			return cachedQuotaStats;
		} catch (JSONException e) {
			myLog.w("JSONException in getQuota: " + e);
			return null;
		}
	}*/
	
	// We want to track the total amount of data sent via the proxy server, to
	// show it to the user and encourage them to donate.
	void persistProxyUsage() {
		if(proxyUsage == 0) {
			return;  // This shouldn't happen, but just for safety
		}
		SharedPreferences prefs = Globals.getContext().
			getSharedPreferences(USAGE_PREFS_NAME, 0); // 0 == private
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(USAGE_PREFS_NAME, proxyUsage);
		editor.commit();
		myLog.d("Persisted proxy usage to preferences");
	}
	
	long getPersistedProxyUsage() {
		// This gets the last persisted value for bytes transferred through
		// the proxy. It can be out of date since it doesn't include data
		// transferred during the current session.
		SharedPreferences prefs = Globals.getContext().
			getSharedPreferences(USAGE_PREFS_NAME, 0); // 0 == private
		return prefs.getLong(USAGE_PREFS_NAME, 0); // Default count of 0
	}
	
	public long getProxyUsage() {
		// This gets the running total of all proxy usage, which may not have
		// been persisted yet.
		return proxyUsage;
	}
	
	void incrementProxyUsage(long num) {
		long oldProxyUsage = proxyUsage;
		proxyUsage += num;
		if(proxyUsage % UPDATE_USAGE_BYTES < oldProxyUsage % UPDATE_USAGE_BYTES) {
			FTPServerService.updateClients();
			persistProxyUsage();
		}
	}
	
	public State getProxyState() {
		return proxyState;
	}
	
	private void setProxyState(State state) {
		proxyState = state;
		myLog.l(Log.DEBUG, "Proxy state changed to " + state, true);
		FTPServerService.updateClients(); // UI update
	}
	
	static public String stateToString(State s) {
//		Context ctx = Globals.getContext();
//		switch(s) {
//		case DISCONNECTED:
//			return ctx.getString(R.string.pst_disconnected);
//		case CONNECTING:
//			return ctx.getString(R.string.pst_connecting);
//		case CONNECTED:
//			return ctx.getString(R.string.pst_connected);
//		case FAILED:
//			return ctx.getString(R.string.pst_failed);
//		case UNREACHABLE:
//			return ctx.getString(R.string.pst_unreachable);
//		default:
//			return ctx.getString(R.string.unknown); 
//		}
	    return "";
	}
	
	/**
	 * The URL to which users should point their FTP client.
	 */
	public String getURL() {
		if(proxyState == State.CONNECTED) {
			String username = Globals.getUsername();
			if(username != null) {
				return "ftp://" + prefix + "_" + username + "@" + hostname;
			}
		}
		return "";
	}
	
	/** If the proxy sends a human-readable message, it can be retrieved by
	 * calling this function. Returns null if no message has been received.
	 */
	public String getProxyMessage() {
		return proxyMessage;
	}

}

