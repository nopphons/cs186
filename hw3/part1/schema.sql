CREATE TABLE committees (
  "id" varchar(9) PRIMARY KEY,
  "name" varchar(200),
  "tres_name" varchar(90),
  "street1" varchar(34),
  "street2" varchar(34),
  "city" varchar(30),
  "state" varchar(2),
  "zip" varchar(9),
  "designation" varchar(1),
  "type" varchar(1),
  "pty_affiliation" varchar(3),
  "filing_freq" varchar(1),
  "org_tp" varchar(1),
  "connected_org_nm" varchar(200),
  "cand_id" varchar(9)
);

CREATE TABLE candidates (
  "id" varchar(9) PRIMARY KEY,
  "name" varchar(200),
  "pty_affiliation" varchar(3),
  "election_yr" int,
  "office_st" varchar(2),
  "office" varchar(1),
  "office_district" varchar(2),
  "ici" varchar(1),
  "status" varchar(1),
  "pcc" varchar(9),
  "street1" varchar(34),
  "street2" varchar(34),
  "city" varchar(30),
  "st" varchar(2),
  "zip" varchar(9)
);

CREATE TABLE linkages (
  "cand_id" varchar(9),
  "cand_election_yr" int,
  "fec_election_yr" int,
  "cmte_id" varchar(9),
  "cmte_tp" varchar(1),
  "cmte_dsgn" varchar(1),
  "linkage_id" numeric(12)
);

CREATE TABLE intercommittee_transactions (
  "cmte_id" varchar(9),
  "amndt_ind" varchar(1),
  "rpt_tp" varchar(3),
  "transaction_pgi" varchar(5),
  "image_num" varchar(11),
  "transaction_tp" varchar(3),
  "entity_tp" varchar(3),
  "name" varchar(200),
  "city" varchar(30),
  "state" varchar(2),
  "zip_code" varchar(9),
  "employer" varchar(38),
  "occupation" varchar(38),
  "transaction_dt" date,
  "transaction_amt" numeric(14,2),
  "other_id" varchar(9),
  "tran_id" varchar(32),
  "file_num" numeric(22),
  "memo_cd" varchar(1),
  "memo_text" varchar(100),
  "sub_id" numeric(19)
);


CREATE TABLE committee_contributions (
  "cmte_id" varchar(9),
  "amndt_ind" varchar(1),
  "rpt_tp" varchar(3),
  "transaction_pgi" varchar(5),
  "image_num" varchar(11),
  "transaction_tp" varchar(3),
  "entity_tp" varchar(3),
  "name" varchar(200),
  "city" varchar(30),
  "state" varchar(2),
  "zip_code" varchar(9),
  "employer" varchar(38),
  "occupation" varchar(38),
  "transaction_dt" date,
  "transaction_amt" numeric(14,2),
  "other_id" varchar(9),
  "cand_id" varchar(9),
  "tran_id" varchar(32),
  "file_num" numeric(22),
  "memo_cd" varchar(1),
  "memo_text" varchar(100),
  "sub_id" numeric(19)
);

CREATE TABLE individual_contributions (
  "cmte_id" varchar(9),
  "amndt_ind" varchar(1),
  "rpt_tp" varchar(3),
  "transaction_pgi" varchar(5),
  "image_num" varchar(11),
  "transaction_tp" varchar(3),
  "entity_tp" varchar(3),
  "name" varchar(200),
  "city" varchar(30),
  "state" varchar(2),
  "zip_code" varchar(9),
  "employer" varchar(38),
  "occupation" varchar(38),
  "transaction_dt" date,
  "transaction_amt" numeric(14,2),
  "other_id" varchar(9),
  "tran_id" varchar(32),
  "file_num" numeric(22),
  "memo_cd" varchar(1),
  "memo_text" varchar(100),
  "sub_id" numeric(19)
);
