package com.example.xiao2.listeners;

import android.util.Log;

import com.example.xiao2.viewmodel.RobotViewModel;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;


public class CustomVoiceEventListener implements VoiceEventListener {

    private static final String TAG = "CustomVoiceEventListener";
    private final RobotViewModel robotViewModel;
    private String userName;
    private String userId;
    private String personality;
    private String channel;


    public CustomVoiceEventListener(RobotViewModel robotViewModel) {
        this.robotViewModel = robotViewModel;
    }
    public void setChannel(String channel){
        this.channel = channel;
    }

    public void setUser(String userName, String userId, String personality, String channel)
    {
        this.userName = userName;
        this.personality = personality;
        this.channel = channel;
        this.userId = userId;
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

        String result_string = null;

        if (isValidJson(json)) {
            result_string = VoiceResultJsonParser.parseVoiceResult(json);
        } else {
            Log.e(TAG, "Invalid JSON format in onSpeech2TextComplete");
        }

        if (isError || result_string == null || result_string.trim().isEmpty()) {
            Log.e(TAG, "Speech to text result is empty or error occurred, restarting listening");
            robotViewModel.setAction("Listening");  // 機器人回到聆聽狀態
            new Thread(robotViewModel::startListening).start();
            return;
        }

        // 語音轉文字成功後，設定機器人進入思考狀態
        robotViewModel.setAction("Thinking");
        robotViewModel.stopListening();
    }

    @Override
    public void onMixUnderstandComplete(boolean isError, ResultType resultType, String s) {
        Log.d(TAG, "onMixUnderstandComplete isError:" + isError + ", json:" + s);
        String result_string = null;

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
            Log.e(TAG, "MixUnderstand error occurred, restarting listening");
            robotViewModel.setAction("Listening");
            new Thread(robotViewModel::startListening).start();
            return;
        }

        if (result_string == null || result_string.trim().isEmpty()) {
            Log.e(TAG, "MixUnderstand result is empty, restarting listening");
            robotViewModel.setAction("Listening");
            new Thread(robotViewModel::startListening).start();
            return;
        }

        // 檢查是否包含拍照指令
        if (result_string.contains("你看") || result_string.contains("這是什麼")) {
            Log.d(TAG, "Taking picture based on the command");
            robotViewModel.takePicture(result_string, userName,userId, personality, channel);

        } else {
            Log.d(TAG, "Sending message to server");
            robotViewModel.sendResultToServerViaHttp(result_string, "", userName,userId, personality, channel);
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


//    private String getCurrentTime() {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss ", Locale.getDefault());
//        return sdf.format(new Date());
//    }

    private boolean isValidJson(String json) {
        try {
            new JSONObject(json);
            return true;
        } catch (JSONException ex) {
            return false;
        }
    }
}
