insert into customer (customer_id, full_name, customer_type, mobile_number, email, bank_code, bank_name, saved_beneficiary, status, created_at)
values
('CUST001', 'Demo User', 'CUSTOMER', '0771234567', 'demo@example.local', '6995', 'Dialog Finance PLC', false, 'ACTIVE', current_timestamp),
('BEN001', 'Varuni', 'BENEFICIARY', '0770000001', 'varuni@example.local', '6995', 'Dialog Finance PLC', true, 'ACTIVE', current_timestamp),
('BEN002', 'Dormant Beneficiary', 'BENEFICIARY', '0770000002', 'dormant@example.local', '6995', 'Dialog Finance PLC', true, 'DORMANT', current_timestamp),
('BEN003', 'Frozen Beneficiary', 'BENEFICIARY', '0770000003', 'frozen@example.local', '6995', 'Dialog Finance PLC', true, 'FROZEN', current_timestamp),
('BEN004', 'Closed Beneficiary', 'BENEFICIARY', '0770000004', 'closed@example.local', '6995', 'Dialog Finance PLC', true, 'CLOSED', current_timestamp),
('BEN005', 'Restricted Beneficiary', 'BENEFICIARY', '0770000005', 'restricted@example.local', '6995', 'Dialog Finance PLC', true, 'RESTRICTED', current_timestamp),
('BEN006', 'Unsaved Person', 'BENEFICIARY', '0770000006', 'unsaved@example.local', '6995', 'Dialog Finance PLC', false, 'ACTIVE', current_timestamp);

insert into savings_account (account_number, customer_id, account_name, account_alias, currency, available_balance, status, branch_code, version, created_at)
values
('001020020001', 'CUST001', 'Demo Salary Account', 'salary account', 'LKR', 1000.00, 'ACTIVE', '001', 0, current_timestamp),
('001020020002', 'CUST001', 'Demo Main Account', 'main account', 'LKR', 25.00, 'ACTIVE', '001', 0, current_timestamp),
('001020020003', 'CUST001', 'Demo Dormant Account', 'dormant account', 'LKR', 1000.00, 'DORMANT', '001', 0, current_timestamp),
('001020020004', 'CUST001', 'Demo Frozen Account', 'frozen account', 'LKR', 1000.00, 'FROZEN', '001', 0, current_timestamp),
('001020020005', 'CUST001', 'Demo Closed Account', 'closed account', 'LKR', 1000.00, 'CLOSED', '001', 0, current_timestamp),
('001020020006', 'CUST001', 'Demo Restricted Account', 'restricted account', 'LKR', 1000.00, 'RESTRICTED', '001', 0, current_timestamp),
('001020020974', 'BEN001', 'Varuni Savings Account', 'Varuni', 'LKR', 500.00, 'ACTIVE', '002', 0, current_timestamp),
('001020020975', 'BEN002', 'Dormant Beneficiary Account', 'Dormant Beneficiary', 'LKR', 500.00, 'DORMANT', '002', 0, current_timestamp),
('001020020976', 'BEN003', 'Frozen Beneficiary Account', 'Frozen Beneficiary', 'LKR', 500.00, 'FROZEN', '002', 0, current_timestamp),
('001020020977', 'BEN004', 'Closed Beneficiary Account', 'Closed Beneficiary', 'LKR', 500.00, 'CLOSED', '002', 0, current_timestamp),
('001020020978', 'BEN005', 'Restricted Beneficiary Account', 'Restricted Beneficiary', 'LKR', 500.00, 'RESTRICTED', '002', 0, current_timestamp),
('001020020979', 'BEN006', 'Unsaved Person Account', 'Unsaved Person', 'LKR', 500.00, 'ACTIVE', '002', 0, current_timestamp);
