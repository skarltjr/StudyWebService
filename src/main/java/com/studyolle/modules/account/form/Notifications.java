package com.studyolle.modules.account.form;

import lombok.Data;

@Data
public class Notifications {

    private boolean studyCreatedByEmail;

    private boolean studyCreatedByWeb;

    private boolean studyEnrollResultByEmail;

    private boolean studyEnrollResultByWeb;

    private boolean studyUpdatedByEmail;

    private boolean studyUpdatedByWeb;

/**     모델 매퍼는 빈이다 근데 이건 그냥 dto. 그러니 여기에 당연히 주입받을 수 없다 . component가 없으니
 *      그래서 컨트롤러에서 만드는데 생각해보면 stream map -> 해서 dto로 변환시켜서 넣는거랑 똑같다*/
}
