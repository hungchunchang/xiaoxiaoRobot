package com.example.xiao2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentTransaction;

import com.example.xiao2.fragment.UserFragment;
import com.example.xiao2.listeners.CustomRobotEventListener;
import com.example.xiao2.repository.DataRepository;
import com.example.xiao2.util.CameraHandler;
import com.example.xiao2.util.HttpHandler;
import com.example.xiao2.util.RobotEventCallback;
import com.example.xiao2.viewmodel.RobotViewModel;
import com.example.xiao2.viewmodel.RobotViewModelFactory;
import com.example.xiao2.fragment.LoginFragment;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements RobotEventCallback {

    // SharedPreferences 文件名和鍵
    private static final String PREF_NAME = "user_data";
    private static final String KEY_IS_INITIALIZED = "is_initialized";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_USER_ID = "user_id";

    private TextView tapToStartTextView;
    private RelativeLayout mainLayout;
    private FragmentContainerView fragmentContainer;

    private Handler mainHandler;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private NuwaRobotAPI mRobotAPI;
    private CameraHandler cameraHandler;
    private CustomRobotEventListener customRobotEventListener;
    private RobotViewModel robotViewModel;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 設置對應佈局

        // 初始化 TextView 和 Layout
        tapToStartTextView = findViewById(R.id.tap_to_start);
        mainLayout = findViewById(R.id.main_layout);
        fragmentContainer = findViewById(R.id.fragment_container);

        // 確保 tapToStartTextView 不為 null
        if (tapToStartTextView != null) {
            Log.d("MainActivity", "tapToStartTextView initialized successfully.");
        } else {
            Log.e("MainActivity", "tapToStartTextView is null.");
        }

        // 呼吸閃爍效果
        startBreathingEffect();

        mainLayout.setOnClickListener(v -> {
            Log.d("MainActivity", "Screen tapped.");

            // 清除動畫並隱藏 "輕按以開始" 的文本
            if (tapToStartTextView != null) {
                Log.d("MainActivity", "Hiding tapToStartTextView");
                tapToStartTextView.clearAnimation();  // 清除動畫
                tapToStartTextView.setVisibility(View.GONE);
                tapToStartTextView.requestLayout();  // 強制重新佈局
            }

            // 顯示 Fragment 容器並加載 LoginFragment
            fragmentContainer.setVisibility(View.VISIBLE);

            // 使用 FragmentManager 加載 LoginFragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
        });


        // 初始化 Handler 以在主線程處理任務
        mainHandler = new Handler(Looper.getMainLooper());

        // 檢查權限並初始化
        checkPermissions();

        // 初始化 SharedPreferences
        initializeSharedPreferences();

        // 觀察 ViewModel 中的 Fragment 切換請求
        robotViewModel.getSwitchToUserFragment().observe(this, switchToUserFragment -> {
            if (switchToUserFragment != null && switchToUserFragment) {
                switchToUserFragment();
            }
        });
    }

    private void switchToUserFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new UserFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }


    private void startBreathingEffect() {
        // 創建呼吸閃爍的動畫，淡入淡出
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.2f);
        alphaAnimation.setDuration(1200); // 動畫持續時間
        alphaAnimation.setRepeatCount(Animation.INFINITE); // 無限重複
        alphaAnimation.setRepeatMode(Animation.REVERSE); // 反向重複

        // 啟動動畫
        if (tapToStartTextView != null) {
            tapToStartTextView.startAnimation(alphaAnimation);
        } else {
            Log.e("MainActivity", "TextView tap_to_start is null");
        }
    }

    private void loadLoginFragment() {
        // 創建並執行 FragmentTransaction 來替換畫面
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new LoginFragment());  // 替換為 LoginFragment
        transaction.addToBackStack(null);  // 加入返回棧
        transaction.commit();
    }

    @Override
    public void postToUiThread(Runnable runnable) {
        // 使用 Handler 將任務傳遞到主線程
        if (mainHandler != null) {
            mainHandler.post(runnable);
        }
    }

    // 初始化 SharedPreferences 的方法
    private void initializeSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 檢查是否已經初始化
        boolean isInitialized = sharedPreferences.getBoolean(KEY_IS_INITIALIZED, false);

        if (!isInitialized) {
            // 如果沒有初始化，則執行初始化邏輯
            SharedPreferences.Editor editor = sharedPreferences.edit();

            // 設置初始化標記，避免下次重複初始化
            editor.putBoolean(KEY_IS_INITIALIZED, true);

            // 提交變更
            editor.apply();

            // 打印初始化成功的消息
            Log.i("MainActivity", "SharedPreferences 已初始化，存儲了預設的使用者資訊。");
        } else {
            // 如果已經初始化過，則打印消息
            Log.i("MainActivity", "SharedPreferences 已經初始化，無需重複操作。");
        }
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
        };
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), REQUEST_ID_MULTIPLE_PERMISSIONS);
        } else {
            // 如果所有權限都被授予，則進行初始化
            initializeComponents();
        }
    }

    private void initializeComponents() {
        // 初始化 NuwaRobotAPI
        IClientId mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);

        // 初始化其他組件
        HttpHandler httpHandler = new HttpHandler(executorService);
        cameraHandler = new CameraHandler(this);
        DataRepository dataRepository = new DataRepository(httpHandler, executorService);
        httpHandler.setDataRepository(dataRepository);
        cameraHandler.setDataRepository(dataRepository);

        // 初始化 CustomRobotEventListener
        customRobotEventListener = new CustomRobotEventListener(mRobotAPI, null, this);

        // 註冊 RobotEventListener
        mRobotAPI.registerRobotEventListener(customRobotEventListener);

        // 初始化 CameraX 並選擇鏡頭方向
        initializeCameraX();

        // 初始化 RobotViewModel 並傳遞 customRobotEventListener
        RobotViewModelFactory factory = new RobotViewModelFactory(mRobotAPI, httpHandler, dataRepository, cameraHandler, customRobotEventListener);
        robotViewModel = new ViewModelProvider(this, factory).get(RobotViewModel.class);

    }

    private void initializeCameraX() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 設置預覽
                Preview preview = new Preview.Builder().build();

                // 選擇相機鏡頭方向（前置鏡頭）
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)  // 選擇前置鏡頭
                        .build();

                // 綁定相機和預覽
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "相機初始化失敗: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                initializeComponents(); // 如果權限授予成功，進行初始化
            } else {
                Toast.makeText(this, "需要授予所有權限才能使用此應用", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清理 NuwaRobotAPI 資源
        if (mRobotAPI != null) {
            mRobotAPI.release();
        }

        // 關閉 ExecutorService 以確保所有線程正確終止
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        cameraHandler.destroy();
    }
}
