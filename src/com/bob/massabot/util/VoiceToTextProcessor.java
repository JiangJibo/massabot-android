/**
 * Copyright(C) 2017 MassBot Co. Ltd. All rights reserved.
 *
 */
package com.bob.massabot.util;

import static com.bob.massabot.constant.HttpRequestURLConfig.USER_VOICE_WORDS_URL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.json.JSONObject;

import com.bob.massabot.constant.MassabotConstant;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.UserWords;
import com.iflytek.sunflower.FlowerCollector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.Toast;

/**
 * 将语音解析成中文文字
 * 
 * @since 2017年4月21日 下午7:23:05
 * @version $Id$
 * @author JiangJibo
 *
 */
public class VoiceToTextProcessor {

	private Activity activity;
	private String requestPrefix;

	// 用户语音指令的词表
	private static final String VOICE_WORDS_FILE_NAME = "voicewords.txt";

	public static final String PREFER_NAME = "com.iflytek.setting";

	// 语音听写对象
	private SpeechRecognizer mIat;
	// 语音听写UI
	private RecognizerDialog mIatDialog;
	// 用HashMap存储听写结果
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_CLOUD;

	private Toast mToast;
	private SharedPreferences mSharedPreferences;

	private boolean onceSended; // 一次调节发送一次指令,语音解析机制是多次解析,会重复请求,发送了:true; 未发送:false

	/**
	 * @param activity
	 */
	@SuppressLint("ShowToast")
	public VoiceToTextProcessor(Activity activity, String requestPrefix) {
		this.activity = activity;
		this.requestPrefix = requestPrefix;
		SpeechUtility.createUtility(activity, SpeechConstant.APPID + "=58f48791");
		// 使用SpeechRecognizer对象，可根据回调消息自定义界面；
		mIat = SpeechRecognizer.createRecognizer(activity, null);
		// 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
		// 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
		mIatDialog = new RecognizerDialog(activity, null);
		mSharedPreferences = activity.getSharedPreferences(PREFER_NAME, Activity.MODE_PRIVATE);
		mToast = Toast.makeText(activity, "", Toast.LENGTH_SHORT);
	}

	/**
	 * 从服务端获取用户词表,对比自身配置的词表,如果不同则更新词表配置
	 */
	public void uploadUserWords() {
		Callable<String> callable = new Callable<String>() {

			@Override
			public String call() throws Exception {
				String result = HttpRequestUtils.doGet(requestPrefix + "/" + USER_VOICE_WORDS_URL, 2000);
				if (result == null) {
					return null;
				}
				List<String> requestedWords = new Gson().fromJson(result, new TypeToken<ArrayList<String>>() {
				}.getType());
				List<String> configWords = getVoiceWords();
				if (requestedWords.equals(configWords)) {
					return null;
				}
				updateVoiceWordsConfig(requestedWords);
				UserWords userWords = new UserWords();
				userWords.putWords("语音指令", (ArrayList<String>) requestedWords);
				mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
				mIat.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
				mIat.updateLexicon("userword", userWords.toString(), null);
				showTip("用户上传的词表为:[" + userWords.toString() + "]");
				return null;
			}
		};
		HttpRequestUtils.EXECUTOR.submit(callable);
	}

	public void processVoice() {
		// 移动数据分析，收集开始听写事件
		FlowerCollector.onEvent(activity, "iat_recognize");

		mIatResults.clear();
		// 设置参数
		setParam();
		// boolean isShowDialog =
		// mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
		// 显示听写对话框
		mIatDialog.setListener(mRecognizerDialogListener);
		mIatDialog.show();
		onceSended = false;
		showTip("请开始说话…");
	}

	/**
	 * 获取词表文件内的词表数据
	 * 
	 * @return
	 */
	private List<String> getVoiceWords() {
		BufferedReader reader = null;
		List<String> voiceWords = new ArrayList<String>();
		try {
			reader = new BufferedReader(new InputStreamReader(activity.openFileInput(VOICE_WORDS_FILE_NAME)));
			String line;
			while ((line = reader.readLine()) != null) {
				voiceWords.add(line);
			}
		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
			}
		}
		return voiceWords;
	}

	/**
	 * 更新词表文件
	 */
	private void updateVoiceWordsConfig(List<String> voiceWords) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(activity.openFileOutput(VOICE_WORDS_FILE_NAME, Context.MODE_PRIVATE), "UTF-8"));
			for (String voiceWord : voiceWords) {
				bw.write(voiceWord);
				bw.newLine();
			}
		} catch (Exception e) {

		} finally {
			try {
				if (bw != null) {
					bw.close();
				}
			} catch (IOException e) {

			}
		}
	}

	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	public void setParam() {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);

		// 设置听写引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		// 设置返回结果格式
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

		String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");
		/*if (lag.equals("en_us")) {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
		} else {
		}*/
		// 设置语言
		mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
		// 设置语言区域
		mIat.setParameter(SpeechConstant.ACCENT, lag);

		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

		// 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
		mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
	}

	/**
	 * 解析语音,生成中文
	 * 
	 * @param results
	 * @return
	 */
	private String printResult(RecognizerResult results) {
		String text = JsonParser.parseIatResult(results.getResultString());

		String sn = null;
		// 读取json结果中的sn字段
		try {
			JSONObject resultJson = new JSONObject(results.getResultString());
			sn = resultJson.optString("sn");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mIatResults.put(sn, text);

		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
		}
		showTip(resultBuffer.toString());
		return resultBuffer.toString();
	}

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}

	/**
	 * 听写UI监听器
	 */
	private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {

		public void onResult(RecognizerResult results, boolean isLast) {
			String result = printResult(results);
			try {
				if (!onceSended) {
					result = HttpRequestUtils.doPutWithThread(requestPrefix + USER_VOICE_WORDS_URL + "?voiceWords=" + result, null, 2000);
				}
				onceSended = true;
			} catch (Exception e) {
				showTip("使用语音指令调节超时,请重新尝试");
			}
			if (!MassabotConstant.SUCCESS_FLAG.equals(result)) {
				showTip(result);
			}
		}

		/**
		 * 识别回调错误.
		 */
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

	};

}
