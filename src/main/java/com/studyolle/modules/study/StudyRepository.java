package com.studyolle.modules.study;

import com.studyolle.modules.account.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly = true)
public interface StudyRepository extends JpaRepository<Study,Long>, StudyRepositoryExtension {

    boolean existsByPath(String path);

    @EntityGraph(value = "Study.withAll",type = EntityGraph.EntityGraphType.LOAD)
    Study findByPath(String path);

    // path로 스터디 가져올 때 연관관계인 zone tag (member manager)account 여러방 쿼리가 나간다
    //그래서 studu에서 엔티티그래프로 구성해둔 것들은
    // value = "Study.withAll",type = EntityGraph.EntityGraphType.LOAD  -> eager로 바로 가져오게

    @EntityGraph(value = "Study.withTagsAndManagers",type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithTagsByPath(String path);
    // WithTags는 data jpa한테 무의미한 이름내용이라 무시 -> findStudyByPath로  그러나 오버로딩으로 엔티티그래프 다르게

    @EntityGraph(value = "Study.withZonesAndManagers",type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithZonesByPath(String path);

    @EntityGraph(value = "Study.withManagers",type = EntityGraph.EntityGraphType.FETCH)
    Study findWithManagersByPath(String path);

    @EntityGraph(value = "Study.withMembers",type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithMembersByPath(String path);

    Study findStudyOnlyByPath(String path);

    @EntityGraph(value = "Study.withTagsAndZones",type = EntityGraph.EntityGraphType.FETCH)
    Study findStudyWithTagsAndZonesById(Long id);

    /**     사실 엔티티그래프를 위처럼 사용하는건 원래는 여러곳에서 자주사용할 때 그게아니라면 아래처럼*/
    @EntityGraph(attributePaths = {"members","managers"})
    Study findStudyWithManagersAndMembersById(Long id);

    @EntityGraph(attributePaths = {"zones", "tags"})
    List<Study> findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(boolean b, boolean b1);


    //아래 두개는 스터디의 이름만 보여준다. 연관관계 매핑되어있는 필드는 건드리지않는다
    //그래서 기본 lazy로딩이니까 어차피 안불러와서 당연히 굳이 엔티티그래프나 페치조인이 필요가없다.
    List<Study> findFirst5ByManagersContainingAndClosedOrderByPublishedDateTime(Account account, boolean b);

    List<Study> findFirst5ByMembersContainingAndClosedOrderByPublishedDateTime(Account account, boolean b);
    //이걸 보면 findStudy처럼 study가 없어도된다 first9 스터디에대해 처음 9개의 스터디
}
