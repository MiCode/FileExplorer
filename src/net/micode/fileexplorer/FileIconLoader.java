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

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import net.micode.fileexplorer.FileCategoryHelper.FileCategory;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Handler.Callback;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Files.FileColumns;
import android.util.Log;
import android.widget.ImageView;

/**
 * Asynchronously loads file icons and thumbnail, mostly single-threaded.
 */
public class FileIconLoader implements Callback {

    private static final String LOADER_THREAD_NAME = "FileIconLoader";

    /**
     * Type of message sent by the UI thread to itself to indicate that some
     * photos need to be loaded.
     */
    private static final int MESSAGE_REQUEST_LOADING = 1;

    /**
     * Type of message sent by the loader thread to indicate that some photos
     * have been loaded.
     */
    private static final int MESSAGE_ICON_LOADED = 2;

    private static abstract class ImageHolder {
        public static final int NEEDED = 0;

        public static final int LOADING = 1;

        public static final int LOADED = 2;

        int state;

        public static ImageHolder create(FileCategory cate) {
            switch (cate) {
                case Apk:
                    return new DrawableHolder();
                case Picture:
                case Video:
                    return new BitmapHolder();
            }

            return null;
        };

        public abstract boolean setImageView(ImageView v);

        public abstract boolean isNull();

        public abstract void setImage(Object image);
    }

    private static class BitmapHolder extends ImageHolder {
        SoftReference<Bitmap> bitmapRef;

        @Override
        public boolean setImageView(ImageView v) {
            if (bitmapRef.get() == null)
                return false;
            v.setImageBitmap(bitmapRef.get());
            return true;
        }

        @Override
        public boolean isNull() {
            return bitmapRef == null;
        }

        @Override
        public void setImage(Object image) {
            bitmapRef = image == null ? null : new SoftReference<Bitmap>((Bitmap) image);
        }
    }

    private static class DrawableHolder extends ImageHolder {
        SoftReference<Drawable> drawableRef;

        @Override
        public boolean setImageView(ImageView v) {
            if (drawableRef.get() == null)
                return false;

            v.setImageDrawable(drawableRef.get());
            return true;
        }

        @Override
        public boolean isNull() {
            return drawableRef == null;
        }

        @Override
        public void setImage(Object image) {
            drawableRef = image == null ? null : new SoftReference<Drawable>((Drawable) image);
        }
    }

    /**
     * A soft cache for image thumbnails. the key is file path
     */
    private final static ConcurrentHashMap<String, ImageHolder> mImageCache = new ConcurrentHashMap<String, ImageHolder>();

    /**
     * A map from ImageView to the corresponding photo ID. Please note that this
     * photo ID may change before the photo loading request is started.
     */
    private final ConcurrentHashMap<ImageView, FileId> mPendingRequests = new ConcurrentHashMap<ImageView, FileId>();

    /**
     * Handler for messages sent to the UI thread.
     */
    private final Handler mMainThreadHandler = new Handler(this);

    /**
     * Thread responsible for loading photos from the database. Created upon the
     * first request.
     */
    private LoaderThread mLoaderThread;

    /**
     * A gate to make sure we only send one instance of MESSAGE_PHOTOS_NEEDED at
     * a time.
     */
    private boolean mLoadingRequested;

    /**
     * Flag indicating if the image loading is paused.
     */
    private boolean mPaused;

    private final Context mContext;

    private IconLoadFinishListener iconLoadListener;

    /**
     * Constructor.
     *
     * @param context content context
     */
    public FileIconLoader(Context context, IconLoadFinishListener l) {
        mContext = context;
        iconLoadListener = l;
    }

    public static class FileId {
        public String mPath;

        public long mId; // database id

        public FileCategory mCategory;

        public FileId(String path, long id, FileCategory cate) {
            mPath = path;
            mId = id;
            mCategory = cate;
        }
    }

    public abstract static interface IconLoadFinishListener {
        void onIconLoadFinished(ImageView view);
    }

    /**
     * Load photo into the supplied image view. If the photo is already cached,
     * it is displayed immediately. Otherwise a request is sent to load the
     * photo from the database.
     *
     * @param id, database id
     */
    public boolean loadIcon(ImageView view, String path, long id, FileCategory cate) {
        boolean loaded = loadCachedIcon(view, path, cate);
        if (loaded) {
            mPendingRequests.remove(view);
        } else {
            FileId p = new FileId(path, id, cate);
            mPendingRequests.put(view, p);
            if (!mPaused) {
                // Send a request to start loading photos
                requestLoading();
            }
        }
        return loaded;
    }

    public void cancelRequest(ImageView view) {
        mPendingRequests.remove(view);
    }

    /**
     * Checks if the photo is present in cache. If so, sets the photo on the
     * view, otherwise sets the state of the photo to
     * {@link BitmapHolder#NEEDED}
     */
    private boolean loadCachedIcon(ImageView view, String path, FileCategory cate) {
        ImageHolder holder = mImageCache.get(path);
        
        if (holder == null) {
            holder = ImageHolder.create(cate);
            if (holder == null)
                return false;

            mImageCache.put(path, holder);
        } else if (holder.state == ImageHolder.LOADED) {
            if (holder.isNull()) {
                return true;
            }

            // failing to set imageview means that the soft reference was
            // released by the GC, we need to reload the photo.
            if (holder.setImageView(view)) {
                return true;
            }
        }

        holder.state = ImageHolder.NEEDED;
        return false;
    }

