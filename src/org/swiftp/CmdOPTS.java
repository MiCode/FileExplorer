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


public class CmdOPTS extends FtpCmd implements Runnable {
	public static final String message = "TEMPLATE!!";
	private String input;
	
	public CmdOPTS(SessionThread sessionThread, String input) {
		super(sessionThread, CmdOPTS.class.toString());
		this.input = input;
	}
	
	public void run() {
		String param = getParameter(input);
		String errString = null;
		
		mainBlock: {
			if(param == null) {
				errString = "550 Need argument to OPTS\r\n";
				myLog.w("Couldn't understand empty OPTS command");
				break mainBlock;
			}
			String[] splits = param.split(" ");
			if(splits.length != 2) {
				errString = "550 Malformed OPTS command\r\n";
				myLog.w("Couldn't parse OPTS command");
				break mainBlock;
			}
			String optName = splits[0].toUpperCase();
			String optVal = splits[1].toUpperCase();
			if(optName.equals("UTF8")) {
				// OK, whatever. Don't really know what to do here. We
				// always operate in UTF8 mode.
				if(optVal.equals("ON")) {
					myLog.d("Got OPTS UTF8 ON");
					sessionThread.setEncoding("UTF-8");
				} else {
					myLog.i("Ignoring OPTS UTF8 for something besides ON");
				}
				break mainBlock;
			} else {
				myLog.d("Unrecognized OPTS option: " + optName);
				errString = "502 Unrecognized option\r\n";
				break mainBlock;
			}
		}
		if(errString != null) {
			sessionThread.writeString(errString);
			myLog.i("Template log message");
		} else {
			sessionThread.writeString("200 OPTS accepted\r\n");
			myLog.d("Handled OPTS ok");
		}
	}

}
