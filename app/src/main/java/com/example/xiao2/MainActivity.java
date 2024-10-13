package com.example.xiao2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.xiao2.fragment.ButtonFragment;
import com.example.xiao2.listeners.CustomRobotEventListener;
import com.example.xiao2.repository.DataRepository;
import com.example.xiao2.util.CameraHandler;
import com.example.xiao2.util.HttpHandler;
import com.example.xiao2.util.RobotEventCallback;
import com.example.xiao2.viewmodel.RobotViewModel;
import com.example.xiao2.viewmodel.RobotViewModelFactory;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements RobotEventCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private NuwaRobotAPI mRobotAPI;
    private HttpHandler httpHandler;
    private CameraHandler cameraHandler;
    private DataRepository dataRepository;
    private RobotViewModel robotViewModel;
    private CustomRobotEventListener customRobotEventListener;
    private EditText usernameEditText, passwordEditText;
    private Button loginButton;
    private View loginContainer;
    private View fragmentContainer;

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
    };
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

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

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initializeComponents() {
        // 初始化 UI 元件
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        loginContainer = findViewById(R.id.loginContainer);
        fragmentContainer = findViewById(R.id.fragment_container);

        // 初始化 NuwaRobotAPI
        IClientId mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this, mClientId);
        Log.d(TAG, "NuwaRobotAPI initialized");

        // 初始化其他組件
        httpHandler = new HttpHandler(executorService);
        cameraHandler = new CameraHandler(this);
        dataRepository = new DataRepository(httpHandler, executorService);
        httpHandler.setDataRepository(dataRepository);
        cameraHandler.setDataRepository(dataRepository);

        // 初始化 RobotViewModel
        RobotViewModelFactory factory = new RobotViewModelFactory(mRobotAPI, httpHandler, dataRepository, cameraHandler, customRobotEventListener);
        robotViewModel = new ViewModelProvider(this, factory).get(RobotViewModel.class);
        Log.d(TAG, "RobotViewModel initialized");

        // 初始化 CustomRobotEventListener
        customRobotEventListener = new CustomRobotEventListener(mRobotAPI, robotViewModel, this);

        // 註冊 RobotEventListener
        mRobotAPI.registerRobotEventListener(customRobotEventListener);
        Log.d(TAG, "Event listeners registered");

        // 設置登入按鈕點擊事件
        loginButton.setOnClickListener(v -> attemptLogin());

        // 觀察 ViewModel 的訊息變化
        robotViewModel.getReceivedMessage().observe(this, message -> {
            Log.d(TAG, "Received message: " + message);
            // 播放 TTS
            robotViewModel.speak(message);
        });
    }

    private void attemptLogin() {
        String username = usernameEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (username.isEmpty() && password.isEmpty()) {
            // 隱藏鍵盤
            hideKeyboard();

            loginContainer.setVisibility(View.GONE);
            fragmentContainer.setVisibility(View.VISIBLE);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ButtonFragment())
                    .commit();

            Log.d(TAG, "attemptLogin: ButtonFragment has been added to fragment_container");
        } else {
            Toast.makeText(this, "請輸入使用者名稱和密碼", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (view instanceof EditText) {
                Rect outRect = new Rect();
                view.getGlobalVisibleRect(outRect);
                Log.d(TAG, "Rect: " + outRect.toString());
                Log.d(TAG, "Event coordinates: " + event.getRawX() + ", " + event.getRawY());
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    view.clearFocus();
                    hideKeyboard();
                    Log.d(TAG, "Hiding keyboard");
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public RobotViewModel getRobotViewModel() {
        return robotViewModel;
    }

    public CustomRobotEventListener getCustomRobotEventListener() {
        return customRobotEventListener;
    }

    @Override
    public void postToUiThread(Runnable runnable) {
        runOnUiThread(runnable);
    }

    public void switchToButtonFragment() {
        Log.d(TAG, "switchToButtonFragment: Starting");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ButtonFragment())
                .commitNow(); // 使用 commitNow 来确保事务立即执行
        Log.d(TAG, "switchToButtonFragment: Completed");
    }

    @Override
    public void resetUIToInitialState() {
        Log.d(TAG, "resetUIToInitialState: Starting");
        runOnUiThread(() -> {
            loginContainer.setVisibility(View.VISIBLE);
            findViewById(R.id.fragment_container).setVisibility(View.GONE);
            mRobotAPI.UnityFaceManager().hideFace();
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Log.d(TAG, "resetUIToInitialState: Completed");
        });
    }

}