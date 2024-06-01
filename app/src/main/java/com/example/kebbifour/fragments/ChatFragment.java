package com.example.kebbifour.fragments;

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
import androidx.lifecycle.ViewModelProvider;

import com.example.kebbifour.MainActivity;
import com.example.kebbifour.R;
import com.example.kebbifour.listeners.CustomRobotEventListener;
import com.example.kebbifour.message.AudioMessage;
import com.example.kebbifour.message.ImageMessage;
import com.example.kebbifour.message.TextMessage;
import com.example.kebbifour.util.SocketHandler;
import com.example.kebbifour.viewmodel.MessagesViewModel;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;


public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    private NuwaRobotAPI mRobotAPI;
    private IClientId mClientId;
    private SocketHandler socketHandler;
    private MessagesViewModel MessagesViewModel;
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
        MessagesViewModel = new ViewModelProvider(requireActivity()).get(MessagesViewModel.class);
        mRobotAPI.registerRobotEventListener(new CustomRobotEventListener(mRobotAPI, MessagesViewModel, socketHandler));

        mStartBtn.setOnClickListener(this::BtnStart);
        mStopBtn.setOnClickListener(this::BtnStop);
        mBackBtn.setOnClickListener(v -> navigateToMainFragment());


        requireActivity().runOnUiThread(() -> {
            mStartBtn.setEnabled(true);
            mStopBtn.setEnabled(false);
        });

        // 接收 Server 的消息然後唸出來，所以應該是 Socket 收到消息，更新到 responseReceived 上面
        MessagesViewModel.responseReceived().observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                if (response instanceof TextMessage) {
                    TextMessage textMessage = (TextMessage) response;
                    mResult.append("Received: " + textMessage.getText() + " \n");
                    mRobotAPI.startTTS(textMessage.getText());
                } else if (response instanceof ImageMessage) {
                    Log.d(TAG, "Image message received: " + response);
                } else if (response instanceof AudioMessage) {
                    Log.d(TAG, "Audio message received: " + response);
                } else {
                    Log.e(TAG, "Unknown message type: " + response.getClass().getSimpleName());
                }
            }
        });
        MessagesViewModel.getMessages().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                // 獲取最後一個訊息
                TextMessage textMessage = (TextMessage) message;
                // 將最後一個訊息附加到 mInput 上
                mInput.append("\n" + textMessage.getText());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRobotAPI.release();
    }

    public void BtnStart(View view) {
        mRobotAPI.startMixUnderstand();
        Log.d(TAG, "startMixUnderstand");
        requireActivity().runOnUiThread(() -> {
            mStartBtn.setEnabled(false);
            mStopBtn.setEnabled(true);
        });
    }

    public void BtnStop(View view) {
        mRobotAPI.stopListen();
        requireActivity().runOnUiThread(() -> {
            mStartBtn.setEnabled(true);
            mStopBtn.setEnabled(false);
        });
    }
    private void navigateToMainFragment() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new MainFragment())
                .addToBackStack(null)
                .commit();
    }

}
