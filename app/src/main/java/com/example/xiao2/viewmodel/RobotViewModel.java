package com.example.xiao2.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.xiao2.listeners.CustomRobotEventListener;
import com.example.xiao2.repository.DataRepository;
import com.example.xiao2.util.CameraHandler;
import com.example.xiao2.util.HttpHandlerInterface;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RobotViewModel extends ViewModel {
    private final NuwaRobotAPI mRobotAPI;
    private final MutableLiveData<String> currentAction = new MutableLiveData<>();
    private final MutableLiveData<String> currentExpression = new MutableLiveData<>();
    private final Map<String, String> motionMap;
    private final Map<String, String> expressionMap;
    private boolean mMotionComplete = true;
    private final CameraHandler cameraHandler;
    private final HttpHandlerInterface httpHandler;
    private final DataRepository dataRepository;
    private Timer timer;
    private TimerTask repeatTask;
    private final String TAG = "RobotViewModel";
    private final MutableLiveData<String> receivedMessage = new MutableLiveData<>();
    private final CustomRobotEventListener customRobotEventListener;

    public RobotViewModel(NuwaRobotAPI robotAPI, HttpHandlerInterface httpHandler, DataRepository dataRepository, CameraHandler cameraHandler, CustomRobotEventListener customRobotEventListener) {
        this.mRobotAPI = robotAPI;
        this.httpHandler = httpHandler;
        this.dataRepository = dataRepository;
        this.cameraHandler = cameraHandler;
        this.customRobotEventListener = customRobotEventListener;
        this.motionMap = new HashMap<>();
        this.expressionMap = new HashMap<>();
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

    public LiveData<String> getReceivedMessage() {
        return dataRepository.getReceivedMessage();
    }

    public void setMessage(String message) {
        receivedMessage.setValue(message);
    }
    public LiveData<String> getCurrentAction() {
        return currentAction;
    }

    public LiveData<String> getCurrentExpression() {
        return currentExpression;
    }

    // 機器人行為控制
    public void setAction(String actionKey) {
        new Thread(() -> {
            String motion = motionMap.get(actionKey);
            String expression = expressionMap.get(actionKey);
            mMotionComplete = false;
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

    public void repeatAction(String actionKey, long duration) {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        repeatTask = new TimerTask() {
            @Override
            public void run() {
                setAction(actionKey);
            }
        };
        timer.scheduleAtFixedRate(repeatTask, 0, duration);
    }

    public void stopRepeatingAction() {
        if (timer != null) {
            timer.cancel();
        }
    }

    public void setMotionComplete(boolean motionComplete) {
        this.mMotionComplete = motionComplete;
    }

    public void prepareAction(String actionKey) {
        String motion = motionMap.get(actionKey);
        if (motion != null) {
            mRobotAPI.motionPrepare(motion);
        }
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
    public void sendResultToServerViaHttp(String resultText, String imageBitmap, String channel) {
        Log.d(TAG, "Send result to server");
        setAction("Thinking");
        dataRepository.sendDataViaHttp(resultText,imageBitmap, channel);
    }


    // 機器人語音輸出控制
    public void speak(String message) {
        setAction("Speaking");
        mRobotAPI.startTTS(message);

    }

    public void takePicture(String result_string, String channel) {
        Log.d(TAG, "Taking picture...");
        setAction("TakingPicture");
        cameraHandler.takePicture(result_string, channel);
    }

    public void interruptAndReset() {
        Log.d(TAG, "interruptAndReset: Starting to interrupt robot actions.");

        // 停止所有動作，並保持機器人處於靜止狀態
        setAction("Idle");  // 設定機器人狀態為 "Idle"
        Log.d(TAG, ": Robot set to Idle.");

        if (mRobotAPI != null) {
            Log.d(TAG, "interruptAndReset: Stopping TTS and listening.");

            mRobotAPI.stopTTS();  // 停止所有 TTS 操作
            mRobotAPI.stopListen();  // 停止語音聆聽
            mRobotAPI.motionStop(true);  // 停止所有動作
            Log.d(TAG, "interruptAndReset: Stopped all robot motions.");

            mRobotAPI.UnityFaceManager().hideFace();// 隱藏機器人臉部畫面
            mRobotAPI.motionReset();
            Log.d(TAG, "interruptAndReset: Robot face hidden.");
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
