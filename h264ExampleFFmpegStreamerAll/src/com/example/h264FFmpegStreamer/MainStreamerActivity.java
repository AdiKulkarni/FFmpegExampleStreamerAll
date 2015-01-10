package com.example.h264FFmpegStreamer;

import java.io.IOException;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.example.h264codecstreamer.R;

public class MainStreamerActivity extends Activity implements
		SurfaceHolder.Callback {

	private static final String TAG = "com.example.h264FFmpegStreamer.MainActivity";

	private Camera camera;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private static boolean previewing = false;

	public static int frameRate = 60;
	public static int width = 640;
	public static int height = 480;
	public static int bitrate = 3000000;
	public static int maxBFrames = 0;
	public static int gopSize = 1;
	private int mCount = 0;

	private AvcEncoder avcEncode = new AvcEncoder();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button buttonStartCameraPreview = (Button) findViewById(R.id.startcamerapreview);
		Button buttonStopCameraPreview = (Button) findViewById(R.id.stopcamerapreview);

		getWindow().setFormat(PixelFormat.UNKNOWN);
		surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		buttonStartCameraPreview
				.setOnClickListener(new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub

						if (!previewing) {
							camera = Camera.open();
							if (camera != null) {
								try {

									Parameters parameters = camera
											.getParameters();
									parameters.setPreviewSize(width, height);
									parameters
											.setPreviewFormat(ImageFormat.YV12);
									//parameters.setPreviewFpsRange(4000, 60000);
									parameters.setPreviewFrameRate(frameRate);
									parameters
											.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
									camera.setParameters(parameters);
									avcEncode.setFFmpegEncoder(width, height,
											frameRate, bitrate, maxBFrames,
											gopSize);
									camera.setPreviewDisplay(surfaceHolder);
									camera.setDisplayOrientation(90);
									camera.setPreviewCallback(new Camera.PreviewCallback() {
										long now = System.nanoTime() / 1000,
												oldnow = now;

										@Override
										public void onPreviewFrame(
												byte[] bytes, Camera camera) {

											avcEncode
													.encodeFrame(bytes, mCount);
											
											oldnow = now;
											now = System.nanoTime() / 1000;

											Log.d("Frames",  "frame: " + mCount + " fps: "
													+ 1000000L / (now - oldnow) + " time: " + (now - oldnow)/1000);
											mCount++;

										}
									});

									camera.startPreview();
									previewing = true;

								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				});

		buttonStopCameraPreview
				.setOnClickListener(new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub

						if (camera != null && previewing) {
							camera.stopPreview();
							camera.release();
							camera = null;
							avcEncode.close();
							previewing = false;
						}
						finish();
					}
				});
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

	public static boolean getPreviewStatus() {
		return previewing;
	}
}