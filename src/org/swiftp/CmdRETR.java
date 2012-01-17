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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;

public class CmdRETR extends FtpCmd implements Runnable {
	//public static final String message = "TEMPLATE!!";
	protected String input;
	
	public CmdRETR(SessionThread sessionThread, String input) {
		super(sessionThread, CmdRETR.class.toString());
		this.input = input;
	}
	
	public void run() {
		myLog.l(Log.DEBUG, "RETR executing");
		String param = getParameter(input);
		File fileToRetr;
		String errString = null;
		
		mainblock: {
			fileToRetr = inputPathToChrootedFile(sessionThread.getWorkingDir(), param);
			if(violatesChroot(fileToRetr)) {
				errString = "550 Invalid name or chroot violation\r\n";
				break mainblock;
			} else if(fileToRetr.isDirectory()) {
				myLog.l(Log.DEBUG, "Ignoring RETR for directory");
				errString = "550 Can't RETR a directory\r\n";
				break mainblock;
			} else if(!fileToRetr.exists()) {
				myLog.l(Log.INFO, "Can't RETR nonexistent file: " + 
						fileToRetr.getAbsolutePath());
				errString = "550 File does not exist\r\n";
				break mainblock;
			} else if(!fileToRetr.canRead()) {
				myLog.l(Log.INFO, "Failed RETR permission (canRead() is false)");
				errString = "550 No read permissions\r\n";
				break mainblock;
			} /*else if(!sessionThread.isBinaryMode()) {
				myLog.l(Log.INFO, "Failed RETR in text mode");
				errString = "550 Text mode RETR not supported\r\n";
				break mainblock;
			}*/
			try {
				FileInputStream in = new FileInputStream(fileToRetr);
				byte[] buffer = new byte[Defaults.getDataChunkSize()];
				int bytesRead;
				if(sessionThread.startUsingDataSocket()) {
					myLog.l(Log.DEBUG, "RETR opened data socket");
				} else {
					errString = "425 Error opening socket\r\n";
					myLog.l(Log.INFO, "Error in initDataSocket()");
					break mainblock;
				}
				sessionThread.writeString("150 Sending file\r\n");
				if(sessionThread.isBinaryMode()) {
					myLog.l(Log.DEBUG, "Transferring in binary mode");
					while((bytesRead = in.read(buffer)) != -1) {
						//myLog.l(Log.DEBUG,
						//		String.format("CmdRETR sending %d bytes", bytesRead));
						if(sessionThread
						   .sendViaDataSocket(buffer, bytesRead) == false) 
						{
							errString = "426 Data socket error\r\n";
							myLog.l(Log.INFO, "Data socket error");
							break mainblock;
						}
					}
				} else { // We're in ASCII mode
					myLog.l(Log.DEBUG, "Transferring in ASCII mode");
					// We have to convert all solitary \n to \r\n
					boolean lastBufEndedWithCR = false;
					while((bytesRead = in.read(buffer)) != -1) {
						int startPos = 0, endPos = 0;
						byte[] crnBuf = {'\r','\n'};
						for(endPos = 0; endPos<bytesRead; endPos++) {
							if(buffer[endPos] == '\n') {
								// Send bytes up to but not including the newline
								sessionThread.sendViaDataSocket(buffer, 
										startPos, endPos-startPos);
								if(endPos == 0) {
									// handle special case where newline occurs at
									// the beginning of a buffer
									if(!lastBufEndedWithCR) {
										// Send an \r only if the the previous
										// buffer didn't end with an \r
										sessionThread.sendViaDataSocket(crnBuf, 1);
									}
								} else if(buffer[endPos-1] != '\r') {
									// The file did not have \r before \n, add it
									sessionThread.sendViaDataSocket(crnBuf, 1);
								} else {
									// The file did have \r before \n, don't change
								}
								startPos = endPos;
							}
						}
						// Now endPos has finished traversing the array, send remaining
						// data as-is
						sessionThread.sendViaDataSocket(buffer, startPos, 
								endPos-startPos);
						if(buffer[bytesRead-1] == '\r') {
							lastBufEndedWithCR = true;
						} else {
							lastBufEndedWithCR = false;
						}
					}
				}
			} catch (FileNotFoundException e) {
				errString = "550 File not found\r\n";
				break mainblock;
			} catch(IOException e) {
				errString = "425 Network error\r\n";
				break mainblock;
			}
		}
		sessionThread.closeDataSocket();
		if(errString != null) {
			sessionThread.writeString(errString);
		} else {
			sessionThread.writeString("226 Transmission finished\r\n");
		}
		myLog.l(Log.DEBUG, "RETR done");
	}
}
