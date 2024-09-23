package com.cusob.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "customThreadPool")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：线程池创建时初始化的线程数
        executor.setCorePoolSize(10);

        // 最大线程数：线程池最大能容纳的线程数
        executor.setMaxPoolSize(20);

        // 队列容量：线程池中的任务队列最大容量
        executor.setQueueCapacity(100);

        // 线程名称前缀：方便日志记录
        executor.setThreadNamePrefix("MyThreadPool-");

        // 线程空闲时间：当线程超过核心线程数时，多余的线程在等待任务时能存活的最大时间
        executor.setKeepAliveSeconds(60);

        // 初始化线程池
        executor.initialize();

        return executor;
    }
}
