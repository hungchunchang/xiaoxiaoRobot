package com.example.xiao2.listeners;

import android.util.Log;

import com.example.xiao2.MainActivity;
import com.example.xiao2.util.RobotEventCallback;
import com.example.xiao2.viewmodel.RobotViewModel;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.SimpleGrammarData;

public class CustomRobotEventListener implements RobotEventListener {
    private boolean isProcessingLongPress = false;
    private static final String TAG = "CustomRobotEventListener";
    private final NuwaRobotAPI mRobotAPI;
    private final RobotViewModel robotViewModel;
    private boolean isExpressionMode = false;
    private final CustomVoiceEventListener customVoiceEventListener;
    private final RobotEventCallback callback;

    public CustomRobotEventListener(NuwaRobotAPI robotAPI, RobotViewModel robotViewModel, RobotEventCallback callback) {
        this.mRobotAPI = robotAPI;
        this.callback = callback;
        this.robotViewModel = robotViewModel;
        this.customVoiceEventListener = new CustomVoiceEventListener(robotViewModel);
        mRobotAPI.registerVoiceEventListener(this.customVoiceEventListener);
    }

    @Override
    public void onWikiServiceStart() {
        Log.d(TAG, "onWikiServiceStart, robot ready to be controlled");
        mRobotAPI.requestSensor(NuwaRobotAPI.SENSOR_TOUCH | NuwaRobotAPI.SENSOR_PIR | NuwaRobotAPI.SENSOR_DROP );

        prepareGrammarToRobot();
    }
    public void setChannel(String channel) {
        Log.d("CustomRobotEventListener", "setChannel called with channel: " + channel);
        if (customVoiceEventListener != null) {
            customVoiceEventListener.setChannel(channel);
        } else {
            Log.e("CustomRobotEventListener", "customVoiceEventListener is null in setChannel");
        }
    }

    private void prepareGrammarToRobot() {
        Log.d(TAG, "prepareGrammarToRobot");
        SimpleGrammarData mGrammarData = new SimpleGrammarData("example");
        mGrammarData.addSlot("your command");
        mGrammarData.updateBody();
        mRobotAPI.createGrammar(mGrammarData.grammar, mGrammarData.body);
    }

    @Override
    public void onWikiServiceStop() {}

    @Override
    public void onWikiServiceCrash() {}

    @Override
    public void onWikiServiceRecovery() {}

    @Override
    public void onStartOfMotionPlay(String s) {}

    @Override
    public void onPauseOfMotionPlay(String s) {}

    @Override
    public void onStopOfMotionPlay(String s) {}

    @Override
    public void onCompleteOfMotionPlay(String s) {
        Log.d(TAG, "onCompleteOfMotionPlay: " + s);
        if (robotViewModel != null) {
            robotViewModel.setMotionComplete(true); // 通知 RobotViewModel 動作已完成
        }
    }

    @Override
    public void onPlayBackOfMotionPlay(String s) {}

    @Override
    public void onErrorOfMotionPlay(int i) {}

    @Override
    public void onPrepareMotion(boolean b, String s, float v) {}

    @Override
    public void onCameraOfMotionPlay(String s) {}

    @Override
    public void onGetCameraPose(float v, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8, float v9, float v10, float v11) {}

    @Override
    public void onTouchEvent(int i, int i1) {
        Log.i(TAG, "onTouchEvent: Type: " + i + ", Value: " + i1);
        if (isExpressionMode) {
            mRobotAPI.hideWindow(false);
            isExpressionMode = false;
        }
    }

    @Override
    public void onPIREvent(int i) {}

    @Override
    public void onTap(int i) {
        Log.d(TAG, "Tap with "+ i);
    }

    public void onLongPress(int i) {
        Log.d(TAG, "onLongPress: Long press detected with value: " + i);

        if (i == 3 && !isProcessingLongPress) {  // 如果按住的是對應的按鍵且不在处理中
            isProcessingLongPress = true;
            Log.d(TAG, "onLongPress: Long press action for value 3, returning to ButtonFragment.");

            callback.postToUiThread(() -> {
                Log.d(TAG, "onLongPress: Interrupting and resetting robot.");
                robotViewModel.interruptAndReset();  // 重置機器人狀態

                robotViewModel.interruptAndReset();

                Log.d(TAG, "onLongPress: Switching from VideoFragment to ButtonFragment.");
                if (callback instanceof MainActivity) {
                    MainActivity activity = (MainActivity) callback;
                    activity.switchToButtonFragment();
                }

                Log.d(TAG, "onLongPress: Switched to ButtonFragment.");
                isProcessingLongPress = false;
            });
        }
    }

    @Override
    public void onWindowSurfaceReady() {}

    @Override
    public void onWindowSurfaceDestroy() {}

    @Override
    public void onTouchEyes(int i, int i1) {
        Log.i(TAG, "onTouchEyes: " + i + ", " + i1);
        if (isExpressionMode) {
            mRobotAPI.hideWindow(false);
            isExpressionMode = false; // 退出表情模式
        }
    }

    @Override
    public void onRawTouch(int i, int i1, int i2) {}

    @Override
    public void onFaceSpeaker(float v) {}

    @Override
    public void onActionEvent(int i, int i1) {}

    @Override
    public void onDropSensorEvent(int i) {}

    @Override
    public void onMotorErrorEvent(int i, int i1) {}


}
