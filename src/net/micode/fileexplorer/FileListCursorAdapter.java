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
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import java.util.Collection;
import java.util.HashMap;

public class FileListCursorAdapter extends CursorAdapter {

    private final LayoutInflater mFactory;

    private FileViewInteractionHub mFileViewInteractionHub;

    private FileIconHelper mFileIcon;

    private HashMap<Integer, FileInfo> mFileNameList = new HashMap<Integer, FileInfo>();

    public FileListCursorAdapter(Context context, Cursor cursor, FileViewInteractionHub f, FileIconHelper fileIcon) {
        super(context, cursor, false /* auto-requery */);
        mFactory = LayoutInflater.from(context);
        mFileViewInteractionHub = f;
        mFileIcon = fileIcon;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        FileListItem listItem = (FileListItem) view;
        FileInfo fileInfo = getFileItem(cursor.getPosition());
        if (fileInfo == null) {
            // file is not existing, create a fake info
            fileInfo = new FileInfo();
            fileInfo.dbId = cursor.getLong(FileCategoryHelper.COLUMN_ID);
            fileInfo.filePath = cursor.getString(FileCategoryHelper.COLUMN_PATH);
            fileInfo.fileName = Util.getNameFromFilepath(fileInfo.filePath);
            fileInfo.fileSize = cursor.getLong(FileCategoryHelper.COLUMN_SIZE);
            fileInfo.ModifiedDate = cursor.getLong(FileCategoryHelper.COLUMN_DATE);
        }

        listItem.bind(fileInfo, mFileViewInteractionHub, mFileIcon);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mFactory.inflate(R.layout.file_browse_item, parent, false);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        mFileNameList.clear();
        super.changeCursor(cursor);
    }

    public Collection<FileInfo> getAllFiles() {
        if (mFileNameList.size() == getCount())
            return mFileNameList.values();

        Cursor cursor = getCursor();
        if (cursor.moveToFirst()) {
            do {
                Integer position = Integer.valueOf(cursor.getPosition());
                if (mFileNameList.containsKey(position))
                    continue;
                FileInfo fileInfo = getFileInfo(cursor);
                if (fileInfo != null) {
                    mFileNameList.put(position, fileInfo);
                }
            } while (cursor.moveToNext());
        }

        return mFileNameList.values();
    }

    public FileInfo getFileItem(int pos) {
        Integer position = Integer.valueOf(pos);
        if (mFileNameList.containsKey(position))
            return mFileNameList.get(position);

        Cursor cursor = (Cursor) getItem(pos);
        FileInfo fileInfo = getFileInfo(cursor);
        if (fileInfo == null)
            return null;

        fileInfo.dbId = cursor.getLong(FileCategoryHelper.COLUMN_ID);
        mFileNameList.put(position, fileInfo);
        return fileInfo;
    }

    private FileInfo getFileInfo(Cursor cursor) {
        return (cursor == null || cursor.getCount() == 0) ? null : Util.GetFileInfo(cursor.getString(FileCategoryHelper.COLUMN_PATH));
    }
}
