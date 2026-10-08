package com.bigstock.biz.infra;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bigstock.biz.dto.NewsDataInfo;
import com.bigstock.biz.schedule.GraspTWPMOnTheFly;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsStreamConsumer {

    private final StringRedisTemplate redisTemplate;

    private final GraspTWPMOnTheFly graspTWPMOnTheFly;

    private static final String STREAM_KEY = "news_stream_for_server";
    private static final String GROUP = "news_group";
    private static final String CONSUMER = "consumer_1";


    // ============================================================
    // Consumer Thread
    // ============================================================

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private volatile boolean running = false;


    @PostConstruct
    public void start() {

        running = true;

        executorService.submit(this::consume);

        log.info("NewsStreamConsumer started");
    }


    public void consume() {

        while (running && !Thread.currentThread().isInterrupted()) {

            try {

                List<MapRecord<String, Object, Object>> messages =
                        redisTemplate.opsForStream().read(
                                Consumer.from(
                                        GROUP,
                                        CONSUMER
                                ),
                                StreamReadOptions.empty()
                                        .count(10)
                                        .block(Duration.ofSeconds(2)),
                                StreamOffset.create(
                                        STREAM_KEY,
                                        ReadOffset.lastConsumed()
                                )
                        );


                if (messages != null) {

                    for (MapRecord<String, Object, Object> msg : messages) {

                        /*
                         * Spring may start shutting down while
                         * messages are being processed.
                         */
                        if (!running) {
                            break;
                        }


                        Map<Object, Object> value =
                                msg.getValue();


                        String title =
                                (String) value.get("title");

                        String content =
                                (String) value.get("content");

                        String source =
                                (String) value.get("source");


                        ObjectMapper objectMapper =
                                new ObjectMapper();


                        List<NewsDataInfo> list =
                                objectMapper.readValue(
                                        content,
                                        new TypeReference<List<NewsDataInfo>>() {}
                                );


                        processNews(
                                title,
                                list,
                                source
                        );


                        /*
                         * ACK only after processing succeeds.
                         */
                        redisTemplate.opsForStream()
                                .acknowledge(
                                        STREAM_KEY,
                                        GROUP,
                                        msg.getId()
                                );
                    }
                }

            } catch (Exception e) {

                /*
                 * Application is shutting down.
                 *
                 * Do not try Redis again because
                 * LettuceConnectionFactory may already
                 * be shutting down.
                 */
                if (!running ||
                        Thread.currentThread().isInterrupted()) {

                    log.info(
                            "NewsStreamConsumer stopping..."
                    );

                    break;
                }


                /*
                 * Real runtime error.
                 */
                log.error(
                        "NewsStreamConsumer consume failed",
                        e
                );


                /*
                 * Prevent an infinite high-speed retry loop
                 * if Redis is temporarily unavailable.
                 */
                try {

                    Thread.sleep(1000);

                } catch (InterruptedException interruptedException) {

                    Thread.currentThread().interrupt();

                    break;
                }
            }
        }


        log.info(
                "NewsStreamConsumer stopped"
        );
    }


    @PreDestroy
    public void shutdown() {

        log.info(
                "Stopping NewsStreamConsumer..."
        );


        /*
         * Tell consume() to leave its loop.
         */
        running = false;


        /*
         * Interrupt the consumer thread.
         *
         * Important because Redis read() may currently
         * be blocking for up to 2 seconds.
         */
        executorService.shutdownNow();


        try {

            if (!executorService.awaitTermination(
                    5,
                    TimeUnit.SECONDS
            )) {

                log.warn(
                        "NewsStreamConsumer did not stop within 5 seconds"
                );
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }


    private void processNews(
            String title,
            List<NewsDataInfo> newsDataInfos,
            String source
    ) throws JsonProcessingException {

        log.info(
                "Processing: {}",
                title
        );


        ObjectMapper objectMapper =
                new ObjectMapper();


        for (NewsDataInfo newsDataInfo : newsDataInfos) {

            String newsDataInfoStr =
                    objectMapper.writeValueAsString(
                            newsDataInfo
                    );


            graspTWPMOnTheFly.newsDataBroadCase(
                    newsDataInfoStr,
                    newsDataInfo.getData().getTitle(),
                    newsDataInfo.getData().getContent()
            );
        }
    }


    public List<Map<String, Object>> getAllNews(
            int limit
    ) {

        List<MapRecord<String, Object, Object>> records =
                redisTemplate.opsForStream().range(
                        STREAM_KEY,
                        Range.unbounded(),
                        Limit.limit().count(limit)
                );


        List<Map<String, Object>> result =
                new ArrayList<>();


        for (MapRecord<String, Object, Object> record : records) {

            Map<String, Object> map =
                    new HashMap<>();


            map.put(
                    "id",
                    record.getId().getValue()
            );

            map.put(
                    "data",
                    record.getValue()
            );


            result.add(map);
        }


        return result;
    }
}