package com.example.xiao2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.xiao2.listeners.CustomRobotEventListener;
import com.example.xiao2.repository.DataRepository;
import com.example.xiao2.util.CameraHandler;
import com.example.xiao2.util.HttpHandler;
import com.example.xiao2.viewmodel.RobotViewModel;
import com.example.xiao2.viewmodel.RobotViewModelFactory;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

public class MainActivity extends AppCompatActivity {
    private NuwaRobotAPI mRobotAPI;
    private IClientId mClientId;
    private HttpHandler httpHandler;
    private CameraHandler cameraHandler;
    private DataRepository dataRepository;
    private RobotViewModel robotViewModel;
    private Button startButton;
    private final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (allPermissionsGranted()) {
            initializeComponents();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void initializeComponents() {
        // 初始化 NuwaRobotAPI
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);
        Log.d(TAG, "NuwaRobotAPI initialized");

        // 初始化 Handler
        httpHandler = new HttpHandler(this);
        cameraHandler = new CameraHandler(this);

        // 初始化 DataRepository
        dataRepository = new DataRepository(httpHandler);
        // 將 DataRepository 設置到 http, cameraHandler
        httpHandler.setDataRepository(dataRepository);
        cameraHandler.setDataRepository(dataRepository);

        // 初始化 RobotViewModel
        RobotViewModelFactory factory = new RobotViewModelFactory(mRobotAPI, httpHandler, dataRepository, cameraHandler);
        robotViewModel = new ViewModelProvider(this, factory).get(RobotViewModel.class);
        Log.d(TAG, "RobotViewModel initialized");

        // 創建並設置 CustomRobotEventListener 和 CustomVoiceEventListener
        CustomRobotEventListener customRobotEventListener = new CustomRobotEventListener(mRobotAPI, robotViewModel);
        mRobotAPI.registerRobotEventListener(customRobotEventListener);
        Log.d(TAG, "Event listeners registered");

        // 觀察 ViewModel 的訊息變化
        robotViewModel.getReceivedMessage().observe(this, message -> {
            // 播放 TTS
            robotViewModel.speak(message);
        });

        // 初始化連線按鈕
        startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            Log.d(TAG, "Connect button clicked");
            Toast.makeText(MainActivity.this, "開始！", Toast.LENGTH_SHORT).show();
            dataRepository.sendDataViaHttp("hello!","");
            startButton.setVisibility(View.GONE);
            robotViewModel.setAction("Thinking");
//            robotViewModel.startListening();
        });
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeComponents();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraHandler.destroy();
        if (mRobotAPI != null) {
            mRobotAPI.release();
        }

    }
}
