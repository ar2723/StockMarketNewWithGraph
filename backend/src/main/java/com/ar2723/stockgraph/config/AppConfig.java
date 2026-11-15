package com.ar2723.stockgraph.config;

import com.ar2723.stockgraph.collector.CollectorProperties;
import com.ar2723.stockgraph.collector.upbit.UpbitClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(CollectorProperties.class)
public class AppConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	UpbitClient upbitClient(CollectorProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(5));
		requestFactory.setReadTimeout(Duration.ofSeconds(10));
		RestClient restClient = RestClient.builder()
			.baseUrl(properties.upbitBaseUrl())
			.requestFactory(requestFactory)
			.build();
		return new UpbitClient(restClient, properties.upbitRequestInterval());
	}

}
