package com.dealermanagementsysstem.project.Model;

public enum VehicleStatus {
    // Catalog template vehicle - not a physical inventory unit yet
    TEMPLATE,
    // Vehicle physically in dealer or EVM stock and available for allocation
    IN_STOCK,
    // Vehicle reserved for a specific sale/purchase order but not yet delivered
    RESERVED,
    // Vehicle allocated (committed) to an order; moving through processing workflow
    ALLOCATED,
    // Vehicle currently in production/assembly stage
    IN_PRODUCTION,
    // Vehicle delivered/handed over and marked as sold
    SOLD,
    // Vehicle transferred to another dealer or entity
    TRANSFERRED
}
