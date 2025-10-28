-- Manual migration to add DiscountPercent column required by DTOQuotation
-- Run this in SQL Server Management Studio or via a JDBC client before restarting the app.
-- Adjust schema name if not dbo.

ALTER TABLE Quotation ADD DiscountPercent FLOAT NULL;
GO

-- Optional: If you prefer controlled precision, drop the above and use DECIMAL instead:
-- ALTER TABLE Quotation ADD DiscountPercent DECIMAL(5,2) NULL;
-- GO

-- (Optional) Initialize existing rows to 0 (meaning no discount) if you want explicit values:
-- UPDATE Quotation SET DiscountPercent = 0 WHERE DiscountPercent IS NULL;
-- GO

-- Verification query:
-- SELECT TOP (10) QuotationID, DiscountPercent FROM Quotation ORDER BY QuotationID DESC;

