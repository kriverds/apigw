package com.soluvis.ds.apigw.v1.application.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

/**
 * @Class 		: JasyptConfig
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  비밀번호 암호화 관련 Jasypt 설정
 * 
 *  1. Application 실행 시 propertie 파일의 암호화를 풀어줌
 *  2. Properties 파일의 ENC($#%#$@%@) 부분
 */
@Configuration
@EnableEncryptableProperties
public class JasyptConfig {
	
	static final Logger logger = LoggerFactory.getLogger(JasyptConfig.class);

	static final String DECODE_PROPERTY_KEY = "jasyptPassword";
	static final String ALGORITHM = "PBEWithMD5AndDES";
	static final String KEY_OBJECTION_ITERATIONS = "1000";
	static final String POOL_SIZE = "1";
	static final String PROVIDER_NAME = "SunJCE";
	static final String SALT_GENERATOR_CLASS_NAME = "org.jasypt.salt.RandomSaltGenerator";
	static final String STRING_OUTPUT_TYPE = "base64";

	@Bean("jasyptStringEncryptor")
	StringEncryptor stringEncryptor() {
		PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
		SimpleStringPBEConfig config = new SimpleStringPBEConfig();
		config.setPassword(System.getProperty(DECODE_PROPERTY_KEY));
		config.setAlgorithm(ALGORITHM);
		config.setKeyObtentionIterations(KEY_OBJECTION_ITERATIONS);
		config.setPoolSize(POOL_SIZE);
		config.setProviderName(PROVIDER_NAME);
		config.setSaltGeneratorClassName(SALT_GENERATOR_CLASS_NAME);
		config.setStringOutputType(STRING_OUTPUT_TYPE);
		encryptor.setConfig(config);
		
		String enc = encryptor.encrypt("SWMadmin1310!!D");
		logger.info("{}", enc);
		logger.info("{}", encryptor.decrypt(enc));
		
		enc = encryptor.encrypt("SWM");
		logger.info("{}", enc);
		logger.info("{}", encryptor.decrypt(enc));
		
		return encryptor;
	}
}
