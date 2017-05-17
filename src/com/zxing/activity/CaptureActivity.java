package com.zxing.activity;

import java.io.IOException;
import java.util.Vector;

import com.bob.massabot.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.zxing.camera.CameraManager;
import com.zxing.decoding.CaptureActivityHandler;
import com.zxing.decoding.InactivityTimer;
import com.zxing.view.ViewfinderView;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.Toast;

/**
 * Initial the camera
 * 
 * @author Ryan.Tang
 */
public class CaptureActivity extends Activity implements Callback {

	private CaptureActivityHandler handler;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Vector<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private MediaPlayer mediaPlayer;
	private boolean playBeep;
	private static final float BEEP_VOLUME = 0.10f;
	private boolean vibrate;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.camera);
		// ViewUtil.addTopView(getApplicationContext(), this, R.string.scan_card);
		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		/**
		 * 提供一个专用的绘图面，嵌入在视图层次结构中。你可以控制这个表面的格式，它的大小； SurfaceView负责将面在屏幕上正确的位置显示。
		 * 
		 * 表面是Z序是窗口举行SurfaceView落后；SurfaceView打出了一个洞在它的窗口，让其表面显示。
		 * 视图层次结构将负责正确的合成与表面的任何兄弟SurfaceView通常会出现在它的上面
		 * 。这可以用来放置覆盖如表面上的按钮，但注意，这可能会对性能产生影响因为完整的alpha混合复合材料将每一次表面的变化进行。
		 * 
		 * 使表面可见的透明区域是基于视图层次结构中的布局位置的。如果布局后的变换属性用于在顶部的图形绘制一个兄弟视图，视图可能不正确的复合表面。
		 * 
		 * 访问底层的表面通过SurfaceHolder接口提供，这可以通过调用getholder()检索。
		 * 
		 * 表面将被创建为你而SurfaceView的窗口是可见的；你应该实现surfacecreated（SurfaceHolder）
		 * 和surfacedestroyed（SurfaceHolder）发现当表面被创建和销毁窗口的显示和隐藏。
		 * 
		 * 这个类的目的之一是提供一个表面，其中一个二级线程可以呈现到屏幕上。如果你要使用它，你需要知道一些线程的语义：
		 * 
		 * 所有的图形和SurfaceHolder。回调方法将从线程运行的窗口叫SurfaceView（通常是应用程序的主线程）。因此， 他们需要正确地与任何状态，也接触了绘图线程的同步。
		 * 
		 * 你必须确保拉丝只触及表面，底层是有效的——SurfaceHolder.lockCanvas。回调。surfacecreated() 和surfacedestroyed()
		 * SurfaceHolder。回调。
		 */
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);

		/**
		 * SurfaceHolder 类解释
		 * 
		 * 抽象接口，有人拿着一个显示面。允许你
		 * 
		 * 控制的表面大小和格式，编辑在表面的像素，和
		 * 
		 * *显示器更改为表面。此接口通常可用
		 * 
		 * 通过SurfaceView类 {@link SurfaceView}
		 * 
		 * 当使用该接口从一个线程以外的一个运行 {@link SurfaceView}, 你要仔细阅读
		 * 
		 * 方法 {@link #lockCanvas} and {@link Callback#surfaceCreated Callback.surfaceCreated()}.
		 */

		/**
		 * surfaceView.getHolder() 返回SurfaceHolder 对象
		 */
		SurfaceHolder surfaceHolder = surfaceView.getHolder();

		if (hasSurface) { // 判断是否 有显示
			initCamera(surfaceHolder);
		} else {
			// 添加回调监听
			surfaceHolder.addCallback(this);
			// 设置视图类型 这是被忽略的，这个值是在需要时自动设定的。
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		// 解码格式
		decodeFormats = null;
		// 字符集
		characterSet = null;

		playBeep = true;
		// 获取系统音频服务 AUDIO_SERVICE（音频服务）
		// AudioManager 提供了访问音量和振铃模式控制
		/*AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
		// 判断当前的模式 是否为 （铃声模式，可能是声音和振动。）
		if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			// 设置 播放闹铃 为false
			playBeep = false;
		}
		// * 初始化 报警音频
		initBeepSound();*/
		// 设置震动状态为 true
		vibrate = true;

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	/**
	 * Handler scan result
	 * 
	 * @param result
	 * @param barcode
	 */
	public void handleDecode(Result result, Bitmap barcode) {
		inactivityTimer.onActivity();
		playBeepSoundAndVibrate();
		String resultString = result.getText();
		// FIXME
		if (resultString.equals("")) {
			Toast.makeText(CaptureActivity.this, "Scan failed!", Toast.LENGTH_SHORT).show();
		} else {
			// System.out.println("Result:"+resultString);
			Intent resultIntent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putString("result", resultString);
			resultIntent.putExtras(bundle);
			// Toast.makeText(this, resultString, Toast.LENGTH_SHORT).show();
			this.setResult(RESULT_OK, resultIntent);
		}
		CaptureActivity.this.finish();
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			return;
		} catch (RuntimeException e) {
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;

	}

	public ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();

	}

	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private static final long VIBRATE_DURATION = 200L;

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {

		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

}