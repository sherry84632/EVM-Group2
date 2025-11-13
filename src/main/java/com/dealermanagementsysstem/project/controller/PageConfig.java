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
        // Note: /evmOrderHistory is now handled by EVMOrderController
        registry.addViewController("/dealerCustomerManagement").setViewName("dealerPage/customerManagement");
        // NOTE: /dealerCustomerList removed - use /customer/list instead (with data loading)
        registry.addViewController("/dealerCreateANewCustomer").setViewName("dealerPage/createANewCustomer");
        registry.addViewController("/dealerCustomerDetail").setViewName("dealerPage/customerDetail");
        registry.addViewController("/dealerVehiclesInformation").setViewName("dealerPage/vehiclesInformation");
        registry.addViewController("/dealerCustomerOrderFunctionPage").setViewName("dealerPage/customerOrder");
        registry.addViewController("/dealerCreateACustomerOrderPage").setViewName("dealerPage/createACustomerOrder");
        registry.addViewController("/dealerInventory").setViewName("dealerPage/dealerInventory");
        registry.addViewController("/showTestVehicleDetail").setViewName("evmPage/vehicleListDetail");
        registry.addViewController("/dealerDiscountManagementPage").setViewName("dealerPage/discountManagement");
        registry.addViewController("/dealerCreateANewDiscount").setViewName("dealerPage/createADealerDiscount");
        registry.addViewController("/dealerShowDiscountDetailTest").setViewName("dealerPage/discountDetail");
        registry.addViewController("/dealerShowCustomerOrderListTest").setViewName("dealerPage/dealerCustomerOrderList");
        registry.addViewController("/dealerShowCustomerOrderDetailTest").setViewName("dealerPage/dealerCustomerOrderDetail");
        registry.addViewController("/aboutUsPage").setViewName("mainPage/aboutUs");
    }

}
