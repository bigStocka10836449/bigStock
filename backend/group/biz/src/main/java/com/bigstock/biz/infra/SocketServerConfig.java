//package com.bigstock.biz.infra;
//
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.bigstock.biz.component.SocketIOHandler;
//import com.bigstock.biz.service.BizService;
//import com.bigstock.sharedComponent.service.SocketPushService;
//import com.corundumstudio.socketio.SocketIOServer;
//
//import lombok.RequiredArgsConstructor;
//
//@Configuration
//@RequiredArgsConstructor
//public class SocketServerConfig {
//	
//    private final SocketPushService socketPushService;
//	
//    @Bean(destroyMethod = "stop")
//    public SocketIOServer socketIOServer() {
//    	//netstat -ano | findstr :<PORT> Port被占用的話 netstat -plnt | grep :<PORT>
//    	com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
//        config.setPort(9092);
//        config.setOrigin("*");
//
//        SocketIOServer server = new SocketIOServer(config);
//        server.addListeners(new SocketIOHandler(socketPushService)); // 註冊事件處理器
//        server.start();
//
//        return server;
//    }
//}
