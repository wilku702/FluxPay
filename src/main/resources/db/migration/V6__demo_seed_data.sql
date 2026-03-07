-- ============================================================
-- V6: Demo seed data for FluxPay demos
-- 3 users, 6 accounts, ~30 transactions, daily summaries
-- All passwords: demo123
-- ============================================================

-- BCrypt hash of "demo123"
-- Generated with rounds=10

-- -------------------------------------------------------
-- 1. USERS
-- -------------------------------------------------------
INSERT INTO users (id, email, password_hash, full_name, created_at) VALUES
  (1001, 'alice@demo.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice Johnson',  NOW() - INTERVAL '30 days'),
  (1002, 'bob@demo.com',     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bob Williams',   NOW() - INTERVAL '28 days'),
  (1003, 'charlie@demo.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Charlie Garcia',  NOW() - INTERVAL '25 days');

-- Advance the users sequence past our demo IDs
SELECT setval('users_id_seq', GREATEST((SELECT MAX(id) FROM users), 1003));

-- -------------------------------------------------------
-- 2. ACCOUNTS
-- -------------------------------------------------------
-- Alice: 2 USD accounts (checking + savings)
-- Bob:   1 USD account, 1 EUR account
-- Charlie: 1 USD account (active), 1 USD account (frozen)
INSERT INTO accounts (id, user_id, account_name, balance, currency, status, version, created_at) VALUES
  (1001, 1001, 'Checking',        5250.0000, 'USD', 'ACTIVE', 12, NOW() - INTERVAL '30 days'),
  (1002, 1001, 'Savings',         8200.0000, 'USD', 'ACTIVE',  5, NOW() - INTERVAL '28 days'),
  (1003, 1002, 'Main Account',    3150.0000, 'USD', 'ACTIVE',  8, NOW() - INTERVAL '28 days'),
  (1004, 1002, 'Euro Account',    1500.0000, 'EUR', 'ACTIVE',  3, NOW() - INTERVAL '20 days'),
  (1005, 1003, 'Primary',         1800.0000, 'USD', 'ACTIVE',  6, NOW() - INTERVAL '25 days'),
  (1006, 1003, 'Old Account',        0.0000, 'USD', 'FROZEN',  2, NOW() - INTERVAL '25 days');

SELECT setval('accounts_id_seq', GREATEST((SELECT MAX(id) FROM accounts), 1006));

-- -------------------------------------------------------
-- 3. TRANSACTIONS
-- -------------------------------------------------------
-- We build realistic history so balance_after is consistent.
-- Account 1001 (Alice Checking): starts 0 → ends 5250
-- Account 1002 (Alice Savings):  starts 0 → ends 8200
-- Account 1003 (Bob Main):       starts 0 → ends 3150
-- Account 1004 (Bob Euro):       starts 0 → ends 1500
-- Account 1005 (Charlie Primary): starts 0 → ends 1800
-- Account 1006 (Charlie Old):    starts 0 → ends 0

INSERT INTO transactions (id, account_id, type, amount, description, correlation_id, idempotency_key, status, balance_after, created_at) VALUES

  -- === Day -14: Initial deposits ===
  -- Alice deposits 5000 into Checking
  (1001, 1001, 'CREDIT', 5000.0000, 'Initial deposit',         NULL, 'demo-seed-00001', 'COMPLETED', 5000.0000, NOW() - INTERVAL '14 days'),
  -- Alice deposits 8000 into Savings
  (1002, 1002, 'CREDIT', 8000.0000, 'Initial deposit',         NULL, 'demo-seed-00002', 'COMPLETED', 8000.0000, NOW() - INTERVAL '14 days'),
  -- Bob deposits 3000 into Main
  (1003, 1003, 'CREDIT', 3000.0000, 'Initial deposit',         NULL, 'demo-seed-00003', 'COMPLETED', 3000.0000, NOW() - INTERVAL '14 days'),
  -- Charlie deposits 2000 into Primary
  (1004, 1005, 'CREDIT', 2000.0000, 'Initial deposit',         NULL, 'demo-seed-00004', 'COMPLETED', 2000.0000, NOW() - INTERVAL '14 days'),

  -- === Day -13: Alice transfers 500 from Checking to Savings ===
  (1005, 1001, 'DEBIT',  500.0000, 'Transfer to Savings',      'a0000000-0000-0000-0000-000000000001', 'demo-seed-00005', 'COMPLETED', 4500.0000, NOW() - INTERVAL '13 days'),
  (1006, 1002, 'CREDIT', 500.0000, 'Transfer from Checking',   'a0000000-0000-0000-0000-000000000001', 'demo-seed-00006', 'COMPLETED', 8500.0000, NOW() - INTERVAL '13 days'),

  -- === Day -12: Bob pays Alice 200 (Bob Main → Alice Checking) ===
  (1007, 1003, 'DEBIT',  200.0000, 'Payment to Alice',         'a0000000-0000-0000-0000-000000000002', 'demo-seed-00007', 'COMPLETED', 2800.0000, NOW() - INTERVAL '12 days'),
  (1008, 1001, 'CREDIT', 200.0000, 'Payment from Bob',         'a0000000-0000-0000-0000-000000000002', 'demo-seed-00008', 'COMPLETED', 4700.0000, NOW() - INTERVAL '12 days'),

  -- === Day -11: Charlie withdraws 300 ===
  (1009, 1005, 'DEBIT',  300.0000, 'ATM withdrawal',           NULL, 'demo-seed-00009', 'COMPLETED', 1700.0000, NOW() - INTERVAL '11 days'),

  -- === Day -10: Bob deposits 1000 into Euro account ===
  (1010, 1004, 'CREDIT', 1000.0000, 'EUR deposit',             NULL, 'demo-seed-00010', 'COMPLETED', 1000.0000, NOW() - INTERVAL '10 days'),
  -- Bob deposits 500 into Main
  (1011, 1003, 'CREDIT', 500.0000, 'Paycheck deposit',         NULL, 'demo-seed-00011', 'COMPLETED', 3300.0000, NOW() - INTERVAL '10 days'),

  -- === Day -9: Alice pays Charlie 150 (Alice Checking → Charlie Primary) ===
  (1012, 1001, 'DEBIT',  150.0000, 'Payment to Charlie',       'a0000000-0000-0000-0000-000000000003', 'demo-seed-00012', 'COMPLETED', 4550.0000, NOW() - INTERVAL '9 days'),
  (1013, 1005, 'CREDIT', 150.0000, 'Payment from Alice',       'a0000000-0000-0000-0000-000000000003', 'demo-seed-00013', 'COMPLETED', 1850.0000, NOW() - INTERVAL '9 days'),

  -- === Day -8: Alice withdraws 300 from Savings ===
  (1014, 1002, 'DEBIT',  300.0000, 'Emergency withdrawal',     NULL, 'demo-seed-00014', 'COMPLETED', 8200.0000, NOW() - INTERVAL '8 days'),

  -- === Day -7: Bob deposits 600 into Euro ===
  (1015, 1004, 'CREDIT', 600.0000, 'EUR freelance payment',    NULL, 'demo-seed-00015', 'COMPLETED', 1600.0000, NOW() - INTERVAL '7 days'),

  -- === Day -6: Charlie pays Bob 100 ===
  (1016, 1005, 'DEBIT',  100.0000, 'Dinner split to Bob',      'a0000000-0000-0000-0000-000000000004', 'demo-seed-00016', 'COMPLETED', 1750.0000, NOW() - INTERVAL '6 days'),
  (1017, 1003, 'CREDIT', 100.0000, 'Dinner split from Charlie','a0000000-0000-0000-0000-000000000004', 'demo-seed-00017', 'COMPLETED', 3400.0000, NOW() - INTERVAL '6 days'),

  -- === Day -5: Alice deposits 800 into Checking ===
  (1018, 1001, 'CREDIT', 800.0000, 'Freelance payment',        NULL, 'demo-seed-00018', 'COMPLETED', 5350.0000, NOW() - INTERVAL '5 days'),

  -- === Day -4: Bob withdraws 250 from Main, Alice withdraws 100 from Checking ===
  (1019, 1003, 'DEBIT',  250.0000, 'Utility bill',             NULL, 'demo-seed-00019', 'COMPLETED', 3150.0000, NOW() - INTERVAL '4 days'),
  (1020, 1001, 'DEBIT',  100.0000, 'Grocery shopping',         NULL, 'demo-seed-00020', 'COMPLETED', 5250.0000, NOW() - INTERVAL '4 days'),

  -- === Day -3: Bob deposits into Euro, Charlie deposits 50 ===
  (1021, 1004, 'DEBIT',  100.0000, 'EUR withdrawal',           NULL, 'demo-seed-00021', 'COMPLETED', 1500.0000, NOW() - INTERVAL '3 days'),
  (1022, 1005, 'CREDIT',  50.0000, 'Refund',                   NULL, 'demo-seed-00022', 'COMPLETED', 1800.0000, NOW() - INTERVAL '3 days'),

  -- === Day -2: Alice transfers 200 from Savings to Checking (net zero shown below) ===
  -- Wait — that would change final balances. Let's keep it simpler:
  -- Alice Checking is already at 5250, Savings at 8200 — done.

  -- === Day -1: Small transactions for chart variety ===
  (1023, 1001, 'CREDIT', 50.0000,  'Interest credit',          NULL, 'demo-seed-00023', 'COMPLETED', 5300.0000, NOW() - INTERVAL '1 day'),
  (1024, 1001, 'DEBIT',  50.0000,  'Subscription fee',         NULL, 'demo-seed-00024', 'COMPLETED', 5250.0000, NOW() - INTERVAL '1 day'),
  (1025, 1003, 'CREDIT', 75.0000,  'Cashback reward',          NULL, 'demo-seed-00025', 'COMPLETED', 3225.0000, NOW() - INTERVAL '1 day'),
  (1026, 1003, 'DEBIT',  75.0000,  'Coffee subscription',      NULL, 'demo-seed-00026', 'COMPLETED', 3150.0000, NOW() - INTERVAL '1 day');

SELECT setval('transactions_id_seq', GREATEST((SELECT MAX(id) FROM transactions), 1026));

-- -------------------------------------------------------
-- 4. DAILY ACCOUNT SUMMARIES
-- -------------------------------------------------------
-- These power the balance chart on the dashboard.
-- total_credits = sum of CREDIT amounts, total_debits = sum of DEBIT amounts for the day.
-- closing_balance = account balance at end of day.

INSERT INTO daily_account_summaries (account_id, summary_date, total_credits, total_debits, transaction_count, closing_balance) VALUES

  -- Day -14
  (1001, CURRENT_DATE - 14, 5000.0000,    0.0000, 1, 5000.0000),
  (1002, CURRENT_DATE - 14, 8000.0000,    0.0000, 1, 8000.0000),
  (1003, CURRENT_DATE - 14, 3000.0000,    0.0000, 1, 3000.0000),
  (1005, CURRENT_DATE - 14, 2000.0000,    0.0000, 1, 2000.0000),

  -- Day -13: Alice internal transfer
  (1001, CURRENT_DATE - 13,    0.0000,  500.0000, 1, 4500.0000),
  (1002, CURRENT_DATE - 13,  500.0000,    0.0000, 1, 8500.0000),

  -- Day -12: Bob → Alice transfer
  (1003, CURRENT_DATE - 12,    0.0000,  200.0000, 1, 2800.0000),
  (1001, CURRENT_DATE - 12,  200.0000,    0.0000, 1, 4700.0000),

  -- Day -11: Charlie withdrawal
  (1005, CURRENT_DATE - 11,    0.0000,  300.0000, 1, 1700.0000),

  -- Day -10: Bob deposits
  (1004, CURRENT_DATE - 10, 1000.0000,    0.0000, 1, 1000.0000),
  (1003, CURRENT_DATE - 10,  500.0000,    0.0000, 1, 3300.0000),

  -- Day -9: Alice → Charlie
  (1001, CURRENT_DATE -  9,    0.0000,  150.0000, 1, 4550.0000),
  (1005, CURRENT_DATE -  9,  150.0000,    0.0000, 1, 1850.0000),

  -- Day -8: Alice Savings withdrawal
  (1002, CURRENT_DATE -  8,    0.0000,  300.0000, 1, 8200.0000),

  -- Day -7: Bob EUR deposit
  (1004, CURRENT_DATE -  7,  600.0000,    0.0000, 1, 1600.0000),

  -- Day -6: Charlie → Bob
  (1005, CURRENT_DATE -  6,    0.0000,  100.0000, 1, 1750.0000),
  (1003, CURRENT_DATE -  6,  100.0000,    0.0000, 1, 3400.0000),

  -- Day -5: Alice deposit
  (1001, CURRENT_DATE -  5,  800.0000,    0.0000, 1, 5350.0000),

  -- Day -4: Bob + Alice withdrawals
  (1003, CURRENT_DATE -  4,    0.0000,  250.0000, 1, 3150.0000),
  (1001, CURRENT_DATE -  4,    0.0000,  100.0000, 1, 5250.0000),

  -- Day -3: Bob EUR withdrawal + Charlie refund
  (1004, CURRENT_DATE -  3,    0.0000,  100.0000, 1, 1500.0000),
  (1005, CURRENT_DATE -  3,   50.0000,    0.0000, 1, 1800.0000),

  -- Day -1: Small transactions
  (1001, CURRENT_DATE -  1,   50.0000,   50.0000, 2, 5250.0000),
  (1003, CURRENT_DATE -  1,   75.0000,   75.0000, 2, 3150.0000);
