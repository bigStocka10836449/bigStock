package com.bigstock.biz.infra;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.biz.dto.NewsDataInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewsStreamConsumer {

    private final StringRedisTemplate redisTemplate;

    private static final String STREAM_KEY = "news_stream_for_server";
    private static final String GROUP = "news_group";
    private static final String CONSUMER = "consumer_1";

    @PostConstruct
    public void start() {
        try {
//            redisTemplate.opsForStream().createGroup(
//                "news_stream_for_server",
//                ReadOffset.latest(),
//                "news_group"
//            );
            Executors.newSingleThreadExecutor().submit(this::consume);
        } catch (Exception e) {
            if (!e.getMessage().contains("BUSYGROUP")) {
                throw e;
            }
        }
    }

    public void consume() {
        while (true) {
            try {
                List<MapRecord<String, Object, Object>> messages =
                        redisTemplate.opsForStream().read(
                                Consumer.from(GROUP, CONSUMER),
                                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                        );

                if (messages != null) {
                    for (MapRecord<String, Object, Object> msg : messages) {

                        Map<Object, Object> value = msg.getValue();

                        String title = (String) value.get("title");
                        String content = (String) value.get("content");
                        String source = (String) value.get("source");
                        ObjectMapper objectMapper = new ObjectMapper();

                        List<NewsDataInfo> list = objectMapper.readValue(
                        		content,
                            new TypeReference<List<NewsDataInfo>>() {}
                        );
                        // 👉 YOUR LOGIC HERE
                        processNews(title, content, source);

                        // ✅ ACK
                        redisTemplate.opsForStream()
                                .acknowledge(STREAM_KEY, GROUP, msg.getId());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processNews(String title, String content, String source) {
        System.out.println("Processing: " + title);

        // 1. Save to DB
        // 2. Cache
        // 3. WebSocket push
    }
}
