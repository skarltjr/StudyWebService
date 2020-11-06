package com.studyolle.modules.account;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : account")
// @CurrentUser Account account에서 
// AuthenticationPrincipal -> 이 CurrentUser 어노테이션을 사용하는 객체가 널이 아니면 principal에 넣어둔 account사용
public @interface CurrentUser {
}
