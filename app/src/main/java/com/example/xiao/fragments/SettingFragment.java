package com.example.xiao.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.xiao.MainActivity;
import com.example.xiao.R;
import com.example.xiao.util.SocketHandler;
import com.example.xiao.viewmodel.MessagesViewModel;

public class SettingFragment extends Fragment {
    private EditText ipAddressEditText;
    private EditText portEditText;
    private Button connectButton;
    private Button backButton;
    private SocketHandler socketHandler;
    private MessagesViewModel messagesViewModel;
    private final String TAG = SettingFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ipAddressEditText = view.findViewById(R.id.edit_text_ip_address);
        portEditText = view.findViewById(R.id.edit_text_port);
        connectButton = view.findViewById(R.id.button_connect);
        backButton = view.findViewById(R.id.button_back);

        socketHandler = ((MainActivity) requireActivity()).getSocketHandler();

        connectButton.setOnClickListener(v -> {
            String ipAddress = ipAddressEditText.getText().toString();
            String portString = portEditText.getText().toString();
            if (TextUtils.isEmpty(ipAddress) || TextUtils.isEmpty(portString)) {
                Toast.makeText(requireContext(), "Please enter IP address and port", Toast.LENGTH_SHORT).show();
                return;
            }


            int port;
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid port number", Toast.LENGTH_SHORT).show();
                return;
            }

            socketHandler = new SocketHandler(requireContext(), ipAddress, port, messagesViewModel);
            socketHandler.connect(new SocketHandler.ConnectionListener() {
                @Override
                public void onConnected() {
                    Toast.makeText(requireContext(), "Connected to server", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "connected to server");
                    navigateToFragment(new ChatFragment());

                }

                @Override
                public void onDisconnected() {
                    Toast.makeText(requireContext(), "Disconnected from server", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "disconnected to server");

                }
            });
        });
        backButton.setOnClickListener(v -> navigateToFragment(new MainFragment()));
    }
    private void navigateToFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
