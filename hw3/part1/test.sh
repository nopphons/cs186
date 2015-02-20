#!/bin/bash

rm -rf your_output 2> /dev/null
mkdir your_output 2>/dev/null

rm -rf diffs 2> /dev/null
mkdir diffs 2> /dev/null

psql -q fec < part1.sql

pass=true

function test_query() {
	query=$1
	test_name=$2

	# First test if the view exists
	if ! (psql -c "$query" fec &> your_output/$test_name.txt ) ; then
		pass=false
		echo -e "ERROR $test_name! See your_output/$test_name.txt"
	else
	    diff your_output/$test_name.txt expected_output/$test_name.txt > diffs/$test_name.txt
	    if [ $? -ne 0 ]
	    then
		pass=false
		echo -e "ERROR $test_name output differed! See diffs/$test_name.txt"
	    else
		echo -e "PASS $test_name"
	    fi
	fi
}

test_query "SELECT * FROM q1a ORDER BY id, amount;" q1a
test_query "SELECT * FROM q1b ORDER BY id, name, amount;" q1b
test_query "SELECT * FROM q1c ORDER BY id, name, avg_amount;" q1c
test_query "SELECT * FROM q1d ORDER BY id, name, avg_amount;" q1d
test_query "SELECT * FROM q2 ORDER BY from_name, to_name;" q2
test_query "SELECT * FROM q3 ORDER BY name;" q3
test_query "SELECT * FROM q4 ORDER BY name;" q4
test_query "SELECT * FROM q5 ORDER BY name, total_pac_donations;" q5
test_query "SELECT * FROM q6 ORDER BY id;" q6
test_query "SELECT * FROM q7 ORDER BY cand_name1, cand_name2;" q7

if $pass; then
echo -e "SUCCESS: Your queries worked on this dataset!"
fi
