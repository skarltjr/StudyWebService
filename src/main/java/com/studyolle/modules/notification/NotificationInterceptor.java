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

/** 모든 요청(홈으로 가거나 다른 스터디로 가거나 했을 때를 포함해서)에서 알림이 오면 알림아이콘 색이 변하도록 설정하고싶다
 * 그럼 모든 요청에 대해 이벤트는 처리하고 ★뷰를 렌더링 하기 전에 postHandle
 * 알림이 생성되었다는 것 자체가 예를들어 내가 관심있는 지역에 대한 스터디가 만들어지고 studyCreatedEvent가 핸들러로
 * 처리가 되었다는 뜻 그러니 post
 * pre 는 핸들러 실행 전 post핸들러 실행 후 뷰 렌더링 전 after는 뷰 렌더링 이후*/

@Component
@RequiredArgsConstructor
public class NotificationInterceptor implements HandlerInterceptor {
    private final NotificationRepository notificationRepository;

     @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //일단 인증된 계정에서만 작동해야하니까


         Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (modelAndView != null && !isRedirectView(modelAndView) && authentication != null &&
                authentication.getPrincipal() instanceof UserAccount) {
    /** model을 안쓰는 경우나 리다이렉트하는 경우나 인증이 안된경우나 .인증이 된 객체가 UserAccount타입이 아닌 경우에는 해당 x*/

            // 중요한 관점 . 모든 계정에 대해서가아니라 현재 사용자 계정에 대해
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
