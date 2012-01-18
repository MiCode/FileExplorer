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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public class FileListAdapter extends ArrayAdapter<FileInfo> {
    private static final String LOG_TAG = "FileListAdapter";

    private LayoutInflater mInflater;

    private FileViewInteractionHub mFileViewInteractionHub;

    private FileIconHelper mFileIcon;

    public FileListAdapter(Context context, int resource, List<FileInfo> objects,
            FileViewInteractionHub f, FileIconHelper fileIcon) {
        super(context, resource, objects);
        mInflater = LayoutInflater.from(context);
        mFileViewInteractionHub = f;
        mFileIcon = fileIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        if (convertView != null) {
            view = convertView;
        } else {
            view = mInflater.inflate(R.layout.file_browse_item, parent, false);
        }

        FileListItem listItem = (FileListItem) view;
        FileInfo lFileInfo = mFileViewInteractionHub.getItem(position);
        listItem.bind(lFileInfo, mFileViewInteractionHub, mFileIcon);

        return view;
    }
}
