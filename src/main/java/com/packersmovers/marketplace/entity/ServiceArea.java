package com.packersmovers.marketplace.entity;

import com.packersmovers.marketplace.common.util.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "service_areas")
public class ServiceArea extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String cityOrRegion;

    @Column(length = 10)
    private String pincode;

    @Column(length = 60)
    private String state;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
