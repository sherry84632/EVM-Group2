IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name='DealerModelPrice')
BEGIN
    CREATE TABLE DealerModelPrice (
        DealerID INT NOT NULL,
        ModelID INT NOT NULL,
        DealerSellingPrice DECIMAL(18,2) NOT NULL,
        UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        CONSTRAINT PK_DealerModelPrice PRIMARY KEY (DealerID, ModelID),
        CONSTRAINT FK_DMP_Dealer FOREIGN KEY (DealerID) REFERENCES Dealer(DealerID),
        CONSTRAINT FK_DMP_Model FOREIGN KEY (ModelID) REFERENCES VehicleModel(ModelID)
    );
END
GO
-- Seed existing dealers with base prices if not present
INSERT INTO DealerModelPrice (DealerID, ModelID, DealerSellingPrice, UpdatedAt)
SELECT d.DealerID, vm.ModelID, ISNULL(vm.DealerSellingPrice, vm.BasePrice), SYSUTCDATETIME()
FROM Dealer d CROSS JOIN VehicleModel vm
LEFT JOIN DealerModelPrice dmp ON dmp.DealerID=d.DealerID AND dmp.ModelID=vm.ModelID
WHERE dmp.DealerID IS NULL;
GO

