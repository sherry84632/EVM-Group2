package com.dealermanagementsysstem.project.service;

import com.dealermanagementsysstem.project.Model.DTOCustomer;
import com.dealermanagementsysstem.project.dto.CustomerForm;

import java.util.List;

public interface CustomerService {
    void updateCustomer(Integer customerId, CustomerForm  customerForm);
    void deleteCustomer(Integer  customerID);
    List<DTOCustomer> getCustomers();
    DTOCustomer getCustomer(int id);
    List<DTOCustomer> searchCustomer(String name);
    void createCustomerWithTestDrive(CustomerForm customerForm,String testDrive);
}
