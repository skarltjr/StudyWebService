package com.studyolle.infra.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("app")
public class AppProperties {

    private String host;

    //# 웹 서버 호스트 가져온다 
    //app.host=http://localhost:8080
}
