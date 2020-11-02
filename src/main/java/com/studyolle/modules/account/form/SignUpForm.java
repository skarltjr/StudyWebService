package com.studyolle.modules.account.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class SignUpForm {

    @NotBlank//비어있으면 안된다
    @Length(min=3,max = 20)
    @Pattern(regexp = "^[ㄱ-ㅎ가-힣a-z0-9_-]{3,20}$")  //닉네임 패턴. 한글 영어 숫자 다 지원하도록
    // ^ = 시작 $ = 끝 그 안에 한 영 숫자 다 가능하도록
    private String nickname;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Length(min=8,max = 50)
    private String password;
}
