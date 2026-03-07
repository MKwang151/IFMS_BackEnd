package com.mkwang.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig implements SchedulingConfigurer {

    /**
     * Scheduler pool — cho @Scheduled tasks (cron jobs, periodic cleanup).
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("ifms-scheduler-");
        taskScheduler.initialize();
        taskRegistrar.setScheduler(taskScheduler);
    }

    /**
     * Default async executor — cho @Async tasks chung (push notification, etc.).
     * Bean name "taskExecutor" là default executor cho @Async.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ifms-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated mail executor — isolated thread pool for email sending.
     * <p>
     * WHY a separate pool instead of reusing taskExecutor?
     * <ul>
     *   <li><b>Isolation:</b> SMTP calls are I/O-bound and can be slow (2-5s per email).
     *       A slow mail server shouldn't starve other @Async tasks (notifications, audits).</li>
     *   <li><b>Backpressure:</b> CallerRunsPolicy means if all 5 threads are busy AND
     *       the queue (50) is full, the caller thread sends the email itself instead of
     *       dropping it — guaranteeing no email is silently lost.</li>
     *   <li><b>Sizing:</b> core=2, max=5 is enough for normal load. Payroll blast (500 emails)
     *       goes through Redis queue → Worker pops batchSize=10 → 5 threads handle them.
     *       Next batch starts after fixedDelay. No thread explosion.</li>
     * </ul>
     */
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ifms-mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);          // Wait longer — let in-flight emails finish
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated audit executor — isolated thread pool for async audit log persistence.
     * <p>
     * WHY a separate pool?
     * <ul>
     *   <li><b>Isolation:</b> Audit writes should not compete with notification pushes
     *       or other @Async tasks in the general pool.</li>
     *   <li><b>Backpressure:</b> CallerRunsPolicy ensures no audit event is silently
     *       dropped — if queue is full, the caller thread writes the log itself.</li>
     *   <li><b>Sizing:</b> core=2, max=5 handles normal load. Burst events (e.g. bulk
     *       user import) queue up to 200 items before triggering backpressure.</li>
     * </ul>
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ifms-audit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
