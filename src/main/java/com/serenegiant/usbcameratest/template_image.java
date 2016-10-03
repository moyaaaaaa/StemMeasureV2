package com.serenegiant.usbcameratest;

/**
 * Created by Tomoya on 16/05/13.
 */

import org.opencv.core.*;
import org.opencv.android.*;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Spinner;

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
    }


    public void init(Context context){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false; //リサイズしないで読み込む

        //茎テンプレート画像読み込み
        switch (spinner1.getSelectedItemPosition()){
            case 0:
                stemTemplateName = "stemtemplate5_1200px";
                break;
            case 1:
                stemTemplateName = "stemtemplate6_1200px";
                break;
            case 2:
                stemTemplateName = "stemtemplate7_1000px";
                break;
            case 3:
                stemTemplateName = "stemtemplate8_1000px";
                break;
        }
        int imageID = context.getResources().getIdentifier(stemTemplateName, "drawable", context.getPackageName());
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageID, options);
        stemImg = new Mat(bitmap.getWidth(), bitmap.getHeight(), CvType.CV_32F);
        Utils.bitmapToMat(bitmap, stemImg);
    }


    public void saveImg(final String filename, Mat saveImage)
    {
        //Bitmapを作成
        Bitmap saveBitmap = Bitmap.createBitmap(saveImage.width(), saveImage.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(saveImage, saveBitmap); //Mat型からBitmap型に変換
        OutputStream os = null;
        try{
            os = this.openFileOutput(filename, MODE_PRIVATE); //保存
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public Mat roadImg(final String filename)
    {
        Mat roadImage = new Mat();

        int imageID = context.getResources().getIdentifier(filename, "drawable", context.getPackageName()); //ファイル名からIDを取得
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), imageID);
        Utils.bitmapToMat(bitmap, roadImage);

        return roadImage;
    }

}
