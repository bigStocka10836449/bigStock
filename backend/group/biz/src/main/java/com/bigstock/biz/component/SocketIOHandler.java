package com.bigstock.biz.component;

import org.springframework.stereotype.Component;

import com.bigstock.biz.service.SocketPushService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SocketIOHandler {

    private final SocketPushService socketPushService;

    @OnConnect
    public void onConnect(SocketIOClient client) {
        socketPushService.registerClient(client);
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        socketPushService.removeClient(client);
    }
}