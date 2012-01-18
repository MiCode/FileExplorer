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

import java.util.Arrays;
import java.util.HashSet;

public class FilenameOtherExtFilter extends FilenameExtFilter {

    private HashSet<FilenameExtFilter> mExts;

    // using lower case
    public FilenameOtherExtFilter(FilenameExtFilter[] exts) {
        super(null);
        mExts = new HashSet<FilenameExtFilter>();
        mExts.addAll(Arrays.asList(exts));
    }

    @Override
    public boolean contains(String ext) {
        for (FilenameExtFilter f : mExts) {
            if (f.contains(ext))
                return false;
        }
        return true;
    }
}
