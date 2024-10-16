package com.example.xiao2.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;
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
    private VideoView videoView;

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
        videoView = view.findViewById(R.id.video_view);

        startConversation();
        observeReceivedMessage();

        return view;
    }

    private void startConversation() {
//        robotViewModel.setAction("Thinking");
        playEmotionVideo("neutral");
    }

    private void observeReceivedMessage() {
        robotViewModel.getReceivedMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Log.d(TAG, "接收到訊息：" + message);

                // 顯示訊息為 Toast
                Toast.makeText(getContext(), "接收到訊息: " + message.getTalk(), Toast.LENGTH_LONG).show();
                playEmotionVideo("happy");
                // 播放 TTS
                robotViewModel.speak(message.getTalk());
//                playEmotionVideo(message.getEmotion());

            }
        });
    }


    private void playEmotionVideo(String emotion) {
        // 從 RobotViewModel 獲取對應情緒的影片路徑
        String videoPath = robotViewModel.getVideoPathForEmotion(emotion);

        if (videoPath != null) {
            Uri videoUri = Uri.parse(videoPath);
            videoView.setVideoURI(videoUri);
            videoView.start();  // 播放影片
            Log.d("VideoFragment", "Video Player");
        } else {
            Log.e("VideoFragment", "No video path available for the given emotion.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        robotViewModel.interruptAndReset();
    }
}