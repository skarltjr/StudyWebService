package com.studyolle.infra.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("app")   //spring_boot 에 정리해둔것과 마찬가지로 component로 올린 이 빈을 재정의해서 사용하기보단 빈으로 등록하고 application.properties에서 값만 수정하여 쓰도록
public class AppProperties {

    private String host;

    //# 웹 서버 호스트 가져온다 
    //app.host=http://localhost:8080
}
