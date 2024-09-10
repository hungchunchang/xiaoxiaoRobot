package com.example.xiao.listeners;

import android.util.Log;

import com.example.xiao.message.TextMessage;
import com.example.xiao.util.SocketHandler;
import com.example.xiao.viewmodel.MessagesViewModel;
import com.example.xiao.viewmodel.RobotViewModel;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomVoiceEventListener implements VoiceEventListener {

    private static final String TAG = CustomVoiceEventListener.class.getSimpleName();
    private final NuwaRobotAPI mRobotAPI;
    private final MessagesViewModel messagesViewModel;
    private final RobotViewModel robotViewModel;
    private final SocketHandler socketHandler;

    public CustomVoiceEventListener(NuwaRobotAPI robotAPI, MessagesViewModel messagesViewModel, RobotViewModel robotViewModel, SocketHandler handler) {
        this.mRobotAPI = robotAPI;
        this.messagesViewModel = messagesViewModel;
        this.socketHandler = handler;
        this.robotViewModel = robotViewModel;
    }

    @Override
    public void onWakeup(boolean isError, String score, float direction) {}

    @Override
    public void onTTSComplete(boolean isError) {
        Log.d(TAG, "TTS Complete, starting to listen again");
        startListening();
        // Add action and expression for Idle phase after TTS completes
        robotViewModel.setAction("Idle");
    }

    @Override
    public void onSpeechRecognizeComplete(boolean isError, ResultType iFlyResult, String json) {}

    @Override
    public void onSpeech2TextComplete(boolean isError, String json) {
        Log.d(TAG, "onSpeech2TextComplete:" + !isError + ", json:" + json);
        String result_string = VoiceResultJsonParser.parseVoiceResult(json);

        if (result_string == null || result_string.trim().isEmpty()) {
            Log.e(TAG, "Result is empty, not sending to server");
            startListening();
            return;
        }

        // Add action and expression for Thinking phase
        robotViewModel.setAction("Thinking");

        messagesViewModel.setResultToSend(result_string);
        mRobotAPI.stopListen();
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

            new Thread(mRobotAPI::startMixUnderstand).start();
            return;
        }

        if (result_string == null || result_string.trim().isEmpty()) {
            Log.e(TAG, "Result is empty, not sending to server");
            robotViewModel.setAction("Listening");

            new Thread(mRobotAPI::startMixUnderstand).start();
            return;
        }

        TextMessage message = new TextMessage(getCurrentTime() + " onMixUnderstandComplete: " + isError + ", result: " + result_string);
        robotViewModel.setAction("Thinking");
        messagesViewModel.setMessages(message);
        messagesViewModel.setResultToSend(result_string);
        new Thread(mRobotAPI::stopListen).start();
    }

    @Override
    public void onSpeechState(ListenType listenType, SpeechState speechState) {}

    @Override
    public void onSpeakState(SpeakType speakType, SpeakState speakState) {}

    @Override
    public void onGrammarState(boolean isError, String s) {
        if (!isError) {
            startListening();
        } else {
            Log.d(TAG, "onGrammarState error, " + s);
        }
    }

    @Override
    public void onListenVolumeChanged(ListenType listenType, int i) {}

    @Override
    public void onHotwordChange(HotwordState hotwordState, HotwordType hotwordType, String s) {}

    private void startListening() {
        Log.d(TAG, "Starting MixUnderstand");
        robotViewModel.setAction("Listening");
        mRobotAPI.startMixUnderstand();
    }

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
