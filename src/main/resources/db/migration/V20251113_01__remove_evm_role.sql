-- Migration Script: Remove EVM Role
-- Purpose: Convert existing EVM role accounts to ADMIN role
-- Date: 2025-11-13
-- Database: CarDealerDBI

-- ==========================================
-- STEP 1: Check existing EVM role accounts
-- ==========================================
SELECT
    AccountId,
    Username,
    Email,
    Role,
    IsActive
FROM Account
WHERE Role = 'EVM';

-- ==========================================
-- STEP 2: Backup existing EVM accounts (optional)
-- ==========================================
-- Uncomment if you want to backup before migration
/*
SELECT *
INTO Account_Backup_20251113
FROM Account
WHERE Role = 'EVM';
*/

-- ==========================================
-- STEP 3: Update EVM accounts to ADMIN
-- ==========================================
UPDATE Account
SET Role = 'ADMIN'
WHERE Role = 'EVM';

-- ==========================================
-- STEP 4: Verify no EVM roles remain
-- ==========================================
SELECT COUNT(*) AS RemainingEVMAccounts
FROM Account
WHERE Role = 'EVM';
-- Expected result: 0

-- ==========================================
-- STEP 5: Verify updated accounts
-- ==========================================
SELECT
    AccountId,
    Username,
    Email,
    Role,
    IsActive,
    CreatedAt,
    UpdatedAt
FROM Account
WHERE Role = 'ADMIN'
ORDER BY UpdatedAt DESC;

-- ==========================================
-- STEP 6: Check all account roles
-- ==========================================
SELECT
    Role,
    COUNT(*) AS Count,
    SUM(CASE WHEN IsActive = 1 THEN 1 ELSE 0 END) AS ActiveCount
FROM Account
GROUP BY Role
ORDER BY Role;

-- Expected roles:
-- ADMIN
-- EVMSTAFF
-- DEALER
-- DEALERSTAFF

-- ==========================================
-- NOTES:
-- ==========================================
-- 1. This script should be run AFTER deploying the code changes
-- 2. All EVM role accounts will become ADMIN accounts
-- 3. ADMIN role has all EVM functions plus account management
-- 4. No data will be lost
-- 5. Users can still login with the same credentials
-- 6. Only their role permissions will change

-- ==========================================
-- ROLLBACK (if needed):
-- ==========================================
-- If you need to rollback, restore from backup:
/*
UPDATE Account
SET Role = 'EVM'
WHERE AccountId IN (
    SELECT AccountId FROM Account_Backup_20251113
);
*/

