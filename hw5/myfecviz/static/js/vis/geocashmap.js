/**
 * USCashMap
 *
 * Object encapsulating the U.S. state heatmap of monetary contributions. Each state's
 * color intensity maps to it's total contribution amount with respect to all other
 * states' contribution amounts.
 *
 * @constructor
 * @params {d3.selection} selector
 * @params {FeatureCollection} usTopoJSON
 * @params {d3.map} stateMap
 * @params {d3.map} stateNameMap
 * @params {d3.dispatch} dispatcher
 */
var USCashMap = function (selector, usTopoJSON, stateIdToStateCodeMap, stateNameMap, dispatcher) {
    this.dispatch = dispatcher;  // for triggering events
    this.selector = selector;

    // Store height and width parameters
    this.width = 740;
    this.height = 500;

    // Parameters for rendering U.S. map from TopoJSON
    this.projection = d3.geo.albersUsa()
        .scale(1000)
        .translate([this.width / 2, this.height / 2]);

    this.geopath = d3.geo.path()
        .projection(this.projection);

    // Maps stateCode to stateName (e.g. 'CA' --> 'California')
    this.stateNameMap = stateNameMap;
    // Store geography features
    this.us = usTopoJSON;
    this.landSubunit = topojson.feature(usTopoJSON, usTopoJSON.objects.land);
    this.statePaths = topojson.feature(usTopoJSON, usTopoJSON.objects.states).features.map(function(d) {
        d['state'] = stateIdToStateCodeMap.get(d['id']);
        return d;
    });

    this.setupMap();

    // Initialize selected states
    this._selectedStates = d3.set();
};

/*
 * setupMap()
 *
 * Sets up the SVG container and inspection text.
 */
USCashMap.prototype.setupMap = function () {
    // Create the map's svg container
    this.svg = this.selector.append("svg")
        .attr("width", this.width)
        .attr("height", this.height);

    // Render the bounding land region first
    this.svg.insert("path", ".graticule")
        .datum(this.landSubunit)
        .attr("class", "land")
        .attr("d", this.geopath);

    // Container for holding the states
    this.stateGroup = this.svg.append("g")
        .attr("class", "state-group");

    // Label for inspection information interaction
    this.textGroup = this.svg.append("g");
    this.mainText = this.textGroup.insert("text")
        .attr('class', 'map-main-text')
        .attr('x', 500)
        .attr('y', 50)
        .text("");  // E.g. California
    this.subText = this.textGroup.append("text")
        .attr('class', 'map-sub-text')
        .attr('x', 500)
        .attr('y', 70)
        .text(""); // E.g. $3,999,102

    // Add state paths
    this._states = this.stateGroup.selectAll('path')
        .data(this.statePaths, function(d) { return d.state; });

    this._states.enter().append('path')
        .attr('class', 'states')
        .attr('d', this.geopath)
        .attr('fill', '#E8F5E9');
};

/*
 * render(data)
 *
 * Renders the map with each state path colored based on a color mapping determined
 * by the total amounts of each state.
 *
 * @params {Array} data is a list of objects containg the keys 'state' and 'total_amount'
 */
USCashMap.prototype.render = function (data) {
    // Hack needed to access class object in functions defined in this scope
    var that = this;
    var moneyColorScale = d3.scale.sqrt()
        .domain([
            d3.min(data, function (d) { return d['total_amount']; }),
            d3.max(data, function (d) { return d['total_amount']; })
        ])
        .range(['#E8F5E9', '#4CAF50']).nice();

    this.states = this.stateGroup.selectAll('path')
        .data(data, function(d) { return d['state']; });

    /** Enter phase **/
    // No enter phase. We'll be binding to the state SVG paths that have already 
    // been rendered. (see assignment of this._states)

    /** Update phase **/
    this.states.transition().duration(200)
        .attr("fill", function(d) {
            // Change the color based on the state's total contribution amount
            // Hint: take a look at moneyColorScale defined above
            // Implement
            return moneyColorScale(d['total_amount']);  // Return a hexcode
        });

    /** Exit phase **/
    // No exit anticipated. The state paths are just going to stay there.

    /** Setup/Rebind event handlers **/

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
        // NOTE: if you want to reference the USCashMap instantiated object,  you must use the
        // `that` variable defined above rather than `this`, since `this` is rebound to a newly
        // defined function.
        that.addStateToSelection(d['state']);
        d3.select(this).attr("fill", '#4CAF50');
        that.setInspectionInfo(that.stateNameMap.get(d['state']),d['total_amount']);
    });

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
        // NOTE: if you want to reference the USCashMap instantiated object,  you must use the
        // `that` variable defined above rather than `this`, since `this` is rebound to a newly
        // defined function.
        // Implement
        that.clearInspectionInfo();
        that.removeStateFromSelection(d['state']);
        d3.select(this).attr("fill", moneyColorScale(d['total_amount']));
    });

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
        // NOTE: if you want to reference the USCashMap instantiated object,  you must use the
        // `that` variable defined above rather than `this`, since `this` is rebound to a newly
        // defined function.
        that.addStateToSelection(d['state']);
        d3.select(this).attr("fill", "orange");
        that.setInspectionInfo(that.stateNameMap.get(d['state']),d['total_amount']);
    });
};


