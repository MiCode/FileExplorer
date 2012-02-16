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

import android.mtp.MtpConstants;

import java.util.HashMap;

public class MediaFile {

    // Audio file types
    public static final int FILE_TYPE_MP3 = 1;
    public static final int FILE_TYPE_M4A = 2;
    public static final int FILE_TYPE_WAV = 3;
    public static final int FILE_TYPE_AMR = 4;
    public static final int FILE_TYPE_AWB = 5;
    public static final int FILE_TYPE_WMA = 6;
    public static final int FILE_TYPE_OGG = 7;
    public static final int FILE_TYPE_AAC = 8;
    public static final int FILE_TYPE_MKA = 9;
    public static final int FILE_TYPE_FLAC = 10;
    private static final int FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3;
    private static final int LAST_AUDIO_FILE_TYPE = FILE_TYPE_FLAC;

    // MIDI file types
    public static final int FILE_TYPE_MID = 11;
    public static final int FILE_TYPE_SMF = 12;
    public static final int FILE_TYPE_IMY = 13;
    private static final int FIRST_MIDI_FILE_TYPE = FILE_TYPE_MID;
    private static final int LAST_MIDI_FILE_TYPE = FILE_TYPE_IMY;

    // Video file types
    public static final int FILE_TYPE_MP4 = 21;
    public static final int FILE_TYPE_M4V = 22;
    public static final int FILE_TYPE_3GPP = 23;
    public static final int FILE_TYPE_3GPP2 = 24;
    public static final int FILE_TYPE_WMV = 25;
    public static final int FILE_TYPE_ASF = 26;
    public static final int FILE_TYPE_MKV = 27;
    public static final int FILE_TYPE_MP2TS = 28;
    public static final int FILE_TYPE_AVI = 29;
    public static final int FILE_TYPE_WEBM = 30;
    private static final int FIRST_VIDEO_FILE_TYPE = FILE_TYPE_MP4;
    private static final int LAST_VIDEO_FILE_TYPE = FILE_TYPE_WEBM;

    // Image file types
    public static final int FILE_TYPE_JPEG = 31;
    public static final int FILE_TYPE_GIF = 32;
    public static final int FILE_TYPE_PNG = 33;
    public static final int FILE_TYPE_BMP = 34;
    public static final int FILE_TYPE_WBMP = 35;
    public static final int FILE_TYPE_WEBP = 36;
    private static final int FIRST_IMAGE_FILE_TYPE = FILE_TYPE_JPEG;
    private static final int LAST_IMAGE_FILE_TYPE = FILE_TYPE_WEBP;

    public static class MediaFileType {
        public final int fileType;
        public final String mimeType;

        MediaFileType(int fileType, String mimeType) {
            this.fileType = fileType;
            this.mimeType = mimeType;
        }
    }

    private static HashMap<String, MediaFileType> sFileTypeMap = new HashMap<String, MediaFileType>();
    private static HashMap<String, Integer> sMimeTypeMap = new HashMap<String, Integer>();

    static void addFileType(String extension, int fileType, String mimeType) {
        sFileTypeMap.put(extension, new MediaFileType(fileType, mimeType));
        sMimeTypeMap.put(mimeType, Integer.valueOf(fileType));
    }

    static void addFileType(String extension, int fileType, String mimeType, int mtpFormatCode) {
        addFileType(extension, fileType, mimeType);
    }

    static {
        addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg", MtpConstants.FORMAT_MP3);
        addFileType("M4A", FILE_TYPE_M4A, "audio/mp4", MtpConstants.FORMAT_MPEG);
        addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav", MtpConstants.FORMAT_WAV);
        addFileType("AMR", FILE_TYPE_AMR, "audio/amr");
        addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb");
        addFileType("OGG", FILE_TYPE_OGG, "application/ogg", MtpConstants.FORMAT_OGG);
        addFileType("OGA", FILE_TYPE_OGG, "application/ogg", MtpConstants.FORMAT_OGG);
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac", MtpConstants.FORMAT_AAC);
        addFileType("AAC", FILE_TYPE_AAC, "audio/aac-adts", MtpConstants.FORMAT_AAC);
        addFileType("MKA", FILE_TYPE_MKA, "audio/x-matroska");

        addFileType("MPEG", FILE_TYPE_MP4, "video/mpeg", MtpConstants.FORMAT_MPEG);
        addFileType("MPG", FILE_TYPE_MP4, "video/mpeg", MtpConstants.FORMAT_MPEG);
        addFileType("MP4", FILE_TYPE_MP4, "video/mp4", MtpConstants.FORMAT_MPEG);
        addFileType("M4V", FILE_TYPE_M4V, "video/mp4", MtpConstants.FORMAT_MPEG);
        addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("MKV", FILE_TYPE_MKV, "video/x-matroska");
        addFileType("WEBM", FILE_TYPE_WEBM, "video/webm");
        addFileType("TS", FILE_TYPE_MP2TS, "video/mp2ts");
        addFileType("AVI", FILE_TYPE_AVI, "video/avi");

        addFileType("JPG", FILE_TYPE_JPEG, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG);
        addFileType("JPEG", FILE_TYPE_JPEG, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG);
        addFileType("GIF", FILE_TYPE_GIF, "image/gif", MtpConstants.FORMAT_GIF);
        addFileType("PNG", FILE_TYPE_PNG, "image/png", MtpConstants.FORMAT_PNG);
        addFileType("BMP", FILE_TYPE_BMP, "image/x-ms-bmp", MtpConstants.FORMAT_BMP);
        addFileType("WBMP", FILE_TYPE_WBMP, "image/vnd.wap.wbmp");
        addFileType("WEBP", FILE_TYPE_WEBP, "image/webp");
    }

    public static boolean isAudioFileType(int fileType) {
        return ((fileType >= FIRST_AUDIO_FILE_TYPE && fileType <= LAST_AUDIO_FILE_TYPE) || (fileType >= FIRST_MIDI_FILE_TYPE && fileType <= LAST_MIDI_FILE_TYPE));
    }

    public static boolean isVideoFileType(int fileType) {
        return (fileType >= FIRST_VIDEO_FILE_TYPE && fileType <= LAST_VIDEO_FILE_TYPE);
    }

    public static boolean isImageFileType(int fileType) {
        return (fileType >= FIRST_IMAGE_FILE_TYPE && fileType <= LAST_IMAGE_FILE_TYPE);
    }

    public static MediaFileType getFileType(String path) {
        int lastDot = path.lastIndexOf(".");
        if (lastDot < 0)
            return null;
        return sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase());
    }
}
