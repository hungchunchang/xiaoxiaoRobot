package com.example.kebbifour.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.kebbifour.message.Message;

public class MessagesViewModel extends ViewModel {
    // 接收 Sever 的回應
    private final MutableLiveData<Message> response = new MutableLiveData<>();
    // 語音輸入轉文字
    private final MutableLiveData<String> result = new MutableLiveData<>();
    // 內部 UI 訊息變化
    private final MutableLiveData<Message> messages = new MutableLiveData<>();

    // 當有改變發生時，返回 LiveData
    public LiveData<Message> responseReceived() { return response; }
    public LiveData<String> getResultToSend() { return result; }
    public LiveData<Message> getMessages() { return messages; }

    // 主動更新 Live Data
    public void setResponseReceived(Message message) {
        response.postValue(message);
    }
    public void setResultToSend(String resultText) {
        result.postValue(resultText);
    }
    public void setMessages(Message message) {
        messages.postValue(message);
    }
}
