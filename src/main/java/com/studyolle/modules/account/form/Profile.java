package com.studyolle.modules.account.form;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

//todo 디폴트 생성자의 필요성  -> Controller에서 @ModerAttribute를 하면 기본생성자가 없어도 알아서 직접생성하여 대응
@Data
@NoArgsConstructor
public class Profile {

    @Length(max = 35)
    private String bio; //소개
    @Length(max = 50)
    private String url;
    @Length(max = 50)
    private String occupation; //직업
    @Length(max = 50)
    private String location; //지역

    private String profileImage;


}
