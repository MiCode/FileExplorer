/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.fileexplorer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;

public class FilenameExtFilter implements FilenameFilter {

    private HashSet<String> mExts = new HashSet<String>();

    // using lower case
    public FilenameExtFilter(String[] exts) {
        if (exts != null) {
            mExts.addAll(Arrays.asList(exts));
        }
    }

    public boolean contains(String ext) {
        return mExts.contains(ext.toLowerCase());
    }

    @Override
    public boolean accept(File dir, String filename) {

        File file = new File(dir + File.separator + filename);
        if (file.isDirectory()) {
            return true;
        }

        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            String ext = (String) filename.subSequence(dotPosition + 1, filename.length());
            return contains(ext.toLowerCase());
        }

        return false;
    }
}
