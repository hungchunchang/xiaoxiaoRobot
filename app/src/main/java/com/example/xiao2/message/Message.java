package com.example.xiao2.message;

public abstract class Message {
    public int length;
    private long timestamp;

    public Message() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


}
