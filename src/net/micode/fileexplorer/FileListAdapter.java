
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
