package com.example.xiao2.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.example.xiao2.MainActivity;
import com.example.xiao2.R;
import com.example.xiao2.listeners.CustomRobotEventListener;
import com.example.xiao2.viewmodel.RobotViewModel;

public class ButtonFragment extends Fragment {
    private static final String TAG = ButtonFragment.class.getSimpleName();
    private RobotViewModel robotViewModel;
    private CustomRobotEventListener customRobotEventListener;
    private Button abButton, chatButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buttons, container, false);

        MainActivity mainActivity = (MainActivity) requireActivity();
        robotViewModel = mainActivity.getRobotViewModel();
        customRobotEventListener = mainActivity.getCustomRobotEventListener();

        abButton = view.findViewById(R.id.ab_button);
        chatButton = view.findViewById(R.id.chat_button);

        setupButtons();

        return view;
    }

    private void setupButtons() {
        abButton.setOnClickListener(v -> handleButtonClick("biography"));
        chatButton.setOnClickListener(v -> handleButtonClick("chat"));
    }

    private void handleButtonClick(String channel) {
        Log.d(TAG, channel + " button clicked");

        // 初始化機器人通信
        initializeRobotCommunication(channel);

        // 切換到 VideoFragment
        VideoFragment videoFragment = VideoFragment.newInstance(channel);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, videoFragment)
                .addToBackStack(null)
                .commit();
    }

    private void initializeRobotCommunication(String channel) {
        robotViewModel.getDataRepository().sendDataViaHttp("hi", "", channel);
        customRobotEventListener.setChannel(channel);
    }
}