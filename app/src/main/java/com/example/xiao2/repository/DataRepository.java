package com.example.xiao2.repository;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.xiao2.objects.ActionResponse;
import com.example.xiao2.util.HttpHandlerInterface;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;

public class DataRepository {
    private final HttpHandlerInterface httpHandler;
    private final MutableLiveData<ActionResponse> receivedMessage = new MutableLiveData<>();
    private final String TAG = "DataRepository";
    private final ExecutorService executorService;

    public DataRepository(HttpHandlerInterface httpHandler, ExecutorService executorService) {
        this.httpHandler = httpHandler;
        this.executorService = executorService;
    }

    public void handleCapturedImage(String resultString, Bitmap imageBitmap, String userName, String userId, String personality, String channel) {
        if (httpHandler != null) {
            executorService.execute(() -> {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                String imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                Log.d(TAG, "Sending picture via Http...");
                httpHandler.sendDataAndFetch(resultString, imageBase64, userName, userId, personality, channel);
            });
        }
    }

    public void sendDataViaHttp(String resultString, String imageBitmap, String userName, String userId, String personality, String channel) {
        if (resultString != null) {
            executorService.execute(() -> httpHandler.sendDataAndFetch(resultString, imageBitmap, userName, userId, personality, channel));
        } else {
            Log.e(TAG, "result Text is null or empty(http)");
        }
    }

    public LiveData<ActionResponse> getReceivedMessage() {
        return receivedMessage;
    }

    public void updateMessage(ActionResponse message) {
        receivedMessage.postValue(message);
    }
}