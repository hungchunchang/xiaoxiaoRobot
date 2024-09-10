package com.example.xiao.listeners;

import android.util.Log;

import com.example.xiao.util.SocketHandler;
import com.example.xiao.viewmodel.MessagesViewModel;
import com.example.xiao.viewmodel.RobotViewModel;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.SimpleGrammarData;

public class CustomRobotEventListener implements RobotEventListener {
    private static final String TAG = "CustomRobotEventListener";
    private final NuwaRobotAPI mRobotAPI;
    private final MessagesViewModel messagesViewModel;
    private RobotViewModel robotViewModel;
    private final SocketHandler socketHandler;
    private boolean isExpressionMode = false;

    public CustomRobotEventListener(NuwaRobotAPI robotAPI, MessagesViewModel messagesModelView, RobotViewModel robotViewModel, SocketHandler socketHandler) {
        this.mRobotAPI = robotAPI;
        this.messagesViewModel = messagesModelView;
        this.socketHandler = socketHandler;
        this.robotViewModel = robotViewModel;
    }

    @Override
    public void onWikiServiceStart() {
        Log.d(TAG, "onWikiServiceStart, robot ready to be controlled");
        mRobotAPI.registerVoiceEventListener(new CustomVoiceEventListener(mRobotAPI, messagesViewModel, robotViewModel, socketHandler));
        prepareGrammarToRobot();
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
    public void onTap(int i) {}

    @Override
    public void onLongPress(int i) {}

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

    public void setExpressionMode(boolean expressionMode) {
        isExpressionMode = expressionMode;
    }

    public void setRobotViewModel(RobotViewModel robotViewModel) {
        this.robotViewModel = robotViewModel;
    }
}
