package com.bigstock.biz.component;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class NodeIdentity {

    private final String nodeId = UUID.randomUUID().toString();

    public String getNodeId() {
        return nodeId;
    }
}
