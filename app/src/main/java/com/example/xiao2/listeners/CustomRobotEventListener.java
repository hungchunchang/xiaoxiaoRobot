package com.example.xiao2.listeners;

import android.content.ComponentName;
import android.util.Log;

import com.example.xiao2.util.RobotEventCallback;
import com.example.xiao2.viewmodel.RobotViewModel;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.SimpleGrammarData;
import com.nuwarobotics.service.facecontrol.UnityFaceCallback;
import com.nuwarobotics.service.facecontrol.utils.ServiceConnectListener;

public class CustomRobotEventListener implements RobotEventListener {
    private static final String TAG = "CustomRobotEventListener";
    private boolean isProcessingLongPress = false;
    private boolean isExpressionMode = false;

    private final NuwaRobotAPI mRobotAPI;
    private final RobotViewModel robotViewModel;
    private final CustomVoiceEventListener customVoiceEventListener;
    private final RobotEventCallback callback;

    // Unity Face 回調，用來監聽機器人臉部觸摸事件
    private final UnityFaceCallback mUnityFaceCallback = new UnityFaceCallback() {
        @Override
        public void on_touch_left_eye() { Log.d(TAG, "on_touch_left_eye()"); }

        @Override
        public void on_touch_right_eye() { Log.d(TAG, "on_touch_right_eye()"); }

        @Override
        public void on_touch_nose() { Log.d(TAG, "on_touch_nose()"); }

        @Override
        public void on_touch_mouth() { Log.d(TAG, "on_touch_mouth()"); }

        @Override
        public void on_touch_head() { Log.d(TAG, "on_touch_head()"); }

        @Override
        public void on_touch_left_edge() { Log.d(TAG, "on_touch_left_edge()"); }

        @Override
        public void on_touch_right_edge() { Log.d(TAG, "on_touch_right_edge()"); }

        @Override
        public void on_touch_bottom() { Log.d(TAG, "on_touch_bottom()"); }
    };

    // 臉部控制服務的連接監聽器
    private final ServiceConnectListener FaceControlConnect = new ServiceConnectListener() {
        @Override
        public void onConnectChanged(ComponentName componentName, boolean isConnected) {
            Log.d(TAG, "FaceControl service connected: " + isConnected);
            if (isConnected) {
                mRobotAPI.UnityFaceManager().registerCallback(mUnityFaceCallback);
                Log.d(TAG, "Face control callback registered.");
            } else {
                Log.e(TAG, "Failed to connect to face control.");
            }
        }
    };

    public CustomRobotEventListener(NuwaRobotAPI robotAPI, RobotViewModel robotViewModel, RobotEventCallback callback) {
        this.mRobotAPI = robotAPI;
        this.robotViewModel = robotViewModel;
        this.callback = callback;
        this.customVoiceEventListener = new CustomVoiceEventListener(robotViewModel);
        mRobotAPI.registerVoiceEventListener(this.customVoiceEventListener);
    }

    @Override
    public void onWikiServiceStart() {
        Log.d(TAG, "onWikiServiceStart: Robot is ready to be controlled");
        // 啟用感測器
        mRobotAPI.requestSensor(NuwaRobotAPI.SENSOR_TOUCH | NuwaRobotAPI.SENSOR_PIR | NuwaRobotAPI.SENSOR_DROP);
        // 初始化臉部控制
        mRobotAPI.initFaceControl(mRobotAPI.getContext(), mRobotAPI.getContext().getClass().getName(), FaceControlConnect);
        // 準備語法數據
        prepareGrammarToRobot();
    }

    private void prepareGrammarToRobot() {
        Log.d(TAG, "prepareGrammarToRobot");
        SimpleGrammarData mGrammarData = new SimpleGrammarData("example");
        mGrammarData.addSlot("your command");
        mGrammarData.updateBody();
        mRobotAPI.createGrammar(mGrammarData.grammar, mGrammarData.body);
    }

    public void setUser(String userName, String userId, String personality, String channel) {
        Log.d(TAG, "setUser called with userName, ID, personality, channel: " + channel);
        if (customVoiceEventListener != null) {
            customVoiceEventListener.setUser(userName, userId, personality, channel);
        } else {
            Log.e(TAG, "customVoiceEventListener is null in setUser");
        }
    }

    @Override
    public void onCompleteOfMotionPlay(String s) {
        Log.d(TAG, "onCompleteOfMotionPlay: " + s);
        if (robotViewModel != null) {
            robotViewModel.setMotionComplete(); // 通知 RobotViewModel 動作已完成
        }
    }

    @Override
    public void onStartOfMotionPlay(String motionName) {
        Log.d(TAG, "onStartOfMotionPlay: Motion started: " + motionName);
    }

    @Override
    public void onTouchEvent(int i, int i1) {
        Log.i(TAG, "onTouchEvent: Type: " + i + ", Value: " + i1);
        if (isExpressionMode) {
            mRobotAPI.hideWindow(false);
            isExpressionMode = false;  // 退出表情模式
        }
    }

    @Override
    public void onTap(int i) {
        Log.d(TAG, "Tap event detected with value: " + i);
    }

    @Override
    public void onLongPress(int i) {
        Log.d(TAG, "onLongPress detected with value: " + i);

        if (i == 3 && !isProcessingLongPress) {  // 如果按住的是對應的按鍵且不在处理中
            isProcessingLongPress = true;
            Log.d(TAG, "onLongPress: Long press action for value 3, showing face.");

            callback.postToUiThread(() -> {
                Log.d(TAG, "onLongPress: Interrupting and resetting robot.");
                try {
                    robotViewModel.interruptAndReset();  // 重置機器人狀態
                    Thread.sleep(500);  // 延遲 500 毫秒
                    robotViewModel.interruptAndReset();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 發出切換到 UserFragment 的請求
                robotViewModel.requestSwitchToUserFragment();

                isProcessingLongPress = false;
            });
        }
    }

    @Override
    public void onPIREvent(int eventCode) {
        Log.d(TAG, "onPIREvent: PIR event detected with code: " + eventCode);
    }

    @Override
    public void onWindowSurfaceReady() {
        Log.d(TAG, "Window surface is ready.");
    }

    @Override
    public void onWindowSurfaceDestroy() {
        Log.d(TAG, "Window surface is destroyed.");
    }

    @Override
    public void onTouchEyes(int i, int i1) {
        Log.i(TAG, "onTouchEyes event: " + i + ", " + i1);
        if (isExpressionMode) {
            mRobotAPI.hideWindow(false);
            isExpressionMode = false;  // 退出表情模式
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

    @Override
    public void onWikiServiceStop() {}

    @Override
    public void onWikiServiceCrash() {}

    @Override
    public void onWikiServiceRecovery() {}

    @Override
    public void onPrepareMotion(boolean b, String s, float v) {}

    @Override
    public void onCameraOfMotionPlay(String s) {}

    @Override
    public void onGetCameraPose(float v, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8, float v9, float v10, float v11) {}

    @Override
    public void onErrorOfMotionPlay(int errorCode) {Log.e(TAG, "onErrorOfMotionPlay: Motion play error occurred with error code: " + errorCode);}

    @Override
    public void onPlayBackOfMotionPlay(String s){}

    @Override
    public void onStopOfMotionPlay(String s){}

    @Override
    public void onPauseOfMotionPlay(String s){}


}
