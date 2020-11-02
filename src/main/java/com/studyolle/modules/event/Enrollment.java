package com.studyolle.modules.event;

import com.studyolle.modules.account.Account;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @EqualsAndHashCode(of = "id")
// 모임이랑 스터디조회까지 하도록 해야해서 엔티티그래프 잘 구성해야한다
// enrollment와 직접적인 연관이있는 event와 event아래 study까지 가져오고 싶다 -> 서브그래프활용
@NamedEntityGraph(
        name = "Enrollment.withEventAndStudy",
        attributeNodes = {
                @NamedAttributeNode(value = "event", subgraph = "study")
        },
        subgraphs = @NamedSubgraph(name = "study", attributeNodes = @NamedAttributeNode("study"))
)
public class Enrollment {  //참가신청

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)  //한명이 여러 모임참가가능이니까
    private Account account;

    private LocalDateTime enrolledAt;

    private boolean accepted; //모임에 합격했는지

    private boolean attend;  //실제 참석했는지
}
