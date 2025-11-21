package com.dealermanagementsysstem.project.configuration;

import com.dealermanagementsysstem.project.Model.DAOBusinessSetting;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BusinessSettingsInitializer {
    @Autowired
    private BusinessConfig businessConfig;
    @Autowired
    private DAOBusinessSetting daoBusinessSetting;

    @PostConstruct
    public void loadSettings() {
        try {
            Double persistedVat = daoBusinessSetting.getDecimalSetting("VAT_RATE");
            if (persistedVat != null && persistedVat >= 0 && persistedVat <= 100) {
                businessConfig.getVat().setRate(persistedVat);
                System.out.println("[BusinessSettingsInitializer] Loaded persisted VAT_RATE=" + persistedVat + "%");
            } else {
                System.out.println("[BusinessSettingsInitializer] Using default VAT rate=" + businessConfig.getVat().getRate());
            }
        } catch (Exception e) {
            System.out.println("[BusinessSettingsInitializer] Failed to load VAT setting: " + e.getMessage());
        }
    }
}

