-- Add dealer fixed amount discount column to support manufacturer-like logic
ALTER TABLE QuotationDetail ADD AppliedDealerDiscountAmount DECIMAL(18,2) NULL;

