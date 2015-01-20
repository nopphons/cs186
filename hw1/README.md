# Homework 1: Project Gutenberg command-line wrangling 
### CS186, UC Berkeley, Spring 2015
### Points: [5% of your final grade](https://sites.google.com/site/cs186spring2015/home/basic-information)
### Note: *This homework is to be done individually!*
### Due: Tuesday, 1/27, 11:59 PM

##Description
This assignment will give you some experience with a typical task in modern data management:
using command-line tools to "wrangle" a bunch of publicly-available data into a more structured 
format suitable for subsequent analysis.  In particular, we will 
look at data from [Project Gutenberg](https://www.gutenberg.org/).
 
Along the way you will need to exercise your thinking about working with data that doesn't fit in memory. 

Submission instructions for this homework assignment can be found in [HW0](https://github.com/cs186-spring15/course/tree/master/hw0).

###Your challenge
Given a large data file of ebooks, generate [csv](http://en.wikipedia.org/wiki/Comma-separated_values) files 
that efficiently capture the ebook contents in a structured form suitable for analysis via a database 
or statistical package.

###Your tools
For this assignment, you are limited to using [Python](https://www.python.org/), [bash](http://www.gnu.org/software/bash/), and the [standard Unix utilities](http://en.wikipedia.org/wiki/List_of_Unix_utilities). All of these are pre-installed on a Virtual Machine for you.

We assume that CS186 students can pick up scripting languages like Python and bash on their own; there will be no Python or bash training as part of the class. 


###Your constraints
You need to be able to handle an input file that is far larger than the memory of the computer that runs the script.  To do so, you should:

1. write streaming Python and bash code that only requires a fraction of the data to be in memory at a time, and
2. utilize UNIX utilities like `sort` that provide out-of-core divide-and-conquer algorithms.  


You should not need to write very complicated code in either Python or bash.  Take advantage of UNIX utilities as much as you can.  In particular, note that there is no need for you to write an out-of-core algorithm to complete this homework: UNIX utilities can do the heavy lifting if you orchestrate them properly.

##Getting started
To follow these instructions you will need to set up a copy of the standard Virtual Machine we will use for grading.  We encourage you to run it on your own computer rather than the instructional machines -- it should run comfortably on any recent 64-bit computer under OSX, Windows or Linux.  You will need about 1-2 GB of free space on your disk to get going.

*(You can code outside the VM if you like--we're using fairly vanilla Ubuntu 12.04.5, with Python 2.7.3 and bash4.2.25.  HOWEVER, the CS186 staff will only answer configuration questions pertaining to the VM.  And eventually you need to turn in a solution that runs "out of the box" on a clean CS186 VM, so you'll want to test your code within the VM.)*

To get started with the VM:

1. [Download and Install VirtualBox](https://www.virtualbox.org/wiki/Downloads) for your computer.  Current version is 4.3.20.
1. [Download and Install vagrant](https://www.vagrantup.com/downloads).  Current version is 1.7.2.

Now open a terminal on your machine, create a cs186 directory to work in, and build your VM with vagrant.  This will download a large VM image from the cloud; make sure you've got some decent bandwidth.

    % mkdir cs186
    % cd cs186
    % vagrant init cs186-spring15/cs186spring15
    % vagrant up

If all goes well, your VM should now be up and running.  You can ssh into it as follows:

    % vagrant ssh

This is almost everything you need to know about vagrant, but you may want to [reference the vagrant docs](https://docs.vagrantup.com/v2/) occasionally.

Once you're logged into your VM, you should check out the git repository for the course within the VM:

    vagrant@precise64:~$ cd
    vagrant@precise64:~$ git clone https://github.com/cs186-spring15/course.git

This will create a directory called `course` in your home directory in the VM, which will contain a subdirectory called `hw1`. Change directory into there and look around:

    vagrant@precise64:~$ cd course/hw1
    vagrant@precise64:~$ ls
    ebook.csv  name_counts.csv    README.md  token_counts.csv
    hw1.sh     popular_names.txt  test       tokens.csv
    vagrant@precise64:~$

In addition to this README file, you will see:

* `hw1.sh`, a skeleton of the bash file you will write
* `test`, a subdir with python unit tests you can use to validate your solution,
* `popular_names.txt`, a list of popular names to look for in the tokens,
* `ebook.csv`, `name_counts.csv`, `token_counts.csv` and `tokens.csv`:
   correctly-formatted example outputs.

Finally, within your VM download the ebook data [here](https://www.dropbox.com/s/tmlaiccd7okac1h/ebooks.zip?dl=0) and unzip ebooks.zip in the hw1 directory.
Move `ebooks/ebooks_full.txt` and `ebooks/ebooks_tiny.txt` into hw1, and  `ebooks/ebooks_full.txt.out` and  `ebooks/ebooks_tiny.txt.out` into hw1/test/.

    vagrant@precise64:~$ cd ~/course/hw1
    vagrant@precise64:~$ wget -O ebooks.zip https://www.dropbox.com/s/tmlaiccd7okac1h/ebooks.zip?dl=1
    vagrant@precise64:~$ unzip ebooks.zip
    vagrant@precise64:~$ mv ebooks/*txt .
    vagrant@precise64:~$ mv ebooks/*.out test
    vagrant@precise64:~$ rm -r ebooks ebooks.zip
    
When you're done working you'll want to log out of the VM and shut it down:

    vagrant@precise64:~$ exit
    logout
    Connection to 127.0.0.1 closed.
    % vagrant halt

Next time you want to fire it up, just cd back to the cs186 directory you created and type `vagrant up` to reboot it.

##Specification
Your solution should be driven by a `hw1.sh` script that is passed one argument: a .txt file that contains a concatenation of valid ebooks:

    vagrant@precise64:~$ ./hw1.sh ebooks_tiny.txt

The script should overwrite the four example csv output files, as follows:

* `ebook.csv` should be a legal csv file containing the same header row as the example. Take a look at a few of the ebooks to determine how to best parse the necessary fields. 
    * If the ebook title is more than one line, just take the first line as the title. 
    * The ebook body should only include text from the start of the actual ebook (not including the headers added by Gutenberg Project). The start of the ebook's body is indicated by a line beginning with the string `*** START OF THE PROJECT GUTENBERG`, and the end of the body is indicated by a line beginning with `*** END OF THE PROJECT GUTENBERG`.
    * If one of the fields is not available for an ebook, the entry should be "null".
    * Depending on how you parse the fields, be sure to strip the carriage return and line feed (\\r\\n) at the end of the entries, except for the body.
* `tokens.csv` should be a csv file with the same header row as the example: `ebook_id,token`.  This file is generated by taking the `body` field of an ebook, and splitting it on non-alphabetical characters into separate tokens (substrings) which are converted to all-lowercase characters.  After splitting and lowercasing, each token should be copied into the `tokens.csv` file, prepended by the associated `ebook_id` (and a comma).  *Note that a given token may appear multiple times per ebook\_id, and/or multiple times across different ebook\_id  s.*
* `token_counts.csv` should have the same header row as the example, and sum up the number of occurrences of each *distinct* token in the `tokens.csv` file.
* `name_counts.csv` is intended to store the rough result of the question "how often is each name mentioned in all of the books?" It should have the same header as the example file, and then contain those rows from `token_counts.csv` with the (lowercase) name in the `token` field. For simplicity, we will only be looking at the top 50 most popular names for boys and girls. These names are provided in `popular_names.txt`.
	
###Testing
A simple [Python unit test] is provided in `test_ebook.py`.  If your code is working, you should see something like this if you type the first line to a bash shell in the hw1 directory:

    vagrant@precise64:~/course/hw1$ python test/ebook_test.py TestEbook.test_sanity
    .
    ----------------------------------------------------------------------
    Ran 1 test in 10.550s

    OK
    vagrant@precise64:~/course/hw1$

The sanity test runs your `hw1.sh` script against a handful of ebooks taken from the Gutenberg Project, and compares your output to what the solution produced.  You should have a look at the unit test files -- they are simple and you'd be wise to understand what they're checking.

Our grading script will compare your code against approximately 1100 ebooks provided on the Gutenberg Project website.  (We may also test against other data.)  To test against the full data set, you type:

    vagrant@precise64:~/course/hw1$ python test/ebook_test.py TestEbook.test_full
    .
    ----------------------------------------------------------------------
    Ran 1 test in 85.789s

    OK
    vagrant@precise64:~/course/hw1$

and hopefully you get 0-failure/0-error output similar to the listing above. 

To run both tests, you simply type:

    vagrant@precise64:~$ ./test/ebook_test.py 

We need to ensure that your code will scale to data sets that are bigger than memory -- no matter how much memory is on your test machine.  To this end, the test scripts use Python's [setrlimit](https://docs.python.org/2/library/resource.html) command to cap the amount of virtual memory your hw1.sh script allocates.  If you get a Segmentation fault error, then your code is not doing appropriate streaming and/or divide-and-conquer!

##Notes
* As noted in `hw1.sh`, the last line should say "exit 0" to indicate a successful completion.  This is important for making the tests run correctly!
* Consider using `sys.stdin`, `sys.stdout`, and `sys.stderr` in Python, and [UNIX pipes](http://en.wikipedia.org/wiki/Pipeline_(Unix)) to put together separate scripts.  method for writing to csv files.
* Python has a handy [CSV library](https://docs.python.org/2/library/csv.html) for csv manipulation and string manipulation.  It will make your life simpler.
* The UNIX utilties are written in C and are faster than anything you will write in Python.  So if your code seems very slow and you want to speed it up, try to use less Python and work more with the UNIX utilities.  Your final solution should complete the `test_full` test in a few minutes on the VM.  (The 85 second number above was from a fairly recent MacBook Pro with only lightly tuned code.  Your mileage may vary.)
* If using Python's CSV reader to read csvs, the body fields may be too large for Python's current field limit, even though they should certainly fit in memory. To fix that, change the field size limit:
    % csv.field_size_limit(sys.maxint)
* If you lose the original example csv output files, you can always recreate the original copies by typing:
```
% git checkout <foo>.csv
```
or by looking on the website at [https://github.com/cs186-spring15](https://github.com/cs186-spring15).

## Turnin instructions
Follow the submission instructions in [HW0](https://github.com/cs186-spring15/course/tree/master/hw0).

*Be sure to use `git add` and `git commit` to turn in any other files (e.g. bash or Python code) that your solution uses!*
