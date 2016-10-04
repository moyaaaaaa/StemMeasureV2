package com.serenegiant.usbcameratest;

/**
 * Created by Tomoya on 16/05/13.
 */

import org.opencv.core.*;
import org.opencv.android.*;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.*;
import java.lang.reflect.Field;


public class template_image extends Activity{
    public final Context context;
    public final Spinner spinner1;
    public String stemTemplateName = "stemtemplate5_1200px";
    public final String baseTemplateName = "basetemplate2_1000px";
    public final String captureFileName = "capture";
    public final String resultFileName = "result";

    public Mat stemImg;
    public Mat baseImg;
    public Mat captureImg;
    public Mat frame;

    //自作テンプレート画像を保存するパス
    public String TEMPLATE_IMAGE_PATH;
    public final String TEMPLATE_IMAGE_NAME = "CreatedTemplateImage.bmp";
    public File templateImagePath;

    public template_image(Context context){
        this.context = context;
        spinner1 = (Spinner)((com.serenegiant.usbcameratest.MainActivity) context).findViewById(R.id.spinner1);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; //リサイズしないで読み込む

        //茎テンプレート画像読み込み
        int imageID = context.getResources().getIdentifier(stemTemplateName, "drawable", context.getPackageName());
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageID, options);
        stemImg = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_32F);
        Utils.bitmapToMat(bitmap, stemImg);

        //比較対象テンプレート画像読み込み
        imageID = context.getResources().getIdentifier(baseTemplateName, "drawable", context.getPackageName());
        bitmap = BitmapFactory.decodeResource(context.getResources(), imageID, options);
        baseImg = new Mat();
        Utils.bitmapToMat(bitmap, baseImg);

        imageID = context.getResources().getIdentifier(captureFileName, "drawable", context.getPackageName());
        bitmap = BitmapFactory.decodeResource(context.getResources(), imageID, options);
        captureImg = new Mat();
        Utils.bitmapToMat(bitmap, captureImg);

        TEMPLATE_IMAGE_PATH = context.getFilesDir().toString();
    }


    public void init(Context context){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; //リサイズしないで読み込む

        //茎テンプレート画像読み込み
        switch (spinner1.getSelectedItemPosition()){
            case 0:
                //stemTemplateName = "stemtemplate5_1200px";
                stemImg = roadImg("stemtemplate5_1200px");
                break;
            case 1:
                //stemTemplateName = "stemtemplate6_1200px";
                stemImg = roadImg("stemtemplate6_1200px");
                break;
            case 2:
                //stemTemplateName = "stemtemplate7_1000px";
                stemImg = roadImg("stemtemplate7_1000px");
                break;
            case 3:
                //stemTemplateName = "stemtemplate8_1000px";
                stemImg = roadImg("stemtemplate8_1000px");
                break;
            case 4:
                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                Cursor cursor = context.getContentResolver().query(uri, null, "_display_name LIKE ?" ,new String[] { TEMPLATE_IMAGE_NAME } , "_ID DESC");
                cursor.moveToFirst();
                long id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
                Bitmap bitmap = BitmapFactory.decodeFile(TEMPLATE_IMAGE_PATH + "/" + TEMPLATE_IMAGE_NAME, options);
                stemImg = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_32F);
                Utils.bitmapToMat(bitmap, stemImg);
                break;
        }
        /*
        int imageID = context.getResources().getIdentifier(stemTemplateName, "drawable", context.getPackageName());
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageID, options);
        stemImg = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_32F);
        Utils.bitmapToMat(bitmap, stemImg);
        */
    }


    public void saveImg(final String filename, Mat saveImage)
    {
        //Bitmapを作成
        Bitmap saveBitmap = Bitmap.createBitmap(saveImage.width(), saveImage.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(saveImage, saveBitmap); //Mat型からBitmap型に変換
        try{
            File dataDir = new File(Environment.getExternalStorageDirectory(), "sampleDir");
            dataDir.mkdirs();
            File filePath = new File(dataDir, filename);
            OutputStream outputStream = new FileOutputStream(filePath);
            saveBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

            //DB登録
            ContentValues values = new ContentValues();
            ContentResolver contentResolver = getContentResolver();
            values.put(MediaStore.Images.Media.TITLE, filename);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE,"image/bmp");
            values.put(MediaStore.Images.Media.DATA, TEMPLATE_IMAGE_PATH + filename);
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void saveImg(Mat saveImage)
    {
        //Bitmapを作成
        Bitmap saveBitmap = Bitmap.createBitmap(saveImage.width(), saveImage.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(saveImage, saveBitmap); //Mat型からBitmap型に変換
        try{
            //File dataDir = new File(Environment.getExternalStorageDirectory(), "sampleDir");
            File dataDir = context.getFilesDir();
            //dataDir.mkdirs();
            templateImagePath = new File(dataDir, TEMPLATE_IMAGE_NAME);
            OutputStream outputStream = new FileOutputStream(templateImagePath);
            saveBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

            //DB登録
            ContentValues values = new ContentValues();
            ContentResolver contentResolver = context.getContentResolver();
            values.put(MediaStore.Images.Media.TITLE, TEMPLATE_IMAGE_NAME);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, TEMPLATE_IMAGE_NAME);
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.Images.Media.MIME_TYPE,"image/bmp");
            values.put(MediaStore.Images.Media.DATA, context.getFilesDir().toString() + "/" + TEMPLATE_IMAGE_NAME);
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();

        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public Mat roadImg(final String filename)
    {
        Mat roadImage = new Mat();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; //リサイズしないで読み込む

        int imageID = context.getResources().getIdentifier(filename, "drawable", context.getPackageName());
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageID, options);
        roadImage = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_32F);
        Utils.bitmapToMat(bitmap, roadImage);

        return roadImage;
    }


}
