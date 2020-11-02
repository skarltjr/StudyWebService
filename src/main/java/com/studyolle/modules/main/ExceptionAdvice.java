package com.studyolle.modules.main;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.account.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler
    public String handleRuntimeException(@CurrentUser Account account, HttpServletRequest req, RuntimeException e) {
        if (account != null) {
            log.info("'{}' requested '{}'", account.getNickname(), req.getRequestURI());
            //누가 어떤 요청을 보냈는지 로그남기기
        } else {
            log.info("requested '{}'", req.getRequestURI());
        }
        log.error("bad request", e);
        return "error";
    }
}
