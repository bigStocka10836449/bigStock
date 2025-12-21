//package com.bigstock.biz.component;
//
//import org.redisson.api.RedissonClient;
//import org.springframework.stereotype.Component;
//
//import com.bigstock.sharedComponent.service.SocketPushService;
//import com.corundumstudio.socketio.SocketIOClient;
//import com.corundumstudio.socketio.annotation.OnConnect;
//import com.corundumstudio.socketio.annotation.OnDisconnect;
//
//import lombok.RequiredArgsConstructor;
//
//@Component
//@RequiredArgsConstructor
//public class SocketIOHandler {
//    private final SocketPushService socketPushService;
//
//    @OnConnect
//    public void onConnect(SocketIOClient client) {
//        String sessionId = client.getHandshakeData().getSingleUrlParam("sessionId");
//        String userId = socketPushService.getClientId(sessionId); // 假設 sessionId 映射 userId
//        if (userId != null) {
//            socketPushService.registerClient(userId, client);
//        } else {
//            client.disconnect();
//        }
//    }
//
//    @OnDisconnect
//    public void onDisconnect(SocketIOClient client) {
//        socketPushService.removeClient(client);
//    }
//}
