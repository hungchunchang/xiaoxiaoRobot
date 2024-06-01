package com.example.kebbifour.message;
// 筆記：不要設為靜態，這樣每一個對象都會有自己的 audioData
public class AudioMessage extends Message {
    private byte[] audioData;

    public AudioMessage(byte[] audioData) {
        this.audioData = audioData;
    }
    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData){
        this.audioData = audioData;
    }

    // getter, setter, and other methods
}
