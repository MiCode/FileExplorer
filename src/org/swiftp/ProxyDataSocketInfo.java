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

import java.net.Socket;

public class ProxyDataSocketInfo extends Socket {
	private int remotePublicPort;
	private Socket socket;
	
	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public ProxyDataSocketInfo(Socket socket, int remotePublicPort) {
		this.remotePublicPort = remotePublicPort;
		this.socket = socket;
	}
	
	public int getRemotePublicPort() {
		return remotePublicPort;
	}

	public void setRemotePublicPort(int remotePublicPort) {
		this.remotePublicPort = remotePublicPort;
	}
	
}
