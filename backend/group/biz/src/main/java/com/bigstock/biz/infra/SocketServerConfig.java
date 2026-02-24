package com.bigstock.biz.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bigstock.biz.component.SocketIOHandler;
import com.corundumstudio.socketio.SocketIOServer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SocketServerConfig {

    private final SocketIOHandler socketIOHandler;

    @Value("${bigstock.socketio.host:0.0.0.0}")
    private String host;

    @Value("${bigstock.socketio.port:9092}")
    private int port;

    @Bean(destroyMethod = "stop")
    public SocketIOServer socketIOServer() {

        com.corundumstudio.socketio.Configuration config =
                new com.corundumstudio.socketio.Configuration();
        //netstat -ano | findstr :<PORT> Port被占用的話 netstat -plnt | grep :<PORT>
        config.setHostname(host);
        config.setPort(port);
        config.setOrigin("*");
        config.setPingInterval(25000);  // client ping every 25s
        config.setPingTimeout(60000);   // timeout if no pong in 60s

        SocketIOServer server = new SocketIOServer(config);
        server.addListeners(socketIOHandler);

        server.start();
        return server;
    }
}
