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
import java.lang.reflect.Constructor;

import android.util.Log;

public abstract class FtpCmd implements Runnable {
	protected SessionThread sessionThread;
	protected MyLog myLog;
	protected static MyLog staticLog = new MyLog(FtpCmd.class.toString());
	
	private static FtpCmd getCmd(String cmd, SessionThread sessionThread, String input) {
		if ("SYST".equals(cmd))
			return new CmdSYST(sessionThread, input);
		if ("USER".equals(cmd))
			return new CmdUSER(sessionThread, input);
		if ("PASS".equals(cmd))
			return new CmdPASS(sessionThread, input);
		if ("TYPE".equals(cmd))
			return new CmdTYPE(sessionThread, input);
		if ("CWD".equals(cmd))
			return new CmdCWD(sessionThread, input);
		if ("PWD".equals(cmd))
			return new CmdPWD(sessionThread, input);
		if ("LIST".equals(cmd))
			return new CmdLIST(sessionThread, input);
		if ("PASV".equals(cmd))
			return new CmdPASV(sessionThread, input);
		if ("RETR".equals(cmd))
			return new CmdRETR(sessionThread, input);
		if ("NLST".equals(cmd))
			return new CmdNLST(sessionThread, input);
		if ("NOOP".equals(cmd))
			return new CmdNOOP(sessionThread, input);
		if ("STOR".equals(cmd))
			return new CmdSTOR(sessionThread, input);
		if ("DELE".equals(cmd))
			return new CmdDELE(sessionThread, input);
		if ("RNFR".equals(cmd))
			return new CmdRNFR(sessionThread, input);
		if ("RNTO".equals(cmd))
			return new CmdRNTO(sessionThread, input);
		if ("RMD".equals(cmd))
			return new CmdRMD(sessionThread, input);
		if ("MKD".equals(cmd))
			return new CmdMKD(sessionThread, input);
		if ("OPTS".equals(cmd))
			return new CmdOPTS(sessionThread, input);
		if ("PORT".equals(cmd))
			return new CmdPORT(sessionThread, input);
		if ("QUIT".equals(cmd))
			return new CmdQUIT(sessionThread, input);
		if ("FEAT".equals(cmd))
			return new CmdFEAT(sessionThread, input);
		if ("SIZE".equals(cmd))
			return new CmdSIZE(sessionThread, input);
		if ("CDUP".equals(cmd))
			return new CmdCDUP(sessionThread, input);
		if ("APPE".equals(cmd))
			return new CmdAPPE(sessionThread, input);
		if ("XCUP".equals(cmd))
			return new CmdCDUP(sessionThread, input); // synonym
		if ("XPWD".equals(cmd))
			return new CmdPWD(sessionThread, input);  // synonym
		if ("XMKD".equals(cmd))
			return new CmdMKD(sessionThread, input);  // synonym
		if ("XRMD".equals(cmd))
			return new CmdRMD(sessionThread, input);   // synonym
		return null;
	};
	
	public FtpCmd(SessionThread sessionThread, String logName) {
		this.sessionThread = sessionThread;
		myLog = new MyLog(logName);
	}
	
	abstract public void run();
	
	protected static void dispatchCommand(SessionThread session, 
	                                      String inputString) {
		String[] strings = inputString.split(" ");
		String unrecognizedCmdMsg = "502 Command not recognized\r\n";
		if(strings == null) {
			// There was some egregious sort of parsing error
			String errString = "502 Command parse error\r\n";
			staticLog.l(Log.INFO, errString);
			session.writeString(errString);
			return;
		}
		if(strings.length < 1) {
			staticLog.l(Log.INFO, "No strings parsed");
			session.writeString(unrecognizedCmdMsg);
			return;
		}
		String verb = strings[0];
		if(verb.length() < 1) {
			staticLog.l(Log.INFO, "Invalid command verb");
			session.writeString(unrecognizedCmdMsg);
			return;
		}

		verb = verb.trim();
		verb = verb.toUpperCase();
		FtpCmd cmdInstance = getCmd(verb, session, inputString);

		if(cmdInstance == null) {
			// If we couldn't find a matching command,
			staticLog.l(Log.DEBUG, "Ignoring unrecognized FTP verb: " + verb);
			session.writeString(unrecognizedCmdMsg);
			return;
		} else if(session.isAuthenticated() 
				|| cmdInstance.getClass().equals(CmdUSER.class)
				|| cmdInstance.getClass().equals(CmdPASS.class)
				|| cmdInstance.getClass().equals(CmdUSER.class))
		{
			// Unauthenticated users can run only USER, PASS and QUIT 
			cmdInstance.run();
		} else {
			session.writeString("530 Login first with USER and PASS\r\n");
		}
	}
		
	/**
	 * An FTP parameter is that part of the input string that occurs
	 * after the first space, including any subsequent spaces. Also,
	 * we want to chop off the trailing '\r\n', if present.
	 * 
	 * Some parameters shouldn't be logged or output (e.g. passwords),
	 * so the caller can use silent==true in that case.
	 */
	static public String getParameter(String input, boolean silent) {
		if(input == null) {
			return "";
		}
		int firstSpacePosition = input.indexOf(' ');
		if(firstSpacePosition == -1) {
			return "";
		}
		String retString = input.substring(firstSpacePosition+1);
		
		// Remove trailing whitespace
		// todo: trailing whitespace may be significant, just remove \r\n
		retString = retString.replaceAll("\\s+$", "");
		
		if(!silent) {
			staticLog.l(Log.DEBUG, "Parsed argument: " + retString);
		}
		return retString; 
	}
	
	/**
	 * A wrapper around getParameter, for when we don't want it to be silent.
	 */
	static public String getParameter(String input) {
		return getParameter(input, false);
	}

	public static File inputPathToChrootedFile(File existingPrefix, String param) {
		try {
			if(param.charAt(0) == '/') {
				// The STOR contained an absolute path
				File chroot = Globals.getChrootDir();
				return new File(chroot, param);
			}
		} catch (Exception e) {} 
		
		// The STOR contained a relative path
		return new File(existingPrefix, param); 
	}
	
	public boolean violatesChroot(File file) {
		File chroot = Globals.getChrootDir();
		try {
			String canonicalPath = file.getCanonicalPath();
			if(!canonicalPath.startsWith(chroot.toString())) {
				myLog.l(Log.INFO, "Path violated folder restriction, denying");
				myLog.l(Log.DEBUG, "path: " + canonicalPath);
				myLog.l(Log.DEBUG, "chroot: " + chroot.toString());
				return true; // the path must begin with the chroot path
			}
			return false;
		} catch(Exception e) {
			myLog.l(Log.INFO, "Path canonicalization problem: " + e.toString());
			myLog.l(Log.INFO, "When checking file: " + file.getAbsolutePath());
			return true;  // for security, assume violation
		}
	}
}
