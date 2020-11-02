package com.studyolle.modules.study;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPQLQuery;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.QZone;
import com.studyolle.modules.zone.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
import java.util.Set;


import static com.studyolle.modules.study.QStudy.study;
import static com.studyolle.modules.tag.QTag.tag;
import static com.studyolle.modules.zone.QZone.zone;

public class StudyRepositoryExtensionImpl extends QuerydslRepositorySupport implements StudyRepositoryExtension {


    public StudyRepositoryExtensionImpl() {
        super(Study.class);
    } // 디폴트 생성자 필요  어느 클래스에 대해서인지 . 여기서는 studyclass


    @Override
    public Page<Study> findByKeyword(String keyWord, Pageable pageable) {
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.title.containsIgnoreCase(keyWord))
                        .or(study.tags.any().title.containsIgnoreCase(keyWord)) //혹은 스터디의 태그 중 아무거나 키워들르
                        .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyWord)))
                .leftJoin(study.tags, tag).fetchJoin()
                .leftJoin(study.zones, zone).fetchJoin()
                .distinct();
        /**  성능 최적화를 위해선 페치조인쓰고 페치조인 쓸려면 당연히 레프트조인써야한다.
                 * 페치조인은 엔티티 그래프를 조회하는 거니까 당연히*/

        JPQLQuery<Study> pageableQuery = getQuerydsl().applyPagination(pageable, query);
        QueryResults<Study> fetchResult = pageableQuery.fetchResults();
        return new PageImpl<>(fetchResult.getResults(),pageable,fetchResult.getTotal());
    }

    /**  필요하면 쿼리dsl로 동적쿼리로 키워드에 해당하거 뽑아내도 된다. */

    @Override
    public List<Study> findAccount(Set<Tag> tags, Set<Zone> zones) {
        JPQLQuery<Study> query = from(study)
                .where(study.published.isTrue()
                        .and(study.closed.isFalse())
                        .and(study.tags.any().in(tags))
                        .and(study.zones.any().in(zones)))
                .leftJoin(study.tags, tag).fetchJoin()
                .leftJoin(study.zones, zone).fetchJoin()
                .orderBy(study.publishedDateTime.desc())
                .distinct()
                .limit(9);
        return query.fetch();
    }
}