/*
 * currencyFormatter(number)
 *
 * Formats a number into readable currency.
 *
 * @params {number} number
 */
USCashMap.prototype.currencyFormatter = d3.format('$,');


/*
 * setInspectionInfo(stateName, amount)
 *
 * Set the display text for a particular state with its state name and total
 * contribution amount.
 *
 * @params {String} stateName (e.g. California)
 * @params {number} amount (e.g. 1000000)
 */
USCashMap.prototype.setInspectionInfo = function(stateName, amount) {
    this.mainText.text(stateName);
    this.subText.text(this.currencyFormatter(amount));
};


/*
 * clearInspectionInfo()
 *
 * Clears the inspection info text.
 */
USCashMap.prototype.clearInspectionInfo = function() {
    this.mainText.text('');
    this.subText.text('');
};


/*
 * addStateToSelection(stateCode)
 *
 * Adds the state to the internal representation of a selection. This method will
 * also fire off the 'mapselectionchange' event.
 *
 * @params {String} stateCode is the 2 letter abbreviation of a state (e.g. 'CA')
 */
USCashMap.prototype.addStateToSelection = function (stateCode) {
    this._selectedStates.add(stateCode);  // e.g. stateCode = 'CA'
    this.dispatch.mapselectionchange();  // dispatch event for controller to consume
};


 /*
 * removeStateFromSelection(stateCode)
 *
 * Removes the state to the internal representation of a selection. This method will
 * also fire off the 'mapselectionchange' event.
 *
 * @params {String} stateCode is the 2 letter abbreviation of a state (e.g. 'CA')
 */
USCashMap.prototype.removeStateFromSelection = function (stateCode) {
    if (this._selectedStates.has(stateCode)) {
        var ret = this._selectedStates.remove(stateCode);
        this.dispatch.mapselectionchange();
        return ret;
    }
};

/*
 * clearStatesFromSelection()
 *
 * Clears all states from the selection and triggers a 'mapselectionchange' event.
 */
USCashMap.prototype.clearStatesFromSelection = function () {
    this._selectedStates = d3.set();
    this.dispatch.mapselectionchange();
    this.setSelectionClickBoolean(false);
};


/*
 * hasStateInSelection(stateCode)
 *
 * Return true if the state code is in the selection.
 *
 * @params {String} stateCode is the 2 letter abbreviation of a state (e.g. 'CA')
 */
USCashMap.prototype.hasStateInSelection = function (stateCode) {
    return this._selectedStates.has(stateCode);
};


/*
 * getStatesInSelection()
 *
 * Return an array containing the states selected by this visualization.
 * e.g. will return: ['CA']
 *
 * @returns {Array} of strings that indicate the selected state codes
 */
USCashMap.prototype.getStatesInSelection = function () {
    return this._selectedStates.values();
};


/*
 * hasSelection()
 * 
 * Return true if there is at least one state selected.
 */
USCashMap.prototype.hasSelection = function () {
    return !this._selectedStates.empty();
};

/*
 * isSelectionClick()
 *
 * Return true if the selection was due to a click event.
 * @returns {boolean} true if click selection
 */
USCashMap.prototype.isSelectionClick = function () {
    return this.hasSelection() && this._clicked;
};

/*
 * setSelectionClickBoolean(clickBoolean)
 *
 * Set the internal representation of whether or not the selection was clicked.
 *
 * @params {boolean} clickBoolean
 */
USCashMap.prototype.setSelectionClickBoolean = function (clickBoolean) {
    this._clicked = clickBoolean;
};
