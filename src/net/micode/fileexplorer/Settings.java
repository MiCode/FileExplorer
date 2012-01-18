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

public class Settings {
    // whether show system and cache images, default not
    private boolean mShowDotAndHiddenFiles;
    private static Settings mInstance;

    private Settings() {

    }

    public static Settings instance() {
        if(mInstance == null) {
            mInstance = new Settings();
        }
        return mInstance;
    }

    public boolean getShowDotAndHiddenFiles() {
        return mShowDotAndHiddenFiles;
    }

    public void setShowDotAndHiddenFiles(boolean s) {
        mShowDotAndHiddenFiles = s;
    }
}
