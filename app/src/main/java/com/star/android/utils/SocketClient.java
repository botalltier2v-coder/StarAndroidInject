package com.star.android.utils;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketClient {
    private static final String SOCKET_NAME = "StarcoolPRO_socket";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void send(final String message) {
        executor.execute(() -> {
            try (LocalSocket socket = new LocalSocket()) {
                socket.connect(new LocalSocketAddress(SOCKET_NAME, LocalSocketAddress.Namespace.ABSTRACT));
                OutputStream out = socket.getOutputStream();
                out.write(message.getBytes());
                out.flush();
            } catch (Exception ignored) {
            }
        });
    }

    public static void setJump(boolean active) {
        send("JUMP_HACK:" + (active ? "1" : "0"));
    }

    public static void setScore(boolean active) {
        send("SCORE_HACK:" + (active ? "1" : "0"));
    }

    public static void setSpeedValue(float value) {
        send("SPEED_VAL:" + value);
    }

    public static void setGetCoin(boolean active) {
        send("GET_COIN:" + (active ? "1" : "0"));
    }

    public static void setNoClip(boolean active) {
        send("NOCLIP_HACK:" + (active ? "1" : "0"));
    }
}
