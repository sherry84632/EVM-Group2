-- Add DiscountPercent column to DiscountPolicy table
-- This field stores the discount percentage for dealers (e.g., 25%)
-- Separate from HangPercent (manufacturer share) and DailyPercent (dealer reward)

-- Add DiscountPercent column
IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID(N'DiscountPolicy')
               AND name = 'DiscountPercent')
BEGIN
    ALTER TABLE DiscountPolicy
    ADD DiscountPercent DECIMAL(5,2) NULL;

    PRINT 'Column DiscountPercent added to DiscountPolicy table';
END
ELSE
BEGIN
    PRINT 'Column DiscountPercent already exists in DiscountPolicy table';
END
GO

-- Update existing records: Set DiscountPercent = HangPercent if null
-- (Migration strategy: assume HangPercent was used as discount before)
UPDATE DiscountPolicy
SET DiscountPercent = HangPercent
WHERE DiscountPercent IS NULL AND HangPercent IS NOT NULL;
GO

-- Add comment
EXEC sp_addextendedproperty
    @name = N'MS_Description',
    @value = N'Discount percentage for dealer (e.g., 25% off original price)',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'DiscountPolicy',
    @level2type = N'COLUMN', @level2name = N'DiscountPercent';
GO

PRINT 'Migration V20251103_02 completed: DiscountPercent added to DiscountPolicy';

