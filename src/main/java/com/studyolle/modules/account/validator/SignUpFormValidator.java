package com.studyolle.modules.account.validator;


import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.form.SignUpForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component  //빈 등록
@RequiredArgsConstructor
public class SignUpFormValidator implements Validator {

    private final AccountRepository accountRepository;


   //SignUpForm타입을
    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.isAssignableFrom(SignUpForm.class);
    }
    //db에서 이메일 닉네임 등 중복같은건 검사해야지
    @Override
    public void validate(Object object, Errors errors) {
        //TODO email,nickname 검사
        SignUpForm signUpForm = (SignUpForm)object;
        if (accountRepository.existsByEmail(signUpForm.getEmail())) {
            //이미 db에 있으면 당연히 중복이지
            errors.rejectValue("email", "invalid.email", new Object[]{signUpForm.getEmail()}, "이미 사용중인 이메일 입니다");
        }
        if(accountRepository.existsByNickname(signUpForm.getNickname()))
        {
            //이메일도 마찬가지로 이미 db에있으면 중복이니까
            errors.rejectValue("nickname", "invalid.nickname", new Object[]{signUpForm.getNickname()}, "이미 사용중인 닉네임 입니다");
        }
    }
}
