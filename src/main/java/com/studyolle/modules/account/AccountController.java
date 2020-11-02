package com.studyolle.modules.account;


import com.studyolle.modules.account.form.SignUpForm;
import com.studyolle.modules.account.validator.SignUpFormValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AccountController {

    //이미 validator만든거 component로 빈등록했으니 사용
    private final SignUpFormValidator signUpFormValidator;
    private final AccountService accountService;
    private final AccountRepository accountRepository;

    @InitBinder("signUpForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(signUpFormValidator);
    }

    @GetMapping("/sign-up")
    public String signUpForm(Model model) {
        model.addAttribute("signUpForm", new SignUpForm());
        return "account/sign-up";
    }

  /*  @PostMapping("/sign-up")
    public String signUpSubmit(@Valid @ModelAttribute SignUpForm signUpForm, Errors errors) {
        //ModelAttribute 채워서 가져온다  error가 있으면 받아오기
        if (errors.hasErrors()) {
            return "account/sign-up";  //에러있으면 폼 다시 보여준다
        }

        //다시 signuppformvalidator에서 db에 닉네임,이메일 중복검사하고
        signUpFormValidator.validate(signUpForm,errors);

        //검증마치면 계정만들어서
        Account account = Account.builder() // 빌더를 사용하면 생성자, 생성자체도 더 직관적
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(signUpForm.getPassword()) //TODO encoding password를 바로 저장하면 아주위험 해쉬로 인코딩해야한다
                .studyCreatedByWeb(true)
                .studyEnrollResultByWeb(true)
                .studyUpdatedByWeb(true)  //web 관련만 켜놓기일단
                //.emailVerified(false) // 아직 검증안되었으니까 그치만 기본값 null알아서 들어있을테니
                //.bio()
                .build();// 마무리 build

        //저장
        Account newAccount = accountRepository.save(account);

        //미리 토큰 만들어두고 - 가입확인 메일보낼 떄
        newAccount.generateEmailCheckToken();

        //메일 보내기(가입하면 가입 이메일 뭐 날려주는거)  - 지금은 로컬 콘솛에서만
        //메세지 만들고
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setTo(newAccount.getEmail()); //받는사람
        mailMessage.setSubject("스터디올레 회원가입 인증");//이메일 제목
        mailMessage.setText("/check-email-token?token" + newAccount.getEmailCheckToken() + //생성전에는 token null
                "&email=" + newAccount.getEmail()); //본문

        //만든 메세지 보내기
        javaMailSender.send(mailMessage);


        return "redirect:/";
    }*/

    /**
     * 위와 똑같은 코드지만 리펙토링  훨씬 읽기 편하다 추가로 saveNewAccount,sendSignUpConfirmEmail 를 매서드
     * 추출하고 이것들을 서비스로 옮겨뒀다 AccountRepository,JavaMailSender 도 옮겨놨고 여기선 서비스를 주입
     */

    @PostMapping("/sign-up")
    public String signUpSubmit(@Valid SignUpForm signUpForm, Errors errors) {
        //에러있으면 빠지고
        if (errors.hasErrors()) {
            return "account/sign-up";
        }
        // 중복검사 검증하고 initBinder로 처리
        // signUpFormValidator.validate(signUpForm, errors);
        //서비스에서 일단 save하고 인증메일 날린다
        Account account = accountService.processNewAccount(signUpForm);

        //로그인처리
        accountService.login(account);
        return "redirect:/";
    }

    @GetMapping("/check-email-token") //이메일 인증
    public String checkEmailToken(String token, String email, Model model) {
        //이메일에 해당하는 유저가있는지 먼저확인
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            model.addAttribute("error", "wrong.email");
            return "account/checked-email";
        }

        //받아온 토큰과 비교
        if (!account.isValidToken(token)) {
            model.addAttribute("error", "wrong.token");
            return "account/checked-email";
        }

        //여기까지 통과했다면
        //account.completeSignUp();
        //accountService.login(account);
        /** 여기서. 중요한 것. completeSignUp통해 엔티티의 가입날짜는 변경되었지만 db에는 반영이 아직 안되었다.
         *  컨트롤러에서는 트랜잭션이 일어나지않아서 아직 영속성컨텍스트에 존재. 그렇기 때문에 trancsaction있는 서비스에서 관리하도록*/
        accountService.completeSignUp(account);
        model.addAttribute("numberOfUser", accountRepository.count()); // 몇 번째 가입자인지 그냥 알려줘보기
        model.addAttribute("nickname", account.getNickname());
        return "account/checked-email";
    }

    /**
     * 처음에 가입을 처음 한다면  - 가입은 되는거지만 아직 이메일 인증을 한 건 아니다
     */

    @GetMapping("/check-email") //회원가입 후 이메일 인증
    public String checkEmail(@CurrentUser Account account, Model model) {
        model.addAttribute("email", account.getEmail());
        return "account/check-email";
        /** 이메일 인증을 하기 위해 먼저 가입된(= service의 로그인에서 userAccount로 + @CurrentUser로 principle인)
         *      계정에만 보여야하니까 */
    }

    @GetMapping("resend-confirm-email")
    public String resendConfirmEmail(@CurrentUser Account account, Model model) {
        if (!account.canSendConfirmEmail()) {
            model.addAttribute("error", "인증 이메일은 1시간에 한 번만 전송할 수 있습니다");
            model.addAttribute("email", account.getEmail());
            return "account/check-email";
        }
        accountService.sendSignUpConfirmEmail(account);
        return "redirect:/";
    }

    @GetMapping("/profile/{nickname}")
    public String viewProfile(@PathVariable String nickname, Model model,@CurrentUser Account account)
    {
        /** 내가 내 프로필을 볼 땐 설정버튼 등이 나오지만 다른 사람이 내 프로필을 볼 떈 다르게 나와야한다
         * 그래서 현재 이 프로필을 보는 사람이 누구인지 알기위해서 CurrentUser로 어카운트 받는다
         * 즉 nickname에 해당하는 account와 파라미터로 받은 account 가 일치하면 주인 아니면 다른 사람*/
        Account accountToView = accountService.getAccount(nickname);

        //model.addAttribute("account",byNickname);
        model.addAttribute(accountToView); //위랑 똑같다
        model.addAttribute("isOwner", accountToView.equals(account));
        return "account/profile";

    }

    @GetMapping("/email-login")
    public String emailLoginForm() {
        return "account/email-login";
    }

    @PostMapping("/email-login")
    public String sendEmailLoginLink(String email, Model model, RedirectAttributes attributes) {
        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            model.addAttribute("error", "유효한 이메일 주소가 아닙니다");
            return "account/email-login";
        }

        if (!account.canSendConfirmEmail()) {
            model.addAttribute("error", "이메일 로그인은 1시간 뒤에 사용할 수 있습니다");
            return "account/email-login";
        }
        accountService.sendLoginLink(account);
        attributes.addFlashAttribute("message", "이메일 인증 메일을 발송했습니다");
        return "redirect:/email-login";
    }

    @GetMapping("/login-by-email")
    public String loginByEmail(String token, String email, Model model) {
        Account account = accountRepository.findByEmail(email);
        String view = "account/logged-in-by-email";
        if (account == null || !account.isValidToken(token)) {
            model.addAttribute("error", "로그인할 수 없습니다.");
            return view;
        }

        accountService.login(account);
        return view;
    }
}
