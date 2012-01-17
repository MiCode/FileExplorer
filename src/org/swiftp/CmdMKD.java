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

import java.io.File;

import android.util.Log;

public class CmdMKD extends FtpCmd implements Runnable {
	String input;
	
	public CmdMKD(SessionThread sessionThread, String input) {
		super(sessionThread, CmdMKD.class.toString());
		this.input = input;
	}
	
	public void run() {
		myLog.l(Log.DEBUG, "MKD executing");
		String param = getParameter(input);
		File toCreate;
		String errString = null;
		mainblock: {
			// If the param is an absolute path, use it as is. If it's a
			// relative path, prepend the current working directory.
			if(param.length() < 1) {
				errString = "550 Invalid name\r\n";
				break mainblock;
			}
			toCreate = inputPathToChrootedFile(sessionThread.getWorkingDir(), param);
			if(violatesChroot(toCreate)) {
				errString = "550 Invalid name or chroot violation\r\n";
				break mainblock;
			}
			if(toCreate.exists()) {
				errString = "550 Already exists\r\n";
				break mainblock;
			}
			if(!toCreate.mkdir()) {
				errString = "550 Error making directory (permissions?)\r\n";
				break mainblock;
			}
		}
		if(errString != null) {
			sessionThread.writeString(errString);
			myLog.l(Log.INFO, "MKD error: " + errString.trim());
		} else {
			sessionThread.writeString("250 Directory created\r\n");
		}
		myLog.l(Log.INFO, "MKD complete");
	}

}
