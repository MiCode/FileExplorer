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

import java.io.File;
import java.util.ArrayList;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.fileexplorer.FileListItem.ModeCallback;
import net.micode.fileexplorer.FileOperationHelper.IOperationProgressListener;
import net.micode.fileexplorer.FileSortHelper.SortMethod;
import net.micode.fileexplorer.FileViewActivity.SelectFilesCallback;
import net.micode.fileexplorer.TextInputDialog.OnFinishListener;

public class FileViewInteractionHub implements IOperationProgressListener {
    private static final String LOG_TAG = "FileViewInteractionHub";

    private IFileInteractionListener mFileViewListener;

    private ArrayList<FileInfo> mCheckedFileNameList = new ArrayList<FileInfo>();

    private FileOperationHelper mFileOperationHelper;

    private FileSortHelper mFileSortHelper;

    private View mConfirmOperationBar;

    private ProgressDialog progressDialog;

    private View mNavigationBar;

    private TextView mNavigationBarText;

    private View mDropdownNavigation;

    private ImageView mNavigationBarUpDownArrow;

    private Context mContext;

    public enum Mode {
        View, Pick
    };

    public FileViewInteractionHub(IFileInteractionListener fileViewListener) {
        assert (fileViewListener != null);
        mFileViewListener = fileViewListener;
        setup();
        mFileOperationHelper = new FileOperationHelper(this);
        mFileSortHelper = new FileSortHelper();
        mContext = mFileViewListener.getContext();
    }

