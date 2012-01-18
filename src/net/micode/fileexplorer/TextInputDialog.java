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
