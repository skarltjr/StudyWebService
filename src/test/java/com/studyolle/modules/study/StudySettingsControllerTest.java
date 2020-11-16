package com.studyolle.modules.study;

import com.studyolle.modules.account.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
public class StudySettingsControllerTest {

    @Autowired AccountFactory accountFactory;
    @Autowired AccountRepository accountRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired StudyFactory studyFactory;
    @Autowired MockMvc mockMvc;

    @Test
    @WithAccount("kiseok")
    @DisplayName("스터디 소개 수정 폼 조회 - 실패(권한 없는 유저)")
    void updateDescriptionForm_fail() throws Exception {
        Account kiseok2 = accountFactory.createAccount("kiseok2");
        Study study = studyFactory.createStudy("test-study", kiseok2);

        mockMvc.perform(get("/study/" + study.getPath() + "/settings/description"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }

    @Test
    @WithAccount("kiseok")
    @DisplayName("스터디 소개 수정 폼 조회 - 성공")
    void updateDescriptionForm_success() throws Exception {
        Account kiseok = accountRepository.findByNickname("kiseok");
        Study study = studyFactory.createStudy("test-study", kiseok);

        mockMvc.perform(get("/study/" + study.getPath() + "/settings/description"))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/description"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("studyDescriptionForm"))
                .andExpect(model().attributeExists("study"));
    }

    @Test
    @WithAccount("kiseok")
    @DisplayName("스터디 소개 수정 - 성공")
    void updateDescription_success() throws Exception {
        Account kiseok = accountRepository.findByNickname("kiseok");
        Study study = studyFactory.createStudy("test-study", kiseok);

        mockMvc.perform(post("/study/" + study.getPath() + "/settings/description")
                .param("shortDescription", "short description")
                .param("fullDescription", "full description")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/study/" + study.getPath() + "/settings/description"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @WithAccount("kiseok")
    @DisplayName("스터디 소개 수정 - 실패")
    void updateDescription_fail() throws Exception {
        Account kiseok = accountRepository.findByNickname("kiseok");
        Study study = studyFactory.createStudy("test-study", kiseok);

        mockMvc.perform(post("/study/" + study.getPath() + "/settings/description")
                .param("shortDescription", "")
                .param("fullDescription", "full description")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("study/settings/description"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("study"))
                .andExpect(model().attributeExists("studyDescriptionForm"));

    }
}