    private void showProgress(String msg) {
        progressDialog = new ProgressDialog(mContext);
        // dialog.setIcon(R.drawable.icon);
        progressDialog.setMessage(msg);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void sortCurrentList() {
        mFileViewListener.sortCurrentList(mFileSortHelper);
    }

    public boolean canShowCheckBox() {
        return mConfirmOperationBar.getVisibility() != View.VISIBLE;
    }

    private void showConfirmOperationBar(boolean show) {
        mConfirmOperationBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void addContextMenuSelectedItem() {
        if (mCheckedFileNameList.size() == 0) {
            int pos = mListViewContextMenuSelectedItem;
            if (pos != -1) {
                FileInfo fileInfo = mFileViewListener.getItem(pos);
                if (fileInfo != null) {
                    mCheckedFileNameList.add(fileInfo);
                }
            }
        }
    }

    public ArrayList<FileInfo> getSelectedFileList() {
        return mCheckedFileNameList;
    }

    public boolean canPaste() {
        return mFileOperationHelper.canPaste();
    }

    // operation finish notification
    @Override
    public void onFinish() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        mFileViewListener.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showConfirmOperationBar(false);
                clearSelection();
                refreshFileList();
            }
        });
    }

    public FileInfo getItem(int pos) {
        return mFileViewListener.getItem(pos);
    }

    public boolean isInSelection() {
        return mCheckedFileNameList.size() > 0;
    }

    public boolean isMoveState() {
        return mFileOperationHelper.isMoveState() || mFileOperationHelper.canPaste();
    }

    private void setup() {
        setupNaivgationBar();
        setupFileListView();
        setupOperationPane();
    }

    private void setupNaivgationBar() {
        mNavigationBar = mFileViewListener.getViewById(R.id.navigation_bar);
        mNavigationBarText = (TextView) mFileViewListener.getViewById(R.id.current_path_view);
        mNavigationBarUpDownArrow = (ImageView) mFileViewListener.getViewById(R.id.path_pane_arrow);
        View clickable = mFileViewListener.getViewById(R.id.current_path_pane);
        clickable.setOnClickListener(buttonClick);

        mDropdownNavigation = mFileViewListener.getViewById(R.id.dropdown_navigation);

        setupClick(mNavigationBar, R.id.path_pane_up_level);
    }

    // buttons
    private void setupOperationPane() {
        mConfirmOperationBar = mFileViewListener.getViewById(R.id.moving_operation_bar);
        setupClick(mConfirmOperationBar, R.id.button_moving_confirm);
        setupClick(mConfirmOperationBar, R.id.button_moving_cancel);
    }

    private void setupClick(View v, int id) {
        View button = (v != null ? v.findViewById(id) : mFileViewListener.getViewById(id));
        if (button != null)
            button.setOnClickListener(buttonClick);
    }

    private View.OnClickListener buttonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_operation_copy:
                    onOperationCopy();
                    break;
                case R.id.button_operation_move:
                    onOperationMove();
                    break;
                case R.id.button_operation_send:
                    onOperationSend();
                    break;
                case R.id.button_operation_delete:
                    onOperationDelete();
                    break;
                case R.id.button_operation_cancel:
                    onOperationSelectAllOrCancel();
                    break;
                case R.id.current_path_pane:
                    onNavigationBarClick();
                    break;
                case R.id.button_moving_confirm:
                    onOperationButtonConfirm();
                    break;
                case R.id.button_moving_cancel:
                    onOperationButtonCancel();
                    break;
                case R.id.path_pane_up_level:
                    onOperationUpLevel();
                    ActionMode mode = ((FileExplorerTabActivity) mContext).getActionMode();
                    if (mode != null) {
                        mode.finish();
                    }
                    break;
            }
        }

    };

    private void onOperationReferesh() {
        refreshFileList();
    }

    private void onOperationFavorite() {
        String path = mCurrentPath;

        if (mListViewContextMenuSelectedItem != -1) {
            path = mFileViewListener.getItem(mListViewContextMenuSelectedItem).filePath;
        }

        onOperationFavorite(path);
    }

    private void onOperationSetting() {
        Intent intent = new Intent(mContext, FileExplorerPreferenceActivity.class);
        if (intent != null) {
            try {
                mContext.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to start setting: " + e.toString());
            }
        }
    }

    private void onOperationFavorite(String path) {
        FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
        if (databaseHelper != null) {
            int stringId = 0;
            if (databaseHelper.isFavorite(path)) {
                databaseHelper.delete(path);
                stringId = R.string.removed_favorite;
            } else {
                databaseHelper.insert(Util.getNameFromFilepath(path), path);
                stringId = R.string.added_favorite;
            }

            Toast.makeText(mContext, stringId, Toast.LENGTH_SHORT).show();
        }
    }

    private void onOperationShowSysFiles() {
        Settings.instance().setShowDotAndHiddenFiles(!Settings.instance().getShowDotAndHiddenFiles());
        refreshFileList();
    }

    public void onOperationSelectAllOrCancel() {
        if (!isSelectedAll()) {
            onOperationSelectAll();
        } else {
            clearSelection();
        }
    }

    public void onOperationSelectAll() {
        mCheckedFileNameList.clear();
        for (FileInfo f : mFileViewListener.getAllFiles()) {
            f.Selected = true;
            mCheckedFileNameList.add(f);
        }
        FileExplorerTabActivity fileExplorerTabActivity = (FileExplorerTabActivity) mContext;
        ActionMode mode = fileExplorerTabActivity.getActionMode();
        if (mode == null) {
            mode = fileExplorerTabActivity.startActionMode(new ModeCallback(mContext, this));
            fileExplorerTabActivity.setActionMode(mode);
            Util.updateActionModeTitle(mode, mContext, getSelectedFileList().size());
        }
        mFileViewListener.onDataChanged();
    }

    private OnClickListener navigationClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            String path = (String) v.getTag();
            assert (path != null);
            showDropdownNavigation(false);
            if (mFileViewListener.onNavigation(path))
                return;

            if(path.isEmpty()){
                mCurrentPath = mRoot;
            } else{
                mCurrentPath = path;
            }
            refreshFileList();
        }

    };

    protected void onNavigationBarClick() {
        if (mDropdownNavigation.getVisibility() == View.VISIBLE) {
            showDropdownNavigation(false);
        } else {
            LinearLayout list = (LinearLayout) mDropdownNavigation.findViewById(R.id.dropdown_navigation_list);
            list.removeAllViews();
            int pos = 0;
            String displayPath = mFileViewListener.getDisplayPath(mCurrentPath);
            boolean root = true;
            int left = 0;
            while (pos != -1 && !displayPath.equals("/")) {//如果当前位置在根文件夹则不显示导航条
                int end = displayPath.indexOf("/", pos);
                if (end == -1)
                    break;

                View listItem = LayoutInflater.from(mContext).inflate(R.layout.dropdown_item,
                        null);

                View listContent = listItem.findViewById(R.id.list_item);
                listContent.setPadding(left, 0, 0, 0);
                left += 20;
                ImageView img = (ImageView) listItem.findViewById(R.id.item_icon);

                img.setImageResource(root ? R.drawable.dropdown_icon_root : R.drawable.dropdown_icon_folder);
                root = false;

                TextView text = (TextView) listItem.findViewById(R.id.path_name);
                String substring = displayPath.substring(pos, end);
                if(substring.isEmpty())substring = "/";
                text.setText(substring);

                listItem.setOnClickListener(navigationClick);
                listItem.setTag(mFileViewListener.getRealPath(displayPath.substring(0, end)));
                pos = end + 1;
                list.addView(listItem);
            }
            if (list.getChildCount() > 0)
                showDropdownNavigation(true);

        }
    }

    public boolean onOperationUpLevel() {
        showDropdownNavigation(false);

        if (mFileViewListener.onOperation(GlobalConsts.OPERATION_UP_LEVEL)) {
            return true;
        }

        if (!mRoot.equals(mCurrentPath)) {
            mCurrentPath = new File(mCurrentPath).getParent();
            refreshFileList();
            return true;
        }

        return false;
    }

    public void onOperationCreateFolder() {
        TextInputDialog dialog = new TextInputDialog(mContext, mContext.getString(
                R.string.operation_create_folder), mContext.getString(R.string.operation_create_folder_message),
                mContext.getString(R.string.new_folder_name), new OnFinishListener() {
                    @Override
                    public boolean onFinish(String text) {
                        return doCreateFolder(text);
                    }
                });

        dialog.show();
    }

    private boolean doCreateFolder(String text) {
        if (TextUtils.isEmpty(text))
            return false;

        if (mFileOperationHelper.CreateFolder(mCurrentPath, text)) {
            mFileViewListener.addSingleFile(Util.GetFileInfo(Util.makePath(mCurrentPath, text)));
            mFileListView.setSelection(mFileListView.getCount() - 1);
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_create_folder))
                    .setPositiveButton(R.string.confirm, null).create().show();
            return false;
        }

        return true;
    }

    public void onOperationSearch() {

    }

    public void onSortChanged(SortMethod s) {
        if (mFileSortHelper.getSortMethod() != s) {
            mFileSortHelper.setSortMethog(s);
            sortCurrentList();
        }
    }

    public void onOperationCopy() {
        onOperationCopy(getSelectedFileList());
    }

    public void onOperationCopy(ArrayList<FileInfo> files) {
        mFileOperationHelper.Copy(files);
        clearSelection();

        showConfirmOperationBar(true);
        View confirmButton = mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }

    public void onOperationCopyPath() {
        if (getSelectedFileList().size() == 1) {
            copy(getSelectedFileList().get(0).filePath);
        }
        clearSelection();
    }

    private void copy(CharSequence text) {
        ClipboardManager cm = (ClipboardManager) mContext.getSystemService(
                Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }

    private void onOperationPaste() {
        if (mFileOperationHelper.Paste(mCurrentPath)) {
            showProgress(mContext.getString(R.string.operation_pasting));
        }
    }

    public void onOperationMove() {
        mFileOperationHelper.StartMove(getSelectedFileList());
        clearSelection();
        showConfirmOperationBar(true);
        View confirmButton = mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        confirmButton.setEnabled(false);
        // refresh to hide selected files
        refreshFileList();
    }

    public void refreshFileList() {
        clearSelection();
        updateNavigationPane();

        // onRefreshFileList returns true indicates list has changed
        mFileViewListener.onRefreshFileList(mCurrentPath, mFileSortHelper);

        // update move operation button state
        updateConfirmButtons();

    }

    private void updateConfirmButtons() {
        if (mConfirmOperationBar.getVisibility() == View.GONE)
            return;

        Button confirmButton = (Button) mConfirmOperationBar.findViewById(R.id.button_moving_confirm);
        int text = R.string.operation_paste;
        if (isSelectingFiles()) {
            confirmButton.setEnabled(mCheckedFileNameList.size() != 0);
            text = R.string.operation_send;
        } else if (isMoveState()) {
            confirmButton.setEnabled(mFileOperationHelper.canMove(mCurrentPath));
        }

        confirmButton.setText(text);
    }

    private void updateNavigationPane() {
        View upLevel = mFileViewListener.getViewById(R.id.path_pane_up_level);
        upLevel.setVisibility(mRoot.equals(mCurrentPath) ? View.INVISIBLE : View.VISIBLE);

        View arrow = mFileViewListener.getViewById(R.id.path_pane_arrow);
        arrow.setVisibility(mRoot.equals(mCurrentPath) ? View.GONE : View.VISIBLE);

        mNavigationBarText.setText(mFileViewListener.getDisplayPath(mCurrentPath));
    }

    public void onOperationSend() {
        ArrayList<FileInfo> selectedFileList = getSelectedFileList();
        for (FileInfo f : selectedFileList) {
            if (f.IsDir) {
                AlertDialog dialog = new AlertDialog.Builder(mContext).setMessage(
                        R.string.error_info_cant_send_folder).setPositiveButton(R.string.confirm, null).create();
                dialog.show();
                return;
            }
        }

        Intent intent = IntentBuilder.buildSendFile(selectedFileList);
        if (intent != null) {
            try {
                mFileViewListener.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Log.e(LOG_TAG, "fail to view file: " + e.toString());
            }
        }
        clearSelection();
    }

    public void onOperationRename() {
        int pos = mListViewContextMenuSelectedItem;
        if (pos == -1)
            return;

        if (getSelectedFileList().size() == 0)
            return;

        final FileInfo f = getSelectedFileList().get(0);
        clearSelection();

        TextInputDialog dialog = new TextInputDialog(mContext, mContext.getString(R.string.operation_rename),
                mContext.getString(R.string.operation_rename_message), f.fileName, new OnFinishListener() {
                    @Override
                    public boolean onFinish(String text) {
                        return doRename(f, text);
                    }

                });

        dialog.show();
    }

    private boolean doRename(final FileInfo f, String text) {
        if (TextUtils.isEmpty(text))
            return false;

        if (mFileOperationHelper.Rename(f, text)) {
            f.fileName = text;
            mFileViewListener.onDataChanged();
        } else {
            new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.fail_to_rename))
                    .setPositiveButton(R.string.confirm, null).create().show();
            return false;
        }

        return true;
    }

    private void notifyFileSystemChanged(String path) {
        if (path == null)
            return;
        final File f = new File(path);
        final Intent intent;
        if (f.isDirectory()) {
            intent = new Intent(Intent.ACTION_MEDIA_MOUNTED);
            intent.setClassName("com.android.providers.media", "com.android.providers.media.MediaScannerReceiver");
            intent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
            Log.v(LOG_TAG, "directory changed, send broadcast:" + intent.toString());
        } else {
            intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(Uri.fromFile(new File(path)));
            Log.v(LOG_TAG, "file changed, send broadcast:" + intent.toString());
        }
        mContext.sendBroadcast(intent);
    }

    public void onOperationDelete() {
        doOperationDelete(getSelectedFileList());
    }

    public void onOperationDelete(int position) {
        FileInfo file = mFileViewListener.getItem(position);
        if (file == null)
            return;

        ArrayList<FileInfo> selectedFileList = new ArrayList<FileInfo>();
        selectedFileList.add(file);
        doOperationDelete(selectedFileList);
    }

    private void doOperationDelete(final ArrayList<FileInfo> selectedFileList) {
        final ArrayList<FileInfo> selectedFiles = new ArrayList<FileInfo>(selectedFileList);
        Dialog dialog = new AlertDialog.Builder(mContext)
                .setMessage(mContext.getString(R.string.operation_delete_confirm_message))
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (mFileOperationHelper.Delete(selectedFiles)) {
                            showProgress(mContext.getString(R.string.operation_deleting));
                        }
                        clearSelection();
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearSelection();
                    }
                }).create();
        dialog.show();
    }

    public void onOperationInfo() {
        if (getSelectedFileList().size() == 0)
            return;

        FileInfo file = getSelectedFileList().get(0);
        if (file == null)
            return;

        InformationDialog dialog = new InformationDialog(mContext, file, mFileViewListener
                .getFileIconHelper());
        dialog.show();
        clearSelection();
    }

    public void onOperationButtonConfirm() {
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(mCheckedFileNameList);
            mSelectFilesCallback = null;
            clearSelection();
        } else if (mFileOperationHelper.isMoveState()) {
            if (mFileOperationHelper.EndMove(mCurrentPath)) {
                showProgress(mContext.getString(R.string.operation_moving));
            }
        } else {
            onOperationPaste();
        }
    }

    public void onOperationButtonCancel() {
        mFileOperationHelper.clear();
        showConfirmOperationBar(false);
        if (isSelectingFiles()) {
            mSelectFilesCallback.selected(null);
            mSelectFilesCallback = null;
            clearSelection();
        } else if (mFileOperationHelper.isMoveState()) {
            // refresh to show previously selected hidden files
            mFileOperationHelper.EndMove(null);
            refreshFileList();
        } else {
            refreshFileList();
        }
    }

    // context menu
    private OnCreateContextMenuListener mListViewContextMenuListener = new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (isInSelection() || isMoveState())
                return;

            showDropdownNavigation(false);

            AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

            FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
            FileInfo file = mFileViewListener.getItem(info.position);
            if (databaseHelper != null && file != null) {
                int stringId = databaseHelper.isFavorite(file.filePath) ? R.string.operation_unfavorite
                        : R.string.operation_favorite;
                addMenuItem(menu, GlobalConsts.MENU_FAVORITE, 0, stringId);
            }

            addMenuItem(menu, GlobalConsts.MENU_COPY, 0, R.string.operation_copy);
            addMenuItem(menu, GlobalConsts.MENU_COPY_PATH, 0, R.string.operation_copy_path);
            // addMenuItem(menu, GlobalConsts.MENU_PASTE, 0,
            // R.string.operation_paste);
            addMenuItem(menu, GlobalConsts.MENU_MOVE, 0, R.string.operation_move);
            addMenuItem(menu, MENU_SEND, 0, R.string.operation_send);
            addMenuItem(menu, MENU_RENAME, 0, R.string.operation_rename);
            addMenuItem(menu, MENU_DELETE, 0, R.string.operation_delete);
            addMenuItem(menu, MENU_INFO, 0, R.string.operation_info);

            if (!canPaste()) {
                MenuItem menuItem = menu.findItem(GlobalConsts.MENU_PASTE);
                if (menuItem != null)
                    menuItem.setEnabled(false);
            }
        }
    };

    // File List view setup
    private ListView mFileListView;

    private int mListViewContextMenuSelectedItem;

    private void setupFileListView() {
        mFileListView = (ListView) mFileViewListener.getViewById(R.id.file_path_list);
        mFileListView.setLongClickable(true);
        mFileListView.setOnCreateContextMenuListener(mListViewContextMenuListener);
        mFileListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(parent, view, position, id);
            }
        });
    }

    // menu
    private static final int MENU_SEARCH = 1;

    // private static final int MENU_NEW_FOLDER = 2;
    private static final int MENU_SORT = 3;

    private static final int MENU_SEND = 7;

    private static final int MENU_RENAME = 8;

    private static final int MENU_DELETE = 9;

    private static final int MENU_INFO = 10;

    private static final int MENU_SORT_NAME = 11;

    private static final int MENU_SORT_SIZE = 12;

    private static final int MENU_SORT_DATE = 13;

    private static final int MENU_SORT_TYPE = 14;

    private static final int MENU_REFRESH = 15;

    private static final int MENU_SELECTALL = 16;

    private static final int MENU_SETTING = 17;

    private static final int MENU_EXIT = 18;

    private OnMenuItemClickListener menuItemClick = new OnMenuItemClickListener() {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
            mListViewContextMenuSelectedItem = info != null ? info.position : -1;

            int itemId = item.getItemId();
            if (mFileViewListener.onOperation(itemId)) {
                return true;
            }

            addContextMenuSelectedItem();

            switch (itemId) {
                case MENU_SEARCH:
                    onOperationSearch();
                    break;
                case GlobalConsts.MENU_NEW_FOLDER:
                    onOperationCreateFolder();
                    break;
                case MENU_REFRESH:
                    onOperationReferesh();
                    break;
                case MENU_SELECTALL:
                    onOperationSelectAllOrCancel();
                    break;
                case GlobalConsts.MENU_SHOWHIDE:
                    onOperationShowSysFiles();
                    break;
                case GlobalConsts.MENU_FAVORITE:
                    onOperationFavorite();
                    break;
                case MENU_SETTING:
                    onOperationSetting();
                    break;
                case MENU_EXIT:
                    ((FileExplorerTabActivity) mContext).finish();
                    break;
                // sort
                case MENU_SORT_NAME:
                    item.setChecked(true);
                    onSortChanged(SortMethod.name);
                    break;
                case MENU_SORT_SIZE:
                    item.setChecked(true);
                    onSortChanged(SortMethod.size);
                    break;
                case MENU_SORT_DATE:
                    item.setChecked(true);
                    onSortChanged(SortMethod.date);
                    break;
                case MENU_SORT_TYPE:
                    item.setChecked(true);
                    onSortChanged(SortMethod.type);
                    break;

                case GlobalConsts.MENU_COPY:
                    onOperationCopy();
                    break;
                case GlobalConsts.MENU_COPY_PATH:
                    onOperationCopyPath();
                    break;
                case GlobalConsts.MENU_PASTE:
                    onOperationPaste();
                    break;
                case GlobalConsts.MENU_MOVE:
                    onOperationMove();
                    break;
                case MENU_SEND:
                    onOperationSend();
                    break;
                case MENU_RENAME:
                    onOperationRename();
                    break;
                case MENU_DELETE:
                    onOperationDelete();
                    break;
                case MENU_INFO:
                    onOperationInfo();
                    break;
                default:
                    return false;
            }

            mListViewContextMenuSelectedItem = -1;
            return true;
        }

    };

    private net.micode.fileexplorer.FileViewInteractionHub.Mode mCurrentMode;

    private String mCurrentPath;

    private String mRoot;

    private SelectFilesCallback mSelectFilesCallback;

    public boolean onCreateOptionsMenu(Menu menu) {
        clearSelection();
        showDropdownNavigation(false);

        // menu.add(0, MENU_SEARCH, 0,
        // R.string.menu_item_search).setOnMenuItemClickListener(
        // menuItemClick);
        addMenuItem(menu, MENU_SELECTALL, 0, R.string.operation_selectall,
                R.drawable.ic_menu_select_all);

        SubMenu sortMenu = menu.addSubMenu(0, MENU_SORT, 1, R.string.menu_item_sort).setIcon(
                R.drawable.ic_menu_sort);
        addMenuItem(sortMenu, MENU_SORT_NAME, 0, R.string.menu_item_sort_name);
        addMenuItem(sortMenu, MENU_SORT_SIZE, 1, R.string.menu_item_sort_size);
        addMenuItem(sortMenu, MENU_SORT_DATE, 2, R.string.menu_item_sort_date);
        addMenuItem(sortMenu, MENU_SORT_TYPE, 3, R.string.menu_item_sort_type);
        sortMenu.setGroupCheckable(0, true, true);
        sortMenu.getItem(0).setChecked(true);

        // addMenuItem(menu, GlobalConsts.MENU_PASTE, 2,
        // R.string.operation_paste);
        addMenuItem(menu, GlobalConsts.MENU_NEW_FOLDER, 3, R.string.operation_create_folder,
                R.drawable.ic_menu_new_folder);
        addMenuItem(menu, GlobalConsts.MENU_FAVORITE, 4, R.string.operation_favorite,
                R.drawable.ic_menu_delete_favorite);
        addMenuItem(menu, GlobalConsts.MENU_SHOWHIDE, 5, R.string.operation_show_sys,
                R.drawable.ic_menu_show_sys);
        addMenuItem(menu, MENU_REFRESH, 6, R.string.operation_refresh,
                R.drawable.ic_menu_refresh);
        addMenuItem(menu, MENU_SETTING, 7, R.string.menu_setting, drawable.ic_menu_preferences);
        addMenuItem(menu, MENU_EXIT, 8, R.string.menu_exit, drawable.ic_menu_close_clear_cancel);
        return true;
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string) {
        addMenuItem(menu, itemId, order, string, -1);
    }

    private void addMenuItem(Menu menu, int itemId, int order, int string, int iconRes) {
        if (!mFileViewListener.shouldHideMenu(itemId)) {
            MenuItem item = menu.add(0, itemId, order, string).setOnMenuItemClickListener(menuItemClick);
            if (iconRes > 0) {
                item.setIcon(iconRes);
            }
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        updateMenuItems(menu);
        return true;
    }

    private void updateMenuItems(Menu menu) {
        menu.findItem(MENU_SELECTALL).setTitle(
                isSelectedAll() ? R.string.operation_cancel_selectall : R.string.operation_selectall);
        menu.findItem(MENU_SELECTALL).setEnabled(mCurrentMode != Mode.Pick);

        MenuItem menuItem = menu.findItem(GlobalConsts.MENU_SHOWHIDE);
        if (menuItem != null) {
            menuItem.setTitle(Settings.instance().getShowDotAndHiddenFiles() ? R.string.operation_hide_sys
                    : R.string.operation_show_sys);
        }

        FavoriteDatabaseHelper databaseHelper = FavoriteDatabaseHelper.getInstance();
        if (databaseHelper != null) {
            MenuItem item = menu.findItem(GlobalConsts.MENU_FAVORITE);
            if (item != null) {
                item.setTitle(databaseHelper.isFavorite(mCurrentPath) ? R.string.operation_unfavorite
                        : R.string.operation_favorite);
            }
        }

    }

    public boolean isFileSelected(String filePath) {
        return mFileOperationHelper.isFileSelected(filePath);
    }

    public void setMode(Mode m) {
        mCurrentMode = m;
    }

    public Mode getMode() {
        return mCurrentMode;
    }

    public void onListItemClick(AdapterView<?> parent, View view, int position, long id) {
        FileInfo lFileInfo = mFileViewListener.getItem(position);
        showDropdownNavigation(false);

        if (lFileInfo == null) {
            Log.e(LOG_TAG, "file does not exist on position:" + position);
            return;
        }

        if (isInSelection()) {
            boolean selected = lFileInfo.Selected;
            ActionMode actionMode = ((FileExplorerTabActivity) mContext).getActionMode();
            ImageView checkBox = (ImageView) view.findViewById(R.id.file_checkbox);
            if (selected) {
                mCheckedFileNameList.remove(lFileInfo);
                checkBox.setImageResource(R.drawable.btn_check_off_holo_light);
            } else {
                mCheckedFileNameList.add(lFileInfo);
                checkBox.setImageResource(R.drawable.btn_check_on_holo_light);
            }
            if (actionMode != null) {
                if (mCheckedFileNameList.size() == 0) actionMode.finish();
                else actionMode.invalidate();
            }
            lFileInfo.Selected = !selected;

            Util.updateActionModeTitle(actionMode, mContext, mCheckedFileNameList.size());
            return;
        }

        if (!lFileInfo.IsDir) {
            if (mCurrentMode == Mode.Pick) {
                mFileViewListener.onPick(lFileInfo);
            } else {
                viewFile(lFileInfo);
            }
            return;
        }

        mCurrentPath = getAbsoluteName(mCurrentPath, lFileInfo.fileName);
        ActionMode actionMode = ((FileExplorerTabActivity) mContext).getActionMode();
        if (actionMode != null) {
            actionMode.finish();
        }
        refreshFileList();
    }

    public void setRootPath(String path) {
        mRoot = path;
        mCurrentPath = path;
    }

    public String getRootPath() {
        return mRoot;
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    public void setCurrentPath(String path) {
        mCurrentPath = path;
    }

    private String getAbsoluteName(String path, String name) {
        return path.equals(GlobalConsts.ROOT_PATH) ? path + name : path + File.separator + name;
    }

    // check or uncheck
    public boolean onCheckItem(FileInfo f, View v) {
        if (isMoveState())
            return false;

        if(isSelectingFiles() && f.IsDir)
            return false;

        if (f.Selected) {
            mCheckedFileNameList.add(f);
        } else {
            mCheckedFileNameList.remove(f);
        }
        return true;
    }

    private boolean isSelectingFiles() {
        return mSelectFilesCallback != null;
    }

    public boolean isSelectedAll() {
        return mFileViewListener.getItemCount() != 0 && mCheckedFileNameList.size() == mFileViewListener.getItemCount();
    }
    
    public boolean isSelected() {
        return mCheckedFileNameList.size() != 0;
    }

    public void clearSelection() {
        if (mCheckedFileNameList.size() > 0) {
            for (FileInfo f : mCheckedFileNameList) {
                if (f == null) {
                    continue;
                }
                f.Selected = false;
            }
            mCheckedFileNameList.clear();
            mFileViewListener.onDataChanged();
        }
    }

    private void viewFile(FileInfo lFileInfo) {
        try {
            IntentBuilder.viewFile(mContext, lFileInfo.filePath);
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "fail to view file: " + e.toString());
        }
    }

    public boolean onBackPressed() {
        if (mDropdownNavigation.getVisibility() == View.VISIBLE) {
            mDropdownNavigation.setVisibility(View.GONE);
        } else if (isInSelection()) {
            clearSelection();
        } else if (!onOperationUpLevel()) {
            return false;
        }
        return true;
    }

    public void copyFile(ArrayList<FileInfo> files) {
        mFileOperationHelper.Copy(files);
    }

    public void moveFileFrom(ArrayList<FileInfo> files) {
        mFileOperationHelper.StartMove(files);
        showConfirmOperationBar(true);
        updateConfirmButtons();
        // refresh to hide selected files
        refreshFileList();
    }

    private void showDropdownNavigation(boolean show) {
        mDropdownNavigation.setVisibility(show ? View.VISIBLE : View.GONE);
        mNavigationBarUpDownArrow
                .setImageResource(mDropdownNavigation.getVisibility() == View.VISIBLE ? R.drawable.arrow_up
                        : R.drawable.arrow_down);
    }

    @Override
    public void onFileChanged(String path) {
        notifyFileSystemChanged(path);
    }

    public void startSelectFiles(SelectFilesCallback callback) {
        mSelectFilesCallback = callback;
        showConfirmOperationBar(true);
        updateConfirmButtons();
    }

}
