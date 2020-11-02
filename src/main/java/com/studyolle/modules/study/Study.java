package com.studyolle.modules.study;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.account.UserAccount;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;
import lombok.*;

import javax.persistence.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@NamedEntityGraph(name = "Study.withAll",attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")})
@NamedEntityGraph(name = "Study.withTagsAndManagers",attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("managers")})
@NamedEntityGraph(name = "Study.withZonesAndManagers",attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers")})
@NamedEntityGraph(name = "Study.withManagers",attributeNodes = {
        @NamedAttributeNode("managers")})
@NamedEntityGraph(name = "Study.withMembers",attributeNodes = {
        @NamedAttributeNode("members")})
@NamedEntityGraph(name = "Study.withTagsAndZones",attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones")})
public class Study {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    private Set<Account> managers = new HashSet<>();

    @ManyToMany
    private Set<Account> members = new HashSet<>();

    @Column(unique = true)
    private String path; // url이니까 유니크하도록

    private String title;

    private String shortDescription;

    @Lob
    @Basic(fetch = FetchType.EAGER) //스터디정보 조회할 때 같이 딸려오도록하기위해 기본값도 eager
    private String fullDescription;

    @Lob
    @Basic(fetch = FetchType.EAGER)
    private String image;  //배너 이미지

    @ManyToMany
    private Set<Tag> tags = new HashSet<>(); //태그도 여러개 가질 수 있고

    @ManyToMany
    private Set<Zone> zones = new HashSet<>(); // 지역정보도 여러개 가질 수 있게

    private LocalDateTime publishedDateTime;

    private LocalDateTime closedDateTime;

    private LocalDateTime recruitingUpdateDateTime;

    private boolean recruiting;//현재 인원모집중인지

    private boolean published; //공개된 스터디인지 아닌지

    private boolean closed;

    private boolean useBanner;

    private int memberCount;


    public void addManager(Account account) {
        this.managers.add(account);
    }

    public boolean isJoinable(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        return this.isPublished() && this.isRecruiting()
                && !this.members.contains(account) && !this.managers.contains(account);

    }

    public boolean isMember(UserAccount userAccount) {
        return this.members.contains(userAccount.getAccount());
    }

    public boolean isManager(UserAccount userAccount) {
        return this.managers.contains(userAccount.getAccount());
    }

    public boolean isManagedBy(Account account) {
        return this.getManagers().contains(account);
    }

    public String getEncodedPath() {
        return URLEncoder.encode(this.path, StandardCharsets.UTF_8);
    }

    public String getImage() {
        return image != null ? image : "/images/default_banner.png";
    }
    public void publish() {
        if (!this.closed && !this.published) {
            this.published = true;
            this.publishedDateTime = LocalDateTime.now();
        } else {
            throw new RuntimeException("스터디를 공개할 수 없는 상태입니다. 스터디를 이미 공개했거나 종료했습니다.");
        }
    }

    public void close() {
        if (this.published && !this.closed) {
            this.closed = true;
            this.closedDateTime = LocalDateTime.now();
        } else {
            throw new RuntimeException("스터디를 종료할 수 없습니다. 스터디를 공개하지 않았거나 이미 종료한 스터디입니다.");
        }
    }

    public boolean canUpdateRecruiting() {
        return this.published && this.recruitingUpdateDateTime == null ||
                this.recruitingUpdateDateTime.isBefore(LocalDateTime.now().minusHours(1));
    }

    public void startRecruit() {
        if (canUpdateRecruiting()) {
            this.recruiting = true;
            this.recruitingUpdateDateTime = LocalDateTime.now();
        } else {
            throw new RuntimeException("인원 모집을 시작할 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
    }

    public void stopRecruit() {
        if (canUpdateRecruiting()) {
            this.recruiting = false;
            this.recruitingUpdateDateTime = LocalDateTime.now();
        } else {
            throw new RuntimeException("인원 모집을 시작할 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요.");
        }
    }


    public boolean isRemovable() {
        return !this.published;  // TODO 나중에 추가할 모임을 했던 스터디는 삭제불가
    }

    public void addMember(Account account) {
        this.getMembers().add(account);
        this.memberCount++;
    }

    public void removeMember(Account account) {
        this.getMembers().remove(account);
        this.memberCount--;
    }
}
