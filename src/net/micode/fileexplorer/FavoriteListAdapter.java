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
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import java.util.List;

public class FavoriteListAdapter extends ArrayAdapter<FavoriteItem> {
    private Context mContext;

    private LayoutInflater mInflater;

    private FileIconHelper mFileIcon;

    public FavoriteListAdapter(Context context, int resource, List<FavoriteItem> objects, FileIconHelper fileIcon) {
        super(context, resource, objects);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mFileIcon = fileIcon;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        if (convertView != null) {
            view = convertView;
        } else {
            view = mInflater.inflate(R.layout.favorite_item, parent, false);
        }

        FavoriteItem item = getItem(position);
        FileInfo lFileInfo = item.fileInfo;

        Util.setText(view, R.id.file_name, item.title != null ? item.title : lFileInfo.fileName);
        Util.setText(view, R.id.modified_time, Util.formatDateString(mContext, lFileInfo.ModifiedDate));
        Util.setText(view, R.id.file_size, (lFileInfo.IsDir ? "" : Util.convertStorage(lFileInfo.fileSize)));

        ImageView lFileImage = (ImageView) view.findViewById(R.id.file_image);
        ImageView lFileImageFrame = (ImageView) view.findViewById(R.id.file_image_frame);
        lFileImage.setTag(position);

        if (lFileInfo.IsDir) {
            lFileImageFrame.setVisibility(View.GONE);
            lFileImage.setImageResource(R.drawable.folder_fav);
        } else {
            mFileIcon.setIcon(lFileInfo, lFileImage, lFileImageFrame);
        }

        return view;
    }

}
