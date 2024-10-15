package com.example.xiao2.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.xiao2.MainActivity;
import com.example.xiao2.R;
import com.example.xiao2.viewmodel.RobotViewModel;

public class VideoFragment extends Fragment {
    private static final String TAG = "VideoFragment";
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

        robotViewModel = new ViewModelProvider(requireActivity()).get(RobotViewModel.class);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        VideoView videoView = view.findViewById(R.id.video_view);

//        // 從 Bundle 中獲取影片路徑或 URI
//        String videoPath = getArguments() != null ? getArguments().getString("video_path") : null;
//
//        if (videoPath != null) {
//            videoView.setVideoPath(videoPath);
//            videoView.start();  // 播放影片
//        }

        initSDKState();
        startConversation();

        return view;
    }
    private void observeReceivedMessage() {
        robotViewModel.getReceivedMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Log.d(TAG, "接收到訊息：" + message);

                // 顯示訊息為 Toast
                Toast.makeText(getContext(), "接收到訊息: " + message, Toast.LENGTH_LONG).show();

                // 播放 TTS
                robotViewModel.speak(message);
            }
        });
    }

    private void initSDKState() {
        robotViewModel.showRobotFace();
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