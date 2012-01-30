/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.micode.fileexplorer;
import net.micode.fileexplorer.FileViewInteractionHub.Mode;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FileListItem extends LinearLayout {
    private static Context mContext;
    private static FileViewInteractionHub mFileViewInteractionHub;

    public FileListItem(Context context) {
        super(context);
        mContext = context;
    }

    public FileListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public final void bind(FileInfo file, FileViewInteractionHub f, FileIconHelper mFileIcon) {
        mFileViewInteractionHub = f;
        FileInfo lFileInfo = file;

        // if in moving mode, show selected file always
        if (mFileViewInteractionHub.isMoveState()) {
            lFileInfo.Selected = mFileViewInteractionHub.isFileSelected(lFileInfo.filePath);
        }

        ImageView checkbox = (ImageView) findViewById(R.id.file_checkbox);
        if (mFileViewInteractionHub.getMode() == Mode.Pick) {
            checkbox.setVisibility(View.GONE);
        } else {
            checkbox.setVisibility(mFileViewInteractionHub.canShowCheckBox() ? View.VISIBLE : View.GONE);
            checkbox.setImageResource(lFileInfo.Selected ? R.drawable.btn_check_on_holo_light
                    : R.drawable.btn_check_off_holo_light);
            checkbox.setTag(lFileInfo);
            View checkArea = findViewById(R.id.file_checkbox_area);
            checkArea.setOnClickListener(checkClick);
            setSelected(lFileInfo.Selected);
        }

        Util.setText(this, R.id.file_name, lFileInfo.fileName);
        Util.setText(this, R.id.file_count, lFileInfo.IsDir ? "(" + lFileInfo.Count + ")" : "");
        Util.setText(this, R.id.modified_time, Util.formatDateString(mContext, lFileInfo.ModifiedDate));
        Util.setText(this, R.id.file_size, (lFileInfo.IsDir ? "" : Util.convertStorage(lFileInfo.fileSize)));

        ImageView lFileImage = (ImageView) findViewById(R.id.file_image);
        ImageView lFileImageFrame = (ImageView) findViewById(R.id.file_image_frame);

        if (lFileInfo.IsDir) {
            lFileImageFrame.setVisibility(View.GONE);
            lFileImage.setImageResource(R.drawable.folder);
        } else {
            mFileIcon.setIcon(lFileInfo, lFileImage, lFileImageFrame);
        }
    }

    private OnClickListener checkClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            ImageView img = (ImageView) v.findViewById(R.id.file_checkbox);
            assert (img != null && img.getTag() != null);

            FileInfo tag = (FileInfo) img.getTag();
            tag.Selected = !tag.Selected;
            ActionMode actionMode = ((FileExplorerTabActivity) mContext).getActionMode();
            if (actionMode == null) {
                actionMode = startActionMode(new ModeCallback());
                ((FileExplorerTabActivity) mContext).setActionMode(actionMode);
            } else {
                actionMode.invalidate();
            }
            if (mFileViewInteractionHub.onCheckItem(tag, v)) {
                img.setImageResource(tag.Selected ? R.drawable.btn_check_on_holo_light
                        : R.drawable.btn_check_off_holo_light);
            } else {
                tag.Selected = !tag.Selected;
            }

            Util.updateActionModeTitle(actionMode, mContext, mFileViewInteractionHub
                    .getSelectedFileList().size());
        }
    };

    public static class ModeCallback implements ActionMode.Callback {
        private Menu mMenu;

        private void initMenuItemSelectAllOrCancel() {
            boolean isSelectedAll = mFileViewInteractionHub.isSelectedAll();
            mMenu.findItem(R.id.action_cancel).setVisible(isSelectedAll);
            mMenu.findItem(R.id.action_select_all).setVisible(!isSelectedAll);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = ((Activity) mContext).getMenuInflater();
            mMenu = menu;
            inflater.inflate(R.menu.operation_menu, mMenu);
            initMenuItemSelectAllOrCancel();
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mMenu.findItem(R.id.action_copy_path).setVisible(
                    mFileViewInteractionHub.getSelectedFileList().size() == 1);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch(item.getItemId()) {
                case R.id.action_delete:
                    mFileViewInteractionHub.onOperationDelete();
                    mode.finish();
                    break;
                case R.id.action_copy:
                    mFileViewInteractionHub.onOperationCopy();
                    mode.finish();
                    break;
                case R.id.action_move:
                    mFileViewInteractionHub.onOperationMove();
                    mode.finish();
                    break;
                case R.id.action_send:
                    mFileViewInteractionHub.onOperationSend();
                    mode.finish();
                    break;
                case R.id.action_copy_path:
                    mFileViewInteractionHub.onOperationCopyPath();
                    mode.finish();
                    break;
                case R.id.action_cancel:
                    mFileViewInteractionHub.clearSelection();
                    initMenuItemSelectAllOrCancel();
                    mode.finish();
                    break;
                case R.id.action_select_all:
                    mFileViewInteractionHub.onOperationSelectAll();
                    initMenuItemSelectAllOrCancel();
                    break;
            }
            Util.updateActionModeTitle(mode, mContext, mFileViewInteractionHub
                    .getSelectedFileList().size());
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mFileViewInteractionHub.clearSelection();
            ((FileExplorerTabActivity) mContext).setActionMode(null);
        }
    }
}
