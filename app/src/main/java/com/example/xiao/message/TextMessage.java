package com.example.xiao.message;



public class TextMessage extends Message {
    private final String Text;
    public TextMessage(String text) {
        this.Text = text;
    }

    public String getText() {
        return Text;
    }
}
