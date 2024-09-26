package com.example.xiao2.util;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.xiao2.message.TextMessage;
import com.example.xiao2.repository.DataRepository;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpHandler implements HttpHandlerInterface, Serializable {

    public static final String UTF_8 = "utf-8";
    private static final String TAG = HttpHandler.class.getSimpleName();
    private final Context context;
    private final Handler mainHandler;
    private DataRepository dataRepository;

    // Constructor that initializes the context and handler for UI updates
    public HttpHandler(Context context) {
        this.context = context;
        this.mainHandler = new Handler(context.getMainLooper());
    }

    public void setDataRepository(DataRepository dataRepository){
        this.dataRepository = dataRepository;
    }

    // Method for sending HTTP POST requests with JSON data
    @Override
    public void sendDataAndFetch(String resultString, String imgBase64) {
        String urlString = "http://140.112.14.225:1234/api/content";
        JSONObject jsonData = new JSONObject();

        try{
            jsonData.put("id", "001");  // 修改為 "001"
            jsonData.put("robot_mbti", "ENFP");  // 使用 "robot_mbti" 替代 "MBTI"
            jsonData.put("user_name", "傑哥");  // 修改為 "傑哥"
            jsonData.put("chat", resultString);  // 繼續使用聊天內容
            jsonData.put("img_base64", imgBase64);  // 使用 "img_base64" 替代 "img"
        } catch (Exception e){
            e.printStackTrace();
        }
        //POST Request
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                //1. send POST requests
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; utf-8");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                // Write JSON data to output stream
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonData.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // 取得 POST 請求的回應
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }
                TextMessage receivedMessage = new TextMessage(response.toString());
                dataRepository.updateMessage(receivedMessage);


            } catch (Exception e) {
                Log.e(TAG, "Error sending data to the server", e);
            }
        }).start();
    }

}
