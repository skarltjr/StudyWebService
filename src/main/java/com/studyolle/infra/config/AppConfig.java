package com.studyolle.infra.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.NameTokenizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AppConfig {
    //패스워드를 위한 .
    //빈 등록홰주고
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    //만 해주면 패스워드를 평문 절대 x -> 알고리즘으로 복호화


    @Bean  //모델 매퍼 매 번 새로만들긴 그러니까 빈으로 등록해서 사용
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setDestinationNameTokenizer(NameTokenizers.UNDERSCORE)
                .setSourceNameTokenizer(NameTokenizers.UNDERSCORE);
        return modelMapper;
    }
}
