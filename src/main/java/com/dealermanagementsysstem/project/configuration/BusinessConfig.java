package com.dealermanagementsysstem.project.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Business Configuration Properties
 * Contains configurable business rules and rates
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class BusinessConfig {

    private VatConfig vat = new VatConfig();

    public VatConfig getVat() {
        return vat;
    }

    public void setVat(VatConfig vat) {
        this.vat = vat;
    }

    public static class VatConfig {
        /**
         * VAT (Value Added Tax) rate in percentage
         * Default: 10.0 (means 10%)
         */
        private Double rate = 10.0;

        public Double getRate() {
            return rate;
        }

        public void setRate(Double rate) {
            this.rate = rate;
        }

        /**
         * Get VAT rate as decimal (for calculation)
         * Example: 10% -> 0.10
         */
        public Double getRateAsDecimal() {
            return rate / 100.0;
        }
    }
}

