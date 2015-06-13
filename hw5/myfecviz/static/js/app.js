/*
 * DashboardController
 *
 * Manages the application logic and event handling for the dashboard.
 * Instantiates the USCashMap and TransactionHistogram visualization widgets.
 *
 * @constructor
 * @params {d3.selection} mapSelection
 * @params {d3.selection} histogramSelection
 * @params {Object} dependencies
 */
var DashboardController = function(mapSelection, histogramSelection, dependencies) {
    // Instantiate event handling
    this.dispatch = d3.dispatch('mapselectionchange');
    this.dispatch.on('mapselectionchange', this.processChanges.bind(this));
    // NOTE: Binding necessary for `this` will not be bound by default to the dashboardcontroller
    this.dependencies = dependencies;

    // Instantiate the visualization widgets
    this.usCashMap = new USCashMap(
        mapSelection,
        dependencies.usTopoJSON,
        dependencies.convertIdToStateCode,
        dependencies.convertStateCodeToName,
        this.dispatch
    );

    this.transactionHistogram = new TransactionHistogram(histogramSelection, this.dispatch);
};

/*
 * firstLoad()
 *
 * Make the necessary requests for the transaction amounts data and summed transactions data.
 * Run firstProcess when it's done loading.
 */
DashboardController.prototype.firstLoad = function () {
    queue()
        .defer(d3.json, '/fec/all_transaction_amounts')
        .defer(d3.json, '/fec/summed_transactions')
        .await(this.firstProcess.bind(this));
};

/*
 * firstProcess(error, response0, response1)
 *
 * Store the responses for later use and render the the visualizations.
 * 
 * @params {Object} error
 * @params {JSON Blob} response0 is the response from '/fec/all_transaction_amounts'
 * @params {JSON Blob} response1 is the response from '/fec/summed_transactions'
 */
DashboardController.prototype.firstProcess = function (error, response0, response1) {
    // Store for later use
    this.allTransactions = response0['txn_amounts'];
    this.stateTotalTransactions = response1['txn_totals'];

    // Render!
    this.usCashMap.render(this.stateTotalTransactions);
    this.transactionHistogram.render(this.allTransactions);
};

/**
 * processChanges()
 *
 * Event handler for a map selection change event.
 *
 * If the selection is not empty, filter all the transactions by the selected state(s)
 * and re-render the TransactionHistogram. When re-rendering the selected states, check
 * if the selection was due to a click event on the usCashMap. If click, rescale the
 * histogram such that the scaling of this histogram is with respect to the filtered data.
 * If the map selection change was not due to a click, then scale the histogram with
 * respect to all of the objects in this.allTransactions.
 *
 * DO NOT make another request to the server. Filter the data stored in `this.allTransactions`
 */
DashboardController.prototype.processChanges = function () {
    // Implement
    console.log("Controller notified of map change.");  // Remove when implemented
    this.filterTransactionsByMapSelection();
    var renderData;  // filter renderData as needed.
    if (this.usCashMap.isSelectionClick() && this.usCashMap.hasSelection()) {
        // Selection was clicked
        // Make sure transaction histogram is rescaled to just the selection
        renderData = this.filterTransactionsByMapSelection();
        this.transactionHistogram.setScale(this.filterTransactionsByMapSelection());
        this.transactionHistogram.setHistogramColor(this.transactionHistogram.colorStates.PRIMARY);
        
    } else if (this.usCashMap.hasSelection()) {
        // Selection is just hovered upon
        // Use scale representing all of data (for a visually relative measure) 
        renderData = this.filterTransactionsByMapSelection();
        this.transactionHistogram.setScale(this.allTransactions);
        this.transactionHistogram.setHistogramColor(this.transactionHistogram.colorStates.SECONDARY);
    } else {
        // No user interaction
        // Process the map like normal
        renderData = this.allTransactions;
        this.transactionHistogram.setScale(this.allTransactions);
        this.transactionHistogram.setHistogramColor(this.transactionHistogram.colorStates.DEFAULT);
    }

    // Uncomment the following line when you're ready!
    this.transactionHistogram.render(renderData);
};

/*
 * filterTransactionsByMapSelection()
 *
 * Filter the objects in the array `this.allTranscations` for objects that match the selected states
 * in `this.usCashMap`.
 *
 * @return {Array} list of objects filtered by the selected states in `this.USCashMap`'s state selection
 */
DashboardController.prototype.filterTransactionsByMapSelection = function () {
    var cashmap = this.usCashMap;
    var filter = function(d){
        return cashmap.getStatesInSelection().indexOf(d["state"]) > -1
    }
    return this.allTransactions.filter(filter);
};
