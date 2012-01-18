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

import java.util.HashMap;

import android.app.Activity;

public class ActivitiesManager {
    public static final String ACTIVITY_FILE_VIEW = "FileView";

    public static final String ACTIVITY_FILE_CATEGORY = "FileCategory";

    public static final String ACTIVITY_TAB = "FileExplorerTab";

    private static ActivitiesManager instance;

    private HashMap<String, Activity> activities = new HashMap<String, Activity>();

    private ActivitiesManager() {
    }

    // return true indicates successful, false indicates the name exists
    public void registerActivity(String name, Activity a) {
        activities.put(name, a);
    }

    public Activity getActivity(String name) {
        return activities.get(name);
    }

    public static ActivitiesManager getInstance() {
        if (instance == null)
            instance = new ActivitiesManager();
        return instance;
    }
}
