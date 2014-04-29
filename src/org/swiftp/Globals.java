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

import android.content.Context;

public class Globals {
    private static Context context;
    private static String lastError;
    private static File chrootDir = new File(Defaults.chrootDir);
    private static File primaryDir = chrootDir;    // added by Nova, set primary directory for ftp user.
    private static ProxyConnector proxyConnector = null;
    private static String username = null;

    public static ProxyConnector getProxyConnector() {
        if(proxyConnector != null) {
            if(!proxyConnector.isAlive()) {
                return null;
            }
        }
        return proxyConnector;
    }

    public static void setProxyConnector(ProxyConnector proxyConnector) {
        Globals.proxyConnector = proxyConnector;
    }

    public static File getChrootDir() {
        return chrootDir;
    }

    public static void setChrootDir(File chrootDir) {
        if(chrootDir.isDirectory()) {
            Globals.chrootDir = chrootDir;
            Globals.primaryDir = chrootDir;
        }
    }

    /** Get primary directory for ftp user.
     */
    public static File getPrimaryDir() {
        return primaryDir;
    }

    /** Set primary directory for ftp user. when root directory is "/", we can change the primary directory of ftp user by setting this value.
     *  Notes:
     *   1. It must be invoked after setChrootDir.
     *   2. Globals.primaryDir must be subdirectory of Globals.chrootDir.
     */
    public static void setPrimaryDir(File dir) {
        if (dir.isDirectory()) {
            try {
                if (dir.getCanonicalPath().startsWith(Globals.getChrootDir().getPath())) {
                    Globals.primaryDir = dir; // the primary directory name must begin with the Globals.chrootDir.
                }
                return ;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getLastError() {
        return lastError;
    }

    public static void setLastError(String lastError) {
        Globals.lastError = lastError;
    }

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
        if(context != null) {
            Globals.context = context;
        }
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        Globals.username = username;
    }

}
