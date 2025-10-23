-- =========================================================
-- DATABASE MIGRATION: Add QuotationID to SaleOrder tables
-- Date: 2025-10-24
-- Purpose: Support Quotation to SaleOrder conversion tracking
-- =========================================================

USE [YourDatabaseName]; -- CHANGE THIS to your database name
GO

-- Step 1: Check if QuotationID column already exists in SaleOrder
IF NOT EXISTS (
    SELECT * FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[dbo].[SaleOrder]')
    AND name = 'QuotationID'
)
BEGIN
    PRINT 'Adding QuotationID column to SaleOrder table...'

    -- Add QuotationID column (nullable first to allow existing rows)
    ALTER TABLE [dbo].[SaleOrder]
    ADD QuotationID INT NULL;

    PRINT '✓ QuotationID column added to SaleOrder'
END
ELSE
BEGIN
    PRINT '⚠ QuotationID column already exists in SaleOrder'
END
GO

-- Step 2: Add foreign key constraint (optional but recommended)
IF NOT EXISTS (
    SELECT * FROM sys.foreign_keys
    WHERE name = 'FK_SaleOrder_Quotation'
)
BEGIN
    PRINT 'Adding foreign key constraint FK_SaleOrder_Quotation...'

    ALTER TABLE [dbo].[SaleOrder]
    ADD CONSTRAINT FK_SaleOrder_Quotation
    FOREIGN KEY (QuotationID) REFERENCES [dbo].[Quotation](QuotationID);

    PRINT '✓ Foreign key constraint added'
END
ELSE
BEGIN
    PRINT '⚠ Foreign key constraint FK_SaleOrder_Quotation already exists'
END
GO

-- Step 3: Add unique index to prevent duplicate conversions
IF NOT EXISTS (
    SELECT * FROM sys.indexes
    WHERE name = 'IX_SaleOrder_QuotationID'
    AND object_id = OBJECT_ID(N'[dbo].[SaleOrder]')
)
BEGIN
    PRINT 'Adding unique index IX_SaleOrder_QuotationID...'

    -- Create unique index (allows NULL values)
    CREATE UNIQUE NONCLUSTERED INDEX IX_SaleOrder_QuotationID
    ON [dbo].[SaleOrder](QuotationID)
    WHERE QuotationID IS NOT NULL;

    PRINT '✓ Unique index created (prevents duplicate quotation conversion)'
END
ELSE
BEGIN
    PRINT '⚠ Index IX_SaleOrder_QuotationID already exists'
END
GO

-- Step 4: Check if QuotationID column exists in SaleOrderDetail
IF NOT EXISTS (
    SELECT * FROM sys.columns
    WHERE object_id = OBJECT_ID(N'[dbo].[SaleOrderDetail]')
    AND name = 'QuotationID'
)
BEGIN
    PRINT 'Adding QuotationID column to SaleOrderDetail table...'

    -- Add QuotationID column for traceability
    ALTER TABLE [dbo].[SaleOrderDetail]
    ADD QuotationID INT NULL;

    PRINT '✓ QuotationID column added to SaleOrderDetail'
END
ELSE
BEGIN
    PRINT '⚠ QuotationID column already exists in SaleOrderDetail'
END
GO

-- Step 5: Clean up invalid QuotationID references in SaleOrderDetail before adding FK
PRINT 'Checking for invalid QuotationID references in SaleOrderDetail...'

-- Delete or NULL out any SaleOrderDetail rows with invalid QuotationID references
UPDATE [dbo].[SaleOrderDetail]
SET QuotationID = NULL
WHERE QuotationID IS NOT NULL
AND QuotationID NOT IN (SELECT QuotationID FROM [dbo].[Quotation]);

DECLARE @invalidCount INT = @@ROWCOUNT;
IF @invalidCount > 0
BEGIN
    PRINT '⚠ Cleaned up ' + CAST(@invalidCount AS VARCHAR(10)) + ' invalid QuotationID references'
END
ELSE
BEGIN
    PRINT '✓ No invalid QuotationID references found'
END
GO

-- Step 6: Add foreign key for SaleOrderDetail (optional - only for non-NULL values)
IF NOT EXISTS (
    SELECT * FROM sys.foreign_keys
    WHERE name = 'FK_SaleOrderDetail_Quotation'
)
BEGIN
    PRINT 'Adding foreign key constraint FK_SaleOrderDetail_Quotation...'

    -- ✅ FIX: Add WITH NOCHECK to allow existing NULL values
    ALTER TABLE [dbo].[SaleOrderDetail]
    WITH NOCHECK ADD CONSTRAINT FK_SaleOrderDetail_Quotation
    FOREIGN KEY (QuotationID) REFERENCES [dbo].[Quotation](QuotationID);

    -- Enable the constraint for future inserts/updates
    ALTER TABLE [dbo].[SaleOrderDetail]
    CHECK CONSTRAINT FK_SaleOrderDetail_Quotation;

    PRINT '✓ Foreign key constraint added to SaleOrderDetail (allows NULL)'
END
ELSE
BEGIN
    PRINT '⚠ Foreign key constraint FK_SaleOrderDetail_Quotation already exists'
END
GO

-- Step 6: Verify the changes
PRINT ''
PRINT '========================================='
PRINT 'MIGRATION COMPLETED - VERIFICATION:'
PRINT '========================================='

SELECT
    'SaleOrder' AS TableName,
    c.name AS ColumnName,
    t.name AS DataType,
    c.is_nullable AS IsNullable
FROM sys.columns c
JOIN sys.types t ON c.user_type_id = t.user_type_id
WHERE object_id = OBJECT_ID(N'[dbo].[SaleOrder]')
AND c.name = 'QuotationID'

UNION ALL

SELECT
    'SaleOrderDetail' AS TableName,
    c.name AS ColumnName,
    t.name AS DataType,
    c.is_nullable AS IsNullable
FROM sys.columns c
JOIN sys.types t ON c.user_type_id = t.user_type_id
WHERE object_id = OBJECT_ID(N'[dbo].[SaleOrderDetail]')
AND c.name = 'QuotationID';

PRINT ''
PRINT '✓ Migration script completed successfully!'
PRINT 'You can now run your Spring Boot application.'
GO

