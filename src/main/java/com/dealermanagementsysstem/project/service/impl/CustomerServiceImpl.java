package com.dealermanagementsysstem.project.service.impl;

import com.dealermanagementsysstem.project.Model.*;
import com.dealermanagementsysstem.project.dto.CustomerForm;
import com.dealermanagementsysstem.project.exception.BusinessException;
import com.dealermanagementsysstem.project.exception.ErrorCode;
import com.dealermanagementsysstem.project.mapper.CustomerMapper;
import com.dealermanagementsysstem.project.repository.CustomerRepository;
import com.dealermanagementsysstem.project.repository.VehicleRepository;
import com.dealermanagementsysstem.project.service.CustomerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerServiceImpl implements CustomerService {


    DAOCustomer customerDAO;
    DAODealer dealerDAO;
    DAOTestDrive daoTestDrive;
    CustomerRepository customerRepository;
    VehicleRepository vehicleRepository;
    CustomerMapper customerMapper;


    @Override
    public void updateCustomer(Integer customerId, CustomerForm customerForm) {
        DTOCustomer existingCustomer = getCustomer(customerId);

        if (customerRepository.existsByEmail(customerForm.getEmail()) && !existingCustomer.getEmail().equals(customerForm.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, customerForm.getEmail());
        }

        customerMapper.updateCustomer(existingCustomer, customerForm);


        customerRepository.save(existingCustomer);

    }

    @Override
    public void deleteCustomer(Integer customerId) {
        customerRepository.deleteById(customerId);
    }

    @Override
    public List<DTOCustomer> getCustomers() {
        List<DTOCustomer> list = customerRepository.findAll();
        return list.stream().map(c -> new DTOCustomer(c.getCustomerID(), c.getDealer(), c.getFullName(), c.getPhone(), c.getEmail(), c.getAddress(), c.getCreatedAt(), c.getUpdatedAt(), c.getBirthDate(), c.getNote(), c.getVehicleInterest(), c.getTestDrives())).collect(Collectors.toList());
    }

    @Override
    public DTOCustomer getCustomer(int id) {
        return customerRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    @Override
    public List<DTOCustomer> searchCustomer(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return customerRepository.findAll();
        }
        return customerRepository.searchByKeyword(keyword.trim());
    }

    @Override
    public void createCustomerWithTestDrive(CustomerForm customerForm, String testDrive) {
        if (customerRepository.existsByEmail(customerForm.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, customerForm.getEmail());
        }

        DTOCustomer customer = customerMapper.toEntity(customerForm);

        int newCustomerID = customerRepository.saveAndFlush(customer).getCustomerID();
        if (newCustomerID <= 0) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR);
        }

        if (testDrive == null || testDrive.isEmpty()) {
            return;
        }

        LocalDateTime testDateTime;
        try {
            testDateTime = LocalDateTime.parse(testDrive);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_TESTDRIVE_DATE, testDrive);
        }
        Date testDate = Date.from(testDateTime.atZone(ZoneId.systemDefault()).toInstant());

        Integer vehicleID = null;
        if (customer.getVehicleInterest() != null && !customer.getVehicleInterest().trim().isEmpty()) {
            vehicleID = vehicleRepository.findLatestInStockVehicleByModelName("%" + customer.getVehicleInterest() + "%", VehicleStatus.IN_STOCK);
        }

        int dealerID = (customer.getDealer() != null && customer.getDealer().getDealerID() > 0) ? customer.getDealer().getDealerID() : 1;

        Integer staffID = dealerDAO.getFirstStaffIdByDealerId(dealerID);
        // Keep logic: allow null staff

        boolean testDriveSaved = daoTestDrive.insertTestDrive(newCustomerID, testDate, vehicleID, dealerID, staffID);
        if (!testDriveSaved) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR);
        }
    }
}
