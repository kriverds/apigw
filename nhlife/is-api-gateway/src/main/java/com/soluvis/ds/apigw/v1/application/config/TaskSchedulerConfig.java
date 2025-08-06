package com.soluvis.ds.apigw.v1.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @Class 		: TaskSchedulerConfig
 * @date   		: 2025. 3. 28.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  SpringSchedule Config Class
 */
@Configuration
public class TaskSchedulerConfig {

	/**
	 * @Method		: taskScheduler
	 * @date   		: 2025. 3. 28.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 최대 쓰레드풀 10개로 설정
	 */
	@Bean
	ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(10);
		scheduler.setThreadNamePrefix("SchedulerTask-");
		scheduler.initialize();
		return scheduler;
	}
}
