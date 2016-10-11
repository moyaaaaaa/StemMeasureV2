package com.serenegiant.usbcameratest;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Tomoya on 16/05/13.
 */


public class stem_measure {
    public final Context context;
    public final TextView textView1;

    public final double pitch = 0.95;
    public final int minLimitSize = 50;
    public final double Base_mm = 5;
    public int stemDistance_px, baseDistance_px; //（茎/比較対象）の幅[px]
    public double stemDistance_mm, baseDistance_mm; //（茎/比較対象）の幅[mm]
    public Rect stemRect, baseRect;

    public stem_measure(Context context){ //コンストラクタ
        this.context = context;
        textView1 = (TextView)((com.serenegiant.usbcameratest.MainActivity) context).findViewById(R.id.textView1);

        baseDistance_mm = Base_mm;
        baseDistance_px = new calibration_config(context).loadCalibrationConfig();
        if(baseDistance_px == -1) {
            baseDistance_px = 0;
            Toast.makeText(context, "load error!", Toast.LENGTH_SHORT).show();
        }
    }

    public void calibration(){ //比較対象のピクセル数を求めるラッパー関数

    }
    public void stemMeasure() { //比較対象を使って茎径を求めるラッパー関数

    }

    public Rect templateMatching(Mat srcImg, Mat templateImg){
        Mat searchImg = srcImg.clone(); //走査対象
        Rect searchRect; //走査範囲
        Mat prevResultImg, resultImg = srcImg.clone(); //スコア画像
        double prevScore, score; //マッチングのスコア
        Rect detectRect = new Rect(0, 0, templateImg.cols(), templateImg.rows()); //マッチングで見つけた矩形領域
        Rect prevDetectRect;
        Point prevPt, pt = new Point(); //見つけた矩形領域の左上の点座標
        Mat resizedTmpImg = templateImg.clone(); //縮小したテンプレート画像
        Mat cropImg = searchImg.clone(); //切り抜いた走査対象

        //テンプレートマッチング
        Imgproc.matchTemplate(searchImg, templateImg, resultImg, Imgproc.TM_CCORR_NORMED);

        //最大のスコアの場所を探す
        Core.MinMaxLocResult maxr = Core.minMaxLoc(resultImg);
        pt.x = maxr.maxLoc.x;
        pt.y = maxr.maxLoc.y;
        detectRect.x = (int)pt.x;
        detectRect.y = (int)pt.y;
        score = maxr.maxVal;


        //走査画像切り出し
        srcImg.submat(detectRect).copyTo(searchImg);
        searchRect = detectRect.clone(); //走査範囲指定
        do{
            //テンプレート縮小
            Imgproc.resize(templateImg, resizedTmpImg, new Size(resizedTmpImg.width()*pitch, resizedTmpImg.height()*pitch), pitch, pitch, Imgproc.INTER_AREA);

            //テンプレートマッチング
            prevResultImg = resultImg.clone();
            Imgproc.matchTemplate(searchImg, resizedTmpImg, resultImg, Imgproc.TM_CCORR_NORMED);

            //最大のスコアの場所を探す
            prevScore = score;
            prevPt = pt.clone();
            maxr = Core.minMaxLoc(resultImg);
            pt.x = maxr.maxLoc.x; //最大スコアの座標更新
            pt.y = maxr.maxLoc.y;
            score = maxr.maxVal; //最大スコアを更新
            prevDetectRect = detectRect.clone();
            detectRect.x = searchRect.x + (int)pt.x; //走査開始座標＋見つかった座標
            detectRect.y = searchRect.y + (int)pt.y;
            detectRect.width = resizedTmpImg.cols();
            detectRect.height = resizedTmpImg.rows();
        }while(score > prevScore && resizedTmpImg.rows() > minLimitSize);
  /*
    一個前の相関値の方が高くて良くなる見込みがないか
    テンプレート画像が小さくなりすぎたらやめる
   */

        //デバッグ用描画
        Mat drawImg = srcImg.clone();
        Imgproc.rectangle(drawImg, prevDetectRect.tl(), prevDetectRect.br(), new Scalar(0,0,255), 3);
        //tmpImg.saveImg(tmpImg.captureFileName, drawImg);

        return prevDetectRect;
    }

    public double pixelToMillimeter(double _stemDistance_px,
                                    double _baseDistance_px,
                                    double _baseDistance_mm){
        double stemDistance_mm;

        stemDistance_mm = (_stemDistance_px * _baseDistance_mm) / _baseDistance_px;

        return stemDistance_mm;
    }
}
