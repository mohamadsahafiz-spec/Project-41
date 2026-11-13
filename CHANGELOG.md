# Changelog

All notable changes to **Project 41** are documented in this file.

## [1.1.2] - Payday Intelligence V1.1.1 (Data Integrity Polish)
### Fixed & Improved
- **Preserved Debt APRs Across Database Reopens**: Removed unconditional `aprPercent` overwrites from `AppDatabase.onOpen()` while keeping initial reference debt rates for fresh database prepopulation.
- **Complete Debt Metadata Backup & Restore**: Updated `restoreDataFromJson()` to restore debt `aprPercent`, `isHighInterest`, `initialBalance`, `currentBalance`, `monthlyTarget`, and `name`, while remaining compatible with older backups and never fabricating missing rate metadata.
- **Accurate Zero-Surplus vs Shortfall Classification**: Distinguished exact break-even (`ZERO_SURPLUS`) from genuine negative `SHORTFALL` in `PaydayIntelligenceEngine` and Stage 2 UI so zero surplus blocks extra allocations cleanly without showing a false `Shortfall: RM 0.00` warning.

## [1.1.1] - Payday Intelligence V1.1
### Fixed & Improved
- **Cash Reconciliation & Double-Counting Prevention**: Established an explicit single source of truth in Stage 2 (`Opening Bank Cash + Cashflow` vs `Current Bank Balance`) and clearly separated `Current Account Cash` (`bankBalance`) from `Calculated Monthly Cashflow`.
- **Strict Allocation Validation**: Added hard validation rules rejecting negative, non-numeric, and excessive allocations; caps extra debt repayment at eligible outstanding debt balances, caps emergency savings at the remaining emergency target gap, enforces minimum cash buffer targets, and blocks approving allocations that violate Priority 1 obligations.
- **Debt-by-Debt Waterfall Breakdown**: Added structured per-debt extra repayment breakdown ranked by highest eligible rate (`SHOPEE_SLOAN` 18% provisional -> `MAYBANK_CC` 15% APR; excluding 3% reference-only HP1/HP2) and persisted target debt keys/amounts cleanly.
- **Persistent Check-In Stage Progress (`Room v4`)**: Added `monthly_checkin_progress` table and `MIGRATION_3_4` to persist Monthly Check-In Stages 1–6 completion per month accurately across screen/Activity recreation.
- **Full JSON Backup & Restore (`v3.0`)**: Included `monthly_incomes`, `payday_allocations` (with separate recommended vs approved allocations and debt breakdown), and `monthly_checkin_progress` in JSON export/restore with backward compatibility for older backups.

## [1.1.0] - Payday Intelligence V1
### Added
- **Payday Intelligence V1 (`Stage 2 — Payday Plan`)**: Integrated an explainable payday allocation step inside the Monthly Check-In workflow, immediately after income entry and before monthly commitment payments.
- **Deterministic Allocation Engine (`PaydayIntelligenceEngine`)**:
  - Prioritizes upcoming essential commitments, minimum debt payments, and upcoming essential living expenses (Priority 1).
  - Evaluates existing dedicated emergency savings against target and preserves a liquidity cash buffer (Priority 2).
  - Ranks debts by actual effective interest rate (e.g., Shopee SLoan 18% provisional vs Maybank CC 15% p.a.) and excludes reference-only vehicle hire purchases (`HP1`/`HP2` at 3%) from unverified early-settlement assumptions (Priority 3).
  - Recommends controlled discretionary spending from regular salary surplus while shielding OT, incentives, and bonuses from lifestyle creep, with explicit spending consequence summaries (Priority 4).
- **Explainability & Explicit Data Gaps**: Every recommendation line includes a `WHY` explanation, and missing inputs (emergency target, upcoming essentials budget, missing debt rates, reference-only loans, unconfirmed income) are surfaced explicitly without fabricating numbers.
- **Recommended vs Approved Persistence (`Room v3`)**: Added `payday_allocations` table and `MIGRATION_2_3` so engine recommendations and user overrides are stored separately without losing existing database records; never auto-executes payments or transfers.
- **Reimbursement & Unconfirmed Income Separation**: Extended `MonthlyIncomeEntity` and Stage 1 Check-In UI with Other Income, Reimbursements (excluded from spendable income), Unconfirmed Income, and Confirmed Received toggle to prevent double-counting.
- **In-App Version & Changelog**: Added version display and release notes viewer in Profile & Settings.

## [1.0.1] - Monthly Check-In Safety & Compact Widget Sprint
### Fixed & Improved
- Restored Poka-Yoke payment confirmation dialog before committing monthly commitment payments.
- Guarded Monthly Check-In completion status so completion requires genuine prior stage completion.
- Redesigned home-screen expense widget to a compact Samsung-style dark layout with Project 41 green accents and `QuickExpenseActivity` floating popup.
- Configured Malaysian debt tracking (`Maybank CC 15% p.a.`, `Shopee SLoan 1.5%/mo provisional`, `HP1/HP2 3% reference only`) and strict KWSP overtime exclusion.

## [1.0.0] - Project 41 Initial Release
### Added
- Native Android personal financial freedom tracker grounded in UK Personal Finance Flowchart v3.0.10 adapted for Malaysia.
- Dashboard, Flowchart Mission, Monthly Check-In, My Savings Vault, Quick Expenses, Monthly Review, Milestones, and Local-First JSON Backup & Restore.
