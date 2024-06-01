package com.example.kebbifour.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.kebbifour.MainActivity;
import com.example.kebbifour.R;
import com.example.kebbifour.util.SocketHandler;


public class MainFragment extends Fragment {
    private Button chatButton;
    private Button settingButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatButton = view.findViewById(R.id.button_chat);
        settingButton = view.findViewById(R.id.button_setting);

        chatButton.setOnClickListener(v -> {
            SocketHandler socketHandler = ((MainActivity) requireActivity()).getSocketHandler();
            if (socketHandler.isConnected()) {
                navigateToFragment(new ChatFragment());
            } else {
                navigateToFragment(new SettingFragment());
            }
        });

        settingButton.setOnClickListener(v -> navigateToFragment(new SettingFragment()));
    }

    private void navigateToFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
