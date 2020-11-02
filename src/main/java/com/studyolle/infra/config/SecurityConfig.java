package com.studyolle.infra.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity// 아래 일부들은 시큐리티 체크를 안해도 접근가능하도록
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    //이 부분은 accountservice의 userdetail 가져와서 주입하기위해
    private final UserDetailsService userDetailsService;   // 모듈 인프라 분리
    private final DataSource dataSource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers("/", "/login", "/sign-up", "/check-email-token",
                        "/email-login", "/login-by-email", "/search/study").permitAll()
                .mvcMatchers(HttpMethod.GET, "/profile/*").permitAll()
                .anyRequest().authenticated();
        /** 이 내용들은 그냥 접근 허용하겠다  / 프로필은 get만 바로접근 가능하도록 나머지는 다 로그인해야한다 */

        http.formLogin()    //로그인은 스프링 시큐리티에게 맡긴다.
                .loginPage("/login").permitAll();   //로그인 페이지 지정해준다

        http.logout()
                .logoutSuccessUrl("/"); //로그아웃 했을 때 어디로 갈지만 지정

        /**  해커가 쿠키가져다 계정탈취하는 걸 최대한 안정하게 보호*/
        http.rememberMe()
                .userDetailsService(userDetailsService)
                .tokenRepository(tokenRepository());
    }

    @Bean
    public PersistentTokenRepository tokenRepository()
    {
        JdbcTokenRepositoryImpl jdbcTokenRepository = new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource); //jdbc는 데이터소스가 필요한데 jpa를 쓰기때문에
        // private final DataSource dataSource; 가져다 쓰기만하면된다
        return jdbcTokenRepository;

        //JdbcTokenRepositoryImpl 에 맞는 테이블생성을 위해 엔티티 추가 -> PersistentLogins
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .mvcMatchers("/node_modules/**")// 앞서 노드-모듈은 위 시큐리티 안걸러지도록 설정한
                //부분에 포함되어있지 않다 그래서 당연히 걸러지는데  여기서 이그노어 추가해줘서 그냥 넘어가도록
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }
    /** static 에 있는 이미지들이 시큐리티에 걸려서 깨짐. 시큐리티 안거쳐도 된다고 설정 */
}
