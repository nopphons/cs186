DROP VIEW IF EXISTS q1a, q1b, q1c, q1d, q2, q3, q4, q5, q6, q7;

-- Question 1a
CREATE VIEW q1a(id, amount)
AS
  SELECT C.cmte_id, C.transaction_amt 
  FROM committee_contributions C
  WHERE C.transaction_amt > 5000;
;

-- Question 1b
CREATE VIEW q1b(id, name, amount)
AS
  SELECT C.cmte_id, C.name, C.transaction_amt 
  FROM committee_contributions C
  WHERE C.transaction_amt > 5000
  ORDER BY C.cmte_id;
;

-- Question 1c
CREATE VIEW q1c(id, name, avg_amount)
AS
  SELECT cmte_id, name, AVG(transaction_amt) 
  FROM committee_contributions 
  WHERE transaction_amt > 5000 
  GROUP BY cmte_id, name;
;

-- Question 1d
CREATE VIEW q1d(id, name, avg_amount)
AS
  SELECT cmte_id, name, avg 
  FROM (SELECT cmte_id, name, AVG(transaction_amt) as avg FROM committee_contributions WHERE transaction_amt > 5000 GROUP BY cmte_id, name) 
  AS average WHERE avg > 10000; --HAVING
;

-- Question 2
CREATE VIEW q2(from_name, to_name)
AS
  WITH trans(name1, name2) AS (
    SELECT L1.name, L2.name
    FROM intercommittee_transactions C 
    INNER JOIN committees L1 on C.other_id = L1.id
    INNER JOIN committees L2 on C.cmte_id = L2.id
    WHERE L1.pty_affiliation = 'DEM' AND L2.pty_affiliation = 'DEM'
    )
  SELECT T.name1, T.name2
  FROM trans T
  GROUP BY T.name1, T.name2
  ORDER BY COUNT(*) DESC
  LIMIT 10;
;

-- Question 3
CREATE VIEW q3(name)
AS
  WITH combine(id) AS (
    SELECT C1.id
    FROM committees C1
    EXCEPT
    SELECT DISTINCT C.cmte_id
    FROM committee_contributions C INNER JOIN candidates S ON C.cand_id = S.id
    WHERE S.name = 'OBAMA, BARACK'
  )
  SELECT C2.name
  FROM combine L, committees C2
  WHERE C2.id = L.id
;

-- Question 4.
CREATE VIEW q4 (name)
AS
  WITH combine(id, count) AS (
    SELECT S.id, COUNT(DISTINCT C.cmte_id)
    FROM candidates S INNER JOIN committee_contributions C ON S.id = C.cand_id
    GROUP BY S.id
  )
  SELECT S.name
  FROM combine A, candidates S
  WHERE A.count > (SELECT COUNT(DISTINCT id)*0.01 FROM committees) AND A.id = S.id
  ORDER BY S.name;
;

-- Question 5
CREATE VIEW q5 (name, total_pac_donations) AS
  WITH combine(id, count) AS (
    SELECT C.id, SUM(I.transaction_amt)
    FROM committees C LEFT OUTER JOIN individual_contributions I  ON C.id = I.cmte_id AND I.entity_tp = 'ORG'
    GROUP BY C.id
  )
  SELECT S.name, A.count
  FROM combine A, committees S
  WHERE S.id = A.id
  ORDER BY S.name;
;

-- Question 6
CREATE VIEW q6 (id) AS
  SELECT C.cand_id
  FROM committee_contributions C
  WHERE C.entity_tp = 'CCM' AND C.cand_id IS NOT NULL --AND C.cand_id IN (SELECT I.id FROM candidates I)
  INTERSECT
  SELECT C.cand_id
  FROM committee_contributions C
  WHERE C.entity_tp = 'PAC' AND C.cand_id IS NOT NULL; --AND C.cand_id IN (SELECT I.id FROM candidates I);
;

-- Question 7
CREATE VIEW q7 (cand_name1, cand_name2) AS
  WITH combine(name, id) AS (
      SELECT CC.name, C.cmte_id
      FROM committee_contributions C INNER JOIN candidates CC ON C.cand_id = CC.id 
      WHERE C.state = 'RI'
    )
  SELECT DISTINCT A1.name, A2.name
  FROM combine A1, combine A2
  WHERE A1.name != A2.name AND A1.id = A2.id;
;




