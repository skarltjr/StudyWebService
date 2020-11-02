package com.studyolle.modules.study;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.study.event.StudyCreatedEvent;
import com.studyolle.modules.study.event.StudyUpdateEvent;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.tag.TagRepository;
import com.studyolle.modules.zone.Zone;
import com.studyolle.modules.study.form.StudyDescriptionForm;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.utility.RandomString;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import static com.studyolle.modules.study.form.StudyForm.VALID_PATH_PATTERN;

@Service
@Transactional
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository repository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    public Study createNewStudy(Study study, Account account) {
        Study newStudy = repository.save(study);
        newStudy.addManager(account);
        return newStudy;
    }
    //일단 스터디가 있는지 없는지 확인
    public Study getStudy(String path) {
        Study study = this.repository.findByPath(path);
        checkIfExistingStudy(path, study);
        return study;
    }

    public void checkIfExistingStudy(String path, Study study) {
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다");
        }
    }
    //=======================

    //관리자인지 확인
    public void checkIfManager(Account account, Study study) {
        if (!study.isManagedBy(account)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다");
        }
    }
    /** !! !! !! !! */
    public Study getStudyToUpdate(Account account, String path) {
        Study study = this.getStudy(path);  //일단 스터디가 있는지 없는지 확인
        checkIfManager(account, study); //관리자인지 판단
        return study;
    }

    public void updateStudyDescription(Study study, StudyDescriptionForm studyDescriptionForm) {
        modelMapper.map(studyDescriptionForm, study);
        //여기서는 스터디가 영속상태라서 merge 안해도된다 당연히
        eventPublisher.publishEvent(new StudyUpdateEvent(study,"스터디 소개를 수정했습니다"));
    }

    /**
     * 스터디 태그나 zone 설정할 때 쿼리를 줄이기 . 태그를 설정할 때 zone정보나 구성원 같은건 필요없다
     */
    public Study getStudyToUpdateTag(Account account, String path) {
        Study study = repository.findStudyWithTagsByPath(path);
        checkIfExistingStudy(path, study);
        checkIfManager(account, study);
        return study;
    }
    public Study getStudyToUpdateZone(Account account, String path) {
        Study study = repository.findStudyWithZonesByPath(path);
        checkIfExistingStudy(path, study);
        checkIfManager(account, study);
        return study;
    }

    public Study getStudyToUpdateStatus(Account account, String path) {
        Study study = repository.findWithManagersByPath(path);
        checkIfExistingStudy(path, study);
        checkIfManager(account, study);
        return study;
    }


    public void updateStudyImage(Study study, String image) {
        study.setImage(image);
    }

    public void enableStudyBanner(Study study) {
        study.setUseBanner(true);
    }

    public void disableStudyBanner(Study study) {
        study.setUseBanner(false);
    }

    public void addTag(Study study, Tag tag) {
        study.getTags().add(tag);
    }

    public void removeTag(Study study, Tag tag) {
        study.getTags().remove(tag);
    }

    public void addZone(Study study, Zone zone) {
        study.getZones().add(zone);
    }

    public void removeZone(Study study, Zone zone) {
        study.getZones().remove(zone);
    }

    public void publish(Study study) {
        study.publish();
        this.eventPublisher.publishEvent(new StudyCreatedEvent(study));
    }

    public void close(Study study) {
        study.close();
        eventPublisher.publishEvent(new StudyUpdateEvent(study,"스터디 소개를 종료했습니다"));
    }

    public void startRecruit(Study study) {
        study.startRecruit();
        eventPublisher.publishEvent(new StudyUpdateEvent(study,"팀원 모집을 시작합니다"));
    }

    public void stopRecruit(Study study) {
        study.stopRecruit();
        eventPublisher.publishEvent(new StudyUpdateEvent(study,"팀원 모집을 중단했습니다"));
    }

    public boolean isValidPath(String newPath) {
        //1 .일단 패턴을 만족하는지 path가
        if (!newPath.matches(VALID_PATH_PATTERN)) {
            return false;
        }
        /**     기존에는 validator 만들어서 initbinder 했다면 이런 방법도 있다*/
        //2. 이미 존재하는지
        return !repository.existsByPath(newPath);
    }


    public void updateStudyPath(Study study, String newPath) {
        study.setPath(newPath);
    }

    public boolean isValidTitle(String newTitle) {
        return newTitle.length()<=50;
    }

    public void updateStudyTitle(Study study, String newTitle) {
        study.setTitle(newTitle);
    }

    public void remove(Study study) {
        if (study.isRemovable()) {
            repository.delete(study);
        }else{
            throw new IllegalArgumentException("스터디를 삭제할 수 없습니다");
        }

    }

    public void addMember(Study study, Account account) {
        study.addMember(account);
    }

    public void removeMember(Study study, Account account) {
        study.removeMember(account);
    }

    public Study getStudyToEnroll(String path) {
        Study study = repository.findStudyOnlyByPath(path);  //어차피 다 무시되고 findByPath
        checkIfExistingStudy(path, study);
        return study;
    }

   /* public void generateTestStudies(Account account) {
        for (int i = 0; i < 30; i++) {
            String random = RandomString.make(5);
            Study study = Study.builder()
                    .title("테스트 스터디" + random)
                    .path("test" + random)
                    .shortDescription("테스트용 스터디")
                    .fullDescription("test")
                    .tags(new HashSet<>())
                    .managers(new HashSet<>())
                    .build();
            study.publish();
            Study newStudy = this.createNewStudy(study, account);
            Tag tag = tagRepository.findByTitle("JPA");
            newStudy.getTags().add(tag);
        }
    }*/
}
