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
import java.io.IOException;

import android.util.Log;

public class CmdCDUP extends FtpCmd implements Runnable {
	protected String input;
	
	public CmdCDUP(SessionThread sessionThread, String input) {
		super(sessionThread, CmdCDUP.class.toString());
	}
	
	public void run() {
		myLog.l(Log.DEBUG, "CDUP executing");
		File newDir;
		String errString = null;
		mainBlock: {
			File workingDir = sessionThread.getWorkingDir();
			newDir = workingDir.getParentFile();
			if(newDir == null) {
				errString = "550 Current dir cannot find parent\r\n";
				break mainBlock;
			}
			// Ensure the new path does not violate the chroot restriction
			if(violatesChroot(newDir)) {
				errString = "550 Invalid name or chroot violation\r\n";
				break mainBlock;
			}

			try {
				newDir = newDir.getCanonicalFile();
				if(!newDir.isDirectory()) {
					errString = "550 Can't CWD to invalid directory\r\n";
					break mainBlock;
				} else if(newDir.canRead()) {
					sessionThread.setWorkingDir(newDir);
				} else {
					errString = "550 That path is inaccessible\r\n";
					break mainBlock;
				}
			} catch(IOException e) {
				errString = "550 Invalid path\r\n";
				break mainBlock;
			}
		}
		if(errString != null) {
			sessionThread.writeString(errString);
			myLog.i("CDUP error: " + errString);
		} else {
			sessionThread.writeString("200 CDUP successful\r\n");
			myLog.l(Log.DEBUG, "CDUP success");
		}
	}
}
