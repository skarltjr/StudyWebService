package com.studyolle.modules.main;

import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.CurrentUser;
import com.studyolle.modules.account.Account;
import com.studyolle.modules.event.EnrollmentRepository;
import com.studyolle.modules.study.Study;
import com.studyolle.modules.study.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StudyRepository studyRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AccountRepository accountRepository;

    @GetMapping("/")        //currentuser 어노테이션 따로 만들어서 사용하는것 지금
    public String home(@CurrentUser Account account, Model model) {
        if (account != null) {
            Account accountLoaded = accountRepository.findAccountWithTagsAndZonesById(account.getId());
            model.addAttribute(accountLoaded);
            model.addAttribute("enrollmentList",enrollmentRepository.findByAccountAndAcceptedOrderByEnrolledAtDesc(accountLoaded,true));
            // 내account의 enrollment가 accepted된 event를 가져와야한다.
            // enrollment는 account와 event 둘 다를 필드로 갖고있으니 enrollment를 통해 가져온다
            model.addAttribute("studyList",studyRepository.findAccount(accountLoaded.getTags(), accountLoaded.getZones()));
            model.addAttribute("studyManagerOf",studyRepository.findFirst5ByManagersContainingAndClosedOrderByPublishedDateTime(account,false));
            model.addAttribute("studyMemberOf",studyRepository.findFirst5ByMembersContainingAndClosedOrderByPublishedDateTime(account, false));
            return "index-after-login";
        }
        //로그인 안했을 때
        model.addAttribute("studyList", studyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
        // 쿼리dsl등으로 직접 쿼리 짜도 되겠지만 스프링데이터jpa이용해서도 충분히 가능
        return "index";
    }

    /**
     * @CurrentUser의 (expression = " # this = = ' anonymousUser ' ? null : account ")
     * 즉. 첫화면에서 만약 accountservice의 login을 통해 new UserAccount(account)를 거쳤다면
     * 인증된 것이기 때문에 anonymous->null이 아닌 account로 작동할테고 그게 아니라면 null로 작동하는
     * 동적인
     */


    // 회원가입 이 후 로그인할 떄
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * 검색 기능
     * 페이징적용시 기본값설정 . 원래는 20개가 기본.
     * 어노테이션으로 변경 / PageableDefault만 붙이면 기본값 20 -> 10
     * size적용해주면 변경값으로 page는 페이지 시작값 0 1 2 3 ... 이니까 0부터시작하게 근데 기본값이 0이라 굳이
     * 최신순으로 하기위해 내림차순으로
     */

    @GetMapping("/search/study")
    public String searchStudy(String keyword, Model model,
            @PageableDefault(size = 9,sort = "publishedDateTime",
                    direction = Sort.Direction.DESC ) Pageable pageable) {
        Page<Study> studyPage = studyRepository.findByKeyword(keyword,pageable);
        model.addAttribute("studyPage",studyPage);
        /**  ★★ "studyPage"라고 이름을 넣어주는거랑 안넣어주는거랑 큰 차이가 있다
         * 바로 빈 empty 모델로 넘겨주는 여기서는 studyPage가 null이면 이걸 무시해버린다.
         * 이름을 붙이면 무시안함 비어있더라도 모델에 들어간다*/

        model.addAttribute("keyword", keyword);
        model.addAttribute("sortProperty",
                pageable.getSort().toString().contains("publishedDateTime") ? "publishedDateTime" : "memberCount");
        return "search";
    }
}
