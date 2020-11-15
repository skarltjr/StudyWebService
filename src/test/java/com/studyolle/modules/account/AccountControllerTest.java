package com.studyolle.modules.account;

import com.studyolle.infra.mail.EmailMessage;
import com.studyolle.infra.mail.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;

    @MockBean //이메일 보내는지 확인
    EmailService emailService;


    /** //form을 보내는 =  post 테스트는 csrf넣어줘야한다*/

    @Test
    @DisplayName("인증 메일 확인 - 입력값 오류")
    void checkEmailToken_with_wrong_input() throws Exception {
        mockMvc.perform(get("/check-email-token")
                .param("token", "qweqweqwe") //토큰이상하니까
                .param("email", "email@email.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("account/checked-email"))
                .andExpect(unauthenticated()); // 지금은 오류니까 당연히 인증이 안된상태
        //status는 ok지만 에러가 있을거다
    }


    @Test       //트랜잭셔널 위에 추가했다
    @DisplayName("인증 메일 확인 - 입력값 정상")
    void checkEmailToken() throws Exception {

        Account account = Account.builder()
                .email("test@naver.com")
                .password("12345678")
                .nickname("kiseok")
                .build();
        Account newAccount = accountRepository.save(account);
        newAccount.generateEmailCheckToken(); /** !!!!! 이거 get으로 잘못적어놨다가 */

        mockMvc.perform(get("/check-email-token")
                .param("token",newAccount.getEmailCheckToken())
                .param("email", newAccount.getEmail()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error")) //정상이면 에러는 없고
                .andExpect(model().attributeExists("nickname")) //닉네임이랑
                .andExpect(model().attributeExists("numberOfUser")) //있어야한다
                .andExpect(view().name("account/checked-email"))
                .andExpect(authenticated().withUsername("kiseok")); //가입하고 인증까지된다면 이름도 확인할 수 있으니
        //status는 ok지만 에러가 있을거다
    }

    @Test
    @DisplayName("회원가입화면 보이는지 테스트")
    void signUpForm() throws Exception {

        mockMvc.perform(get("/sign-up"))  //security config에서 회원가입은 바로 접근가능하도록 해서 테스트통과 그 부분주석하면 실패
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"))
                .andExpect(unauthenticated());
    }

    @DisplayName("회원 가입 처리 - 입력값 오류")
    @Test
    void signUpSubmit_with_wrong_input() throws Exception {
        mockMvc.perform(post("/sign-up")
                .param("nickname", "kiseok")

                .param("email", "email..")   //이메일 양식오류
                .param("password", "12345")  //5글자면 글자수부족오류
                .with(csrf()))  //form을 보내는 =  post 테스트는 csrf넣어줘야한다
                .andExpect(status().isOk()) //정상이면 200
                .andExpect(view().name("account/sign-up"))// 오류나면 뷰가 다시 회원가입창을 보여줄것
                .andExpect(unauthenticated());
    }

    @DisplayName("회원 가입 처리 - 입력값 정상")
    @Test
    void signUpSubmit_with_correct_input() throws Exception {
        mockMvc.perform(post("/sign-up")
                .param("nickname", "kiseok")
                .param("email", "kisa0828@naver.com")
                .param("password", "ganadaramabasa")
                .with(csrf()))
                .andExpect(status().is3xxRedirection()) //정상이면 redirection응답이 나온다
                .andExpect(view().name("redirect:/")) //당연히 정상가입이면 첫 화면으로 돌아간다
                .andExpect(authenticated().withUsername("kiseok")); //가입하고 인증까지된다면 이름도 확인할 수 있으니

        Account account = accountRepository.findByEmail("kisa0828@naver.com");
        //저장된 계정 이메일 확인

        assertNotNull(account);
        // 위로 대체 assertThat(accountRepository.existsByemail("kisa0828@naver.com"));

        //해싱 처리를 하니까 입력한 패스워드와 다르게 만들어져서 저장되어있을테니
        assertNotEquals(account.getPassword(), "ganadaramabasa");

        //토큰이 널이아닌지도
        assertNotNull(account.getEmailCheckToken());

        then(emailService).should().sendEmail(any(EmailMessage.class));
        //심플메일메세지라는 타입의 아무것이나 보내는지만 확인

    }




}