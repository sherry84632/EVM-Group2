package com.dealermanagementsysstem.project.service;

import com.dealermanagementsysstem.project.Model.DTOTestDrive;

public interface TestDriveService {
    DTOTestDrive findByCustomerID(int id);
}
