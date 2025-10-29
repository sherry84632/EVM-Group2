-- Add delivery-related columns to SaleOrder if they do not exist (SQL Server)
IF COL_LENGTH('SaleOrder','PlannedDeliveryDate') IS NULL
BEGIN
    ALTER TABLE SaleOrder ADD PlannedDeliveryDate DATETIME NULL;
END;
IF COL_LENGTH('SaleOrder','ActualDeliveryDate') IS NULL
BEGIN
    ALTER TABLE SaleOrder ADD ActualDeliveryDate DATETIME NULL;
END;
IF COL_LENGTH('SaleOrder','EtaDays') IS NULL
BEGIN
    ALTER TABLE SaleOrder ADD EtaDays INT NULL;
END;

-- Populate PlannedDeliveryDate & EtaDays for existing rows where missing
-- Rule: base ETA 7 days, add +2 days if Quantity > 5
UPDATE SaleOrder
SET PlannedDeliveryDate = CASE
        WHEN PlannedDeliveryDate IS NULL AND CreatedAt IS NOT NULL THEN DATEADD(DAY, CASE WHEN Quantity > 5 THEN 9 ELSE 7 END, CreatedAt)
        ELSE PlannedDeliveryDate END,
    EtaDays = COALESCE(EtaDays, CASE WHEN Quantity > 5 THEN 9 ELSE 7 END)
WHERE PlannedDeliveryDate IS NULL OR EtaDays IS NULL;

-- Ensure EtaDays never negative
UPDATE SaleOrder SET EtaDays = 0 WHERE EtaDays < 0;

-- Add check constraint to guarantee ActualDeliveryDate >= PlannedDeliveryDate (when both set)
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_SaleOrder_ActualAfterPlanned')
BEGIN
    ALTER TABLE SaleOrder ADD CONSTRAINT CK_SaleOrder_ActualAfterPlanned CHECK (
        ActualDeliveryDate IS NULL OR PlannedDeliveryDate IS NULL OR ActualDeliveryDate >= PlannedDeliveryDate
    );
END;

-- Create index on PlannedDeliveryDate to speed up delivery queries
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes WHERE name = 'IX_SaleOrder_PlannedDeliveryDate' AND object_id = OBJECT_ID('SaleOrder')
)
BEGIN
    CREATE INDEX IX_SaleOrder_PlannedDeliveryDate ON SaleOrder(PlannedDeliveryDate);
END;
