package com.soluvis.ds.apigw.v1.biz.apps.rest.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Class 		: AppsRestWebMvcConfig
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. /v1/api/apps/** URL에 Interceptor 추가
 */
@Configuration
public class AppsRestWebMvcConfig implements WebMvcConfigurer {

	/**
	 * 스프링 DI
	 */
	AppsRestInterceptor interceptor;
	public AppsRestWebMvcConfig(AppsRestInterceptor interceptor){
		this.interceptor = interceptor;
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		WebMvcConfigurer.super.extendMessageConverters(converters);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		WebMvcConfigurer.super.addInterceptors(registry) ;

		registry.addInterceptor(interceptor).addPathPatterns("/api/v1/apps/**");
	}

}