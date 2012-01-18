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

import java.net.InetAddress;
import java.net.Socket;

import android.util.Log;

/**
 * @author david
 *
 */
public class ProxyDataSocketFactory extends DataSocketFactory {
	/**
	 * Implements data socket connections that go through our proxy server
	 * out on the net. The proxy sits between the FTP client and us, the server.
	 * We have to build in some coordination between the server and proxy in order
	 * for data sockets to be handled properly. 
	 * 
	 * When we receive a "PASV" command from a client, we have to request that the
	 * proxy server open a port, accept a connection, and proxy all data on that
	 * socket between ourself and the FTP client.
	 * 
	 * When we receive a PORT command, we store the client's connection info,
	 * and when it's time to being transferring data, we request that the proxy
	 * make a connection to the client's IP & port and then proxy all data between
	 * ourself and the FTP client.
	 */
	
	private Socket socket;
	private int proxyListenPort;
	ProxyConnector proxyConnector;
	InetAddress clientAddress;
	int clientPort;
	
	public ProxyDataSocketFactory() {
		clearState();
	}
	
	private void clearState() {
		if(socket != null) {
			try {
				socket.close();
			} catch (Exception e) {}
		}
		socket = null;
		proxyConnector = null;
		clientAddress = null;
		proxyListenPort = 0;
		clientPort = 0;
	}

	public InetAddress getPasvIp() {
		ProxyConnector pc = Globals.getProxyConnector();
		if(pc == null) {
			return null;
		}
		return pc.getProxyIp();
	}

//	public int getPortNumber() {
//		if(socket == )
//		return 0;
//	}

	public int onPasv() {
		clearState();
		proxyConnector = Globals.getProxyConnector();
		if(proxyConnector == null) {
			myLog.l(Log.INFO, "Unexpected null proxyConnector in onPasv");
			clearState();
			return 0;
		}
		ProxyDataSocketInfo info = proxyConnector.pasvListen();
		if(info == null) {
			myLog.l(Log.INFO, "Null ProxyDataSocketInfo");
			clearState();
			return 0;
		}
		socket = info.getSocket();
		proxyListenPort = info.getRemotePublicPort();
		return proxyListenPort;
	}

	public boolean onPort(InetAddress dest, int port) {
		clearState();
		proxyConnector = Globals.getProxyConnector();
		this.clientAddress = dest;
		this.clientPort = port;
		myLog.d("ProxyDataSocketFactory client port settings stored");
		return true;
	}

	/**
	 * When the it's time for the SessionThread to actually begin PASV
	 * data transfer with the client, it will call this function to get
	 * a valid socket. The socket will have been created earlier with
	 * a call to onPasv(). The result of calling onTransfer() will be
	 * to cause the proxy to accept the incoming connection from the FTP
	 * client and start proxying back to us (the FTP server). The socket
	 * can then be handed back to the SessionThread which can use it as
	 * if it were directly connected to the client.
	 */
	public Socket onTransfer() {
		if(proxyConnector == null) {
			myLog.w("Unexpected null proxyConnector in onTransfer");
			return null;
		}
			
		if(socket == null) {
			// We are in PORT mode (not PASV mode)
			if(proxyConnector == null) {
				myLog.l(Log.INFO, "Unexpected null proxyConnector in onTransfer");
				return null;
			}
			// May return null, that's fine. ProxyConnector will log errors.
			socket = proxyConnector.dataPortConnect(clientAddress, clientPort);
			return socket;
		} else {
			// We are in PASV mode (not PORT mode)
			if(proxyConnector.pasvAccept(socket)) {
				return socket;
			} else {
				myLog.w("proxyConnector pasvAccept failed");
				return null;
			}
		}
	}
	
	public void reportTraffic(long bytes) {
		ProxyConnector pc = Globals.getProxyConnector();
		if(pc == null) {
			myLog.d("Can't report traffic, null ProxyConnector");
		} else {
			pc.incrementProxyUsage(bytes);
		}
	}
}
