# Visualizing FEC Data

## Introduction
![Animation](http://i.imgur.com/F8RQzzA.gif)

In homework 3, we wrote SQL queries that helped us answer certain questions about campaign contribution data in the United States. However, with a large number of results, it's hard to make heads or tails of what the data actually means. In this project, we'll explore using D3.js to help us visualize trends and significance in our data.

This is an individual project. This assignment is due on **Thursday, April 9 at 11:59PM.**

## Setup
The stack for this project consists of the following:

- Vanilla JavaScript + [D3.js](http://d3js.org/) for our client-side web visualization application
- [Flask](http://flask.pocoo.org/) for the python server backend that allows us to query and repackage the data for client-side consumption
- [Postgresql](http://www.postgresql.org/) for storing our data (like in hw3)

![Diagram](http://i.imgur.com/6HAu2Pw.png)

### Vagrant-only setup
Before you begin, you'll need to make some changes to your vagrant config. Locate your `Vagrantfile` and shutdown your VM with `vagrant halt`.

Add the following line to your `Vagrantfile` below the `config.vm.box` line:

    config.vm.network "forwarded_port", guest: 5000, host: 5000

This will allow the webserver inside of your VM to be accessible by your computer.

### Vagrant & Ubuntu setup
Next, if you're in a VM or Ubuntu, you'll need to run a command that will install external dependencies: (skip step if not in vagrant/ubuntu)

    $ cd $HW5_BASE_DIR
    $ ./install_vm_requirements.sh

### OSX-only setup
Download and install the [Postgres.app](http://postgresapp.com/). This is a standalone application for hosting a Postgresql server.

Once installed, run the following to get the postgresql command line tools in your shell:

    $ echo 'export PATH=$PATH:/Applications/Postgres.app/Contents/Versions/9.4/bin' >> ~/.bash_profile
    $ source ~/.bash_profile

### Virtualenv (optional)
We highly encourage you to use a python [virtualenv](https://virtualenv.pypa.io/en/latest/) so you don't pollute your python environment. The Python virtualenv isolates module requirements to a virtual environment, so your global python environment isn't filled with dependencies. There is a bit of setup to get it working.

	$ sudo pip install virtualenv
	$ sudo pip install virtualenvwrapper
	$ echo 'export WORKON_HOME=~/Envs' >> ~/.bash_profile
	$ echo 'source /usr/local/bin/virtualenvwrapper.sh' >> ~/.bash_profile
	$ mkdir ~/Envs
	$ source ~/.bash_profile  # for the current session
	
Once it's setup, you can create a virtual environment that is stored in the `~/Envs` folder.

	$ mkvirtualenv hw5  # creates the virtual environment -- do this just once for a project
	$ workon hw5        # loads this virtual environment as the current shell session's python environment
	(hw5) $

Now, install all your python dependencies with `pip` inside of the virtualenv you created. New shell sessions must call `workon hw5` in order to load the `hw5` python virtual environment.

### Everyone
To get started, you'll need to bootstrap the database (courtesy of hw3). Continue reading through the `README` as you wait for this to download (it takes a bit of time).

	$ cd $HW5_BASE_DIR/bootstrap
	$ ./import-fec.sh

This will populate a database that is configured with credentials accessible by the Flask web service.

There are a few python packages needed before we can begin.

You can install the requirements with [`pip`](https://pip.pypa.io/en/latest/):

	$ cd $HW5_BASE_DIR
	$ pip install -r requirements.txt  # may need to be sudo if not in a virtualenv

Then you should be ready to go! To run the web server, we've provided a convenience script `./serve.py` that will deploy the application on localhost port 5000.

## The files
Before diving into the tasks, take a moment to explore the project. In particular, take a look at the following files:

#### Python files
- `myfecviz/views/fec.py`
- `myfecviz/services/fec.py`

#### Template file
- `myfecviz/templates/index.html`

#### JavaScript files
- `myfecviz/static/js/app.js`
- `myfecviz/static/js/vis/geocashmap.js`
- `myfecviz/static/js/vis/transactionhistogram.js`

## Server-side task
### Task 1 - Server side setup
Before we can even begin to attempt making any kind of visualization, we'll need to process the database data so that it's consumable by the client-side JavaScript.

D3.js is able to consume [a wide variety of formats](https://github.com/mbostock/d3/wiki/Requests#convenience-methods), but we'll be sticking with outputting our data in JSON for simplicity. [JSON](http://en.wikipedia.org/wiki/JSON) is a data format that stores data in key-value pairs and is one of the most commonly used data formats to communicate between the server and the web application.

*(That being said, exporting JSON for huge amounts of structured data isn't necessarily the best idea -- why is that?)*

In this task, you will create intermediate Python datastructures to encapsulate and better describe the results of the database queries. The views that are setup in `myfecviz/views/fec.py` will serialize the output of these methods into JSON.

Use [`psycopg2`](http://initd.org/psycopg/docs/cursor.html) to access the Postgresql database and create an intermediate data structure for storing the results. In `myfecviz/services/fec.py`, **implement the following methods**:

- `get_all_transaction_amounts()` - For all committee contributions with a transaction amount greater than zero, return every transaction amount with the state that the contribution came from.
- `get_total_transaction_amounts_by_state()` - For every state, return a dictionary containing the state and total amount of non-zero contributions from the committee contributions table.

If the record has no state information, simply leave the state as `None`. The JSON serializer will convert this to `null`.

A sample method `get_number_of_candidates` is also included in the file as an example.

#### Testing database query
To test if your methods are formatting the lists of dictionaries correctly, you can run the following tests.

	$ cd $HW5_BASE_DIR  # i.e. where the hw5 folder is
	$ py.test myfecviz/tests/test_fec_services.py

**Note: these tests are not exhaustive.**

These methods are called in `myfecviz/views/fec.py`, which serializes the output into JSON and packages it as a HTTP response. We've done this part for you, but feel free to take a look to see how Flask's views work.

Once you've passed the `test_fec_services.py` tests, you should also pass the full test suite

	$ cd $HW5_BASE_DIR  # i.e. where the hw5 folder is
	$ py.test myfecviz/tests


## Client-side tasks
We've setup the template `index.html` that automatically loads the necessary external/vendor JavaScript depenencies, as well as the JavaScript controller and visualization widgets that you'll be creating and modifying.

You can start the server to access your dashboard by launching the Flask server. We've included a convenience script `serve.py` to do this:

	$ cd $HW5_BASE_DIR  # i.e. where the hw5 folder is
	$ ./serve.py
	
Once the server is up, you can navigate to [http://127.0.0.1:5000/](http://127.0.0.1:5000/) to view your progress.

### A brief note about UI interaction program structure
In a dashboard like this, there will be multiple visualizations and other UI components that interact with each other. The simplest approach to achieve linked interactions between different UI components is to ignore any sense of abstraction barriers and directly wrangle out the interactions we'd want in our dashboard.

Although this might work well on the first pass, not enforcing abstraction barriers will requires UI components to know too much about how other UI components are being used. This is a slippery slope to software engineering hell.

To avoid this, we'll be using a controller and components pattern, where the controller JavaScript (in this case `DashboardController`) will do the heavy lifting in controlling how the application's components are used. The components will be completely agnostic to how they are being used. The only way components will interact with  the rest of the application is through triggering events or setting internal state. The controller will process these events and act accordingly.

### Tools for JavaScript debugging
- use [`console.log`](https://developer.mozilla.org/en-US/docs/Web/API/Console/log) like `printf`
- insert the [`debugger`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/debugger) keyword into your code to pause execution at that line with the current frame available in the JavaScript console
- Use [Google Chrome Developer Tools](https://developer.chrome.com/devtools/docs/javascript-debugging) or [Firefox Developer Tools](https://developer.mozilla.org/en-US/docs/Tools/Debugger)


### Task 2 - U.S. State Map (vis warm-up)
Currently, this map does nothing but look nice. It's your job add some useful interaction to this map.

![US Map](http://i.imgur.com/rd31HIx.png)

Upon the `USCashMap` object construction, the United States land and individual states are rendered as SVG elements. This is done through D3's [geo projections API](https://github.com/mbostock/d3/wiki/Geo-Projections). The dashboard controller (`app.js`) instantiates the `USCashMap` component with the path data necessary to render the boundaries of the country and each state in `USCashMap.prototype.setupMap`. On initialization, the path elements that represent states are joined with the geographic path data that contains unique information like the state boundary's state code (e.g. 'CA').

The `USCashMap.prototype.render(data)` method has been partially implemented so that it will render each individual state. Your job is to (1) color in the states based on total contribution amount and (2) add `mouseover`, `mouseout`, and `click` event handlers to each state such that the desired interactions are achieved (described in 2.2).

**You will be implementing the missing portions of `USCashMap.prototype.render` in `myfecviz/static/js/vis/geocashmap.js`.**

Before you begin, take some time to familiarize yourself with the `data` being passed in `USCashMap.prototype.render`. There are several ways to do this. 

One way to see what the data looks like is to add a line into `USCashMap.prototype.render`:

	USCashMap.prototype.render = function (data) {
		console.log(data);
		// Rest of the code ...
	}

When you refresh the page, you'll be able to see the data in the JavaScript console. In Chrome, this is accessible through `CMD + option + J` (or `CTRL + SHIFT + J` if you're not on a Mac). In Firefox, you can access the console in `CMD + OPTION + K` (or `CTRL + SHIFT + K`).

![Console log](http://i.imgur.com/Amm364N.png)

Another way to see what the data looks looks like is to set a breakpoint in the browser's developer tools. You can set the breakpoint by clicking on the left-hand line number bar for the line which you'd like to break.

![set breakpoint](http://i.imgur.com/wTmZTFC.png)

The JavaScript execution will pause here once you refresh the page. You can then inspect the scope with the developer tools.

![insepct](http://i.imgur.com/pCZSOAU.png)

The last way you can explore the data is to insert the `debugger` statement into your code:

	USCashMap.prototype.render = function (data) {
		debugger;
		// Rest of the code ...
	}
	
This is the JavaScript keyword for setting a breakpoint, and will pause JavaScript execution at this line. You can then go into the JavaScript console and inspect the variables accessible from the current scope.

![scope](http://i.imgur.com/qvsLGG8.png)

Notice that data is a list of objects what keys the objects contain.

#### 2.1 State coloring
In task 1, you prepared the database queries into a format that was serializable into JSON. The output of `get_total_transaction_amounts_by_state` (in the python service) is now accessible as an array of JavaScript objects in `USCashMap.prototype.render` as the `data` parameter.

This data is bound to the states' path elements (created on `USCashMap`'s instantiation) by a [key function](https://square.github.io/intro-to-d3/data-binding/#identity-and-the-key-function) that joins the previously bound geopgrahic data with the database data containing the total contribution amounts.

Begin by making meaning out of the total contribution amounts by  coloring the the individual states (line 118) without handling any user events. **When you've got this part down, your map should look like the colored map [pictured above](http://i.imgur.com/rd31HIx.png).**

Make use of the `moneyScale` scale transformation that transforms a numerical amount into a hex color value. For more on how scale transformations work in D3, take a look at [this article](http://www.jeromecukier.net/blog/2011/08/11/d3-scales-and-color/).

#### 2.2 Event handlers and re-coloring

##### "that" is key
In D3, it's common define anonymous functions that are used to define attributes.

	MyClass.prototype.render = function () {
		d3.selectAll('div').data([1,2,3])
			.enter()
				.append('div')
					.attr('height', function(d) { return d; });
	};

However, if this anonymous function is defined within a JavaScript class object, you'll have difficulty accessing the instantiated class object (in this case the instantiated `MyClass` object). The JavaScript `this` keyword will no longer be bound to the instantiated class object. In the event handler callbacks in D3, `this` is bound to the DOM element that triggered the event. Hence, the conventional hack to make the class object accessible in the callback function is to define a variable `that` that references `this` (the instantiated calss object) in the scope in which the anonymous function is defined.

You'll need to take advantage of `that`.

**Next, implement the event handlers for the user interactions (line 144 and onwards). Only modify `geocashmap.js`.**

In order to handle the events, you'll need to make use of the following:

- the variable `that`, which references the instantiated `USCashMap` component
- the parameter `d`, which references the datum bound to the state SVG that was clicked (e.g. if you clicked California on the state map, the event handler would be passed the datum that is bound to it: `{'state': 'CA', 'total_amount': ... }`)
- the variable `moneyScale`, which is accessible through the anonymous function's scope


##### Mouseover event
![mouseover](http://i.imgur.com/RzeZ09i.gif)

When a user hovers over a state, the inspection info (see `setInspectionInfo`) will be set to the state's full name (see `stateNameMap`) and the state's total contribution amount. An event should be triggered using `this.dispatch`, which is an instantiated instance of [`d3.dispatch`](https://github.com/mbostock/d3/wiki/Internals#d3_dispatch). The event dispatching is already done for you in `addStateToSelection`.

    /*
     * Mouseover event handler
     *
     * When the mouse hovers within a state's boundaries, this event is triggered.
     * On hover, the following will occur:
     *  (1) the state's code (e.g. 'CA') is added to this._selectedStates via `addStateToSelection`
     *      (a) an event needs to be triggered (already done in the `addStateToSelection` method)
     *  (2) the fill of this particular state is an active color (of your choice)
     *      (a) the fill of all other states should still be the original color intensity mapping
     *  (3) Update the inspection information text with the selected state using `setInspectionInfo`
     *  (4) set internal state that signals the selection is not due to a click (already done for you)
     */
    this.states.on('mouseover', function(d, e, p){
        that.setSelectionClickBoolean(false);
		// ... code ...
    });

##### Mouseout event
When a user moves his/her cursor out of a state, the visualization should reset to a state prior to any user interactions. The original color mapping for each state should be restored, and the map's state selection should be cleared. An event should be triggered using `this.dispatch` to notify the controller that the component's selection has changed. The event dispatching is already done for you.

    /*
     * Mouseout event handler
     *
     * When the mouse leaves a state's boundaries, this event is triggered.
     * On the mouse's depature, the following will occur:
     *  (1) Clear the inspection info
     *  (2) Remove the state code (e.g. 'CA') from this._selectedStates
     *      (a) internal state for determining if selection was a click needs to be removed
     *  (3) Make sure the colors of every state are the same as it was prior to any events.
     *      (a) this means using the original `moneyColorScale` mapping
     */
    this.states.on('mouseout', function(d, e, p){
        that.setSelectionClickBoolean(false);
        // ... code ...
    });

##### Click event
![click](http://i.imgur.com/QjFQ323.gif)

When a user clicks over a state, the inspection info is set as in the hover interaction. In addition, state code (e.g. 'CA') should be added to the internal representation of the map's selected states (see `addStateToSelection`). An event should be triggered using `this.dispatch`, which is an instantiated instance of [`d3.dispatch`](https://github.com/mbostock/d3/wiki/Internals#d3_dispatch). The event dispatching is already done for you in `addStateToSelection`. Furthermore, the component should store internal state to indicate that this particular selection was due to a click.

    /*
     * Click event handler
     *
     * When the user click's on the particular region, this event is triggered.
     * On click, the following will occur:
     *  (1) the state's code (e.g. 'CA') is added to this._selectedStates via `addStateToSelection`
     *      (a) an event needs to be triggered (already done in the `addStateToSelection` method)
     *  (2) the fill of this particular state is a bright color different from both your contribution
     *      color mapping and mouseover color choice
     *      (a) the fill of all other states should still be the original color intensity mapping
     *  (3) Update the inspection information text with the selected state using `setInspectionInfo`
     *  (4) set internal state that signals the selection is due to a click (already done for you)
     */
    this.states.on('click', function(d, e, p){
        d3.event.stopPropagation();
        that.setSelectionClickBoolean(true);
        
        // ... code ...
    });


Once you complete this task, you should be able to visually see the following:

1. Each state should be colored according to the state's total contributions
2. For the state under the cursor, the state should be colored a noticeably different color and its inspection information is displayed in text above the state map
3. When a state is clicked, the state changes to a noticeably different color from the hover state and the JavaScript console logs "Controller notified of map change." (Assuming you didn't finish the subsequent tasks). Furthermore, `usCashMap.isSelectionClick()` should return `true`.

### Task 3 - Filtering
Once the user has made a state selection in the `USCashMap` visualization, the dashboard (the entire web app) needs to react to the selected state(s) by re-drawing the histogram visualization to display only the campaign contribution amounts that come from the selected state(s).

Before the histogram and map visualizations are linked, the controller will need to be able to filter all the transactions it has by the states selected by the `USCashMap`.

Implement `filterTransactionsByMapSelection` in the `DashboardController` class, which is located in `myfecviz/static/js/app.js`. This method will be called if a map state selection is detected in `processChanges`.

This method will filter all the objects in `this.allTransactions` by the state(s) that were selected by the `usCashMap` component. `this.allTransactions` contains the output of your database query from `get_all_transaction_amounts` in task 1.

**Note:** that `USCashMap` only has one state in its selection at all times in this implementation, but we are to modify `USCashMap` to be able to keep track of multiple selected states. Hence the references to "state(s)" really mean just the selected state. We won't be implementing interactions that select multiple states in this project, but our filtering method should be able to handle it.

Hints:

- See [Array.prototype.filter](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/filter)
- Please use `USCashMap`'s method accessors rather than accessing any direct property

#### Testing
If you launch the web service and navigate to [http://127.0.0.1:5000/test](http://127.0.0.1:5000/test), the `filterTransactionsByMapSelection` test should pass.

Don't worry about the `firstProcess` and `processChanges` methods not passing until you finish Task 4.


### Task 4 - Transaction Histogram (here we go)
This component will display a histogram of the contribution amounts based on the transaction amounts that this component receives in the `data` parameter of its `render` method. This component will need re-render/re-scale the histogram based on the data passed in.

Implement the `TransactionHistogram` component which is located in `myfecviz/static/js/vis/transactionhistogram.js`. 

#### Just plotting it
Begin by plotting just a histogram of all the transactions let's assume `transactionHistogram.render` is called only once with it's data parameter which is a list of objects with the keys 'state' and 'amount'.

D3 provides a [histogram layout](https://github.com/mbostock/d3/wiki/Histogram-Layout) that can automatically divvy up the data into bins. We won't be using equiwidth bins as the campaign contribution data is heavily skewed towards contributions in the $1 and $1000 range. Instead, we'll be dividing the bins using a predetermined [threshold scale](https://github.com/mbostock/d3/wiki/Quantitative-Scales#threshold-scales) which is defined in `TransactionHistogram.bins` and already configured in the `TransactionHistogram.histogramLayout` layout function.

![Just the histogram](http://i.imgur.com/GpyraIC.png)

Begin by implementing `TransactionHistogram.prototype.setScale`. The scaling methods allow us to map domains into a particular range for plotting. For a deeper dive into how scaling works in D3, have a look [at this article](http://www.jeromecukier.net/blog/2011/08/11/d3-scales-and-color/). We've implemented the X scaling to fit the histogram bins for you, but you'll need to implement the Y scaling for the height of the histogram bars based on the data passed into `setScale`. We recommend starting off with `d3.linear.scale`, but feel free to pick another scaling option that might bring more meaning out of the visualization. 

Next: Using the set scales, bins, and histogram layout, finish the `TransactionHistogram.render` method to plot the histogram of the sizes of the contributions passed in from the `data` parameter.

The following posts might help you in understanding how histograms in D3 are plotted:

- [http://bl.ocks.org/mbostock/3048450](http://bl.ocks.org/mbostock/3048450)
- [https://github.com/mbostock/d3/wiki/Histogram-Layout](https://github.com/mbostock/d3/wiki/Histogram-Layout)
- [https://github.com/mbostock/d3/wiki/Quantitative-Scales#threshold-scales](https://github.com/mbostock/d3/wiki/Quantitative-Scales#threshold-scales)

**Your histogram must have the following:**

- a rectangular bar for each bin
- each bar in the histogram must have a label with a **visible** count
- x axis label

When you think you're ready to see if your histogram is working, just refresh and you should see the histogram that visualizes all of the contribution amounts (regardless of state).

#### Animated changes
![slowmo animation](http://i.imgur.com/HLM7k1V.gif)

As the user is hovering across the states and sees a total contribution amount that is worthy of inspection, the user should see the transaction histogram display the distribution of contributions that belong to the state highlighted under the cursor. The height of the bars should be with respect to all of the contribution amounts (regardless of state). This will give the user a sense of what fraction of the total contributions came from a particular state.

As the user clicks a particular state, the user should see the transaction histogram display the distribution of contributions that belong to the clicked state. The height of the histogram bars should be with respect to just the state's contribution amounts, rather than all of the contribution amounts.

These interactions are triggered through events that the `DashboardController` is listening to. These events are handled by `DashboardController.prototype.processChanges`, which will filter all the committee contribution transaction amounts by the state selected by the map (if there is a selection). After filtering, the controller will call `TransactionHistogram` to render the histogram based on the filtered data, the set scales, and the set colors.

We've started `DashboardController.prototype.processChanges` for you, but you'll need to finish it so that the user interactions will work as described above.

**Uncomment the last line** of `DashboardController.prototype.processChanges` which will re-render the histogram based on the changes specified in `Dashboard.prototype.processChanges`.

**You'll need to work on these two files concurrently to get the interactions working:**

- `DashboardController.prototype.processChanges` in `myfecviz/static/js/app.js`
- `TransactionHistogram.prototype.render` in `myfecviz/static/js/vis/transactionhistogram.js`

Use D3's enter, update, and exit features to perform this transition animation to a new histogram.

The following posts might help you understand the enter, update, and exit phases:

- [http://bost.ocks.org/mike/circles/](http://bost.ocks.org/mike/circles/)
- [http://bost.ocks.org/mike/join/](http://bost.ocks.org/mike/join/)
- [https://github.com/mbostock/d3/wiki/Transitions](https://github.com/mbostock/d3/wiki/Transitions)

#### When this works properly, you should be able to interact in the following ways:
1. When the user hovers on a state in the U.S. Map, he/she sees the transaction histogram of the state under the cursor on a scale with respect to all of the transactions.
2. When the user clicks on a state in the U.S. Map, he/she sees the transaction histogram of the state under the cursor on a scale with respect to just that state's data.
3. When the user moves the cursor outside of any state, he/she sees the transaction histogram of all positive contributions recorded in our FEC databse.

### Sanity Check
In addition to all the UI interactions which will be tested by a CS186/286A peer, all the tests should be passing.

1. Running `py.test myfecviz/tests` in the root hw5 directory should yield all passed tests
2. Loading [http://127.0.0.1:5000/test](http://127.0.0.1:5000/test) should yield all passed tests

## Submission
When you're finished, push all relevant files to the `release/hw5` branch. More instructions on peer grading will be updated later.

	git push origin master:release/hw5

Although you may do your development on a laptop, ensure that your code works inside the VM (it should without any changes). One of your classmates will be grading the usability of your visualization dashboard, and they'll need to be able to run it.

The autograder is not currently online, but when it is you'll be able to test that your Python tests work on the VM as well. The autograder will not test any frontend interactions (your peers will).

	git push origin master:ag/hw5
	
Instructions on the peer evaluation will be updated later. Please check Piazza and/or frequently `git pull` from the course master repository.


## Extras
For kudos. If you decide to do any of these extras, please push this **separately** to `extras/hw5`. The JavaScript tests may not pass (due to missing mocks) in some of the implementations needed for the extras.

	git push origin master:extras/hw5

#### Speeding up the queries
Try building some indices and clustering the relevant tables and columns with Postgresql in `psql`. Do you notice an improvement in speed performance for the initial load?

#### Speeding up serialization
There are a few cons to JSON serialization (what are they?). Serializing CSV/TSV is an ideal replacement. Add new views/services that perform TSV serialization for you, and modify the controller (`myfecviz/static/js/app.js`) to load the TSV file.

#### Speed up filtering
The output from our REST endpoints currently don't have any particular order. If we've clustered our data and/or built an index, we can order the objects by state. Serialization will preserve this order. Modify your database query to output a sorted order, and use this ordering to make filtering by state on the client side  faster.

#### Add another interesting visualization
Explore the data and see what else might be interesting about the data. What percent of the contributions are for Republican vs Democratic candidates? Does this vary state by state? What are the primary interest groups involved in these contributions? Does this also vary state by state?

Plot an additional visualization below the transaction histogram (you may resize the transaction histogram) and encapsulate it in another class (like we did with USCashMap and TransactionHistogram).

## Appendix
### Relevant documentation
- [D3.js](https://github.com/mbostock/d3/wiki/API-Reference)
- [MDN JavaScript](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference)
- [psycopg2](http://initd.org/psycopg/docs/)
- [Flask](http://flask.pocoo.org/docs/0.10/api/)

### Data
- [FEC](http://www.fec.gov/finance/disclosure/ftpdet.shtml)

### Testing
You are not required to write additional tests, as JavaScript testing is a hairy process. But if you're interested, here's a great article on [setting up browser-side JavaScript test suites](https://nicolas.perriault.net/code/2013/testing-frontend-javascript-code-using-mocha-chai-and-sinon/). The article describes a setup that is identical to this project's browser JavaScript tests, which use [Sinon.js](http://sinonjs.org/), [Mocha.js](http://mochajs.org/), and [Chai.js](http://chaijs.com/).
