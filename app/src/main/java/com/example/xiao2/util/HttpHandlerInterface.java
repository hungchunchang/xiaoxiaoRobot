package com.example.xiao2.util;

import com.example.xiao2.repository.DataRepository;

public interface HttpHandlerInterface {

    // Method for sending HTTP POST requests with JSON data
    void sendDataAndFetch(String resultString, String imgBase64, String channel);

    void setDataRepository(DataRepository dataRepository);


}