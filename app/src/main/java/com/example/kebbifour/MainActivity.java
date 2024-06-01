package com.example.kebbifour;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.kebbifour.fragments.ChatFragment;
import com.example.kebbifour.util.SocketHandler;
import com.example.kebbifour.viewmodel.MessagesViewModel;

public class MainActivity extends AppCompatActivity {
    private SocketHandler socketHandler;
    private Button connectButton;
    private final String TAG = MainActivity.class.getSimpleName();
    private boolean btnStartState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 MessagesViewModel
        MessagesViewModel messagesViewModel = new ViewModelProvider(this).get(MessagesViewModel.class);
        // 初始化 SocketHandler
        socketHandler = new SocketHandler(this, "192.168.50.68", 12345, messagesViewModel);
        // 初始化連線按鈕
        connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(v -> {
            Log.d(TAG, "Connect button clicked");
            Toast.makeText(MainActivity.this, "trying connect to server", Toast.LENGTH_SHORT).show();
            if (socketHandler.isConnected()) {
                // 切換到聊天
                openChatFragment();
            } else {
                // 執行連線操作
                socketHandler.connect(new SocketHandler.ConnectionListener() {
                    @Override
                    public void onConnected() {
                        runOnUiThread(() -> {
                            connectButton.setText("聊天");
                            Toast.makeText(MainActivity.this, "Connected to server", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onDisconnected() {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "連線失敗，請重試", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Disconnected from server");
                        });
                    }
                });
            }
        });
    }

    public SocketHandler getSocketHandler() {
        return socketHandler;
    }

    private void openChatFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ChatFragment())
                .addToBackStack(null)
                .commit();
    }
}
