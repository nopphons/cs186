SBT=build/sbt
TEST=sql/test:testOnly
T1=org.apache.spark.sql.execution.DiskPartitionSuite
T2=org.apache.spark.sql.execution.DiskHashedRelationSuite
T3=org.apache.spark.sql.execution.CS186UtilsSuite
T4=org.apache.spark.sql.execution.ProjectSuite

compile:
	$(SBT) compile

clean:
	$(SBT) clean

all:
	$(SBT) "$(TEST) $(T1) $(T2) $(T3) $(T4)"

t1:
	$(SBT) "$(TEST) $(T1)"

t2:
	$(SBT) "$(TEST) $(T2)"

t3:
	$(SBT) "$(TEST) $(T3)"

t4:
	$(SBT) "$(TEST) $(T4)"

