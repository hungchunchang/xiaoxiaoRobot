package com.example.xiao2.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import androidx.fragment.app.Fragment;

import com.example.xiao2.MainActivity;
import com.example.xiao2.R;
import com.example.xiao2.viewmodel.RobotViewModel;

public class VideoFragment extends Fragment {
    private static final String ARG_CHANNEL = "channel";
    private RobotViewModel robotViewModel;

    public static VideoFragment newInstance(String channel) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHANNEL, channel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        robotViewModel = ((MainActivity) requireActivity()).getRobotViewModel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        VideoView videoView = view.findViewById(R.id.video_view);

        // 設置視頻源（這裡需要根據實際情況設置）
        // videoView.setVideoPath("path_to_your_video");

        initSDKState();
        startConversation();

        return view;
    }

    private void initSDKState() {
        robotViewModel.setAction("Idle");
    }

    private void startConversation() {
        robotViewModel.setAction("Thinking");

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        robotViewModel.interruptAndReset();
    }
}