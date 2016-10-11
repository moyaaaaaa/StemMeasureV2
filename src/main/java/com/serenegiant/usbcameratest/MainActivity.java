package com.serenegiant.usbcameratest;
/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MainActivity.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IButtonCallback;
import com.serenegiant.usb.IStatusCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity implements CameraDialog.CameraDialogParent {
	static {
		System.loadLibrary("opencv_java3");
	}
	private static final int TEMPLATE_HEIGHT = 1000; //テンプレート画像の高さ[px]

    // for thread pool
    private static final int CORE_POOL_SIZE = 1;		// initial/minimum threads
    private static final int MAX_POOL_SIZE = 4;			// maximum threads
    private static final int KEEP_ALIVE_TIME = 10;		// time periods while keep the idle thread
    protected static final ThreadPoolExecutor EXECUTER
		= new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
			TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
	private UVCCamera mUVCCamera;
	private UVCCameraTextureView mUVCCameraView;
	// for open&start / stop&close camera preview
	private ImageButton imageButton1;
	private Button button1;
	private Button button2;
	private Button button3;
	private Button button4;
	private Spinner spinner1;
	private ImageView imageView1;
	private TextView textView1;
	private Surface mPreviewSurface;

	template_image tmpImg;
	Mat searchImg;
	Mat showImg;
	stem_measure measure;
	private boolean createTemplateFlag;
	private Rect templateRect;


	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		imageButton1 = (ImageButton)findViewById(R.id.imageButton1);
		button1 = (Button)findViewById(R.id.button1);
		button2 = (Button)findViewById(R.id.button2);
		button3 = (Button)findViewById(R.id.button3);
		button4 = (Button)findViewById(R.id.button4);
		imageView1 = (ImageView)findViewById(R.id.imageView1);
		imageButton1.setOnClickListener(imageButton1ClickListener);
		button1.setOnClickListener(button1ClickListener);
		button2.setOnClickListener(button2ClickListener);
		button3.setOnClickListener(button3ClickListener);
		button4.setOnClickListener(button4ClickListener);
		spinner1 = (Spinner)findViewById(R.id.spinner1);


		mUVCCameraView = (UVCCameraTextureView)findViewById(R.id.UVCCameraTextureView1);
		mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float)UVCCamera.DEFAULT_PREVIEW_HEIGHT);

		mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

		textView1 = (TextView)findViewById(R.id.textView1);

		tmpImg = new template_image(MainActivity.this);
		measure = new stem_measure(MainActivity.this);
		createTemplateFlag = false;
		templateRect = new Rect();
		templateRect.x = -1;
		templateRect.y = -1;

		textView1.setText("base_px:" + String.valueOf(measure.baseDistance_px) + "px");
	}

	@Override
	public void onResume() {
		super.onResume();
		mUSBMonitor.register();
		if (mUVCCamera != null)
			mUVCCamera.startPreview();
	}

	@Override
	public void onPause() {
		if (mUVCCamera != null)
			mUVCCamera.stopPreview();
		mUSBMonitor.unregister();
		super.onPause();
	}

	@Override
	public void onDestroy() {
		if (mUVCCamera != null) {
			mUVCCamera.destroy();
			mUVCCamera = null;
		}
		if (mUSBMonitor != null) {
			mUSBMonitor.destroy();
			mUSBMonitor = null;
		}
		mUVCCameraView = null;
		imageButton1 = null;
		super.onDestroy();
	}

	OnClickListener imageButton1ClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			if (mUVCCamera == null)
				CameraDialog.showDialog(MainActivity.this);
			else {
				mUVCCamera.destroy();
				mUVCCamera = null;
			}
		}
	};

	/**
	 * ボタンクリックでキャリブレーション処理
	 *
	 */
	OnClickListener button1ClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			//検索対象画像読み込み
			Bitmap tmp = mUVCCameraView.getBitmap();
			Mat searchImg = new Mat();
			Utils.bitmapToMat(tmp, searchImg);

			//キャリブレーションとか
			measure.calibration(searchImg, tmpImg.baseImg);
		}
	};


	OnClickListener button2ClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {

		}
	};

	OnClickListener button3ClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			imageView1.setVisibility(View.INVISIBLE); //ImageViewを非表示
		}
	};

	//テンプレート画像作成
	OnClickListener button4ClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			//矩形選択
			if(createTemplateFlag == false) {
				createTemplateFlag = true;
				button4.setText("Create!!");

				//検索対象画像取得
				Bitmap tmp = mUVCCameraView.getBitmap();
				searchImg = new Mat();
				Utils.bitmapToMat(tmp, searchImg);

				//画像表示
				showImg = searchImg.clone();
				Bitmap bitmap = Bitmap.createBitmap(showImg.width(), showImg.height(), Bitmap.Config.ARGB_8888);
				Utils.matToBitmap(showImg, bitmap);
				imageView1.setVisibility(View.VISIBLE);
				imageView1.setImageBitmap(bitmap);
			}else{ //選択終了
				createTemplateFlag = false;
				button4.setText("Create Template");

				//負のwidthをheightを正になるように変換
				if(templateRect.width < 0) {
					templateRect.width = -templateRect.width;
					templateRect.x = templateRect.x - templateRect.width;
				}
				if(templateRect.height < 0) {
					templateRect.height = -templateRect.height;
					templateRect.y = templateRect.y - templateRect.height;
				}

				//切り出し
				Mat templateImg = new Mat();
				searchImg.submat(templateRect).copyTo(showImg);
				showImg.copyTo(templateImg);

				//リサイズ
				double pitch = TEMPLATE_HEIGHT / showImg.height();
				Imgproc.resize(showImg, templateImg, new Size(showImg.width()*pitch, showImg.height()*pitch), pitch, pitch, Imgproc.INTER_AREA);

				//画像保存
				new template_image(MainActivity.this).saveImg(templateImg);

				//画像表示
				Bitmap bitmap = Bitmap.createBitmap(showImg.width(), showImg.height(), Bitmap.Config.ARGB_8888);
				Utils.matToBitmap(showImg, bitmap);
				imageView1.setVisibility(View.VISIBLE);
				imageView1.setImageBitmap(bitmap);

				templateRect.x = -1;
				templateRect.y = -1;
			}
		}
	};


	//タッチイベント処理
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		//テンプレートのトリミング開始
		if (createTemplateFlag) {
			if (templateRect.x == -1 || templateRect.y == -1) {
				templateRect.x = (int) e.getX();
				templateRect.y = (int) e.getY();
			} else {
				templateRect.width = (int) e.getX() - templateRect.x;
				templateRect.height = (int) e.getY() - templateRect.y;
			}
			textView1.setText("tl:" + templateRect.tl());

			//選択範囲描画
			showImg = searchImg.clone();
			Imgproc.rectangle(showImg, templateRect.tl(), templateRect.br(), new Scalar(0, 0, 255), 5);
			Bitmap bitmap = Bitmap.createBitmap(showImg.width(), showImg.height(), Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(showImg, bitmap);
			imageView1.setImageBitmap(bitmap);
		}

		return true;
	}

	private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
		@Override
		public void onAttach(final UsbDevice device) {
			Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
			if (mUVCCamera != null)
				mUVCCamera.destroy();
			mUVCCamera = new UVCCamera();
			EXECUTER.execute(new Runnable() {
				@Override
				public void run() {
					mUVCCamera.open(ctrlBlock);
					mUVCCamera.setStatusCallback(new IStatusCallback() {
						@Override
						public void onStatus(final int statusClass, final int event, final int selector,
											 final int statusAttribute, final ByteBuffer data) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(MainActivity.this, "onStatus(statusClass=" + statusClass
											+ "; " +
											"event=" + event + "; " +
											"selector=" + selector + "; " +
											"statusAttribute=" + statusAttribute + "; " +
											"data=...)", Toast.LENGTH_SHORT).show();
								}
							});

						}
					});
					mUVCCamera.setButtonCallback(new IButtonCallback() {
						@Override
						public void onButton(final int button, final int state) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									if(state == 1) {
										//検索対象画像取得
										Bitmap tmp = mUVCCameraView.getBitmap();
										Mat searchImg = new Mat();
										Utils.bitmapToMat(tmp, searchImg);

										//テンプレート画像再読み込み
										tmpImg.init(MainActivity.this);

										//計測とか
										Mat showImg = measure.stemMeasure(searchImg, tmpImg.stemImg);

										//表示
										Bitmap bitmap = Bitmap.createBitmap(showImg.width(), showImg.height(), Bitmap.Config.ARGB_8888);
										Utils.matToBitmap(showImg, bitmap);
										imageView1.setVisibility(View.VISIBLE);
										imageView1.setImageBitmap(bitmap);
									}
								}
							});
						}
					});
