package com.studyolle.modules.study.event;

import com.studyolle.modules.study.Study;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;

@Getter
@RequiredArgsConstructor
public class StudyUpdateEvent{
  private final Study study;
  private final String message;
}
