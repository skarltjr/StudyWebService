package com.studyolle.modules.event.event;

import com.studyolle.modules.event.Enrollment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EnrollmentEvent {

    private final Enrollment enrollment;

    private final String message;
}