    public long getDbId(String path, boolean isVideo) {
        String volumeName = "external";
        Uri uri = isVideo ? Video.Media.getContentUri(volumeName) : Images.Media.getContentUri(volumeName);
        String selection = FileColumns.DATA + "=?";
        ;
        String[] selectionArgs = new String[] {
            path
        };

        String[] columns = new String[] {
                FileColumns._ID, FileColumns.DATA
        };

        Cursor c = mContext.getContentResolver()
                .query(uri, columns, selection, selectionArgs, null);
        if (c == null) {
            return 0;
        }
        long id = 0;
        if (c.moveToNext()) {
            id = c.getLong(0);
        }
        c.close();
        return id;
    }

    /**
     * Stops loading images, kills the image loader thread and clears all
     * caches.
     */
    public void stop() {
        pause();

        if (mLoaderThread != null) {
            mLoaderThread.quit();
            mLoaderThread = null;
        }

        clear();
    }

    public void clear() {
        mPendingRequests.clear();
        mImageCache.clear();
    }

    /**
     * Temporarily stops loading
     */
    public void pause() {
        mPaused = true;
    }

    /**
     * Resumes loading
     */
    public void resume() {
        mPaused = false;
        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    /**
     * Sends a message to this thread itself to start loading images. If the
     * current view contains multiple image views, all of those image views will
     * get a chance to request their respective photos before any of those
     * requests are executed. This allows us to load images in bulk.
     */
    private void requestLoading() {
        if (!mLoadingRequested) {
            mLoadingRequested = true;
            mMainThreadHandler.sendEmptyMessage(MESSAGE_REQUEST_LOADING);
        }
    }

    /**
     * Processes requests on the main thread.
     */
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_REQUEST_LOADING: {
                mLoadingRequested = false;
                if (!mPaused) {
                    if (mLoaderThread == null) {
                        mLoaderThread = new LoaderThread();
                        mLoaderThread.start();
                    }

                    mLoaderThread.requestLoading();
                }
                return true;
            }

            case MESSAGE_ICON_LOADED: {
                if (!mPaused) {
                    processLoadedIcons();
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Goes over pending loading requests and displays loaded photos. If some of
     * the photos still haven't been loaded, sends another request for image
     * loading.
     */
    private void processLoadedIcons() {
        Iterator<ImageView> iterator = mPendingRequests.keySet().iterator();
        while (iterator.hasNext()) {
            ImageView view = iterator.next();
            FileId fileId = mPendingRequests.get(view);
            boolean loaded = loadCachedIcon(view, fileId.mPath, fileId.mCategory);
            if (loaded) {
                iterator.remove();
                iconLoadListener.onIconLoadFinished(view);
            }
        }

        if (!mPendingRequests.isEmpty()) {
            requestLoading();
        }
    }

    /**
     * The thread that performs loading of photos from the database.
     */
    private class LoaderThread extends HandlerThread implements Callback {
        private Handler mLoaderThreadHandler;

        public LoaderThread() {
            super(LOADER_THREAD_NAME);
        }

        /**
         * Sends a message to this thread to load requested photos.
         */
        public void requestLoading() {
            if (mLoaderThreadHandler == null) {
                mLoaderThreadHandler = new Handler(getLooper(), this);
            }
            mLoaderThreadHandler.sendEmptyMessage(0);
        }

        /**
         * Receives the above message, loads photos and then sends a message to
         * the main thread to process them.
         */
        public boolean handleMessage(Message msg) {
            Iterator<FileId> iterator = mPendingRequests.values().iterator();
            while (iterator.hasNext()) {
                FileId id = iterator.next();
                ImageHolder holder = mImageCache.get(id.mPath);
                if (holder != null && holder.state == ImageHolder.NEEDED) {
                    // Assuming atomic behavior
                    holder.state = ImageHolder.LOADING;
                    switch (id.mCategory) {
                        case Apk:
                            Drawable icon = Util.getApkIcon(mContext, id.mPath);
                            holder.setImage(icon);
                            break;
                        case Picture:
                        case Video:
                            boolean isVideo = id.mCategory == FileCategory.Video;
                            if (id.mId == 0)
                                id.mId = getDbId(id.mPath, isVideo);
                            if (id.mId == 0) {
                                Log.e("FileIconLoader", "Fail to get dababase id for:" + id.mPath);
                            }
                            holder.setImage(isVideo ? getVideoThumbnail(id.mId) : getImageThumbnail(id.mId));
                            break;
                    }

                    holder.state = BitmapHolder.LOADED;
                    mImageCache.put(id.mPath, holder);
                }
            }

            mMainThreadHandler.sendEmptyMessage(MESSAGE_ICON_LOADED);
            return true;
        }

        private static final int MICRO_KIND = 3;

        private Bitmap getImageThumbnail(long id) {
            return Images.Thumbnails.getThumbnail(mContext.getContentResolver(), id, MICRO_KIND, null);
        }

        private Bitmap getVideoThumbnail(long id) {
            return Video.Thumbnails.getThumbnail(mContext.getContentResolver(), id, MICRO_KIND, null);
        }
    }
}
