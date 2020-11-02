package com.studyolle.modules.notification;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.account.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class NotificationInterceptor implements HandlerInterceptor {
    private final NotificationRepository notificationRepository;

    //pre는 핸들러 처리전 post는 뷰 렌더링 전 after는 뷰 렌더링 후. 지금은 매 요청 뷰전에 알림을 미리 설정해놓고 뷰를 보여주기위해
     @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //일단 인증된 계정에서만 작동해야하니까
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (modelAndView != null && !isRedirectView(modelAndView) && authentication != null &&
                authentication.getPrincipal() instanceof UserAccount) {
    /** model을 안쓰는 경우나 리다이렉트하는 경우나 인증이 안된경우나 .인증이 된 객체가 UserAccount타입이 아닌 경우에는 해당 x*/

            Account account = ((UserAccount) authentication.getPrincipal()).getAccount();
            long count = notificationRepository.countByAccountAndChecked(account, false);
            modelAndView.addObject("hasNotification", count > 0);
            //즉 위 조건에 해당하는 경우에는 계정에 checked가 false인 알림이 0보다 많으면 model에 추가적으로 알림을 더한다


        }
    }

    private boolean isRedirectView(ModelAndView modelAndView) {
        return modelAndView.getViewName().startsWith("redirect:") || modelAndView.getView() instanceof RedirectView;
        // 뷰 이름이 redirect로 시작하거나 뷰타입이 redirect면 해당안하도록 리턴
    }

    /**     이렇게 한 후 설정을 사용하고 싶을 떄 infra -> WebConfig에서 설정추가 >*/
}
