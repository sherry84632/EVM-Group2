-- Migration: Add PurchaseOrderID to DealerInventory table
-- Date: 2025-11-03
-- Purpose: Link inventory vehicles to their purchase orders for tracking

-- Check if column exists, if not add it
IF NOT EXISTS (
    SELECT * FROM sys.columns
    WHERE object_id = OBJECT_ID(N'DealerInventory')
    AND name = 'PurchaseOrderID'
)
BEGIN
    ALTER TABLE DealerInventory
    ADD PurchaseOrderID INT NULL;

    PRINT 'Added PurchaseOrderID column to DealerInventory';
END
ELSE
BEGIN
    PRINT 'PurchaseOrderID column already exists in DealerInventory';
END
GO

-- Add foreign key constraint if not exists
IF NOT EXISTS (
    SELECT * FROM sys.foreign_keys
    WHERE name = 'FK_DealerInventory_PurchaseOrder'
    AND parent_object_id = OBJECT_ID(N'DealerInventory')
)
BEGIN
    ALTER TABLE DealerInventory
    ADD CONSTRAINT FK_DealerInventory_PurchaseOrder
    FOREIGN KEY (PurchaseOrderID) REFERENCES PurchaseOrder(PurchaseOrderID)
    ON DELETE SET NULL;  -- When order is deleted, keep inventory but set PurchaseOrderID to NULL

    PRINT 'Added foreign key constraint FK_DealerInventory_PurchaseOrder';
END
ELSE
BEGIN
    PRINT 'Foreign key constraint already exists';
END
GO

-- Create index for better query performance
IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_DealerInventory_PurchaseOrderID'
    AND object_id = OBJECT_ID(N'DealerInventory')
)
BEGIN
    CREATE INDEX IX_DealerInventory_PurchaseOrderID
    ON DealerInventory(PurchaseOrderID);

    PRINT 'Created index IX_DealerInventory_PurchaseOrderID';
END
ELSE
BEGIN
    PRINT 'Index already exists';
END
GO

PRINT 'Migration completed successfully!';

