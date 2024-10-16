package com.example.xiao2.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.xiao2.repository.DataRepository;
import com.example.xiao2.util.CameraHandler;
import com.example.xiao2.util.HttpHandlerInterface;
import com.example.xiao2.listeners.CustomRobotEventListener;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.HashMap;

public class RobotViewModelFactory implements ViewModelProvider.Factory {
    private final NuwaRobotAPI mRobotAPI;
    private final HttpHandlerInterface httpHandler;
    private final DataRepository dataRepository;
    private final CameraHandler cameraHandler;
    private final CustomRobotEventListener customRobotEventListener;
    private final HashMap emotionVideoMap;

    public RobotViewModelFactory(NuwaRobotAPI mRobotAPI, HttpHandlerInterface httpHandler, DataRepository dataRepository, CameraHandler cameraHandler, CustomRobotEventListener customRobotEventListener, HashMap emotionVideoMap) {
        this.mRobotAPI = mRobotAPI;
        this.httpHandler = httpHandler;
        this.dataRepository = dataRepository;
        this.cameraHandler = cameraHandler;
        this.customRobotEventListener = customRobotEventListener;
        this.emotionVideoMap = emotionVideoMap;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(RobotViewModel.class))
            return (T) new RobotViewModel(mRobotAPI, dataRepository, cameraHandler, customRobotEventListener, emotionVideoMap);
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}