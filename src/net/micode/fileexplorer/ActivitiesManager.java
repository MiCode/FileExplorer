
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
