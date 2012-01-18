
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
