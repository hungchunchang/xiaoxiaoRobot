package com.example.xiao2.viewmodel;

import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.xiao2.listeners.CustomRobotEventListener;
import com.example.xiao2.objects.ActionResponse;
import com.example.xiao2.repository.DataRepository;
import com.example.xiao2.util.CameraHandler;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.HashMap;
import java.util.Map;

public class RobotViewModel extends ViewModel {
    private final NuwaRobotAPI mRobotAPI;
    private final MutableLiveData<String> currentAction = new MutableLiveData<>();
    private final MutableLiveData<String> currentExpression = new MutableLiveData<>();
    private final Map<String, String> motionMap;
    private final Map<String, String> expressionMap;
    private final Map<String, String> emotionVideoMap;
    private final CameraHandler cameraHandler;
    private final DataRepository dataRepository;
    private final String TAG = "RobotViewModel";

    private final MutableLiveData<String> receivedMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> switchToUserFragment = new MutableLiveData<>();

    private final CustomRobotEventListener customRobotEventListener;

    public RobotViewModel(NuwaRobotAPI robotAPI, DataRepository dataRepository, CameraHandler cameraHandler, CustomRobotEventListener customRobotEventListener, HashMap<String, String> emotionVideoMap) {
        this.mRobotAPI = robotAPI;
        this.dataRepository = dataRepository;
        this.cameraHandler = cameraHandler;
        this.customRobotEventListener = customRobotEventListener;
        this.motionMap = new HashMap<>();
        this.expressionMap = new HashMap<>();
        this.emotionVideoMap = emotionVideoMap;
        initializeMotionAndExpressionMaps();

    }

    private void initializeMotionAndExpressionMaps() {
        // Example mappings
        motionMap.put("Idle", "666_SA_Discover");
        motionMap.put("Listening", "666_SA_Think");
        motionMap.put("Thinking", "666_PE_PushGlasses");
        motionMap.put("Speaking", "666_RE_Ask");
        motionMap.put("TakingPicture", "");

        expressionMap.put("Idle", "TTS_Contempt");
        expressionMap.put("Listening", "TTS_Surprise");
        expressionMap.put("Thinking", "TTS_Contempt");
        expressionMap.put("Speaking", "TTS_PeaceA");
        expressionMap.put("TakingPicture", "TTS_JoyB");
    }

    public String getVideoPathForEmotion(String emotion) {
        if (emotionVideoMap.containsKey(emotion)) {
            return emotionVideoMap.get(emotion);  // 從 HashMap 中獲取影片路徑
        } else {
            Log.e(TAG, "No video path found for emotion: " + emotion);
            return null;
        }
    }

    public LiveData<ActionResponse> getReceivedMessage() {
        return dataRepository.getReceivedMessage();
    }

    public LiveData<Boolean> getSwitchToUserFragment() {
        return switchToUserFragment;
    }

    // 在需要切換到 UserFragment 的時候調用這個方法
    public void requestSwitchToUserFragment() {
        switchToUserFragment.postValue(true);
    }

    public LiveData<String> getCurrentAction() {
        return currentAction;
    }

    public void showRobotFace() {
        if (mRobotAPI != null) {
            Log.d(TAG, "Showing robot face");
            mRobotAPI.UnityFaceManager().showUnity();
        } else {
            Log.e(TAG, "mRobotAPI is null, cannot show face");
        }
    }

    // 機器人行為控制
    public void setAction(String actionKey) {
        new Thread(() -> {
            String motion = motionMap.get(actionKey);
            String expression = expressionMap.get(actionKey);
            mRobotAPI.mouthOff();
            if(actionKey.equals("Speaking")){
                mRobotAPI.mouthOn(200);
            }

            if (motion != null) {
                mRobotAPI.motionStop(true); // 停止當前動作
                mRobotAPI.motionPlay(motion, false);
                currentAction.postValue(motion);
            } else {
                Log.e("RobotViewModel", "Motion not found for action key: " + actionKey);
            }
            if (expression != null) {
                mRobotAPI.UnityFaceManager().showUnity(); // Launch face
                mRobotAPI.UnityFaceManager().playFaceAnimation(expression);
                currentExpression.postValue(expression);
            } else {
                Log.e("RobotViewModel", "Expression not found for action key: " + actionKey);
            }
        }).start();
    }

    public void setMotionComplete() {
    }

    // 機器人語音輸入控制
    public void startListening() {
        setAction("Listening");
        mRobotAPI.startMixUnderstand();
    }

    public void stopListening(){
        mRobotAPI.stopListen();
    }

    // 將聽到的句子通過 http 傳出去
    public void sendResultToServerViaHttp(String resultString, String imageBitmap, String userName, String userId, String personality, String channel) {
        Log.d(TAG, "Send result to server");
        setAction("Thinking");
        dataRepository.sendDataViaHttp(resultString, "", userName, userId, personality ,channel);
    }


    // 機器人語音輸出控制
    public void speak(String message) {
        setAction("Speaking");
        mRobotAPI.startTTS(message);

    }

    public void takePicture(String result_string, String userName, String userId, String personality, String channel) {
        Log.d(TAG, "Taking picture...");
        setAction("TakingPicture");
        cameraHandler.takePicture(result_string, userName, userId, personality, channel);
    }

    public void interruptAndReset() {
        Log.d(TAG, "interruptAndReset: Starting to interrupt robot actions.");

        if (mRobotAPI != null) {
            Log.d(TAG, "interruptAndReset: Stopping TTS and listening.");

            // 停止所有動作
            mRobotAPI.motionStop(true);
            Log.d(TAG, "interruptAndReset: Stopped all robot motions.");

            // 停止 TTS 和語音
            mRobotAPI.stopTTS();
            mRobotAPI.stopListen();
            Log.d(TAG, "interruptAndReset: Stopped TTS and listening.");

            // 隱藏機器人臉部畫面
            mRobotAPI.UnityFaceManager().hideFace();
            Log.d(TAG, "interruptAndReset: Robot face hidden.");

            // 重置動作
            mRobotAPI.motionReset();
            Log.d(TAG, "interruptAndReset: Robot motions reset.");

            // 確保所有操作已完全停止，添加短暫延遲來等待停止命令生效
            new Handler().postDelayed(() -> {
                Log.d(TAG, "interruptAndReset: Final check after delay to ensure complete reset.");
            }, 1500);  // 延遲 500 毫秒確保機器人完全停止
        } else {
            Log.e(TAG, "interruptAndReset: mRobotAPI is null, cannot stop actions.");
        }
    }

    public DataRepository getDataRepository() {
        return dataRepository;
    }
    public CustomRobotEventListener getCustomRobotEventListener() {
        return customRobotEventListener;
    }



}
