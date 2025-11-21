-- Add DealerSellingPrice columns to VehicleModel and Vehicle
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE Name='DealerSellingPrice' AND Object_ID=Object_ID('VehicleModel'))
BEGIN
    ALTER TABLE VehicleModel ADD DealerSellingPrice DECIMAL(18,2) NULL;
END
GO
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE Name='DealerSellingPrice' AND Object_ID=Object_ID('Vehicle'))
BEGIN
    ALTER TABLE Vehicle ADD DealerSellingPrice DECIMAL(18,2) NULL;
END
GO
-- Optional: Initialize VehicleModel.DealerSellingPrice = BasePrice (first time)
UPDATE VehicleModel SET DealerSellingPrice = ISNULL(DealerSellingPrice, BasePrice);
GO
-- Initialize Vehicle.DealerSellingPrice from its model if null
UPDATE v SET DealerSellingPrice = vm.DealerSellingPrice
FROM Vehicle v
JOIN VehicleVersion vv ON v.VersionID = vv.VersionID
JOIN VehicleModel vm ON vv.ModelID = vm.ModelID
WHERE v.DealerSellingPrice IS NULL;
GO
-- Remove obsolete dealer level share logic: (No structural changes here, logic will be removed in code)
-- End of migration

