package com.studyolle.modules.event;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.account.UserAccount;
import com.studyolle.modules.study.Study;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NamedEntityGraph(name = "Event.withEnrollments",
                    attributeNodes = @NamedAttributeNode("enrollments")
)
@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class Event {  //모임 엔티티

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY)
    private Account createdBy;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdDateTime;

    @Column(nullable = false)
    private LocalDateTime endEnrollmentDateTime;

    @Column(nullable = false)
    private LocalDateTime startDateTime; //모임 시작

    @Column(nullable = false)
    private LocalDateTime endDateTime; //모임 종료

    private Integer limitOfEnrollments; //갑이 null일수도있으니

    @OneToMany(mappedBy = "event")
    @OrderBy("enrolledAt") // 누군가 취소했을 때 대기자중 가장 먼저 신청한 사람 끌어오기위해 미리 시간순으로 정렬해두기
    private List<Enrollment> enrollments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    public boolean isEnrollableFor(UserAccount userAccount) {  //등록
        return isNotClosed() && !isAttended(userAccount) && !isAlreadyEnrolled(userAccount);
    }

    public boolean isDisenrollableFor(UserAccount userAccount)        //취소
    {
        return isNotClosed() && !isAttended(userAccount) && isAlreadyEnrolled(userAccount);
    }

    private boolean isNotClosed() {
        return this.endEnrollmentDateTime.isAfter(LocalDateTime.now());
        //지금보다 후에 종료면 아직 종료안된거니까
    }

    private boolean isAlreadyEnrolled(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        for (Enrollment e : enrollments) {
            if (e.getAccount().equals(account)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAttended(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        for (Enrollment e : enrollments) {
            if (e.getAccount().equals(account) && e.isAttend()) {
                return true;
            }
        }
        return false;
    }
    public int numberOfRemainSpots() {
        return this.limitOfEnrollments - (int) this.enrollments.stream().filter(Enrollment::isAccepted).count();
    }

    public long getNumberOfAcceptedEnrollments() {
        return this.enrollments.stream().filter(Enrollment::isAccepted).count();
    }

    public boolean canAccept(Enrollment enrollment) {
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAccepted()
                && !enrollment.isAttend();
    }

    public boolean canReject(Enrollment enrollment) {
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAttend()
                && enrollment.isAccepted();
    }

    public void addEnrollment(Enrollment enrollment) {
        this.getEnrollments().add(enrollment);
        enrollment.setEvent(this);
        /**  당연히 양방향일 땐 */
    }

    public boolean isAbleToAcceptWaitingEnrollment() {
        return this.eventType == EventType.FCFS && this.limitOfEnrollments >getNumberOfAcceptedEnrollments();
    }

    public void removeEnrollment(Enrollment enrollment) {
        /**양방향*/
        this.enrollments.remove(enrollment);
        enrollment.setEvent(null);
    }

    private Enrollment getTheFirstWaitingEnrollment() {
        for (Enrollment enrollment : enrollments) {
            if (!enrollment.isAccepted()) {
                return enrollment;
            }
        }
        return null;
    }

    public void acceptNextWaitingEnrollment() {
        if (this.isAbleToAcceptWaitingEnrollment()) {// 자리가 남아있으면
            Enrollment enrollmentToAccept = this.getTheFirstWaitingEnrollment(); //첫번째 대기자
            if (enrollmentToAccept != null) { //null이 아니면
                enrollmentToAccept.setAccepted(true);
            }
        }
    }
    private List<Enrollment> getWaitingList() {
        return this.enrollments.stream().filter(enrollment -> !enrollment.isAccepted()).collect(Collectors.toList());
    }

    public void acceptWaitingList() {
        if (this.isAbleToAcceptWaitingEnrollment()) { // 선착순이고 자리를 늘렸으니 자리가 남아있다면
            var waitingList = getWaitingList();  // 자리가없어서 대기중인 사람들 중
            int numberToAccept = (int) Math.min(this.limitOfEnrollments - this.getNumberOfAcceptedEnrollments(), waitingList.size());
            //남는 자리수 구하고

            //대기중인 사람들 중 남는 자리수만큼 accepted true로 바꿔줘서 채우기
            waitingList.subList(0, numberToAccept).forEach(e -> e.setAccepted(true));
        }
    }

    public void accept(Enrollment enrollment) {
        if (this.eventType == EventType.CONFIRMATIVE &&
                this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments()) {
            enrollment.setAccepted(true);
        }
    }

    public void reject(Enrollment enrollment) {
        if (this.eventType == EventType.CONFIRMATIVE ) {
            enrollment.setAccepted(false);
        }
    }
}
