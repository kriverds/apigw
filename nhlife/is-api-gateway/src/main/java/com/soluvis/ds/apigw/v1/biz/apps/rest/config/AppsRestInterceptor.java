package com.soluvis.ds.apigw.v1.biz.apps.rest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @Class 		: AppsRestInterceptor
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  1. RestAPI 호출 전 후로 로그 적재
 */
@Component
public class AppsRestInterceptor implements HandlerInterceptor{

	static final Logger logger = LoggerFactory.getLogger(AppsRestInterceptor.class);

	/**
	 * @Method		: preHandle
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Controller 호출 전 로그
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		
		logger.info("{} {} {}", "#PRH#",
				request.getMethod(), request.getRequestURI()+(request.getQueryString()==null?"":"?"+request.getQueryString()));

		return HandlerInterceptor.super.preHandle(request, response, handler);
	}

	/**
	 * @Method		: postHandle
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Controller 종료 후 로그
	 */
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		logger.info("{}\n{}", "#POH#", request.getRequestURI()+(request.getQueryString()==null?"":"?"+request.getQueryString()));
		HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
	}

}
