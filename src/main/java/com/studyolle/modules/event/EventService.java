package com.studyolle.modules.event;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.event.event.EnrollmentAcceptedEvent;
import com.studyolle.modules.event.event.EnrollmentRejectedEvent;
import com.studyolle.modules.study.Study;
import com.studyolle.modules.event.form.EventForm;
import com.studyolle.modules.study.event.StudyCreatedEvent;
import com.studyolle.modules.study.event.StudyUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Event createEvent(Event event, Study study, Account account) {
        // 모델매퍼로 폼데이터는 이미 이 이벤트에 set되어있고 스터디와 누가만들었는지 account를 set해줘야
        event.setCreatedBy(account);
        event.setCreatedDateTime(LocalDateTime.now());
        event.setStudy(study);
        eventPublisher.publishEvent(new StudyUpdateEvent(event.getStudy(),
                "'"+event.getTitle()+"' 모임을 만들었습니다"));
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, EventForm eventForm) {
        modelMapper.map(eventForm, event);
        event.acceptWaitingList();
        /** acceptWaitingList는 만약에 5명이였는데 수정으로 7명으로 늘린다면 5명은 그대로 있지만 waitinglist.
         * 대기중인 사람들이 4명있다면 신청순으로 앞에서 2명 자동으로 추가해주도록 */
        eventPublisher.publishEvent(new StudyUpdateEvent(event.getStudy(),
                "'" + event.getTitle() + "' 모임 정보를 수정했으니 확인하세요."));
       }

    public void deleteEvent(Event event) {
        eventRepository.delete(event);
        eventPublisher.publishEvent(new StudyUpdateEvent(event.getStudy(),
                "'" + event.getTitle() + "' 모임을 취소했습니다."));
    }

    public void newEnrollment(Event event, Account account) {
        if (!enrollmentRepository.existsByEventAndAccount(event, account)) {
            //이미 요청이 있으면 무시 없다면
            Enrollment enrollment = new Enrollment();
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollment.setAccount(account);
            enrollment.setAccepted(event.isAbleToAcceptWaitingEnrollment());
            //만약에 선착순이고 아직 다 안차있다면
            event.addEnrollment(enrollment);
            //자동으로 추가
            enrollmentRepository.save(enrollment);
        }
    }

    public void cancelEnrollment(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        if (!enrollment.isAttend()) {
            event.removeEnrollment(enrollment);
            enrollmentRepository.delete(enrollment);

            //선착순인 경우 이미 enrollment가 이벤트에 등록되었다는것은 확정난 상태인 enrollment였는데
            //이걸 cancel한다면 자리가 하나 남는다. 그러면 위에서
            // enrollment.setAccepted(event.isAbleToAcceptWaitingEnrollment());  -> 자리가 다 차서 false인 상태인 애들
            // 선착순이니까 누구하나 빠지면 채워넣어줘야지

            event.acceptNextWaitingEnrollment();  //채워넣기
        }
        // 이미 참석했다면 바꿀 수 없지
    }

    public void acceptEnrollment(Event event, Enrollment enrollment) {
        event.accept(enrollment);
        eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
    }

    public void rejectEnrollment(Event event, Enrollment enrollment) {
        event.reject(enrollment);
        eventPublisher.publishEvent(new EnrollmentRejectedEvent(enrollment));
    }

    public void checkInEnrollment(Enrollment enrollment) {
        enrollment.setAttend(true);
    }

    public void cancelCheckInEnrollment(Enrollment enrollment) {
        enrollment.setAttend(false);
    }
}
