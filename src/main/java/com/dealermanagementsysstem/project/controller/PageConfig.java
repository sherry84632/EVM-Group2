package com.dealermanagementsysstem.project.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PageConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/showDealerHomePage").setViewName("dealerPage/DealerHomePage");
        registry.addViewController("/showEVMHomePage").setViewName("evmPage/homePage");
        registry.addViewController("/vehicleDistributionManagement").setViewName("evmPage/vehicleDistributionManagement");
        registry.addViewController("/dealerManagement").setViewName("");
        registry.addViewController("/evmReport").setViewName("");
        registry.addViewController("/evmDiscountPolicy").setViewName("");
        registry.addViewController("/evmVehicleList").setViewName("evmPage/vehicleList");
        registry.addViewController("/evmCreateANewVehicleToList").setViewName("evmPage/createANewVehicleToList");
        registry.addViewController("/evmOrderList").setViewName("evmPage/evmOrderList");
        registry.addViewController("/evmOrderHistory").setViewName("evmPage/evmOrderHistory");
        registry.addViewController("/dealerCustomerManagement").setViewName("dealerPage/customerManagement");
        registry.addViewController("/dealerCustomerList").setViewName("dealerPage/customerList");
        registry.addViewController("/dealerCreateANewCustomer").setViewName("dealerPage/createANewCustomer");
        registry.addViewController("/dealerCustomerDetail").setViewName("dealerPage/customerDetail");
    }

}
