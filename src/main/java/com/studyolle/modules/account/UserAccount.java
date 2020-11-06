package com.studyolle.modules.account;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class UserAccount extends User {
    /** 우리가 accountservice에서 사용하는 로그인에는 account의 요소만 사용하지 account자체는 없다
     *  그래서 account를 들고있는 중간다리 역할을 만들어 줄 필요가있어서
     *  즉 시큐리티가 다루는 유저정보와  도메인에서 다루는 유저정보사이 갭을 메꾸기 위해*/

    private Account account;  //도메인에서의 어카운트
    // 이 account이름이랑 CurrentUser의  null : account") 에서 account랑 이름은 맞춰줘야한다 UserAccount에서 꺼낼거니까
    //파라미터로 도메인의 어카운트
    public UserAccount(Account account) {
        super(account.getNickname(), account.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.account=account;
    }


}
