package com.example.xiao.fragments;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.xiao.MainActivity;
import com.example.xiao.R;
import com.example.xiao.listeners.CustomRobotEventListener;
import com.example.xiao.message.AudioMessage;
import com.example.xiao.message.ImageMessage;
import com.example.xiao.message.TextMessage;
import com.example.xiao.util.SocketHandler;
import com.example.xiao.viewmodel.MessagesViewModel;
import com.example.xiao.viewmodel.RobotViewModel;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    private NuwaRobotAPI mRobotAPI;
    private IClientId mClientId;
    private SocketHandler socketHandler;
    private MessagesViewModel messagesViewModel;
    private RobotViewModel robotViewModel;
    private TextView mResult, mInput;
    private Button mStartBtn, mStopBtn, mBackBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mResult = view.findViewById(R.id.result_text);
        mResult.setMovementMethod(new ScrollingMovementMethod()); // 啟用滾動
        mInput = view.findViewById(R.id.input_text);
        mInput.setMovementMethod(new ScrollingMovementMethod());

        mStartBtn = view.findViewById(R.id.btn_start);
        mStopBtn = view.findViewById(R.id.btn_stop);
        mBackBtn = view.findViewById(R.id.btn_back);

        mClientId = new IClientId(requireContext().getPackageName());
        mRobotAPI = new NuwaRobotAPI(requireContext(), mClientId);
        socketHandler = ((MainActivity) requireContext()).getSocketHandler();
        messagesViewModel = new ViewModelProvider(requireActivity()).get(MessagesViewModel.class);

        robotViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(RobotViewModel.class)) {
                    CustomRobotEventListener customRobotEventListener = new CustomRobotEventListener(mRobotAPI, messagesViewModel, null, socketHandler);
                    return (T) new RobotViewModel(mRobotAPI, customRobotEventListener);
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        }).get(RobotViewModel.class);
        robotViewModel.setAction("Idle");

        (robotViewModel.getCustomRobotEventListener()).setRobotViewModel(robotViewModel);

        mRobotAPI.registerRobotEventListener(robotViewModel.getCustomRobotEventListener());

        robotViewModel.getCurrentAction().observe(getViewLifecycleOwner(), action -> {
            // 更新 UI 或其他操作
            Log.d(TAG, "Current action: " + action);
        });



        mStartBtn.setOnClickListener(this::BtnStart);
        mStopBtn.setOnClickListener(this::BtnStop);
        mBackBtn.setOnClickListener(v -> navigateToMainFragment());

        requireActivity().runOnUiThread(() -> {
            mStartBtn.setEnabled(true);
            mStopBtn.setEnabled(false);
        });

        // 接收 Server 的消息然後唸出來
        messagesViewModel.responseReceived().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) response;
                    mResult.append("Received: " + textMessage.getText() + " \n");
                    speakTextMessage(textMessage.getText());
                } else if (response instanceof ImageMessage) {
                    Log.d(TAG, "Image message received: " + response);
                } else if (response instanceof AudioMessage) {
                    Log.d(TAG, "Audio message received: " + response);
                } else {
                    Log.e(TAG, "Unknown message type: " + response.getClass().getSimpleName());
                }
            }
        });

        messagesViewModel.getMessages().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                TextMessage textMessage = (TextMessage) message;
                mInput.append("\n" + textMessage.getText());
            }
        });
    }

    private void speakTextMessage(String text) {
        robotViewModel.setAction("Speaking");
        long ttsDuration = getTTSDuration(text);
        robotViewModel.repeatAction("Speaking", ttsDuration);
        new Thread(() -> {
            mRobotAPI.startTTS(text);
            robotViewModel.stopRepeatingAction();
        }).start();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRobotAPI.release();
    }

    public void BtnStart(View view) {
        robotViewModel.prepareAction("Listening");
        new Thread(() -> {
            robotViewModel.setAction("Listening");
            mRobotAPI.startMixUnderstand();
            Log.d(TAG, "startMixUnderstand");
            requireActivity().runOnUiThread(() -> {
                mStartBtn.setEnabled(false);
                mStopBtn.setEnabled(true);
            });
        }).start();
    }

    public void BtnStop(View view) {
        new Thread(() -> {
            robotViewModel.setAction("Idle");
            mRobotAPI.stopListen();
            requireActivity().runOnUiThread(() -> {
                mStartBtn.setEnabled(true);
                mStopBtn.setEnabled(false);
            });
        }).start();
    }

    private void navigateToMainFragment() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new MainFragment())
                .addToBackStack(null)
                .commit();
    }

    private long getTTSDuration(String text) {
        // 這裡簡單估算 TTS 的時間，可以根據實際情況調整
        int wordsPerMinute = 210; // 假設每分鐘講150個字
        int words = text.split("\\s+").length;
        return (words * 60000) / wordsPerMinute; // 返回毫秒數
    }

}
