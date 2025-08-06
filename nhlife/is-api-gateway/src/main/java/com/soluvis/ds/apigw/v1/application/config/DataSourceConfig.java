package com.soluvis.ds.apigw.v1.application.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariDataSource;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

/**
 * @Class 		: DataSourceConfig
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  DataSource 설정 클래스
 * 
 *  1. application properties의 db.config prefix를 읽어 Datasource 설정
 *  2. lockProvider 사용하여 배치 수행시 한개의 서버에서만 동작 하도록 설정.
 *  3. mybatis-config.xml 통하여 Alias 추가
 */
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {


	/**
	 * @Method		: lockProvider
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Shedlock 설정
	 * 
	 *  1. 분산 환경에서 배치가 한쪽에서만 동작하도록 설정
	 *  2. swm 스키마의 shedlock 테이블을 참조
	 */
	@Bean(name = "lockProvider")
    LockProvider lockProvider(@Qualifier("dataSource") DataSource dbDataSource) {
		return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
				.withTableName("swm.shedlock").withJdbcTemplate(new JdbcTemplate(dbDataSource)).usingDbTime().build());
    }

	/**
	 * @Method		: dbDataSource
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. db.config를 prefix로 Datasource 생성
	 */
	@Bean(name = "dataSource")
	@ConfigurationProperties(prefix = "db.config")
	DataSource dbDataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

	/**
	 * @Method		: dbSqlSessionFactory
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. Mapper 경로 설정
	 *  2. Config 파일 설정
	 */
	@Bean(name = "sessionFactory")
	SqlSessionFactory dbSqlSessionFactory(
			@Qualifier("dataSource") DataSource dbDataSource,
			ApplicationContext applicationContext) throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setDataSource(dbDataSource);
		sqlSessionFactoryBean.setMapperLocations(applicationContext.getResources("classpath:mapper/*/*/*/*.xml"));
		sqlSessionFactoryBean.setConfigLocation(applicationContext.getResource("classpath:config/mybatis-config.xml"));
		return sqlSessionFactoryBean.getObject();
	}

	/**
	 * @Method		: dbSqlSessionTemplateSimple
	 * @date   		: 2025. 3. 25.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  SqlSessionTemplate 설정
	 */
	@Bean(name = "sessionTemplate")
	SqlSessionTemplate dbSqlSessionTemplateSimple(
			@Qualifier("sessionFactory") SqlSessionFactory dbSqlSessionFactory){
		return new SqlSessionTemplate(dbSqlSessionFactory);
	}

	/**
	 * @Method		: txManager
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. 작업을 트랜잭션 단위로 묶을 경우 사용
	 *  2. @Transactional 어노테이션으로 메서드에 설정하여 사용
	 */
	@Bean(name = "transactionManager")
	PlatformTransactionManager txManager(
			@Qualifier("dataSource") DataSource ds){
		return new DataSourceTransactionManager(ds);
	}
}
