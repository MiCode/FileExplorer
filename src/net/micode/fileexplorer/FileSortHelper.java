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

import java.util.Comparator;
import java.util.HashMap;

public class FileSortHelper {

    public enum SortMethod {
        name, size, date, type
    }

    private SortMethod mSort;

    private boolean mFileFirst;

    private HashMap<SortMethod, Comparator> mComparatorList = new HashMap<SortMethod, Comparator>();

    public FileSortHelper() {
        mSort = SortMethod.name;
        mComparatorList.put(SortMethod.name, cmpName);
        mComparatorList.put(SortMethod.size, cmpSize);
        mComparatorList.put(SortMethod.date, cmpDate);
        mComparatorList.put(SortMethod.type, cmpType);
    }

    public void setSortMethog(SortMethod s) {
        mSort = s;
    }

    public SortMethod getSortMethod() {
        return mSort;
    }

    public void setFileFirst(boolean f) {
        mFileFirst = f;
    }

    public Comparator getComparator() {
        return mComparatorList.get(mSort);
    }

    private abstract class FileComparator implements Comparator<FileInfo> {

        @Override
        public int compare(FileInfo object1, FileInfo object2) {
            if (object1.IsDir == object2.IsDir) {
                return doCompare(object1, object2);
            }

            if (mFileFirst) {
                // the files are listed before the dirs
                return (object1.IsDir ? 1 : -1);
            } else {
                // the dir-s are listed before the files
                return object1.IsDir ? -1 : 1;
            }
        }

        protected abstract int doCompare(FileInfo object1, FileInfo object2);
    }

    private Comparator cmpName = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {
            return object1.fileName.compareToIgnoreCase(object2.fileName);
        }
    };

    private Comparator cmpSize = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {
            return longToCompareInt(object1.fileSize - object2.fileSize);
        }
    };

    private Comparator cmpDate = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {
            return longToCompareInt(object2.ModifiedDate - object1.ModifiedDate);
        }
    };
    
    private int longToCompareInt(long result) {
        return result > 0 ? 1 : (result < 0 ? -1 : 0);
    }

    private Comparator cmpType = new FileComparator() {
        @Override
        public int doCompare(FileInfo object1, FileInfo object2) {
            int result = Util.getExtFromFilename(object1.fileName).compareToIgnoreCase(
                    Util.getExtFromFilename(object2.fileName));
            if (result != 0)
                return result;

            return Util.getNameFromFilename(object1.fileName).compareToIgnoreCase(
                    Util.getNameFromFilename(object2.fileName));
        }
    };
}
