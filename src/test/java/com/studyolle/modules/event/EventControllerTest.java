package com.studyolle.modules.event;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.account.AccountFactory;
import com.studyolle.modules.account.AccountRepository;
import com.studyolle.modules.account.WithAccount;
import com.studyolle.modules.study.Study;
import com.studyolle.modules.study.StudyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class EventControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired AccountFactory accountFactory;
    @Autowired StudyFactory studyFactory;
    @Autowired EventService eventService;
    @Autowired AccountRepository accountRepository;
    @Autowired EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("선착순 모임에 참가 신청 - 자동 수락")
    @WithAccount("kiseok")
    void newEnrollment_to_FCFS_event_accepted() throws Exception {
        Account account = accountFactory.createAccount("kiseok2");
        Study study = studyFactory.createStudy("test-study", account);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, account);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        Account kiseok = accountRepository.findByNickname("kiseok");
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, kiseok);
        assertTrue(enrollment.isAccepted());
    }

    @Test
    @DisplayName("선착순 모임에 참가 신청 - 대기중 (이미 인원이 꽉차서)")
    @WithAccount("kiseok")
    void newEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account account = accountFactory.createAccount("kiseok2");
        Study study = studyFactory.createStudy("test-study", account);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, account);

        Account friend1 = accountFactory.createAccount("friend1");
        Account friend2 = accountFactory.createAccount("friend2");
        eventService.newEnrollment(event, friend1);
        eventService.newEnrollment(event, friend2);
        // 이미 모임에 꽉 차있는 상태 설정완료
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));
        // accept 안된것을 확인하자
        Account kiseok = accountRepository.findByNickname("kiseok");
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, kiseok);
        assertFalse(enrollment.isAccepted());
    }

    @Test
    @DisplayName("관리자 확인 모임에 참가 신청 - 대기중")
    @WithAccount("kiseok")
    void newEnrollment_to_CONFIMATIVE_event_not_accepted() throws Exception {
        Account account = accountFactory.createAccount("kiseok2");
        Study study = studyFactory.createStudy("test-study", account);
        Event event = createEvent("test-event", EventType.CONFIRMATIVE, 2, study, account);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        //enrollment 이벤트랑 계정ㅇ로 찾아서 아직 수락안된것을 확인하기
        Account kiseok = accountRepository.findByNickname("kiseok");
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, kiseok);
        assertFalse(enrollment.isAccepted());
    }

    //취소한 경우에 대해 테스트
    @Test
    @DisplayName("참가신청 확정자가 선착순 모임에 참가 신청을 취소하는 경우, 바로 다음 대기자를 자동으로 신청 확인한다.")
    @WithAccount("kiseok")
    void accepted_account_cancelEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account account = accountFactory.createAccount("kiseok2");
        Study study = studyFactory.createStudy("test-study", account);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, account);

        Account kiseok = accountRepository.findByNickname("kiseok");
        Account friend1 = accountFactory.createAccount("friend1");
        Account friend2 = accountFactory.createAccount("friend2");
        eventService.newEnrollment(event, kiseok);
        eventService.newEnrollment(event, friend1);
        eventService.newEnrollment(event, friend2);

        //이미 선착순에 따라 합격한 kiseok이 취소를했을 때 대기중인 frined2가 자동으로 합격되는지 확인
        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/disenroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        assertTrue(enrollmentRepository.findByEventAndAccount(event, friend1).isAccepted());
        assertTrue(enrollmentRepository.findByEventAndAccount(event, friend2).isAccepted());
        assertNull(enrollmentRepository.findByEventAndAccount(event, kiseok));
    }

    @Test
    @DisplayName("참가신청 비확정자가 선착순 모임에 참가 신청을 취소하는 경우, 기존 확정자를 그대로 유지하고 새로운 확정자는 없다.")
    @WithAccount("kiseok")
    void not_accepterd_account_cancelEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account kiseok2 = accountFactory.createAccount("kiseok2");
        Study study = studyFactory.createStudy("test-study", kiseok2);
        Event event = createEvent("test-event", EventType.FCFS, 2, study, kiseok2);

        Account friend1 = accountFactory.createAccount("friend1");
        Account friend2 = accountFactory.createAccount("friend2");
        Account kiseok = accountRepository.findByNickname("kiseok");

        eventService.newEnrollment(event, friend1);
        eventService.newEnrollment(event, friend2);
        eventService.newEnrollment(event, kiseok);

        mockMvc.perform(post("/study/" + study.getPath() + "/events/" + event.getId() + "/disenroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/events/" + event.getId()));

        assertNull(enrollmentRepository.findByEventAndAccount(event, kiseok));
        assertTrue(enrollmentRepository.findByEventAndAccount(event,friend1).isAccepted());
        assertTrue(enrollmentRepository.findByEventAndAccount(event,friend2).isAccepted());
    }

    private Event createEvent(String eventTitle, EventType eventType, int limit, Study study, Account account) {
        Event event = new Event();
        event.setTitle(eventTitle);
        event.setEventType(eventType);
        event.setLimitOfEnrollments(limit);
        event.setStudy(study);
        event.setCreatedDateTime(LocalDateTime.now());
        event.setEndEnrollmentDateTime(LocalDateTime.now().plusDays(1));
        event.setStartDateTime(LocalDateTime.now().plusDays(1).plusHours(6));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(7));
        return eventService.createEvent(event, study, account); //연관관계 set하고 save
    }

}
