package com.studyolle.infra.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors(); //프로세스 갯수 가져오고
        log.info("processors count {}",processors);
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors * 2);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("AsyncExecutor~"); // 이름주면 나중에 로그볼때 편하니까
        executor.initialize(); // 호출해줘야
        return executor;
    }

    /**
     * 처리할 태스크(이벤트)가 생겼을 때,
     * ‘현재 일하고 있는 쓰레드 개수’(active thread)가 ‘코어 개수’(core pool size)보다 작으면 남아있는 쓰레드를 사용한다.
     * ‘현재 일하고 있는 쓰레드 개수’가 코어 개수만큼 차있으면 ‘큐 용량’(queue capacity)이 찰때까지 큐에 쌓아둔다.
     * 큐 용량이 다 차면, 코어 개수를 넘어서 ‘맥스 개수’(max pool size)에 다르기 전까지 새로운 쓰레드를 만들어 처리한다.
     * 맥스 개수를 넘기면 태스크를 처리하지 못한다.
     * =========================================
     * 즉  setCorePoolSize -> 풀장에 튜브 10개 있는데 3명만 사용하고있으면 4번째사람이 오면 그냥 4번째 튜브주면된다 .
     * setQueueCapacity -> 근데 만약 10명이 다 쓰고있는데 새로운 사람이 오면 큐 -> 줄을 세운다,
     *  setMaxPoolSize  -> 근데 이 큐 -> 줄도 50명 제한인데 줄까지 꽉찼다. 그러면 풀장의 사용가능 튜브를 10 -> 20개로 늘린다 .
     *  그런데도 더 오면 그건 거절
     *  setKeepAliveSeconds -> 최적의 사용 가능인원은 처음 10명인데 꽉차서 꾸역꾸역 새로 만든setMaxPoolSize를 얼마후에 다시 정리할건지
     *  */
}
