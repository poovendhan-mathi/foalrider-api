package com.foalrider.modules.pricing.entity;

import com.foalrider.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "regions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Region extends BaseEntity {

    @Column(nullable = false, unique = true, length = 2)
    private String code; // ISO 3166-1 alpha-2: US, SG, IN, GB

    @Column(nullable = false)
    private String name; // United States, Singapore, India, United Kingdom

    @Column(name = "timezone")
    private String timezone; // America/New_York, Asia/Singapore, etc.

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_currency_id", nullable = false)
    private Currency defaultCurrency;

    @Column(name = "locale_code", length = 10)
    private String localeCode; // en-US, en-SG, en-IN, en-GB

    @Column(name = "date_format", length = 20)
    @Builder.Default
    private String dateFormat = "yyyy-MM-dd"; // or dd/MM/yyyy for UK, IN

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
}
