# Homework 3: Real-world SQL queries and scalable algorithms
#### CS186, UC Berkeley, Spring 2015
#### Points: [10% of your final grade](https://sites.google.com/site/cs186spring2015/home/basic-information)
#### Note: *This homework is to be done individually!*
#### Due: Friday, February 27, 2015, 11:59 PM

###Description

In this homework, we will exercise your newly acquired SQL skills.   You will be writing queries against Postgres using public data.  In the first part, we'll do some warm-up queries and, in the second part, we'll implement a non-trivial graph algorithm in SQL.

###Tools
For this assignment, you are limited to using Postgres and Python.  To follow these instructions **use your CS186 Vagrant VM**. If you do not use the VM, your tests may not execute correctly.

To obtain the homework files, you can run `git pull` to pull with your course remote.

##Part I: SQL Warmup

To start, we're going to learn how to use Postgres and practice our SQL skills on some basic queries.

###About the schema, Part I

Peruse the schema for Part I, which is provided in `part1/schema.sql`.  You will see it is a bit more complicated than the examples in the book!

These tables come from the Federal Election Commission (FEC) and contain finance data for committees, candidates and campaigns for US Federal Elections. As a little background, the [FEC](http://www.fec.gov/ans/answers_general.shtml#What_does_the_FEC_do) was established to limit the source and amounts of the contributions used to finance federal elections and enforce public disclosure of campaign finance information. A candidate is someone who is seeking to be elected for a political position. A committee is type of organization which to support candidates or other political committees, often through monetary donations.

We **strongly** encourage you to consult the following page to better understand the dataset: http://www.fec.gov/finance/disclosure/ftpdet.shtml. For each table we've given you the FEC provides a "Data Dictionary" which explains what each of the fields contains. You'll likely need to consult both of these resources throughout this part of the homework.

#### Some clarifications on the dataset
The way this dataset organizes who a contribution came from and where it is going can be a little confusing. We'll use the following interpretation:

In the `individual_contributions` table:
 * `cmte_id`: who the donation goes to
 * `other_id`: mostly null for this table. only used if the donation came from another committee
 * `name`: name of the individual donating

In the `intercommittee_transactions` table:
 * `cmte_id`: who the donation goes to
 * `other_id`: who the donation came from

In the `committee_contributions` table:
 * `cand_id`: if the donation is to a candidate, the candidate's id
 * `cmte_id`: if `cand_id` is not null: the "filing committee" (so who the donation came from). if `cand_id` is null: who the donation goes to
 * `other_id`: if `cand_id` is null: who the donation came from

###Setting up Postgres

Log in to your vagrant VM. First, we need to create a Postgres user for your vagrant account. You should only need to do this once.

	$ sudo su postgres
	$ createuser -s vagrant
	$ exit

###Using Postgres

You can now create a database, and start up the command-line interface psql to send SQL commands to that database:

	$ createdb test
	$ psql test

Use the `\d` command to describe your current relations. Use `CREATE TABLE` to create new relations. You can also enter `INSERT`, `UPDATE`, `DELETE`, and `SELECT` commands at the `psql` prompt. Remember that each command must be terminated with a semicolon (`;`).

Type `help` at the psql prompt to get more help options on `psql` commands or SQL statements.

When you're done, use `\q` to exit `psql`.

If you messed up creating your database, you can issue the `dropdb` command to delete it.

    $ createdb tst  # oops!
    $ dropdb tst   # drops the db named 'tst'

###Getting started in Part I

Follow the steps above to set up Postgres and test out that it works.

At this point you can load up the sample data:

	$ cd part1
	$ bash import-part1.sh

This will take a little while as it downloads, extracts, and imports into your database. When you are done you have a database called `fec`.  You can connect to it with `psql` and verify that the schema was loaded with the `\dt` command:

	$ psql fec
	fec=# \dt

Try running a few sample commands in the `psql` console and see what they do:

    fec=# \d candidates
    fec=# SELECT name, election_yr FROM candidates ORDER BY name;
    fec=# SELECT COUNT(*) FROM committee_contributions;

###Write these queries

We've provided `part1/part1.sql` to help you get started. In the file, you'll find a `CREATE VIEW` statement for each question below, specifying a particular view name (like q2) and list of column names (like `id`, `name`). The view name and column names constitute the interface against which we will grade this assignment. In order words, don't change or remove these names. Your job is to fill out the view definitions in a way that populates the views with the right tuples.

For example, consider a hypothetical Question 0: "What is the minimum number of widgets at any factory?".

In the warmup file we provide:

	CREATE VIEW q0(min) AS
	;
	
You would edit this with your answer, keeping the schema the same:

	-- solution you provide
	CREATE VIEW q0(min) AS
	 SELECT MIN(widget)
	 FROM factory
	;
	
Create views in the following queries:

1. 
  a. In the `committee_contributions` table, find all the entries with a value for
  the `transaction_amt` attribute that is greater than $5,000, and return the
  `cmte_id` and transaction amount.

  b. Following from the previous part, now also include the contributor name in your output.

  c. Following from the previous part, group together transactions with the same committee id and contributor and report the average transaction amount.

  d. Following from the previous part, only include groups with an average transaction amount > $10,000.

2. Find the names of the top 10 (directed) committee pairs that are affiliated with the Democratic Party, who have the highest number of intercommittee transactions. By directed, we mean (C1 donates to C2) is not the same (C2 donates to C1).

3. Find the names of all committees which have *not* made a contribution to Barack Obama.

4. Find the names of candidates have received contributions from more than **1%** of all committees.

5. **Updated** For each committee, list the total amount of dollars in individual contributions from individuals that are of type **organization**.  If they got no such donations, the total should be listed as null.

6. Find the ids of candidates who have received committee contributions from both PAC and CCM entities.

7. Find the names of distinct candidate pairs that share a common committee contributor from the state of Rhode Island (RI). If you list a pair ("Washington", "Lincoln") you should also list ("Lincoln, Washington").
      
##Help?

You can run your file directly using:

	$ psql fec < part1.sql

This can help you catch any syntax errors in your SQL.

To assist in your assignment, we've provided output from each of the views you need to define for the data set you've been given.  Your views should match ours, but note that your SQL queries should work on ANY data set. Indeed, we will be testing your queries on a (set of) different database(s), so it is *NOT* sufficient to simply return these results in all cases!

To run the test, from within the `part1` directory:

	$ bash test.sh
	
Become familiar with the UNIX [diff](http://en.wikipedia.org/wiki/Diff) command, if you're not already, because our tests saves the `diff` for any query executions that don't match in `diffs/`.  If you care to look at the query outputs directly, ours are located in the `expected_output` directory. Your view output should be located in your solution's `your_output` directory once you run the tests.

**Note:** It doesn't matter how you sort your results; we will reorder them before comparing. Note, however, that our test query output is sorted, so if you're trying to compare yours and ours manually line-by-line, make sure you use the proper ORDER BY clause. The ORDER BY clause to use can be determined by looking in `test.sh`. 
	
#Part II: Maximum flow problem

Now that we're all warmed up with our SQL skills, we're going to put them to use in an interesting algorithmic analysis.  Graph processing is a data-centric problem that's of increasing interest these days, due to applications in web ranking, social networking, recommender systems, and so on.

First of all, let's briefly discuss why we would want to write an algorithm in SQL instead of an application language such as Python. If the database server and application server are different, then it may be expensive to transfer large amounts of data from the database server for processing. SQL lets us do the processing directly on the DB server. Furthermore, as we will see, SQL queries process multiple data at once and can often be parallelized by the DBMS system automatically, rather than the programmer explicitly writing parallel code. 

In this homework we'll focus on the max flow/min cut problem, which you can [read about on Wikipedia](http://en.wikipedia.org/wiki/Maximum_flow_problem).

In the maximum flow problem, we are given a [flow network](http://en.wikipedia.org/wiki/Flow_network) graph with a single source node, `s` and a single sink node `t`. Each edge in the graph has a capacity, which defines the maximum amount of flow that can pass through the edge. Our goal is to try and maximize the amount of flow that is transmitted from the source to the sink.

Maximum flow has some [interesting applications](http://en.wikipedia.org/wiki/Maximum_flow_problem#Real_world_applications), some of which can involve substantial amounts of data (e.g. airline scheduling). However,
in this assignment, we are going just work with generic graphs of `nodes` and `edges`. We'll implement a general purpose algorithm in SQL which 
calculates a maximum flow in the graph.

[Ford-Fulkerson](http://en.wikipedia.org/wiki/Ford%E2%80%93Fulkerson_algorithm) is a relatively intuitive algorithm to compute the maximum flow in a graph. While there is still a path in the graph from `s` to `t` with positive capacity, we increase the amount of flow routed along this path. (The capacity of a path is defined as the minimum capacity of all the edges in this path.)

The only tricky portion of the algorithm is the notion of *residual paths*. Since at each iteration of the algorithm, we are choosing an arbitrary path to route flow along, it is possible we choose a path that will not lead to the maximum flow. Thus, we add equivalent "reverse capacity" to each residual edge in the path that we route flow along--this enables the algorithm to "redirect" flow later on to more optimal paths. Concretely, if we route 2 units of flow from `s->a` then we add two units of capacity to the edge `a->s`, which may or may not appear in the original graph. 

To choose a path to route flow from `s` to `t`, many algorithms can be used including breadth-first search or depth-first search. If you use breadth-first search (as we will be doing in this assignment), the algorithm is called the [Edmonds–Karp algorithm](http://en.wikipedia.org/wiki/Edmonds%E2%80%93Karp_algorithm). It's worth mentioning that Professor Karp is an EECS faculty member here at Berkeley, and a Turing Award winner!

You may find illustrations of [Ford-Fulkerson](http://en.wikipedia.org/wiki/Ford–Fulkerson_algorithm#Integral_example) and [Edmonds-Karp](http://en.wikipedia.org/wiki/Edmonds–Karp_algorithm#Example) useful for understanding these algorithms. (In fact, two of the tests we give you come from these examples.)

##What we've done

We've written a mostly completed implementation of max flow in Python and SQL. Our max flow calculation
requires two steps:

* Breadth first search (BFS): to find paths from `s` to `t` in the graph
* Updating capacity/flows: updating edge capacities to reflect the flow routing across them

We've already parsed the raw node and edge csv files for you, and we've
defined the intermediate tables, views, and program flow you'll need
to complete both of these steps.

Take a first read through `maxflow.py` to understand the control flow.

If you look at pseudocode for a max flow algorithm
like [Edmonds–Karp's
algorithm](http://en.wikipedia.org/wiki/Edmonds%E2%80%93Karp_algorithm#Pseudocode),
you'll see that it's written in an imperative,
[von-Neumann](http://en.wikipedia.org/wiki/Von_Neumann_architecture)
style, processing a single vertex at a time.  The computation proceeds step by step, 
sequentially iterating over a loop
with many individual loads and stores to different memory locations.

By contrast, your SQL implementation of BFS and flow updates will use a 
[declarative](http://en.wikipedia.org/wiki/Declarative_programming), "batch" algorithm designed for efficient execution within a database.  This algorithm takes a [dynamic programming](http://en.wikipedia.org/wiki/Dynamic_programming) approach to the problem:
it iteratively finds paths of increasing length (in terms of hop count, or number of links in the path).

Note that our graphs are directed. Furthermore, note that the capacity of a path is not necessarily the same as the hop count!

The pseudocode for our algorithm, annotated by (relationname) in our code, is as follows:

	(edges) = edges in our graph, including residual graph

	repeat:
		(paths) = set of paths we've seen so far
		(terminated_paths) = paths that start at s and end at t and have positive capacity left

		// to start, we need to check all immediate edges
		insert all edges with non-zero capacity starting at s into (paths)

		while (terminated_paths) is empty and all (paths) not explored:
			join (paths) with (edges) having capacity to extend all paths by one hop, without creating cycles
			add all (paths) which connect s to t to (terminated_paths)

		if (terminated_paths) is empty:
			quit

		(chosen_path) = a path chosen from (terminated_paths)

		compute the (chosen_path)'s capacity := min of all its edges
		compute the updated capacity in the forward graph and residual graph
		update the (edges) table

##Your task

Our implementation is almost done but we need your help to fill in
some of the holes!  We've specifically left holes (marked by `???`) in
our Python implementation, `maxflow.py` for you to fill in.  You may
find alternate ways to implement these queries, and we encourage you
to experiment. However, you shouldn't need to do so, and, if you do
change the query structure, *make sure that your final output matches
ours*. Furthermore, don't add any additional Python code -- this
assignment is meant to be completed primarily in SQL and we use Python
only for control flow, such as loops.

Do *not* change the code within the `DO NOT EDIT` delimiters (see the *Help* section below). Unless you are 100% sure that your changes will correctly compute maxflow, do not change the control flow of the program; simply fill in the blanks as necessary.

In summary, you need to fill in the queries to:

1. For BFS: join all current paths with the edges table to extend each path. Ignore any edges with 0 capacity. Do not include paths with loops.

2. For max flow: compute the "constraining capacity" which is the minimum capacity of an edge along the chosen path

3. For max flow: compute the updated capacity for edges in the forward and residual graph

3. For max flow: update the edges table with the newly computed capacity

## Getting started

You'll need to run these commands before beginning part 2. You should only have to do this once.

	$ createdb part2
	$ sudo apt-get install python-psycopg2

`psycopg2` is the Python module we'll use to drive Postgres.

To run your program:

	$ python maxflow.py TEST
   
Where `TEST` is an edge list csv file found in `part2/tests/`, such as `test1.csv`.
 
You can then examine the results, which will be stored in the `flow` table.

Keep reading below to get some suggestions on how to get started and debug.

### Postgres Arrays
As you will see in `maxflow.py`, we use the arrays feature of Postgres to build up paths in our graph as well as keep track of the nodes traversed. You can find the full documentation for arrays at http://www.postgresql.org/docs/9.3/static/arrays.html, but you'll probably only need the array concatenation and array contains operations described [here](http://www.postgresql.org/docs/9.1/static/functions-array.html). The code later uses `unnest` to unravel the array into rows.

##Help?

There are many intermediate steps to this computation, so we've provided a harness that lets you terminate your program earlier depending on the number of BFS iterations or flow iterations that have passed. Use the following command to see all options to the max flow script:

	$ python maxflow.py --help
	

Keep in mind that you shouldn't edit the code blocks responsible for this (denoted `DO NOT EDIT`) or the main control flow.  We will be using this functionality for *grading*, so, if this functionality breaks, you will receive *ZERO* credit for this portion of the assignment.

We've written a few sanity tests in `test.py` using Python's `unittest` framework. They should be easy to understand. You can run these using:

	$ python test.py

and we strongly encourage you to add your own additional tests, as we will be evaluating your code on more tests including larger graphs and graphs with loops. However, you may always assume the input to your maxflow program is a valid flow network graph.

One additional debugging technique you may find useful is to use a Python debugger such as [`pdb`](https://docs.python.org/2/library/pdb.html) or the one included with [PyCharm](https://www.jetbrains.com/pycharm/) to insert breakpoints in the code. You can keep an interactive `psql` prompt open and poke around at the contents of the tables while they are being populated and updated.

##Submitting

To trigger the autograder, push to the `ag/hw3` branch on GitHub.

To submit, push to the `release/hw3` branch on GitHub. We will only grade what is in this release branch.
