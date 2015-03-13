# Homework 4: Joins in Spark

In this assignment you'll implement two different types of joins in [Apache Spark](http://spark.apache.org) -- one fairly traditional, the other a bit unusual. This will require you to employ key concepts of the join algorithms that we have covered in the course in order to implement more novel joins. You will also get more practice with Scala to further cement your knowledge of the language.

This assignment is due **Friday, March 20th at 11:59 PM** and is worth **10% of your final grade**. This project is an excellent opportunity to collaborate, and we'll require that you complete it with your partners from HW2.

As before, there is a lot of code in this directory. Please look [here](https://github.com/cs186-spring15/course/tree/master/hw4#project-framework) to find the directory where the code is located.


## Assignment tasks

1. [Implement Symmetric Hash Join](https://github.com/cs186-spring15/course/tree/master/hw4#symmetric-hash-join)
2. [Implement DNS Join](https://github.com/cs186-spring15/course/tree/master/hw4#dns-join)

# Project Framework

For this project, you will be working in the `sql/core/src/main/scala/org/apache/spark/sql/execution/joins/dns/` directory. All of the code you will be touching will be in two files -- `SymmetricHashJoin.scala` and `DNSJoin.scala`. You might however need to consult other files within Spark or the general Scala APIs in order to complete the assignment thoroughly.

In general, we have defined most (if not all) of the methods that you will need. It's quite structured: you just need to find the `// IMPLEMENT ME` comments and fill in the code. Stringing together the right components in a memory-efficient way (i.e., not reading the whole relation into memory at once) will require some thought and careful planning.

# Setup

To follow these instructions use your **CS186 Vagrant VM**. If you do not use the VM, your tests may not execute correctly.

To obtain the homework files, you can run `git pull` to pull with your course remote.

## Building Spark

Once you have the pulled the code, `cd` into `{repo root}/hw4` and run `make compile`. The first time you run this command, it should take a while -- `sbt` will download all the dependencies and compile all the code in Spark (there's quite a bit of code). Once the initial assembly commands finish, you can start your project! (Future builds should not take this long -- `sbt` is smart enough to only recompile the changed files, unless you run `make clean`, which will remove all compiled class files.)

# Your Task

If you find the method definitions provided to be counter-intuitive or constraining, feel free to change them or add to them.
However, note that if you do make significant changes:

1. We will have a harder time helping you debug your code.
2. Iterators must implement next and hasNext. If you do not implement those two methods, your code will not compile.


## Symmetric Hash Join

[Symmetric (or "pipelining") hash join](https://cs.uwaterloo.ca/~david/cs448/symmetric-hash-join.pdf) 
is a join algorithm that was designed for one of the original
parallel query engines, and makes effective use of streaming (non-blocking) behaviors.
In this algorithm, you construct a hash table for _both_ sides of the join, not just
one side as in regular hash join.  Having hashtables on both sides means that tuples can arrive
from either input in any order, and they can be handled immediately (non-blocking!) to produce matches and be hashed for later lookups from the other input.

We will be implementing a version of symmetric hash join that works as follows:
We'll begin by considering the "left" table as the inner table of the join, and the "right" table as the "outer".
We begin by streaming tuples in from the inner relation. For every tuple we stream in
from the inner relation, we insert it into the inner hash table. We then check if
there are any matching tuples in the hashtable for the other relation -- if there are, then we join this
tuple with the corresponding matches. Otherwise, we switch the inner
and outer relations -- that is, the old inner becomes the new outer and the old outer
becomes the new inner, and we proceed to repeat this algorithm, streaming from the
new inner.

We have provided you skeleton code for `SymmetricHashJoin.scala`. This file has `trait SymmetricHashJoin` which defines the SymmetricHashJoin interface. 

If you are interested, you can look at `GeneralSymmetricHashJoin.scala`, which is how we implement the `SymmetricHashJoin` trait, but you do not need to understand this!

### Task #1: Implementing `SymmetricHashJoin`

You will need to implement the `next`, `hasNext`, `switchRelations`, and `findNextMatch` methods in the iterator in `SymmetricHashJoin#symmetricHashJoin`for this task. 

**NOTE**: You should return JoinedRows, which take two input rows and returns the concatenation of them. e.g., `new JoinedRow(row1, row2)`.

At this point, you should be passing the tests in `SymmetricHashJoinSuite.scala`.

## "DNS Join"

The trick in this case is to treat asynch REST calls -- which are often handled via operating systems constructs like threads -- and instead handle them using join logic.  (If you follow this idea to its conclusion, you'll realize that joins can be used as the basic construct of asynchronous programming, and there's no need to think about "threads" or even "events" - just data.  See [Bloom](http://bloom-lang.org) for a full realization of this concept.)

Given a batch of REST calls, a naive method of handling these calls is to execute the requests *synchronously*: that is, when you make each request you wait for a response before issuing a new request.  However, waiting after each call is inefficient -- our query processor may lay idle for a long time while the REST call is being serviced. To get more parallelism, you could fire up a thread per request, but these threads would have a lot of overhead associated with them, and programmers tend to get bugs writing parallel threaded code.

Instead, we will implement this logic using a single-threaded join algorithm, an idea that was described in [this research paper](http://infolab.stanford.edu/~royg/wsqdsq.pdf).  The algorithm is similar to symmetric hash join.
However, instead of being provided with two input relations, we are instead going to
be using a single input relation, and doing lookups on the other data remotely -- in this case by
*asynchronous* HTTP requests.  The join algorithm naturally captures the state that would exist in the the threads of a traditional multi-threaded approach to this problem.

The dataset that we are going to focus on is reverse DNS latitude/longitude lookups, hence we will call
this "DNS Join".  That is, given an IP address, we are going to try to obtain the geographical
location of that IP address. For this end, we are going to use a service called
[telize.com](telize.com), the owner of which has graciously allowed us to bang on his system.
(Yay telize.com!)

For that end, we have provided a simple library that asynchronously makes
requests to [telize.com](telize.com) and handles the receipt of responses for you. You should read the
documentation and method signatures in `DNSLookup.scala` closely before jumping into
implementing this.

The implementation is going to look a lot like symmetric hash join.  We will have two hashtables.  There will be one hashtable containing tuples from the input, which will serve as a *request buffer*: it will remember
the tuples for which there are outstanding DNSlookup requests that have not completed.  There will also be a hashtable over responses from the DNSlookup service, which will serve as a local *response cache* for the DNS services: 
it will remember request/response pairs that were made on earlier input tuples, to save doing lookups on duplicate values 
(shades of HW2!)  

The algorithm proceeds as follows.  On the ``initialize`` call, we will read a fixed number of tuples from the input.  
For each tuple we read in, we will (a) insert it into the request buffer (hashed by IP address), and (b) pass it to DNSlookup, which will return quickly (but without a response yet!)  This serves to "prime the pump": the system has 
started working on DNS lookups for us.

On each call to ``next``, we need to produce a pair of an input tuple and a response from the DNS service.  We do this as follows:

1. First we check to see if there are any output tuples that were joined up already in a previous call.  If so, we return the first of those.
2. Otherwise, we check to see if there are any responses ready from previous DNSlookups.  If so, we'll handle all of them now.  For each response tuple, we:
    1. look in the request buffer hashtable for matching request tuples, join up the results, and prepare the results to be return values from  ``next``
    2. *delete* the request tuples we found from the request buffer hashtable.  (This is different from symmetric hash join -- convince yourself that this works!)
    3. for each request tuple that we delete, we fetch a tuple from the input -- this keeps the request buffer size steady. (If we have handled all the input tuples, simply skip this step, and continue handling responses).  For each of the request tuples we:
        1. First look in the response cache hashtable to see if there's a match.  If there is, pair up the input tuple with the cached result and prepare it to be a return value from `next`.  Fetch another tuple from the input since this one did not make it into the request buffer.
        2. If there is no match in the response cache, then insert this tuple into the request buffer hashtable, and pass it to DNSlookup, which will return quickly (but without a response again).

In principle, it's safe to delete ("evict") tuples from the response cache hashtable -- this affects performance but not correctness.  (Again, different from symmetric hash join -- convince yourself it works!)  However for this assignment, do *not* delete tuples from the response cache -- we're keeping all the responses, on the assumption that the cost of an unnecessary duplicate DNS lookup isn't worth the benefit in memory savings.  (Note that we *do* require you to delete from the request buffer hashtable appropriately, as described above.)


We have provided you skeleton code for `DNSJoin.scala`. This file has `trait DNSJoin` which defines the DNSJoin interface that you will be implementing.

### Task #2: Implementing `DNSJoin`

For this part, you will need to fill out the implementations for `next`, `hasNext`, and `makeRequest` for `hashJoin`.

**NOTE**: You should return JoinedRows, which take two input rows and returns the concatenation of them. e.g., `new JoinedRow(row1, row2)`. 

At this point, you should be passing ***all*** given tests.

## Testing

We have provided you some sample tests in `SymmetricHashJoinSuite.scala` and `DNSJoinSuite.scala`. These tests can guide you as you complete this project. However, keep in mind that they are *not* comprehensive, and you are well advised to write your own tests to catch bugs. Hopefully, you can use these tests as models to generate your own tests.

In order to run our tests, we have provided a simple Makefile. In order to run the tests for task 1, run `make t1`. Correspondingly for task, run `make t2`, and the same for all other tests. `make all` will run all the tests. 

### Assignment autograder

We will provide an autograder on this assignment. To run the autograder on your assignment, push a branch called `ag/hw4` to your group repository. Keep in mind that you must still submit this assignment by pushing a branch called `release/hw4`: **we will not grade submissions to the autograder!**

    $ git checkout -b ag/hw4
    $ git push origin ag/hw4

Our machines will e-mail you the results of the autograder within an hour. If you do not receive a response after an hour, please *first* double-check that all your files are in the right place and that you pushed a commit to `ag/hw4`, and *then* notify us that you haven't received a response.

### Assignment submission

To submit your assignment, as before, push a branch containing the commit you want us to grade to `release/hw4`. This is separate from the autograder; pushing a commit here will not trigger the autograder, and vice-versa.

Good luck!

