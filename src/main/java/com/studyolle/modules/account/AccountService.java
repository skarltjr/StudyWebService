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
    public void login(Account account) {

        //이렇게 하는 이유는 패스워드를 인코딩했기 때문
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                //account.getNickname(), 대신
                new UserAccount(account), //로그인을 했다면 인증된 principal
                //사실 첫번째로 넘겨준 파라미터가 principal  / 이 principal에 닉네임이 아니라 어카운트 자체를
                //사용하고 싶어서 UserAccount를 만들었다 그럼 첫번째 파라미터인 UserAccount가 principal이고
                //CurrentUser에서도 UserAccount에 넣어준 account 이걸 참조
                //그래서 로그인하지 않은 사람과 로그인 한 사람을 구분하도록

                /** 중요한 것은 UserAccount가 principal 그리고 account가 안에 담겨있고*/
                /**    @CurrentUser Account account 에서 로그인 한 사람이면 account정보를 가져와서 사용하고 , account가 pricipal인 UserAccount에 담긴 account면
                 * 아니면 anonymousUser  --> CurrentUser에 설정*/
                account.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE USER")));

        // ! 이 토큰을 SecurityContextHolder에서 setAuthentication하는게 로그인 상태유지
        SecurityContextHolder.getContext().setAuthentication(token);
    }   


    // 시큐리티덕분에 로그인 핸들러는 만들지 않아도 되지만 로그인처리는 디비에 저장된 자료를 바탕으로 처리해야하니까
    // 중간다리를 만들어야한다
    /** 즉 로그인 처리 핸들러는 만들지 않아도 되지만(시큐리티가 처리) UserDetailsService을 impl하여 loadUserByUsername 처리해야한다*/
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

        //여기까지 왔다는 것은 해당햐는 이메일, 닉네임으로 찾은 계정이 이미 회원가입 되어있다는 것.
        //그러면 돌려줄 때 principle에 해당하는 객체를 돌려주면된다
        return new UserAccount(account);
        /**  여기서도 로그인을 하면 새로운 principal인 UserAccount 넘겨준다*/
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
