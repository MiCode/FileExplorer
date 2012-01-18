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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FavoriteDatabaseHelper extends SQLiteOpenHelper {

    private final static String DATABASE_NAME = "file_explorer";

    private final static int DATABASE_VERSION = 1;

    private final static String TABLE_NAME = "favorite";

    public final static String FIELD_ID = "_id";

    public final static String FIELD_TITLE = "title";

    public final static String FIELD_LOCATION = "location";

    private boolean firstCreate;

    private FavoriteDatabaseListener mListener;

    private static FavoriteDatabaseHelper instance;

    public interface FavoriteDatabaseListener {
        void onFavoriteDatabaseChanged();
    }

    public FavoriteDatabaseHelper(Context context, FavoriteDatabaseListener listener) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        instance = this;
        mListener = listener;
    }

    public static FavoriteDatabaseHelper getInstance() {
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "Create table " + TABLE_NAME + "(" + FIELD_ID + " integer primary key autoincrement,"
                + FIELD_TITLE + " text, " + FIELD_LOCATION + " text );";
        db.execSQL(sql);
        firstCreate = true;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = " DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }

    public boolean isFirstCreate() {
        return firstCreate;
    }

    public boolean isFavorite(String path) {
        String selection = FIELD_LOCATION + "=?";
        String[] selectionArgs = new String[] {
            path
        };
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null);
        if (cursor == null)
            return false;
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }

    public Cursor query() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        return cursor;
    }

    public long insert(String title, String location) {
        if (isFavorite(location))
            return -1;

        SQLiteDatabase db = this.getWritableDatabase();
        long ret = db.insert(TABLE_NAME, null, createValues(title, location));
        mListener.onFavoriteDatabaseChanged();
        return ret;
    }

    public void delete(long id, boolean notify) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = FIELD_ID + "=?";
        String[] whereValue = {
            Long.toString(id)
        };
        db.delete(TABLE_NAME, where, whereValue);

        if (notify)
            mListener.onFavoriteDatabaseChanged();
    }

    public void delete(String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = FIELD_LOCATION + "=?";
        String[] whereValue = {
            location
        };
        db.delete(TABLE_NAME, where, whereValue);
        mListener.onFavoriteDatabaseChanged();
    }

    public void update(int id, String title, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = FIELD_ID + "=?";
        String[] whereValue = {
            Integer.toString(id)
        };
        db.update(TABLE_NAME, createValues(title, location), where, whereValue);
        mListener.onFavoriteDatabaseChanged();
    }

    private ContentValues createValues(String title, String location) {
        ContentValues cv = new ContentValues();
        cv.put(FIELD_TITLE, title);
        cv.put(FIELD_LOCATION, location);
        return cv;
    }
}
