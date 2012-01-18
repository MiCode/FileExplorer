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

import android.content.Context;

import java.io.FilenameFilter;
import java.util.HashMap;

public class FileCategoryHelper {
    public static final int COLUMN_ID = 0;

    public static final int COLUMN_PATH = 1;

    public static final int COLUMN_SIZE = 2;

    public static final int COLUMN_DATE = 3;

    private static final String LOG_TAG = "FileCategoryHelper";

    public enum FileCategory {
        All, Music, Video, Picture, Theme, Doc, Zip, Apk, Custom, Other, Favorite
    }

    private static String APK_EXT = "apk";
    private static String THEME_EXT = "mtz";
    private static String[] DOCUMENT_EXTS = new String[] {
            "doc", "ppt", "docx", "pptx", "xsl", "xslx", "txt", "log", "pdf", "ini", "lrc"
    };
    private static String[] ZIP_EXTS  = new String[] {
            "zip", "rar"
    };
    private static String[] VIDEO_EXTS = new String[] {
            "mp4", "wmv", "mpeg", "m4v", "3gp", "3gpp", "3g2", "3gpp2", "asf"
    };

    private static String[] PICTURE_EXTS = new String[] {
            "jpg", "jpeg", "gif", "png", "bmp", "wbmp"
    };

    public static HashMap<FileCategory, FilenameExtFilter> filters = new HashMap<FileCategory, FilenameExtFilter>();

    public static HashMap<FileCategory, Integer> categoryNames = new HashMap<FileCategory, Integer>();

    static {
        categoryNames.put(FileCategory.All, R.string.category_all);
        categoryNames.put(FileCategory.Music, R.string.category_music);
        categoryNames.put(FileCategory.Video, R.string.category_video);
        categoryNames.put(FileCategory.Picture, R.string.category_picture);
        categoryNames.put(FileCategory.Theme, R.string.category_theme);
        categoryNames.put(FileCategory.Doc, R.string.category_document);
        categoryNames.put(FileCategory.Zip, R.string.category_zip);
        categoryNames.put(FileCategory.Apk, R.string.category_apk);
        categoryNames.put(FileCategory.Other, R.string.category_other);
        categoryNames.put(FileCategory.Favorite, R.string.category_favorite);
    }

    private FileCategory mCategory;

    private Context mContext;

    public FileCategoryHelper(Context context) {
        mContext = context;
        mCategory = FileCategory.All;
    }

    public FileCategory getCurCategory() {
        return mCategory;
    }

    public void setCurCategory(FileCategory c) {
        mCategory = c;
    }

    public int getCurCategoryNameResId() {
        return categoryNames.get(mCategory);
    }

    public void setCustomCategory(String[] exts) {
        mCategory = FileCategory.Custom;
        if (filters.containsKey(FileCategory.Custom)) {
            filters.remove(FileCategory.Custom);
        }

        filters.put(FileCategory.Custom, new FilenameExtFilter(exts));
    }

    public FilenameFilter getFilter() {
        return filters.get(mCategory);
    }

    private HashMap<FileCategory, CategoryInfo> mCategoryInfo = new HashMap<FileCategory, CategoryInfo>();

    // private IContentProvider mMediaProvider;

    public HashMap<FileCategory, CategoryInfo> getCategoryInfos() {
        return mCategoryInfo;
    }

    public CategoryInfo getCategoryInfo(FileCategory fc) {
        if (mCategoryInfo.containsKey(fc)) {
            return mCategoryInfo.get(fc);
        } else {
            CategoryInfo info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
            return info;
        }
    }

    public class CategoryInfo {
        public long count;

        public long size;
    }

    private void setCategoryInfo(FileCategory fc, long count, long size) {
        CategoryInfo info = mCategoryInfo.get(fc);
        if (info == null) {
            info = new CategoryInfo();
            mCategoryInfo.put(fc, info);
        }
        info.count = count;
        info.size = size;
    }

    public static FileCategory getCategoryFromPath(String path) {
        int dotPosition = path.lastIndexOf('.');
        if (dotPosition == -1)
            return FileCategory.Other;

        String ext = path.substring(dotPosition + 1, path.length());
        if (ext.equalsIgnoreCase(APK_EXT)) {
            return FileCategory.Apk;
        }

        if (ext.equalsIgnoreCase(THEME_EXT)) {
            return FileCategory.Theme;
        }

        if (matchExts(ext, DOCUMENT_EXTS)) {
            return FileCategory.Doc;
        }

        if (matchExts(ext, ZIP_EXTS)) {
            return FileCategory.Zip;
        }

        if (matchExts(ext, VIDEO_EXTS)) {
            return FileCategory.Video;
        }

        if (matchExts(ext, PICTURE_EXTS)) {
            return FileCategory.Picture;
        }

        return FileCategory.Other;
    }

    private static boolean matchExts(String ext, String[] exts) {
        for (String ex : exts) {
            if (ex.equalsIgnoreCase(ext))
                return true;
        }
        return false;
    }
}
