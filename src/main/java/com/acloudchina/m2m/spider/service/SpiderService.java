package com.acloudchina.m2m.spider.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by liuhu on 12/05/2017.
 */

@Service
@Slf4j
public class SpiderService {
    @Value("${m2m.from}")
    private Integer from;

    @Value("${m2m.to}")
    private Integer to;

    @Value("${m2m.sleep}")
    private Integer sleep;

    @Value("${m2m.baseUrl}")
    private String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    private BlockingQueue<Integer> queue;

    private ExecutorService processorPool;


    @PostConstruct
    public void init() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(2000);
        int size = to - from;
        this.queue = new ArrayBlockingQueue<>(size + 1);
        IntStream.rangeClosed(from, to).sorted().forEach(
                x -> {
                    try {
                        queue.put(x);
                    } catch (Exception e) {
                        log.error("Init ERROR");
                    }
                }
        );

        processorPool = Executors.newFixedThreadPool(5, new ProcessorsThreadFactory());
        for (int i = 0; i < 5; i++) {
            processorPool.execute(new BlockingMessageProcessor(queue));
        }
    }

    @PreDestroy
    public void destroy() {

    }

    /** Used for naming processor threads */
    private class ProcessorsThreadFactory implements ThreadFactory {

        /** Counts threads */
        private AtomicInteger counter = new AtomicInteger();

        public Thread newThread(Runnable r) {
            return new Thread(r, "Spider" + counter.incrementAndGet());
        }
    }

    private class BlockingMessageProcessor implements Runnable {

        private BlockingQueue<Integer> queue;

        public BlockingMessageProcessor(BlockingQueue<Integer> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {

            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleep);
                    Integer simId = queue.take();
                    try {
                        ResponseEntity<String> countSimEntity = restTemplate.getForEntity(generateUrl(simId), String.class);
                        log.info(countSimEntity.getBody());
                    } catch (Exception e) {
                        log.error("ERROR:" + simId);
                    }
                } catch (Throwable e) {
                    log.error("Unhandled exception processing.{}", e);
                }
            }
        }
    }

    private String generateUrl(int simId) {
        return baseUrl + simId;
    }
}
