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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class TextInputDialog extends AlertDialog {
    private String mInputText;
    private String mTitle;
    private String mMsg;
    private OnFinishListener mListener;
    private Context mContext;
    private View mView;
    private EditText mFolderName;

    public interface OnFinishListener {
        // return true to accept and dismiss, false reject
        boolean onFinish(String text);
    }

    public TextInputDialog(Context context, String title, String msg, String text, OnFinishListener listener) {
        super(context);
        mTitle = title;
        mMsg = msg;
        mListener = listener;
        mInputText = text;
        mContext = context;
    }

    public String getInputText() {
        return mInputText;
    }

    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.textinput_dialog, null);

        setTitle(mTitle);
        setMessage(mMsg);

        mFolderName = (EditText) mView.findViewById(R.id.text);
        mFolderName.setText(mInputText);

        setView(mView);
        setButton(BUTTON_POSITIVE, mContext.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == BUTTON_POSITIVE) {
                            mInputText = mFolderName.getText().toString();
                            if (mListener.onFinish(mInputText)) {
                                dismiss();
                            }
                        }
                    }
                });
        setButton(BUTTON_NEGATIVE, mContext.getString(android.R.string.cancel),
                (DialogInterface.OnClickListener) null);

        super.onCreate(savedInstanceState);
    }
}
