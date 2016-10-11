package com.serenegiant.usbcameratest;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import org.opencv.android.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by Tomoya on 16/10/06.
 */
public class calibration_config {
    private final Context context;
    private static final String CALIBRATION_CONFIG_FILE_NAME = "calibration_config.txt";

    public calibration_config(Context context) {
        this.context = context;
    }

    public boolean saveCalibrationConfig(Integer baseDistance_px) {
        try{
            File dataDir = context.getFilesDir();
            File configFilePath = new File(dataDir, CALIBRATION_CONFIG_FILE_NAME);
            OutputStream outputStream = new FileOutputStream(configFilePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(baseDistance_px.toString());
            outputStreamWriter.flush();
            outputStreamWriter.close();

            /*
            //DB登録
            ContentValues values = new ContentValues();
            ContentResolver contentResolver = context.getContentResolver();
            values.put(MediaStore.Images.Media.TITLE, TEMPLATE_IMAGE_NAME);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, TEMPLATE_IMAGE_NAME);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE,"text/plain");
            values.put(MediaStore.Images.Media.DATA, context.getFilesDir().toString() + "/" + TEMPLATE_IMAGE_NAME);
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();
*/
            return true;
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
    }

    public int loadCalibrationConfig() {
        try {
            //ファイルオープン
            File dataDir = context.getFilesDir();
            File configFilePath = new File(dataDir, CALIBRATION_CONFIG_FILE_NAME);
            InputStream inputStream = new FileInputStream(configFilePath);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            //ファイル読み込み
            String line;
            String lineSeparator = System.getProperty("line.separator");
            line = bufferedReader.readLine();
            StringBuffer config = new StringBuffer();
            config.append(line);

            //空文字チェック
            if(config.toString().isEmpty()) {
                return -1;
            }

            try{
                return Integer.parseInt(config.toString());
            }catch (NumberFormatException e){
                return -1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
