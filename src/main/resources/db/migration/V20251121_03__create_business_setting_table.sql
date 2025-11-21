-- Create table to persist business settings (VAT and future settings)
GO
END;
    INSERT INTO BusinessSetting(SettingKey, SettingValue) VALUES ('VAT_RATE', 10.00);
BEGIN
IF NOT EXISTS (SELECT 1 FROM BusinessSetting WHERE SettingKey='VAT_RATE')
-- Initialize VAT rate from existing property if not present
GO
END;
    );
        UpdatedAt DATETIME2 NOT NULL DEFAULT GETDATE()
        SettingValue DECIMAL(18,4) NULL,
        SettingKey VARCHAR(100) NOT NULL PRIMARY KEY,
    CREATE TABLE BusinessSetting (
BEGIN
IF NOT EXISTS (SELECT 1 FROM sys.tables WHERE name = 'BusinessSetting')

