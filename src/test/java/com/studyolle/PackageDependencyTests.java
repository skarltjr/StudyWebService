package com.studyolle;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packagesOf = App.class)
public class PackageDependencyTests {

    private final static String STUDY = "..modules.study..";
    private final static String EVENT = "..modules.event..";
    private final static String ACCOUNT = "..modules.account..";
    private final static String TAG = "..modules.tag..";
    private final static String ZONE = "..modules.zone..";


    @ArchTest
    ArchRule studyPackageRule = classes().that().resideInAPackage(STUDY)
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage(STUDY, EVENT);
    //스터디패키지는 이벤트랑 스터디패키지에서만 접근가능

    @ArchTest
    ArchRule eventPackageRule = classes().that().resideInAPackage(EVENT)
            .should().accessClassesThat().resideInAnyPackage(STUDY, ACCOUNT, EVENT);

    @ArchTest
    ArchRule accountPackageRule = classes().that().resideInAPackage(ACCOUNT)
            .should().accessClassesThat().resideInAnyPackage(TAG, ZONE, ACCOUNT);

    /**가장 중요한 순환참조 유무 확인*/
    @ArchTest
    ArchRule cycleCheck = slices().matching("com.studyolle.modules.(*)..")
            .should().beFreeOfCycles();

    @ArchTest
    ArchRule modulesPackageRule = classes().that().resideInAPackage("com.studyolle.modules..")
            .should().onlyBeAccessed().byClassesThat()
            .resideInAnyPackage("com.studyolle.modules..");
    //모듈 인프라 분리  - 모듈은 모듈만 참조하도록
}
