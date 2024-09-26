package com.example.xiao2.message;

public class ImageMessage extends Message {
    private final byte[] imageData;

    public ImageMessage(byte[] imageData) {
        this.imageData = imageData;
    }

    public byte[] getImage() {
        return imageData;
    }

    // getter, setter, and other methods
}
