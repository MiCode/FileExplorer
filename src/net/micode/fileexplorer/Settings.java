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
