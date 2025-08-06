package com.soluvis.ds.apigw.v1.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * @Class 		: SchedulerConfig
 * @date   		: 2025. 3. 28.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. 스프링 스케줄 활성화
 *  2. 비동기 스케줄 활성화
 *  3. 스케줄락 활성화
 */
@Configuration
@EnableScheduling
@EnableAsync
@EnableSchedulerLock(defaultLockAtMostFor = "5s")
public class SchedulerConfig {

}
