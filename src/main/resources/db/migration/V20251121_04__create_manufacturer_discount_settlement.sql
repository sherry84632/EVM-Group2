-- Manufacturer discount settlement table (fix broken previous content)
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name='ManufacturerDiscountSettlement')
BEGIN
    CREATE TABLE ManufacturerDiscountSettlement (
        SettlementID INT IDENTITY PRIMARY KEY,
        SaleOrderID INT NOT NULL,
        DealerID INT NOT NULL,
        TotalManufacturerDiscount DECIMAL(18,2) NOT NULL DEFAULT 0,
        ReimbursedAmount DECIMAL(18,2) NOT NULL DEFAULT 0,
        Status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING / PARTIAL / PAID
        CreatedAt DATETIME2 NOT NULL DEFAULT GETDATE(),
        UpdatedAt DATETIME2 NULL,
        PaidDate DATETIME2 NULL,
        Notes VARCHAR(500) NULL
    );
    CREATE INDEX IX_ManufacturerDiscountSettlement_SaleOrder ON ManufacturerDiscountSettlement(SaleOrderID);
END
GO
-- Add foreign keys if table exists and not yet added
IF EXISTS (SELECT 1 FROM sys.tables WHERE name='ManufacturerDiscountSettlement')
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_MDS_SaleOrder')
        ALTER TABLE ManufacturerDiscountSettlement ADD CONSTRAINT FK_MDS_SaleOrder FOREIGN KEY (SaleOrderID) REFERENCES SaleOrder(SaleOrderID);
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name='FK_MDS_Dealer')
        ALTER TABLE ManufacturerDiscountSettlement ADD CONSTRAINT FK_MDS_Dealer FOREIGN KEY (DealerID) REFERENCES Dealer(DealerID);
END
GO
-- End of migration
