package com.serenegiant.usbcameratest;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * 計測データを保存する．
 *
 * Created by Tomoya on 16/10/12.
 */
public class measurement_data {
    private final Context context;
    private static final String MEASUREMENT_DATA_FILE_NAME = "measurement_data.csv";

    public measurement_data(Context context) {
        this.context = context;
    }

    public boolean addData(String data) {
        try{
            File dataDir = context.getFilesDir();
            File configFilePath = new File(dataDir, MEASUREMENT_DATA_FILE_NAME);
            OutputStream outputStream = new FileOutputStream(configFilePath, true);
            final String BR = System.getProperty("line.separator"); //改行コード取得
            outputStream.write((data.toString() + BR).getBytes());
            outputStream.flush();
            outputStream.close();
            return true;
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }
}
