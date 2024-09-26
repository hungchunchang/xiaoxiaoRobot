package com.example.xiao2.listeners;

import android.util.Log;

import com.example.xiao2.message.TextMessage;
import com.example.xiao2.viewmodel.RobotViewModel;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomVoiceEventListener implements VoiceEventListener {

    private static final String TAG = "CustomVoiceEventListener";
    private final RobotViewModel robotViewModel;

    public CustomVoiceEventListener(RobotViewModel robotViewModel) {
        this.robotViewModel = robotViewModel;
    }

    @Override
    public void onWakeup(boolean isError, String score, float direction) {}

    @Override
    public void onTTSComplete(boolean isError) {
        Log.d(TAG, "TTS Complete, starting to listen again");
        robotViewModel.startListening();
        // Add action and expression for Idle phase after TTS completes
        //robotViewModel.setAction("Idle");
    }

    @Override
    public void onSpeechRecognizeComplete(boolean isError, ResultType iFlyResult, String json) {}

    @Override
    public void onSpeech2TextComplete(boolean isError, String json) {
        Log.d(TAG, "onSpeech2TextComplete:" + !isError + ", json:" + json);
        String result_string = VoiceResultJsonParser.parseVoiceResult(json);

        if (result_string == null || result_string.trim().isEmpty()) {
            Log.e(TAG, "Result is empty, not sending to server");
            robotViewModel.startListening();
            return;
        }

        // Add action and expression for Thinking phase
        robotViewModel.setAction("Thinking");
        robotViewModel.stopListening();
    }

    @Override
    public void onMixUnderstandComplete(boolean isError, ResultType resultType, String s) {
        Log.d(TAG, "onMixUnderstandComplete isError:" + isError + ", json:" + s);
        String result_string = null;

        // 檢查字串是否為有效的 JSON 格式
        if (isValidJson(s)) {
            try {
                JSONObject jsonObject = new JSONObject(s);
                result_string = jsonObject.getString("result");
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse JSON", e);
            }
        } else {
            result_string = s;  // 若不是有效的 JSON 格式，直接使用原始字串
        }

        if (isError) {
            Log.e(TAG, "MixUnderstand error occurred");
            robotViewModel.setAction("Listening");

            new Thread(robotViewModel::startListening).start();
            return;
        }

        if (result_string == null || result_string.trim().isEmpty()) {
            Log.e(TAG, "Result is empty, not sending to server");
            robotViewModel.setAction("Listening");

            new Thread(robotViewModel::startListening).start();
            return;
        }

        // 檢查是否包含拍照指令
        if (result_string.contains("你看") || result_string.contains("這是什麼")) {
            Log.d(TAG, "going to take picture");
            robotViewModel.takePicture(result_string);
        }
        else{
            Log.d(TAG, "going to send message");
            TextMessage message = new TextMessage(getCurrentTime() + " onMixUnderstandComplete: " + isError + ", result: " + result_string);
            //robotViewModel.setAction("Thinking");
            robotViewModel.sendResultToServerViaHttp(result_string, "");
            //new Thread(robotViewModel::stopListening).start();
        }
    }

    @Override
    public void onSpeechState(ListenType listenType, SpeechState speechState) {}

    @Override
    public void onSpeakState(SpeakType speakType, SpeakState speakState) {}

    @Override
    public void onGrammarState(boolean isError, String s) {
        Log.d(TAG, "onGrammarState error, " + s);
    }

    @Override
    public void onListenVolumeChanged(ListenType listenType, int i) {}

    @Override
    public void onHotwordChange(HotwordState hotwordState, HotwordType hotwordType, String s) {}


    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss ", Locale.getDefault());
        return sdf.format(new Date());
    }

    private boolean isValidJson(String json) {
        try {
            new JSONObject(json);
            return true;
        } catch (JSONException ex) {
            return false;
        }
    }
}
