package com.studyolle.modules.main;


import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.AccountService;
import com.studyolle.modules.account.form.SignUpForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MainControllerTest {

    // .with(csrf()))  //post 처럼 form을 전송할 땐 필수
    @Autowired
    MockMvc mockMvc;
    @Autowired
    AccountService accountService;
    @Autowired
    AccountRepository accountRepository;

    @BeforeEach
    void beforeEach() {
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setEmail("kisa0828@naver.com");
        signUpForm.setNickname("kiseok");
        signUpForm.setPassword("123456789");
        accountService.processNewAccount(signUpForm);
    }

    //이렇게하면 매 테스트마다 디비에 똑같은걸 저장시키니 중복된다  그래서 한 번 테스트 끝나고 비우고 다시 만들고 ..
    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }


    @Test
    @DisplayName("이메일로 로그인 성공")
    void login_with_email() throws Exception {
        //이걸 확인하려면 먼저 db에 들어있어야 당연히확인가능 save먼저
        mockMvc.perform(post("/login")
                .param("username", "kisa0828@naver.com")
                .param("password", "123456789")
                .with(csrf()))  //post 처럼 form을 전송할 땐 필수
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("kiseok"));
        //왜 kiseok이라는 이름 . 이메일이 아닌 이름으로 로그인.인증이 확인되는것일까 ?
        // == principle을 UserAccount에서 ->  super(account.getNickname(), account.getPassword(),
        //nickname으로 채워서 principle 객체만드니까
    }

    @Test
    @DisplayName("닉네임으로 로그인 성공")
    void login_with_nickname() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "kiseok")
                .param("password", "123456789")
                .with(csrf()))  //post 처럼 form을 전송할 땐 필수
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("kiseok"));
    }

    @DisplayName("로그인 실패")
    @Test
    void login_fail() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "123123123")
                .param("password", "123456789")
                .with(csrf()))  //post 처럼 form을 전송할 땐 필수
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error")) // 리다이렉션이 에러페이지로
                .andExpect(unauthenticated());
    }

    @DisplayName("로그아웃")
    @Test
    void logout() throws Exception {
        mockMvc.perform(post("/logout")
                .with(csrf()))  //post 처럼 form을 전송할 땐 필수
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/")) // 리다이렉션이 홈으로
                .andExpect(unauthenticated());
    }
}