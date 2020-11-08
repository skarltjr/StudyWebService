package com.studyolle.modules.account;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyolle.modules.account.form.NicknameForm;
import com.studyolle.modules.account.form.Notifications;
import com.studyolle.modules.account.form.PasswordForm;
import com.studyolle.modules.account.form.Profile;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.zone.Zone;
import com.studyolle.modules.account.validator.NicknameValidator;
import com.studyolle.modules.account.validator.PasswordFormValidator;
import com.studyolle.modules.tag.TagForm;
import com.studyolle.modules.tag.TagRepository;
import com.studyolle.modules.zone.ZoneForm;
import com.studyolle.modules.zone.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    @InitBinder("passwordForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(new PasswordFormValidator());
    }

    private final AccountService accountService;
    private final ModelMapper modelMapper;
    private final NicknameValidator nicknameValidator;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;
    private final ZoneRepository zoneRepository;

    @GetMapping("/settings/profile") // 굳이 여기서 {}처럼 개인정보를 안받아도
    public String updateProfileForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, Profile.class)); //이거 그냥 dto만들어서 넣어주는거랑 똑같다
        //이건 model.addAttribute("account, new Profile(account)) 랑 동일
        return "settings/profile";
    }

    /**
     * ModelAttribute 는 입력시 생기는 에러를 항상 같이 받아주기 위해 Errors를 달고 다니자
     * 프로필은 자기소개 이미지 등을 변경하니까 레퍼지토리에서 중복검사를 할 필요가없으니 validator 따로 필요없다
     */
    @PostMapping("/settings/profile")  //Valid로 조건에 맞는지 검사하고
    public String updateProfile(@CurrentUser Account account, @Valid Profile profile, //원래는 @ModelAttribute도 붙여줘야한다생략가능
                                Errors errors, Model model, RedirectAttributes attributes) {
        /**  ★★ 여기서 account는 준영속상태 즉 detached  = 영속상태가 아니다! 그러나 한 번 이미 디비에는
         * 들어갔다 나온애 그래서 id값은 있다. 결국 아무리 이걸갖고 변경해봤자 변경감지가 불가능 jpa가 관리하는 놈이 아니니까~*/

        //본인만 당연히 수정가능하고 , 화면에서 입력한 정보를 담고있는 프로필 채워서 오고 오류있으면 오류 가져오기 위해
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return "settings/profile";
        }
        //데이터를 수정하는 것은 트랜잭션을 가진 서비스에게 위임하자 !
        accountService.updateProfile(account, profile);  // 여기서 account는 ? detached
        attributes.addFlashAttribute("message", "프로필을 수정했습니다.");// 한 번쓰고 버려질 데이터 .그래서 flash
        return "redirect:" + "/settings/profile";
    }

    @GetMapping("/settings/password")
    public String updatePasswordForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new PasswordForm());
        return "settings/password";
    }

    @PostMapping("/settings/password")
    public String updatePassword(@CurrentUser Account account, @Valid PasswordForm passwordForm, Errors errors,
                                 Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return "settings/password";
        }
        accountService.updatePassword(account, passwordForm.getNewPassword());
        attributes.addFlashAttribute("message", "패스워드를 변경했습니다");
        return "redirect:" + "/settings/password";
    }

    @GetMapping("/settings/notifications")
    public String updateNotificationForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, Notifications.class)); //이거 그냥 dto만들어서 map돌려서 넣어주는거랑 똑같다
        return "settings/notifications";
        // Notifications map = modelMapper.map(account, Notifications.class); 그럼 당연히 notification객체 생성되
        //는거니까 addattribute로 넣을 수 있는것
    }

    @PostMapping("/settings/notifications")
    public String updateNotifications(@CurrentUser Account account, @Valid Notifications notifications,
                                      Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return "settings/notifications";
        }
        accountService.updateNotifications(account, notifications);
        attributes.addFlashAttribute("message", "알림 설정을 변경했습니다");
        return "redirect:" + "/settings/notifications";
    }

    @InitBinder("nicknameForm")
    public void nicknameFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(nicknameValidator);
    } //닉네임 폼을 추가할 때 이 밸리데이터추가하기위해

    @GetMapping("/settings/account")
    public String updateAccountForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(modelMapper.map(account, NicknameForm.class));
        // 닉네임폼 디티오에 어카운트 채우도록
        return "settings/account";
    }

    @PostMapping("/settings/account")
    public String updateAccount(@CurrentUser Account account, @Valid NicknameForm nicknameForm,
                                Model model, Errors errors, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return "settings/account";
        }

        accountService.updateNickname(account, nicknameForm.getNickname());
        attributes.addFlashAttribute("message", "닉네임을 수정했습니다");
        return "redirect:" + "/settings/account";
    }

    @GetMapping("/settings/tags")
    public String updateTags(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute(account);
        Set<Tag> tags = accountService.getTags(account);
        model.addAttribute("tags", tags.stream().map(Tag::getTitle).collect(Collectors.toList()));

        //이전에 등록된 적이 있던 태그는 자동완성가능하게   //태그들을 다 태그명  string 으로 돌려서 list로
        List<String> allTags = tagRepository.findAll().stream().map(Tag::getTitle).collect(Collectors.toList());
        //근데 이걸 자바 스트링타입이 아니라 json으로 보내줘야해서 ObjectMapper 주입받고 사용
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allTags));
        // 태그 엔티티자체 말고 태그명만 보내주기위해
        return "settings/tags";
    }

    @PostMapping("/settings/tags/add")
    @ResponseBody //ajax 요청이기때문에 아래와 같이 평소와 다름
    public ResponseEntity addTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Tag tag = tagRepository.findByTitle(title);
        if (tag == null) {
            tag = tagRepository.save(Tag.builder().title(tagForm.getTagTitle()).build());
            //save(new Tag(tageForm.gettitle)) 이랑 똑같은데 빌더어노테이션 사용
        }
        // 태그가 존재하면
        accountService.addTag(account, tag);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settings/tags/remove")
    @ResponseBody //ajax 요청이기때문에 아래와 같이 평소와 다름
    public ResponseEntity removeTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Tag tag = tagRepository.findByTitle(title);
        if (tag == null) {
            return ResponseEntity.badRequest().build();  //태그가 없는데 삭제하려니까
        }
        // 태그가 존재하면
        accountService.removeTag(account, tag);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings/zones")
    public String updateZonesForm(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute(account);
        Set<Zone> zones = accountService.getZones(account);
        model.addAttribute("zones", zones.stream().map(Zone::toString).collect(Collectors.toList()));

        List<String> allZones = zoneRepository.findAll().stream().map(Zone::toString).collect(Collectors.toList());
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allZones));

        return "settings/zones";
    }

    @PostMapping("/settings/zones/add")
    @ResponseBody
    public ResponseEntity addZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName());
        if (zone == null) {
            return ResponseEntity.badRequest().build();
        }
        accountService.addZone(account, zone);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/settings/zones/remove")
    public ResponseEntity removeZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvince(zoneForm.getCityName(), zoneForm.getProvinceName());
        if (zone == null) {
            return ResponseEntity.badRequest().build();
        }
        accountService.removeZone(account, zone);
        return ResponseEntity.ok().build();
    }

}
