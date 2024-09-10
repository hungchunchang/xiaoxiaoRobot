package com.example.xiao.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.xiao.listeners.CustomRobotEventListener;
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
    private final CustomRobotEventListener customRobotEventListener;
    private Timer timer;
    private TimerTask repeatTask;

    public RobotViewModel(NuwaRobotAPI robotAPI, CustomRobotEventListener customRobotEventListener) {
        this.mRobotAPI = robotAPI;
        this.customRobotEventListener = customRobotEventListener;
        this.motionMap = new HashMap<>();
        this.expressionMap = new HashMap<>();
        initializeMotionAndExpressionMaps();
        mRobotAPI.registerRobotEventListener(customRobotEventListener);
    }

    private void initializeMotionAndExpressionMaps() {
        // Example mappings
        motionMap.put("Idle", "666_SA_Discover");
        motionMap.put("Listening", "666_SA_Think");
        motionMap.put("Thinking", "666_DA_Intospace");
        motionMap.put("Speaking", "666_RE_Ask");

        expressionMap.put("Idle", "TTS_Happy");
        expressionMap.put("Listening", "TTS_Sad");
        expressionMap.put("Thinking", "TTS_Surprise");
        expressionMap.put("Speaking", "TTS_Angry");
    }

    public LiveData<String> getCurrentAction() {
        return currentAction;
    }

    public LiveData<String> getCurrentExpression() {
        return currentExpression;
    }

    public void setAction(String actionKey) {
        new Thread(() -> {
            String motion = motionMap.get(actionKey);
            String expression = expressionMap.get(actionKey);
            mMotionComplete = false;

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
                customRobotEventListener.setExpressionMode(true);
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


    public CustomRobotEventListener getCustomRobotEventListener() {
        return customRobotEventListener;
    }
}
