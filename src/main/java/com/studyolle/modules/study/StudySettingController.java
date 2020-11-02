package com.studyolle.modules.study;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyolle.modules.account.CurrentUser;
import com.studyolle.modules.account.Account;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;
import com.studyolle.modules.tag.TagForm;
import com.studyolle.modules.study.form.StudyDescriptionForm;
import com.studyolle.modules.tag.TagRepository;
import com.studyolle.modules.tag.TagService;
import com.studyolle.modules.zone.ZoneForm;
import com.studyolle.modules.zone.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/study/{path}/settings")
public class StudySettingController {

    private final StudyService studyService;
    private final ModelMapper modelMapper;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;
    private final TagService tagService;
    private final ZoneRepository zoneRepository;

    @GetMapping("/description")
    public String viewStudySetting(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        model.addAttribute(modelMapper.map(study, StudyDescriptionForm.class));
        return "study/settings/description";
    }

    @PostMapping("/description")
    public String updateStudyInfo(@CurrentUser Account account, @PathVariable String path,
                                  @Valid StudyDescriptionForm studyDescriptionForm, Errors errors, Model model,
                                  RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdate(account, path);
        if (errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(study);
            return "study/settings/description";
        }

        studyService.updateStudyDescription(study, studyDescriptionForm);
        attributes.addFlashAttribute("message", "스터디 소개를 수정했씁니다");
        return "redirect:/study/" + study.getEncodedPath() + "/settings/description";
    }

