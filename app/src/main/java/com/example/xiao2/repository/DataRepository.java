package com.example.xiao2.repository;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.xiao2.util.HttpHandlerInterface;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;

public class DataRepository {
    private final HttpHandlerInterface httpHandler;
    private final MutableLiveData<String> receivedMessage = new MutableLiveData<>();
    private final String TAG = "DataRepository";
    private final ExecutorService executorService;

    public DataRepository(HttpHandlerInterface httpHandler, ExecutorService executorService) {
        this.httpHandler = httpHandler;
        this.executorService = executorService;
    }

    public void handleCapturedImage(String result_string, Bitmap imageBitmap, String channel) {
        if (httpHandler != null) {
            executorService.execute(() -> {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                String imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                Log.d(TAG, "Sending picture via Http...");
                httpHandler.sendDataAndFetch(result_string, imageBase64, channel);
            });
        }
    }

    public void sendDataViaHttp(String resultText, String imageBitmap, String channel) {
        if (resultText != null) {
            executorService.execute(() -> {
                httpHandler.sendDataAndFetch(resultText, imageBitmap, channel);
            });
        } else {
            Log.e(TAG, "result Text is null or empty(http)");
        }
    }

    public LiveData<String> getReceivedMessage() {
        return receivedMessage;
    }

    public void updateMessage(String message) {
        receivedMessage.postValue(message);
    }
}