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

import net.micode.fileexplorer.FTPServerService;

import android.util.Log;

public class MyLog {
	protected String tag;
	
	public MyLog(String tag) {
		this.tag = tag;
	}
	
	public void l(int level, String str, boolean sysOnly) {
		synchronized (MyLog.class) {
			str = str.trim();
			// Messages of this severity are handled specially
			if(level == Log.ERROR || level == Log.WARN) {
				Globals.setLastError(str);
			}
			if(level >= Defaults.getConsoleLogLevel()) {
				Log.println(level,tag, str);
			}
			if(!sysOnly) { // some messages only go to the Android log
				if(level >= Defaults.getUiLogLevel()) {
					FTPServerService.log(level, str);
				}
			}
		}
	}
	
	public void l(int level, String str) {
		l(level, str, false);
	}
	
	public void e(String s) {
		l(Log.ERROR, s, false);
	}
	public void w(String s) {
		l(Log.WARN, s, false);
	}
	public void i(String s) {
		l(Log.INFO, s, false);
	}
	public void d(String s) {
		l(Log.DEBUG, s, false);
	}
}
