package com.studyolle.modules.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.tag.TagForm;
import com.studyolle.modules.tag.TagRepository;
import com.studyolle.modules.zone.Zone;
import com.studyolle.modules.zone.ZoneForm;
import com.studyolle.modules.zone.ZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper objectMapper;
    @Autowired TagRepository tagRepository;
    @Autowired AccountService accountService;
    @Autowired ZoneRepository zoneRepository;

    private Zone testZone = Zone.builder().city("test").localNameOfCity("테스트시").province("테스트주").build();

    @BeforeEach
    void beforeEach() {
        zoneRepository.save(testZone);
    }

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
        zoneRepository.deleteAll();
    }

    /**
     * 이전테스트와 다르게 당연히 프로필수정같은 경우 인증된 . principal로 접근해야한다. 그래서 단순히 beforeEach로
     * 만들어서 사용할 수도 없어서 @WithSecurityContext를 사용하는 방법밖에 없는데  이걸 WithAccount라는 어노테이션으로
     * 만들어놓고 사용,. 근데 @WithSecurityContext(factory = WithAccountSecurityContextFacotry.class)처럼
     * WithAccountSecurityContextFacotry 팩토리가 필요해서 팩토리클래스를 만들고 거기에서 매 테스트마다 새로
     * 인증된 객체를 만든다 그리고 그걸이용. 당연히 매번 새로운걸 만드니까 매번 afterEach로 지워준다
     */

    @WithAccount("kiseok")
    @DisplayName("계정의 지역 정보 수정 폼")
    @Test
    void updateZonesForm() throws Exception {
        mockMvc.perform(get("/settings/zones"))
                .andExpect(view().name("settings/zones"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("whitelist"))
                .andExpect(model().attributeExists("zones"));
    }

    @WithAccount("kiseok")
    @DisplayName("계정의 지역 정보 추가")
    @Test
    void addZone() throws Exception {
        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());

        mockMvc.perform(post("/settings/zones/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zoneForm))
                .with(csrf()))
                .andExpect(status().isOk());
        Account account = accountRepository.findByNickname("kiseok");
        Zone zone = zoneRepository.findByCityAndProvince(testZone.getCity(), testZone.getProvince());
        assertTrue(account.getZones().contains(zone));
    }
    @WithAccount("kiseok")
    @DisplayName("계정의 지역 정보 추가")
    @Test
    void removeZone() throws Exception {
        //미리 넣어둬야하니까
        Account kiseok = accountRepository.findByNickname("kiseok");
        Zone zone = zoneRepository.findByCityAndProvince(testZone.getCity(), testZone.getProvince());
        accountService.addZone(kiseok, zone);

        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());

        mockMvc.perform(post("/settings/zones/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(zoneForm))
                .with(csrf()))
                .andExpect(status().isOk());

        assertFalse(kiseok.getZones().contains(zone));
    }

    @WithAccount("kiseok")
    @DisplayName("태그 수정 폼")
    @Test
    void updateTagsForm() throws Exception{
        mockMvc.perform(get("/settings/tags"))
                .andExpect(view().name("settings/tags"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("whitelist"))
                .andExpect(model().attributeExists("tags"));

    }

    @WithAccount("kiseok")
    @DisplayName("계정에 태그 추가")
    @Test
    void addTag() throws Exception {
        TagForm tagForm = new TagForm();
        tagForm.setTagTitle("newTag");

        mockMvc.perform(post("/settings/tags/add")
                .contentType(MediaType.APPLICATION_JSON) //태그추가는 ResponseBody라서 파라미터가아닌 본문자체로들어온다
                .content(objectMapper.writeValueAsString(tagForm))
                .with(csrf()))
                .andExpect(status().isOk());

        Tag newTag = tagRepository.findByTitle("newTag");
        assertNotNull(newTag);
        /** 아래 account는 어떤 상태? = detached 그래서 test에 transactional추가 \
         *   session이 없다 -> 하나의 transactional에서 동작해야한다 */
        Account account = accountRepository.findByNickname("kiseok");
        assertTrue(account.getTags().contains(newTag));
    }
    @WithAccount("kiseok")
    @DisplayName("계정에 태그 삭제")
    @Test
    void removeTag() throws Exception {
        Account kiseok = accountRepository.findByNickname("kiseok");
        Tag tag = new Tag();
        tag.setTitle("newTag");
        Tag save = tagRepository.save(tag);
        accountService.addTag(kiseok, save);

        assertTrue(kiseok.getTags().contains(save));


        TagForm tagForm = new TagForm();
        tagForm.setTagTitle("newTag");

        mockMvc.perform(post("/settings/tags/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tagForm))
                .with(csrf()))
                .andExpect(status().isOk());

        assertFalse(kiseok.getTags().contains(save));
    }

    @WithAccount("kiseok")
    @DisplayName("프로필 수정 폼") //프로필 수정도 당연히 인증된 본인만이 가능하니까
    @Test
    void updateProfileForm() throws Exception {
        mockMvc.perform(get("/settings/profile"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));
    }

    @WithAccount("kiseok")
    @DisplayName("프로필 수정하기 - 입력값 정상")
    @Test
    void updateProfile() throws Exception {
        String bio = "짧은 소개를 수정하는 경우";
        mockMvc.perform(post("/settings/profile")
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/profile"))
                .andExpect(flash().attributeExists("message"));

        Account account = accountRepository.findByNickname("kiseok");
        assertEquals(bio, account.getBio());

    }

    @WithAccount("kiseok")
    @DisplayName("프로필 수정하기 - 입력값 오류")
    @Test
    void updateProfile_error() throws Exception {
        String bio = "길게 소개를 수정하는 경우 길게 소개를 수정하는 경우 길게 소개를 수정하는 경우 길게 소개를 수정하는 경우" +
                "길게 소개를 수정하는 경우 길게 소개를 수정하는 길게 소개를 수정하는 경우"; //35자 넘으면 에러
        mockMvc.perform(post("/settings/profile")
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().isOk()) //응답은 ok
                .andExpect(view().name("settings/profile"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().hasErrors());

        Account account = accountRepository.findByNickname("kiseok");
        assertNull(account.getBio()); //팩토리에서 만들 때 bio 입력안했으니

    }

    @WithAccount("kiseok")
    @DisplayName("패스워드 수정 폼")
    @Test
    void updatePassword_form() throws Exception {
        mockMvc.perform(get("/settings/password"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @WithAccount("kiseok")
    @DisplayName("패스워드 수정 -입력값 정상")
    @Test
    void updatePassword_success() throws Exception {
        mockMvc.perform(post("/settings/password")
                .param("newPassword", "123456789")
                .param("newPasswordConfirm", "123456789")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings/password"))
                .andExpect(flash().attributeExists("message"));

        Account account = accountRepository.findByNickname("kiseok");
        assertTrue(passwordEncoder.matches("123456789", account.getPassword()));
    }

    @WithAccount("kiseok")
    @DisplayName("패스워드 수정 -입력값 오류")
    @Test
    void updatePassword_fail() throws Exception {
        mockMvc.perform(post("/settings/password")
                .param("newPassword", "123456789")
                .param("newPasswordConfirm", "1111111111")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/password"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
        //오류라면 에러가 있을거고 다시 입력하도록 입력폼 보내니까 당연히 passwordForm이랑 어카운트도 다시보낸다
    }
}