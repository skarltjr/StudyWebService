package com.studyolle.infra.config;

import com.studyolle.modules.notification.NotificationInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor    //스프링부트가 기본적으로 제공하는 자동설정을 사용하기위해 @EnableWebMvc는 사용하지 않는다
public class WebConfig implements WebMvcConfigurer {    //mvc 설정을 위해 configuerer impl

    private final NotificationInterceptor notificationInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
     //   registry.addInterceptor(notificationInterceptor);
        //이렇게하면 모든!! 뷰에 추가.  근데 리다이렉트는 인터셉터에서 걸렀지만 시큐리티에서 설정한 modules/*, 내부에
        // 적용된 css/* js/* 등
        //static요소들도 걸러내야한다.

        List<String> staticResourcesPath = Arrays.stream(StaticResourceLocation.values())
                .flatMap(StaticResourceLocation::getPatterns)
                .collect(Collectors.toList());
        staticResourcesPath.add("/node_modules/**");
/** StaticResourceLocation에 내부적인 css js 등 요소들이 리스트로 되어있는 것들을
 *  flatmap으로 string 이름(css/** ,~~등등)배열로 바꾼다음 리스트로 감아서 사용*/

        registry.addInterceptor(notificationInterceptor)
                .excludePathPatterns(staticResourcesPath);
    }
}
