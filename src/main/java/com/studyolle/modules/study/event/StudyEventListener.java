package com.studyolle.modules.study.event;

import com.studyolle.infra.config.AppProperties;
import com.studyolle.infra.mail.EmailMessage;
import com.studyolle.infra.mail.EmailService;
import com.studyolle.modules.account.Account;
import com.studyolle.modules.account.AccountPredicate;
import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.notification.Notification;
import com.studyolle.modules.notification.NotificationRepository;
import com.studyolle.modules.notification.NotificationType;
import com.studyolle.modules.study.Study;
import com.studyolle.modules.study.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@Async
@Transactional
@RequiredArgsConstructor
public class StudyEventListener {

    private final StudyRepository studyRepository;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;
    private final NotificationRepository notificationRepository;

    @EventListener
    public void handleStudyCreatedEvent(StudyCreatedEvent studyCreatedEvent) {
        // studyCreatedEvent.getStudy() 이렇게 가져온 스터디는 준영속. 태그,존 등을 참조할 수 없다
        // @EntityGraph(value = "Study.withManagers",type = EntityGraph.EntityGraphType.FETCH) 로 가져온 스터디라
        /**  새로 개설된 스터디에 대한 알림을 보내는게 목표다.
         *  1. 그럼 새로 개설된 스터디의 관심 주제tag와 지역 zone에 대한 정보를 가져오고
         *  2. 사용자들 중 그 태그와 지역을 관심으로 등록한 사람들에게 알림보내기
         *  3. 그런 사용자들 = account 들을 쿼리로 뽑아내야한다*/
        Study study = studyRepository.findStudyWithTagsAndZonesById(studyCreatedEvent.getStudy().getId());
        Iterable<Account> accounts = accountRepository.findAll(AccountPredicate.findByTagsAndZones(study.getTags(), study.getZones()));
        accounts.forEach(account->{
            if (account.isStudyCreatedByEmail()) {
                // 이메일로 보내기
                // TODO 이메일 오류 검토 후 아래 사용
               //sendStudyCreatedEmail(study, account,"새로운 스터디가 생겼습니다","스터디올레, '" + study.getTitle() + "' 스터디가 생겼습니다.");
            }
            if (account.isStudyCreatedByWeb()) {
                //알림보내기 notification
                saveStudyCreatedNotification(study, account,study.getShortDescription(),NotificationType.STUDY_CREATED);
            }
        } );

    }

    private void saveStudyCreatedNotification(Study study, Account account,String message,NotificationType notificationType) {
        Notification notification = new Notification();
        notification.setTitle(study.getTitle());
        notification.setLink("/study/"+ study.getEncodedPath());
        notification.setChecked(false);
        notification.setCreatedLocalDateTime(LocalDateTime.now());
        notification.setMessage(message);
        notification.setAccount(account);
        notification.setNotificationType(notificationType);
        notificationRepository.save(notification);
    }

    private void sendStudyCreatedEmail(Study study, Account account,String contextMessage,String emailSubject) {
        Context context = new Context();
        context.setVariable("nickname", account.getNickname());
        context.setVariable("link", "/study/" + study.getEncodedPath());
        context.setVariable("linkName", study.getTitle());
        context.setVariable("message", contextMessage);
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);


        EmailMessage emailMessage = EmailMessage.builder()
                .subject(emailSubject)
                .to(account.getEmail())
                .message(message)
                .build();
        emailService.sendEmail(emailMessage);
    }

    // 스터디 업데이트 처리 리스너
    @EventListener
    public void handleStudyUpdateEvent(StudyUpdateEvent studyUpdateEvent) {
        //스터디 관리자와 회원에게만 보내니까
        Study study = studyRepository.findStudyWithManagersAndMembersById(studyUpdateEvent.getStudy().getId());
        Set<Account> accounts = new HashSet<>();
        accounts.addAll(study.getManagers());
        accounts.addAll(study.getMembers());
        for (Account account : accounts) {
            if (account.isStudyUpdatedByEmail()) {
               // sendStudyCreatedEmail(study,account,"스터디에 새 소식이 있습니다.","스터디올레, '" + study.getTitle() + "' 스터디에 새 소식이 있습니다.");
            }
            if (account.isStudyUpdatedByWeb()) {
                saveStudyCreatedNotification(study, account, studyUpdateEvent.getMessage(), NotificationType.STUDY_UPDATED);
            }
        }
    }
}
