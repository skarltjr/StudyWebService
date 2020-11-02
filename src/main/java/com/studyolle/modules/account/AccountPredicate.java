package com.studyolle.modules.account;

import com.querydsl.core.types.Predicate;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;

import java.util.Set;

import static com.studyolle.modules.account.QAccount.account;

public class AccountPredicate {
    public static Predicate findByTagsAndZones(Set<Tag> tags, Set<Zone> zones) {
        return account.zones.any().in(zones).and(account.tags.any().in(tags));
        // 어카운트 중 파라미터로 들어온 태그 존을 가진것들 추려내기
    }
}
