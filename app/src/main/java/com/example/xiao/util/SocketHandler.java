package com.example.xiao.util;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.example.xiao.message.AudioMessage;
import com.example.xiao.message.ImageMessage;
import com.example.xiao.message.Message;
import com.example.xiao.message.TextMessage;
import com.example.xiao.viewmodel.MessagesViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SocketHandler implements Serializable {
    private final String serverName;
    private final int serverPort;
    private final transient Handler mainHandler;
    private transient Socket socket;
    private transient DataOutputStream out;
    private transient DataInputStream in;
    private final transient Context context;
    private boolean isConnected = false;
    private OnMessageReceivedListener messageListener;
    private ConnectionListener connectionListener;
    private final Map<String, Function<String, Message>> messageHandlers = new HashMap<>();
    private final MessagesViewModel messagesViewModel;

    private static final String TAG = SocketHandler.class.getSimpleName();

    public SocketHandler(Context context, String serverName, int serverPort, MessagesViewModel messagesViewModel) {
        this.context = context;
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.mainHandler = new Handler(context.getMainLooper());
        this.messagesViewModel = messagesViewModel;

        initializeMessageHandlers();

        this.messagesViewModel.getResultToSend().observeForever(result -> {
            if (result != null && !result.isEmpty()) {
                Log.d(TAG, "Sending result to server: " + result);
                sendData("text", result);
            }
        });
    }

    private void initializeMessageHandlers() {
        messageHandlers.put("text", TextMessage::new);
        messageHandlers.put("audio", msg -> new AudioMessage(android.util.Base64.decode(msg, android.util.Base64.DEFAULT)));
        messageHandlers.put("image", msg -> new ImageMessage(android.util.Base64.decode(msg, android.util.Base64.DEFAULT)));
    }

    public void connect(ConnectionListener listener) {
        this.connectionListener = listener;
        new Thread(() -> {
            try {
                socket = new Socket(serverName, serverPort);
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                isConnected = true; // 連接成功標誌
                Log.d(TAG, "Connected to server");

                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnected());
                }

                // 開始接收訊息
                startListening();

            } catch (Exception e) {
                Log.e(TAG, "Error connecting to server", e);
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDisconnected());
                }
            }
        }).start();
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (isConnected) {
                    int messageLength = in.readInt();
                    Log.d(TAG, "Expected message length: " + messageLength);
                    if (messageLength > 0) {
                        byte[] messageBytes = new byte[messageLength];
                        in.readFully(messageBytes, 0, messageLength);
                        String message = new String(messageBytes, StandardCharsets.UTF_8);
                        Log.d(TAG, "Message received: " + message);
                        handleMessage(message);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error receiving data from the server", e);
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDisconnected());
                }
            } finally {
                // 釋放資源
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error closing DataInputStream", e);
                }
            }
        }).start();
    }

    private void handleMessage(String message) {
        try {
            // 解析 JSON 格式的消息
            JSONObject json = new JSONObject(message);
            String dataType = json.getString("data_type");
            String output = json.getString("message");

            // 根據 data_type 進行不同的處理
            switch (dataType) {
                case "heartbeat":
                    Log.d(TAG, "Heartbeat received");
                    // 處理心跳消息
                    break;

                case "text":
                    // 假設這裡的 output 為 text 類型，可以根據實際情況調整
                    TextMessage receivedMessage = new TextMessage(output);
                    Log.d(TAG, "Received message: " + receivedMessage);
                    messagesViewModel.setResponseReceived(receivedMessage); // 通知 ViewModel 新消息
                    if (messageListener != null) {
                        mainHandler.post(() -> messageListener.onMessageReceived(receivedMessage));
                    } else {
                        Log.e(TAG, "MessageListener is null");
                    }
                    break;

                // 其他類型的消息處理
                default:
                    Log.e(TAG, "Unknown data type: " + dataType);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON message", e);
        } catch (Exception e) {
            Log.e(TAG, "Error handling message", e);
        }
    }

    public void sendData(String dataType, String data) {
        new Thread(() -> {
            try {
                // 構建 JSON 對象
                JSONObject json = new JSONObject();
                json.put("data_type", dataType);
                json.put("message", data);

                byte[] messageBytes = json.toString().getBytes(StandardCharsets.UTF_8);
                int messageLength = messageBytes.length;

                synchronized (this) {
                    if (!isConnected || out == null) {
                        Log.e("SocketHandler", "Not connected to server, cannot send data");
                        return;
                    }
                    Log.d("SocketHandler", "Sending data: " + json.toString());
                    out.writeInt(messageLength);
                    out.write(messageBytes);
                    out.flush();
                }
                Log.d("SocketHandler", "Data sent successfully");
            } catch (Exception e) {
                Log.e("SocketHandler", "Error sending data to the server", e);
            }
        }).start();
    }

    public void disconnect() {
        synchronized (this) {
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
                if (in != null) {
                    in.close();
                    in = null;
                }
                isConnected = false;
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDisconnected());
                }
            } catch (IOException e) {
                Log.e("SocketHandler", "Error closing socket", e);
            }
        }
    }

    public void close() {
        disconnect();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setMessageListener(OnMessageReceivedListener listener) {
        this.messageListener = listener;
    }

    public Context getContext() {
        return context;
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(Message message);
    }

    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
    }
}
