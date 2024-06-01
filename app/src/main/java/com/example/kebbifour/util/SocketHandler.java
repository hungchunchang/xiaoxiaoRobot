package com.example.kebbifour.util;

import android.content.Context;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.example.kebbifour.message.AudioMessage;
import com.example.kebbifour.message.ImageMessage;
import com.example.kebbifour.message.Message;
import com.example.kebbifour.message.TextMessage;
import com.example.kebbifour.viewmodel.MessagesViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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
    private Timer heartbeatTimer;
    private MessagesViewModel messagesViewModel;

    private static final int HEARTBEAT_INTERVAL = 30000; // 心跳訊號間隔（毫秒）
    private static final String TAG = SocketHandler.class.getSimpleName();

    public SocketHandler(Context context, String serverName, int serverPort, MessagesViewModel messagesViewModel) {
        this.context = context;
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.mainHandler = new Handler(context.getMainLooper());
        this.messagesViewModel = messagesViewModel; // 使用傳入的實例

        initializeMessageHandlers();

        this.messagesViewModel.getResultToSend().observeForever(result -> {
            if (result != null && !result.isEmpty()) {
                Log.d(TAG, "Sending result to server: " + result);
                sendData(result, "text");
            }
        });
    }

    private void initializeMessageHandlers() {
        messageHandlers.put("text", TextMessage::new);
        messageHandlers.put("audio", msg -> new AudioMessage(Base64.decode(msg, Base64.DEFAULT)));
        messageHandlers.put("image", msg -> new ImageMessage(Base64.decode(msg, Base64.DEFAULT)));
    }

    public void connect(ConnectionListener listener) {
        this.connectionListener = listener;
        new Thread(() -> {
            try {
                socket = new Socket(serverName, serverPort);
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                isConnected = true; // 連接成功標誌
                Log.d("SocketHandler", "Connected to server");

                startHeartbeat();

                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnected());
                }

                // 開始接收訊息
                startListening();

            } catch (Exception e) {
                Log.e("SocketHandler", "Error connecting to server", e);
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDisconnected());
                }
            }
        }).start();
    }

    private void startHeartbeat() {
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendData("", "heartbeat");
            }
        }, 0, HEARTBEAT_INTERVAL);
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
            JSONObject json = new JSONObject(message);
            String dataType = json.getString("data_type");
            String msg = json.getString("message");

            Function<String, Message> handler = messageHandlers.get(dataType);
            if (handler != null) {
                Message receivedMessage = handler.apply(msg);
                Log.d(TAG, "Received message: " + receivedMessage);
                messagesViewModel.setResponseReceived(receivedMessage); // 通知 ViewModel 新消息
                if (messageListener != null) {
                    mainHandler.post(() -> messageListener.onMessageReceived(receivedMessage));
                } else {
                    Log.e(TAG, "MessageListener is null");
                }
            } else {
                Log.e(TAG, "Unsupported data type: " + dataType);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON message", e);
        }
    }
    public void sendData(Object data, String dataType) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("data_type", dataType);

                switch (dataType) {
                    case "text":
                    case "heartbeat":
                        json.put("message", data);
                        break;
                    case "image": {
                        byte[] fileData;
                        if (data instanceof File) {
                            fileData = readFileToByteArray((File) data);
                        } else {
                            fileData = (byte[]) data;
                        }
                        String encodedData = Base64.encodeToString(fileData, Base64.DEFAULT);
                        json.put("message", encodedData);
                        break;
                    }
                    case "audio": {
                        byte[] audioData = (byte[]) data;
                        String encodedData = Base64.encodeToString(audioData, Base64.DEFAULT);
                        json.put("message", encodedData);
                        break;
                    }
                    default:
                        Log.e("SocketHandler", "Unsupported data type: " + dataType);
                        return;
                }

                byte[] messageBytes = json.toString().getBytes(StandardCharsets.UTF_8);
                int messageLength = messageBytes.length;

                synchronized (this) {
                    if (!isConnected || out == null) {
                        Log.e("SocketHandler", "Not connected to server, cannot send data");
                        return;
                    }
                    Log.d("SocketHandler", "Sending data: " + json);
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


    private byte[] readFileToByteArray(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        }
        return bos.toByteArray();
    }

    public void disconnect() {
        synchronized (this) {
            try {
                if (heartbeatTimer != null) {
                    heartbeatTimer.cancel();
                    heartbeatTimer = null;
                }
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
