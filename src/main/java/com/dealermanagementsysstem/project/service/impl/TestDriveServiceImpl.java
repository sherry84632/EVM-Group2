package com.dealermanagementsysstem.project.service.impl;

import com.dealermanagementsysstem.project.Model.DTOTestDrive;
import com.dealermanagementsysstem.project.repository.TestDriveRepository;
import com.dealermanagementsysstem.project.service.TestDriveService;
import org.springframework.stereotype.Service;

@Service
public class TestDriveServiceImpl implements TestDriveService {

    private final TestDriveRepository testDriveRepository;

    public TestDriveServiceImpl(TestDriveRepository testDriveRepository) {
        this.testDriveRepository = testDriveRepository;
    }

    @Override
    public DTOTestDrive findByCustomerID(int id) {
        return testDriveRepository.findByCustomerId(id);
    }
}
