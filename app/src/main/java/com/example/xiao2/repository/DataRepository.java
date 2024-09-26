package com.example.xiao2.repository;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.xiao2.message.Message;
import com.example.xiao2.util.HttpHandlerInterface;

import java.io.ByteArrayOutputStream;

public class DataRepository {
//    private final SocketHandlerInterface socketHandler;
    private final HttpHandlerInterface httpHandler;
    private final MutableLiveData<Message> receivedMessage = new MutableLiveData<>();
    private final String TAG = "DataRepository";
    // 宣告
    public DataRepository(HttpHandlerInterface httpHandler){
        this.httpHandler = httpHandler;
    }
    public void handleCapturedImage(String result_string, Bitmap imageBitmap) {

        if(httpHandler !=null){
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            String imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            Log.d(TAG, "Sending picture via Http...");
            httpHandler.sendDataAndFetch(result_string, imageBase64);

        }
    }


    public void sendDataViaHttp(String resultText, String imageBitmap){
        if(resultText != null){
            new Thread(()->{
                httpHandler.sendDataAndFetch(resultText, imageBitmap);
            }).start();
        }else{
            Log.e(TAG, "resultTextis null or empty(http)");
        }
    }

    //收訊息
    public LiveData<Message> getReceivedMessage() {
        return receivedMessage;
    }

    public void updateMessage(Message message) {
        receivedMessage.postValue(message);
    }

}
