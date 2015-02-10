#!/bin/bash

URL="http://inst.eecs.berkeley.edu/~cs186/sp15/hw3_fec_data.zip"
wget -nc $URL
unzip -o hw3_fec_data.zip

dropdb --if-exists fec
createdb fec
psql -d fec < schema.sql

BASE="$PWD/data"
TABLES=(candidates committee_contributions committees individual_contributions intercommittee_transactions linkages)

for table in ${TABLES[@]}
do
    psql -e -d fec -c "COPY ${table} FROM '$BASE/${table}.txt' DELIMITER '|' CSV;"
done

psql -e -d fec -c "ANALYZE"
