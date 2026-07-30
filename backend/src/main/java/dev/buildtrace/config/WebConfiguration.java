package dev.buildtrace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class WebConfiguration {

    @Bean(destroyMethod = "close")
    ExecutorService generationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

}
