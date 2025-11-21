-- Add reimbursed amount column to track partial payments for dealer rewards
ALTER TABLE DealerRewardSettlement ADD ReimbursedAmount DECIMAL(18,2) NOT NULL DEFAULT 0;
-- Backfill: ensure any PAID settlement has ReimbursedAmount = RewardAmount
UPDATE DealerRewardSettlement SET ReimbursedAmount = RewardAmount WHERE Status='PAID';

