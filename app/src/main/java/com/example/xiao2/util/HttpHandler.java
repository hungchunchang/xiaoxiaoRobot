package com.example.xiao2.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.xiao2.objects.ActionResponse;
import com.example.xiao2.repository.DataRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

public class HttpHandler implements HttpHandlerInterface, Serializable {

    private static final String TAG = HttpHandler.class.getSimpleName();
    private final Handler mainHandler;
    private DataRepository dataRepository;
    private final ExecutorService executorService;

    // 更新構造函數以接受 ExecutorService
    public HttpHandler(ExecutorService executorService) {
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.executorService = executorService;
    }

    public void setDataRepository(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public void sendDataAndFetch(String resultString, String imgBase64, String userName, String userId, String personality, String channel) {
        String urlString = "http://140.112.14.225:1234/api/" + channel;
        JSONObject jsonData = new JSONObject();

        try {
            jsonData.put("id", userId);
            jsonData.put("robot_mbti", personality);
            jsonData.put("user_name", userName);
            jsonData.put("chat", resultString);
            jsonData.put("img_base64", imgBase64);
            Log.d("http",userId+personality+userName+resultString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 使用 ExecutorService 來執行網絡請求
        executorService.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                ActionResponse action = getActionResponse(connection);

                // 使用 mainHandler 在主線程上更新 UI
                mainHandler.post(() -> {
                    if (dataRepository != null) {
                        dataRepository.updateMessage(action);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error sending data to the server", e);
            }
        });
    }

    private static @NonNull ActionResponse getActionResponse(HttpURLConnection connection) throws IOException, JSONException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        String jsonString = response.toString();
        JSONObject jsonResponse = new JSONObject(jsonString);

        String action = jsonResponse.optString("action", "");
        String emotion = jsonResponse.optString("emotion", "");
        String talk = jsonResponse.optString("talk", "");

        return new ActionResponse(action, emotion, talk);
    }
}