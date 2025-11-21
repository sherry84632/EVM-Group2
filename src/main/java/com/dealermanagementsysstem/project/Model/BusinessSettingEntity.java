package com.dealermanagementsysstem.project.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BusinessSetting")
public class BusinessSettingEntity {
    @Id
    @Column(name = "SettingKey", length = 100)
    private String settingKey;

    @Column(name = "SettingValue")
    private Double settingValue;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public Double getSettingValue() { return settingValue; }
    public void setSettingValue(Double settingValue) { this.settingValue = settingValue; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

