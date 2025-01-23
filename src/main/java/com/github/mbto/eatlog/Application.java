package com.github.mbto.eatlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

@SpringBootApplication(exclude = {
        JooqAutoConfiguration.class,
        TaskSchedulingAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        CacheAutoConfiguration.class
})
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
public class Application implements SchedulingConfigurer {
    static {
        Properties systemProperties = System.getProperties();
        systemProperties.setProperty("org.jooq.no-logo", "true");
        systemProperties.setProperty("org.jooq.no-tips", "true");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);

        scheduler.setThreadGroupName("schedulers");
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setDaemon(false);
//        scheduler.setAllowCoreThreadTimeOut()
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(120);
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        return scheduler;
    }
}