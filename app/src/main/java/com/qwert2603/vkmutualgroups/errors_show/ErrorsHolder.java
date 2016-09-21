package com.qwert2603.vkmutualgroups.errors_show;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;

public class ErrorsHolder {

    private static final ErrorsHolder sErrorsHolder = new ErrorsHolder();

    private ErrorsHolder() {
    }

    public static ErrorsHolder get() {
        return sErrorsHolder;
    }

    private static final String TAG = "ErrorsHolder";

    private static final String FILE_NAME = "errors.txt";

    public void addError(Context context, Throwable throwable) {
        synchronized (ErrorsHolder.this) {
            File file = new File(context.getFilesDir(), FILE_NAME);
            long length = file.length();
            Log.d(TAG, "addError#length == " + length);
            if (length > 256 * 1024) {
                clearErrors(context);
            }
            try {
                FileOutputStream outputStream = context.openFileOutput(FILE_NAME, Context.MODE_APPEND);
                PrintWriter printWriter = new PrintWriter(outputStream, true);
                printWriter.write(new Date() + "\n");
                throwable.printStackTrace(printWriter);
                printWriter.write("\n\n");
                printWriter.flush();
                outputStream.close();
                Log.d(TAG, "addError#throwable == " + throwable);
            } catch (IOException ignored) {
                Log.d(TAG, ignored.toString());
            }
        }
    }

    public String getErrors(Context context) {
        Log.d(TAG, "getErrors");
        try {
            FileInputStream inputStream = context.openFileInput(FILE_NAME);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String s;
            while ((s = bufferedReader.readLine()) != null) {
                stringBuilder.append(s);
            }
            inputStream.close();
            Log.d(TAG, "getErrors#stringBuilder == " + stringBuilder.toString());
            return stringBuilder.toString();
        } catch (IOException ignored) {
            Log.d(TAG, ignored.toString());
            return null;
        }
    }

    public void clearErrors(Context context) {
        boolean deleteFile = context.deleteFile(FILE_NAME);
        Log.d(TAG, "clearErrors#deleteFile == " + deleteFile);
    }
}
