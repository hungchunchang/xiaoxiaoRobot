package com.example.xiao2.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.xiao2.repository.DataRepository;
import com.example.xiao2.util.CameraHandler;
import com.example.xiao2.util.HttpHandler;
import com.example.xiao2.util.HttpHandlerInterface;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

public class RobotViewModelFactory implements ViewModelProvider.Factory {
    private final NuwaRobotAPI mRobotAPI;
    private final HttpHandlerInterface httpHandler;
    private final CameraHandler cameraHandler;
    private final DataRepository dataRepository;

    public RobotViewModelFactory(NuwaRobotAPI robotAPI, HttpHandler httpHandler, DataRepository dataRepository, CameraHandler cameraHandler) {
        this.mRobotAPI = robotAPI;
        this.httpHandler = httpHandler;
        this.cameraHandler = cameraHandler;
        this.dataRepository = dataRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(RobotViewModel.class)) {
            return (T) new RobotViewModel(mRobotAPI, httpHandler, dataRepository, cameraHandler);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
