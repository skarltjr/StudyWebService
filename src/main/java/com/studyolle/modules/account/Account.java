package com.studyolle.modules.account;

import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id")
/**양방향 순환참조 방지 굳이 override안해도 어노테이션으로 생성가능*/
@Builder
/**생성자*/
@AllArgsConstructor
@NoArgsConstructor
@NamedEntityGraph(name="Account.withTagsAndZones",attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones")
})
public class Account {
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)//중복없어야한다
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    private boolean emailVerified;//이메일 검증

    private String emailCheckToken;

    private LocalDateTime emailCheckTokenGeneratedAt;

    private LocalDateTime joinedAt;//가입날짜

    private String bio; //프로필

    private String url;

    private String occupation; //직업

    private String location; //지역

    //string 은 보통 varchar(255)인데 255자가 넘어가는 이미지 처리를 위해 lob 그리고 프로필 바로 바뀌니까 eager
    @Lob @Basic(fetch = FetchType.EAGER)
    private String profileImage;

    private boolean studyCreatedByEmail; //스터디가 만들어졌다는걸 메일로받을건가

    private boolean studyCreatedByWeb=true;

    private boolean studyEnrollResultByEmail;//가입승인을 이메일로받을것인가

    private boolean studyEnrollResultByWeb=true;

    private boolean studyUpdatedByEmail; //스터디에관한변경내용을

    private boolean studyUpdatedByWeb=true;

    @ManyToMany  /**    추가정보가 들어갈 수 있는 실무에서는 1:다 다:1로 풀어나가야한다. */
    private Set<Tag> tags = new HashSet<>();  //list 나 set
    @ManyToMany
    private Set<Zone> zones = new HashSet<>();

    // 이메일 체크 토큰 .
    public void generateEmailCheckToken() {
        this.emailCheckToken = UUID.randomUUID().toString();
        this.emailCheckTokenGeneratedAt= LocalDateTime.now();
    }

    public void completeSignUp() {
        this.emailVerified = true;
        this.joinedAt = LocalDateTime.now();
    }

    public boolean isValidToken(String token) {
        return this.emailCheckToken.equals(token);
    }

    public boolean canSendConfirmEmail() {
        return this.emailCheckTokenGeneratedAt.isBefore(LocalDateTime.now().minusHours(1));
        // 체크토큰 만들어진 이후 1시간 이후부터만 이메일 다시보낼 수 있도록하기위해;
    }
}
