SBT=build/sbt
TEST=sql/test:testOnly
T1=org.apache.spark.sql.execution.joins.SymmetricHashJoinSuite
T2=org.apache.spark.sql.execution.joins.DNSJoinSuite

compile:
	$(SBT) compile

clean:
	$(SBT) clean

all:
	$(SBT) "$(TEST) $(T1) $(T2)"

t1:
	$(SBT) "$(TEST) $(T1)"

t2:
	$(SBT) "$(TEST) $(T2)"