//					mUVCCamera.setPreviewTexture(mUVCCameraView.getSurfaceTexture());
					if (mPreviewSurface != null) {
						mPreviewSurface.release();
						mPreviewSurface = null;
					}
					try {
						mUVCCamera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
					} catch (final IllegalArgumentException e) {
						// fallback to YUV mode
						try {
							mUVCCamera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
						} catch (final IllegalArgumentException e1) {
							mUVCCamera.destroy();
							mUVCCamera = null;
						}
					}
					if (mUVCCamera != null) {
						final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
						if (st != null)
							mPreviewSurface = new Surface(st);
						mUVCCamera.setPreviewDisplay(mPreviewSurface);
//						mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGB565/*UVCCamera.PIXEL_FORMAT_NV21*/);
						mUVCCamera.startPreview();
					}
				}
			});
		}

		@Override
		public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
			// XXX you should check whether the comming device equal to camera device that currently using
			if (mUVCCamera != null) {
				mUVCCamera.close();
				if (mPreviewSurface != null) {
					mPreviewSurface.release();
					mPreviewSurface = null;
				}
			}
		}

		@Override
		public void onDettach(final UsbDevice device) {
			Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onCancel() {
		}
	};

	/**
	 * to access from CameraDialog
	 * @return
	 */
	@Override
	public USBMonitor getUSBMonitor() {
		return mUSBMonitor;
	}

	// if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
	// if you need to create Bitmap in IFrameCallback, please refer following snippet.
/*	final Bitmap bitmap = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
	private final IFrameCallback mIFrameCallback = new IFrameCallback() {
		@Override
		public void onFrame(final ByteBuffer frame) {
			frame.clear();
			synchronized (bitmap) {
				bitmap.copyPixelsFromBuffer(frame);
			}
			mImageView.post(mUpdateImageTask);
		}
	};
	
	private final Runnable mUpdateImageTask = new Runnable() {
		@Override
		public void run() {
			synchronized (bitmap) {
				mImageView.setImageBitmap(bitmap);
			}
		}
	}; */
}
