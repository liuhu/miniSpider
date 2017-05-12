package com.acloudchina.m2m.spider.config;

import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by liuhu on 12/05/2017.
 */
@Configuration
public class RestTemplateConfig {
    @Value("${m2m.token}")
    private String token;

    @Bean
    public RestTemplate restTemplate() {

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient());
        RestTemplate restTemplate = new RestTemplateBuilder()
                // 设置字符集, 不然获取中文数据会乱码
                .additionalMessageConverters(new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .requestFactory(requestFactory)
                .build();
        restTemplate.getInterceptors().add(new HeaderRequestInterceptor("Authorization", token));

        return restTemplate;
    }

    private HttpClient httpClient() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(60000);
        factory.setConnectTimeout(60000);
        return factory.getHttpClient();
    }

    public class HeaderRequestInterceptor implements ClientHttpRequestInterceptor {
        private final String headerName;

        private final String headerValue;

        public HeaderRequestInterceptor(String headerName, String headerValue) {
            this.headerName = headerName;
            this.headerValue = headerValue;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            HttpRequest wrapper = new HttpRequestWrapper(request);
            wrapper.getHeaders().set(headerName, headerValue);
            return execution.execute(wrapper, body);
        }
    }
}
