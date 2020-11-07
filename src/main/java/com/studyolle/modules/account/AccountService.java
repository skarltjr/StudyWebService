package com.studyolle.modules.account;

import com.studyolle.infra.config.AppProperties;
import com.studyolle.modules.account.form.SignUpForm;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;
import com.studyolle.infra.mail.EmailMessage;
import com.studyolle.infra.mail.EmailService;
import com.studyolle.modules.account.form.Notifications;
import com.studyolle.modules.account.form.Profile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Transactional// 당연히 데이터 건드리는 부분은
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService implements UserDetailsService {
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    /** 여기서 총괄*/
    public Account processNewAccount(SignUpForm signUpForm) {
        Account newAccount = saveNewAccount(signUpForm);
        sendSignUpConfirmEmail(newAccount);
        return newAccount;
    }

    private Account saveNewAccount(@Valid SignUpForm signUpForm) {

        signUpForm.setPassword(passwordEncoder.encode(signUpForm.getPassword()));
        Account account = modelMapper.map(signUpForm, Account.class);
        account.generateEmailCheckToken(); //아예 만들 때 체크토큰 생성
        return accountRepository.save(account);

       /* Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(passwordEncoder.encode(signUpForm.getPassword()))
                //여기서 패스워드 인코더로 인코딩을 꼭 해야지 !
                .build();*/
    }

    public void sendSignUpConfirmEmail(Account newAccount) {
        Context context = new Context();
        context.setVariable("link", "/check-email-token?token=" + newAccount.getEmailCheckToken() +
                "&email=" + newAccount.getEmail());
        context.setVariable("nickname", newAccount.getNickname());
        context.setVariable("linkName", "이메일 인증하기");
        context.setVariable("message", "스터디올레 서비스를 사용하려면 링크를 클릭하세요");
        context.setVariable("host", appProperties.getHost());

        String message = templateEngine.process("mail/simple-link", context);
        EmailMessage emailMessage = EmailMessage.builder()
                .to(newAccount.getEmail())
                .subject("스터디 올레, 회원 가입 인증")
                .message(message)
                .build();
        emailService.sendEmail(emailMessage);
    }

    //로그인 과정
    /** ★ 목표 = 컨트롤러에서   @CurrentUser Account account처럼 account 객체를 받기
     *    스프링 시큐리티에서는 로그인 하면 org.springframework.security.core.userdetails.User 클래스로 리턴   /UserAccount extends User
     *    그리고 UserDetailService를 이용해 사용자 정보를 읽어낸다. ( loadUserByUsername )  / AccountService implements UserDetailsService
     * */

    /**   인증정보를 사용하기 위한 객체가 Authentication.  -> setAuthentication(token)
     *     즉 token이  인증된 정보를 가진 Authentication객체이고 , Authentication객체의 첫번째 파라미터로 들어온 UserAccount(account)가 principal 이다.
     *     principal객체는 최상위 인터페이스 -> 원래 nickName or id만 꺼낼 수 있는데  단순히 로그인의 여부를 넘어 로직에서 로그인된 account자체. 모든 필드를 사용하고싶어서
     *     account의 전체필드를 사용할 수 있도록 하기 위해서 User클래스를 상속받은 UserAccount를 만들고
     *     UserAccount가 account를 갖도록 만든다.
     *     @CurrentUser Account account 가 사용될 때 @CurrentUser는 @AuthenticationPrincipal(~)을 갖고 있는데 principal을  account로 설정했으니
     *     @AuthenticationPrincipal AccountAdapter accountAdapter 원래 이렇게 사용하던것을 간편하게
     *     ★  @CurrentUser Account account 처럼 account로 바로 사용할 수 있는 이유는 (expression = "#this == 'anonymousUser' ? null : account")
     *     UserAccount 의 private account와 이름을 맞춰놓았기 때문
     *
     *      원래는
     *         @GetMapping
     *     public ResponseEntity getAccount(@AuthenticationPrincipal AccountAdapter accountAdapter) { AccountAdapter= UserAccount
     *         Account account = accountAdapter.getAccount();
     *          return ResponseEntity.ok(account);
     *    ->
     *     @GetMapping
     *     public ResponseEntity getAccount(@CurrentUser Account account) {
     *         return ResponseEntity.ok(account);
     *
     *      시큐리티가 처리하는 로그인 과정에서 사용자 정보를 읽어오는 UserDetailsService의 loadUserByUsername가 return new UserAccount(account)하는것은
     *      ★ @AuthenticationPrincipal를 사용하면 UserDetailsService에서 return한 객체를 파라미터로 직접 받아 사용할 수 있다.
     * */

    /** 정리하면 로그인 과정에서 token을 인증된상태로 남길 때 token의 첫번째파라미터가 principal인데
     *  account자체를 principal로 만들어 account의 모든 필드를 사용할 수 있도록 adapter로 User를 상속받은 UserAccount를 만든다 UserAccount는 account를 갖는다
     *  시큐리티 로그인 처리과정을 위해 UserDetailsService의 loadUserByUsername에서 db에서 꺼내온 account를  return new UserAccount(account);
     *  컨트롤러에서 로그인된 유저인지 판단하기 위해 @AuthenticationPrincipal UserAccount userAccount 를 편리하게 쓰기 위해
     *  CurrentUser어노테이션을 추가
     * */


    public void login(Account account) {

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                //account.getNickname(), 대신
                new UserAccount(account),
                account.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE USER")));

        // ! 이 토큰을 SecurityContextHolder에서 setAuthentication하는게 로그인 상태유지
        SecurityContextHolder.getContext().setAuthentication(token);
    }


    /** 로그인 처리 핸들러는 만들지 않아도 되지만(시큐리티가 처리) UserDetailsService을 impl하여 loadUserByUsername 처리해야한다*/
    @Transactional(readOnly = true) //로그인할 때 데이터만 읽어오는거니까
    @Override
    public UserDetails loadUserByUsername(String emailOrNickname) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(emailOrNickname);
        if(account == null)
        {
            account = accountRepository.findByNickname(emailOrNickname);
        }

        if (account == null) {
            throw new UsernameNotFoundException(emailOrNickname); //emailOrNickname에 해당하는 유저없음을 던지고
        }

        return new UserAccount(account); //principal 객체로 리턴해주기
    }

    public void completeSignUp(Account account) {
        account.completeSignUp();
        login(account);
        //이거는 사용되는곳이 어카운트 컨트롤러의 checkEmailToken인데 여기선 이미 먼저 디비에서 데이터 꺼내왔기 때문에
        //당연히 영속상태인 놈을 업데이트하는거라 굳이 다시 디비에서 안꺼내와도 된다.
    }

    public void updateProfile(Account account, Profile profile) {
        //반면 여기서는 컨트롤러에서 디비에 접근안했고 여기서 바로 update하면 당연히 영속상태가 아니라 업데이트 안되는거고
        //그러니까 디비에서 꺼내와서 변경감지로 업데이트처리
        // jpa에서는 당연히 업데이트할 때 변경감지로 그럴려면 당연히 db에서 가져와야지 일단
        //근데 여기선 merge
        //편리함을 위해 모델매퍼 추가
        modelMapper.map(profile, account);
        /*
        모델 매퍼 사용하면 map으로 (source , destination) 만 해주면 아래코드를 줄일 수 있다
        account.setBio(profile.getBio());
        account.setOccupation(profile.getOccupation());
        account.setLocation(profile.getLocation());
        account.setUrl(profile.getUrl());
        account.setProfileImage(profile.getProfileImage());
        */
        accountRepository.save(account);
    }

    public void updatePassword(Account account, String newPassword) {
        account.setPassword(passwordEncoder.encode(newPassword));  //반드시 인코딩해줘야지
        //이 객체의 상태는 detached 즉 준영속이다 당연히 이 account의 아이디가져다 디비에서 찾아와서는
        // 변경감지가 불가능하다 그래서 merge
        accountRepository.save(account); // merge
    }

    public void updateNotifications(Account account, Notifications notifications) {
        modelMapper.map(notifications, account);  //source , dest
        /**  근데 config에서 모델매퍼config를 설정해줘야하는데 . 왜냐면 이름방식에 맞춰 알아서 해주다보니
         * 잘 못찾는경우가 있다 그래서 config에서 설정한대로 _ 언더스코어단위로 나누게 . 그러면
         *  다 camelcase로 이름 짜놨으니 언더스코어로 나누는데 안걸려서 통째이름으로 판단할테니 잘 될것.
         *  ex) studyCreatedByEmail 같은게 Email로 설정들어가거나 하는거 방지를 위해*/
       /* account.setStudyCreatedByEmail(notifications.isStudyCreatedByEmail());
        account.setStudyCreatedByWeb(notifications.isStudyCreatedByWeb());

        account.setStudyEnrollResultByEmail(notifications.isStudyEnrollmentResultByEmail());
        account.setStudyEnrollResultByWeb(notifications.isStudyEnrollmentResultByWeb());

        account.setStudyUpdatedByEmail(notifications.isStudyUpdatedByEmail());
        account.setStudyUpdatedByWeb(notifications.isStudyUpdatedByWeb());*/

        accountRepository.save(account);
    }

    public void updateNickname(Account account, String nickname) {
        account.setNickname(nickname);
        accountRepository.save(account);
        login(account);
        //새로 인증을 했다는 느낌
    }

    public void sendLoginLink(Account account) {

        Context context = new Context();
        context.setVariable("link", "/login-by-email?token=" + account.getEmailCheckToken()
                + "&email=" + account.getEmail());
        context.setVariable("nickname", account.getNickname());
        context.setVariable("linkName", "스터디올레 로그인하기");
        context.setVariable("message", "로그인을 하려면 링크를 클릭하세요");
        context.setVariable("host", appProperties.getHost());

        String message = templateEngine.process("mail/simple-link", context);

        EmailMessage emailMessage = EmailMessage.builder()
                .to(account.getEmail())
                .subject("스터디올레, 로그인 링크")
                .message(message)
                .build();

        emailService.sendEmail(emailMessage);
    }

    public void addTag(Account account, Tag tag) {
        // 여기서 받은 account는 @CurrentUser Account account 의 account 즉 준영속상태다
        Optional<Account> byId = accountRepository.findById(account.getId());
        byId.ifPresent(a -> a.getTags().add(tag));//존재하면  a = account 에 태그 추가

        //account 는 tag를 set (or list) 로 갖고있으니 .add로 추가
    }

    public Set<Tag> getTags(Account account) {
        Optional<Account> byId = accountRepository.findById(account.getId());
        return byId.orElseThrow().getTags(); //없으면 에러던지고 있으면 가져오고
    }

    public void removeTag(Account account, Tag tag) {
        Optional<Account> byId = accountRepository.findById(account.getId());
        byId.ifPresent(a -> a.getTags().remove(tag));
    }

    public Set<Zone> getZones(Account account) {
        Optional<Account> byId = accountRepository.findById(account.getId());
        return byId.orElseThrow().getZones();
    }

    public void addZone(Account account, Zone zone) {
        Optional<Account> byId = accountRepository.findById(account.getId());
        byId.ifPresent(a -> a.getZones().add(zone));
    }

    public void removeZone(Account account, Zone zone) {
        Optional<Account> byId = accountRepository.findById(account.getId());
        byId.ifPresent(a -> a.getZones().remove(zone));
    }

    public Account getAccount(String nickname) {
        Account account = accountRepository.findByNickname(nickname);
        if (account == null) {
            throw new IllegalArgumentException(nickname + "에 해당하는 사용자가 없습니다");
        }
        return account;
    }
}
