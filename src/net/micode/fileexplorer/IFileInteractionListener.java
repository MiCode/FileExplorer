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
import android.content.Intent;
import android.view.View;

import java.util.Collection;

public interface IFileInteractionListener {

    public View getViewById(int id);

    public Context getContext();

    public void startActivity(Intent intent);

    public void onDataChanged();

    public void onPick(FileInfo f);

    public boolean shouldShowOperationPane();

    public boolean onOperation(int id);

    public String getDisplayPath(String path);

    public String getRealPath(String displayPath);

    public void runOnUiThread(Runnable r);

    // return true indicates the navigation has been handled
    public boolean onNavigation(String path);

    public boolean shouldHideMenu(int menu);

    public FileIconHelper getFileIconHelper();

    public FileInfo getItem(int pos);

    public void sortCurrentList(FileSortHelper sort);

    public Collection<FileInfo> getAllFiles();

    public void addSingleFile(FileInfo file);

    public boolean onRefreshFileList(String path, FileSortHelper sort);

    public int getItemCount();
}
