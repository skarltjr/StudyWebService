package com.studyolle.modules.zone;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter @Setter @EqualsAndHashCode(of = "id")
@Builder @NoArgsConstructor @AllArgsConstructor
public class Zone {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String localNameOfCity;

    @Column(nullable = true) // 서울특별시 등
    private String province; //도

    @Override
    public String toString() {
        return String.format("%s(%s)/%s", city, localNameOfCity, province);
    }

}
