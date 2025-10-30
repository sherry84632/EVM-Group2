package com.dealermanagementsysstem.project.Model;

public enum VehicleStatus {
    TEMPLATE,      // Xe mẫu trong catalog (chỉ để dealer xem, không phải xe thực)
    IN_STOCK,      // Xe thực trong kho dealer
    ALLOCATED,     // Xe đã được phân bổ cho đơn hàng
    SOLD,          // Xe đã bán
    TRANSFERRED    // Xe đã chuyển giao
}