    private String getPath(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    @GetMapping("/banner")
    public String studyImageForm(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        return "study/settings/banner";
    }

    @PostMapping("/banner")
    public String studyImageSubmit(@CurrentUser Account account, @PathVariable String path, String image,
                                   RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdate(account, path);
        studyService.updateStudyImage(study, image);
        attributes.addFlashAttribute("message", "스터디 이미지를 수정했습니다");
        return "redirect:/study/" + study.getEncodedPath() + "/settings/banner";
    }

    @PostMapping("/banner/enable")
    public String enableStudyBanner(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyService.getStudyToUpdate(account, path);
        studyService.enableStudyBanner(study);
        return "redirect:/study/" + study.getEncodedPath() + "/settings/banner";
    }

    @PostMapping("/banner/disable")
    public String disableStudyBanner(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyService.getStudyToUpdate(account, path);
        studyService.disableStudyBanner(study);
        return "redirect:/study/" + study.getEncodedPath() + "/settings/banner";
    }

    @GetMapping("/tags")
    public String studyTagsForm(@CurrentUser Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Study study = studyService.getStudyToUpdateTag(account, path);
        model.addAttribute(account);
        model.addAttribute(study);

        model.addAttribute("tags", study.getTags().stream().map(Tag::getTitle)
                .collect(Collectors.toList()));
        List<String> allTagTitles = tagRepository.findAll().stream().map(Tag::getTitle).collect(Collectors.toList());
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allTagTitles));
        return "study/settings/tags";
    }

    @PostMapping("/tags/add")
    @ResponseBody
    public ResponseEntity addTag(@CurrentUser Account account, @PathVariable String path,
                                 @RequestBody TagForm tagForm) {
        Study study = studyService.getStudyToUpdateTag(account, path); //설정하고자 하는 스터디 가져오고
        Tag tag = tagService.findOrCreateNew(tagForm.getTagTitle()); // 태그가 존재하면 그대로가져오고 아니면 저장하고
        //태그 가져왔으면
        studyService.addTag(study, tag);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/tags/remove")
    @ResponseBody
    public ResponseEntity removeTag(@CurrentUser Account account, @PathVariable String path,
                                    @RequestBody TagForm tagForm) {
        Study study = studyService.getStudyToUpdateTag(account, path);
        Tag tag = tagRepository.findByTitle(tagForm.getTagTitle());
        if (tag == null) {
            return ResponseEntity.badRequest().build();
        }
        studyService.removeTag(study, tag);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/zones")
    public String studyZonesForm(@CurrentUser Account account, @PathVariable String path, Model model) throws JsonProcessingException {
        Study study = studyService.getStudyToUpdateZone(account, path);
        model.addAttribute(account);
        model.addAttribute(study);

        model.addAttribute("zones", study.getZones().stream().map(Zone::toString)
                .collect(Collectors.toList()));
        List<String> allZones = zoneRepository.findAll().stream().map(Zone::toString).collect(Collectors.toList());
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allZones));
        return "study/settings/zones";
    }

    @PostMapping("/zones/add")
    @ResponseBody
    public ResponseEntity addZone(@CurrentUser Account account, @PathVariable String path,
                                  @RequestBody ZoneForm zoneForm) {
        Study study = studyService.getStudyToUpdateZone(account, path); //설정하고자 하는 스터디 가져오고
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName());
        if (zone == null) {
            return ResponseEntity.badRequest().build();
        }
        studyService.addZone(study, zone);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/zones/remove")
    @ResponseBody
    public ResponseEntity removeZone(@CurrentUser Account account, @PathVariable String path,
                                     @RequestBody ZoneForm zoneForm) {
        Study study = studyService.getStudyToUpdateZone(account, path);
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName());
        if (zone == null) {
            return ResponseEntity.badRequest().build();
        }
        studyService.removeZone(study, zone);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/study")
    public String studySettingForm(@CurrentUser Account account, @PathVariable String path, Model model) {
        Study study = studyService.getStudyToUpdate(account, path);
        model.addAttribute(account);
        model.addAttribute(study);
        return "study/settings/study";
    }

    @PostMapping("/study/publish")
    public String publishStudy(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        studyService.publish(study);
        attributes.addFlashAttribute("message", "스터디를 공개했습니다");
        return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
    }

    @PostMapping("/study/close")
    public String closeStudy(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdate(account, path);
        studyService.close(study);
        attributes.addFlashAttribute("message", "스터디를 종료했습니다.");
        return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
    }

    @PostMapping("/recruit/start")
    public String startRecruit(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!study.canUpdateRecruiting()) {
            attributes.addFlashAttribute("message", "1시간 안에 인원 모집 설정을 여러번 변경할 수 없습니다");
            return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
        }
        studyService.startRecruit(study);
        attributes.addFlashAttribute("message", "인원 모집을 시작합니다");
        return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
    }

    @PostMapping("/recruit/stop")
    public String stopRecruit(@CurrentUser Account account, @PathVariable String path, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!study.canUpdateRecruiting()) {
            attributes.addFlashAttribute("message", "1시간 안에 인원 모집 설정을 여러번 변경할 수 없습니다");
            return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
        }
        studyService.stopRecruit(study);
        attributes.addFlashAttribute("message", "인원 모집을 종료합니다");
        return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
    }

    @PostMapping("/study/path")
    public String updateStudyPath(@CurrentUser Account account, @PathVariable String path, @RequestParam String newPath,
                                  Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!studyService.isValidPath(newPath)) { //만약 패턴이 이상하거나 중복이있으면
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute("studyPathError", "해당 스터디 경로는 사용할 수 없습니다. 다른 값을 입력해주세요");
            return "study/settings/study";
        }
        studyService.updateStudyPath(study, newPath);
        attributes.addFlashAttribute("message", "스터디 경로를 수정했습니다");
        return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
    }

    @PostMapping("/study/title")
    public String updateStudyTitle(@CurrentUser Account account, @PathVariable String path, @RequestParam String newTitle,
                                   Model model, RedirectAttributes attributes) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        if (!studyService.isValidTitle(newTitle)) {
            model.addAttribute(account);
            model.addAttribute(study);
            model.addAttribute("studyTitleError", "스터디 이름을 다시 입력해주세요");
            return "study/settings/study";
        }
        studyService.updateStudyTitle(study, newTitle);
        attributes.addFlashAttribute("message", "스터디 이름을 수정했습니다.");
        return "redirect:/study/" + study.getEncodedPath() + "/settings/study";
    }

    /**
     * 삭제를 고민해봐야한다 . 진짜 지울건가 아니면 . soft delete . 지우는 척을 할것인가 . 보통. 다..
     * 지우는 ..척.
     */
    // 여기서는 스터디 종료가 있으니 삭제는 진짜 삭제
    // 프론트에서 버튼을 못누르게 하더라도 얼마든지 우회할 수 있다. 서버단에서도 처리를 해줘야한다
    @PostMapping("/study/remove")
    public String removeStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyService.getStudyToUpdateStatus(account, path);
        studyService.remove(study);
        return "redirect:/";
    }
}
