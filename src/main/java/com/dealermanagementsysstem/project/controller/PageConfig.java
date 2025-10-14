package com.dealermanagementsysstem.project.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/vehicleDistributionManagement").setViewName("evmPage/vehicleDistributionManagement");
        registry.addViewController("/dealerManagement").setViewName("");
        registry.addViewController("/evmReport").setViewName("");
        registry.addViewController("/evmDiscountPolicy").setViewName("");
        registry.addViewController("/evmVehicleList").setViewName("evmPage/vehicleList");
    }

}
