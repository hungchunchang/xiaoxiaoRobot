package com.example.kebbifour.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.HashMap;
import java.util.Map;

public class RobotViewModel extends ViewModel {
    private final NuwaRobotAPI mRobotAPI;
    private final MutableLiveData<String> currentAction = new MutableLiveData<>();
    private final Map<String, String> actionMap;

    public RobotViewModel(NuwaRobotAPI robotAPI) {
        this.mRobotAPI = robotAPI;
        this.actionMap = new HashMap<>();
        // 初始化動作表情的對應關係
        actionMap.put("Listening", "Listen_Motion");
        actionMap.put("Thinking", "Think_Motion");
        actionMap.put("Speaking", "Speak_Motion");
    }

    public LiveData<String> getCurrentAction() {
        return currentAction;
    }

    public void setAction(String stage) {
        String motion = actionMap.get(stage);
        if (motion != null) {
            mRobotAPI.motionPlay(motion, false);
            currentAction.setValue(motion);
        }
    }
}
