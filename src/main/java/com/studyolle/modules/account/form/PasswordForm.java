package com.studyolle.modules.account.form;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
//@NoArgsConstructor -> modelAttribute는 기본생성자를 알아서 생성해서 사용해줘서 굳이 추가 x
public class PasswordForm {

    @Length(min = 8,max = 50)
    private String newPassword;
    @Length(min = 8,max = 50)
    private String newPasswordConfirm;
    //재확인용
}
