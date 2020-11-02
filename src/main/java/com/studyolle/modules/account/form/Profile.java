package com.studyolle.modules.account.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
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
