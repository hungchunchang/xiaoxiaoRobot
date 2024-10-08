package com.example.xiao2.util;

import com.example.xiao2.repository.DataRepository;

public interface HttpHandlerInterface {

    // Method for sending HTTP POST requests with JSON data
    void sendDataAndFetch(String resultString, String imgBase64);

    void setDataRepository(DataRepository dataRepository);

    interface ResponseListener{
        void onResponseReceived(String response);
        void onErrorReceived(Exception e);
    }

}