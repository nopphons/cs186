var expect = chai.expect;
var assert = chai.assert;

describe("DashboardControllerTest", function() {
    var sandbox;
    beforeEach(function() {
        sandbox = sinon.sandbox.create();
        sandbox.stub(window, 'USCashMap');
        sandbox.stub(window, 'TransactionHistogram');
    });
    afterEach(function() {
        sandbox.restore();
    });
    describe("Constructor", function() {
        it("Expect visualization widgets to be initialized and bound to object.", function() {
            var dc = new DashboardController(
                d3.select('#test-map'),
                d3.select('#test-hist'),
                {}
            );
            assert.isDefined(dc.usCashMap);
            assert.isDefined(dc.transactionHistogram);
            assert.isTrue(USCashMap.calledWithNew())
            assert.isTrue(TransactionHistogram.calledWithNew())
        });
    });
    describe("firstProcess method", function() {
        it("Verify both render methods are called on first process", function() {
            var dc = new DashboardController(
                d3.select('#test-map'),
                d3.select('#test-hist'),
                {}
            );

            dc.usCashMap.render = sinon.spy();
            dc.transactionHistogram.render = sinon.spy();
            dc.firstProcess({}, {}, {});

            assert.isTrue(dc.usCashMap.render.called);
            assert.isTrue(dc.transactionHistogram.render.called);
        });
    });
    describe("processChanges method", function() {
        it("Verify filtering and re-rendering methods are called when there is a selection and it's due to a click", function() {
            var dc = new DashboardController(
                d3.select('#test-map'),
                d3.select('#test-hist'),
                {}
            );

            dc.usCashMap.hasSelection = sandbox.stub().returns(true);
            dc.usCashMap.isSelectionClick = sandbox.stub().returns(true);
            dc.filterTransactionsByMapSelection = sandbox.spy();
            dc.transactionHistogram.render = sandbox.spy();
            dc.transactionHistogram.setScale = sandbox.stub();
            dc.transactionHistogram.setHistogramColor = sandbox.stub();
            dc.transactionHistogram.colorStates = {
                'DEFAULT': 0,
                'PRIMARY': 1,
                'SECONDARY': 2
            }
            dc.processChanges();

            // Make sure filter is called
            assert.isTrue(dc.filterTransactionsByMapSelection.called);
            // Make sure rendering function is called
            assert.isTrue(dc.transactionHistogram.render.called);
        });
        it("Verify re-rendering method is called when there is no selection", function() {
            var dc = new DashboardController(
                d3.select('#test-map'),
                d3.select('#test-hist'),
                {}
            );

            dc.usCashMap.hasSelection = sandbox.stub().returns(false);
            dc.usCashMap.isSelectionClick = sandbox.stub().returns(false);
            dc.filterTransactionsByMapSelection = sandbox.spy();
            dc.transactionHistogram.render = sandbox.spy();
            dc.transactionHistogram.setScale = sandbox.stub();
            dc.transactionHistogram.setHistogramColor = sandbox.stub();
            dc.transactionHistogram.colorStates = {
                'DEFAULT': 0,
                'PRIMARY': 1,
                'SECONDARY': 2
            }
            dc.processChanges();

            // Make sure rendering function is called
            assert.isTrue(dc.transactionHistogram.render.called);
        });
    });
    describe("filterTransactionsByMapSelection method", function () {
        it("Ensure transactions are being filtered by selection", function() {
            var dc = new DashboardController(
                d3.select('#test-map'),
                d3.select('#test-hist'),
                {}
            );

            dc.usCashMap = {};
            dc.allTransactions = [
                {'state': 'CA', 'amount': 20},
                {'state': 'CA', 'amount': 10},
                {'state': 'CA', 'amount': 90},
                {'state': 'FL', 'amount': 90},
                {'state': 'WA', 'amount': 90}
            ];

            // Stub out essential methods to force a selection
            dc.usCashMap.hasStateInSelection = function(sc) {
                return sc === 'CA';
            };

            dc.usCashMap.hasSelection = sandbox.stub().returns(true);
            dc.usCashMap.getStatesInSelection = sandbox.stub().returns(['CA']);
            // Filter and check results
            var results = dc.filterTransactionsByMapSelection();
            expect(results.length).to.equal(3);
            console.log(results);
            for (var i = 0; i < results.length; i++ )
                expect(results[i]['state']).to.equal('CA');
        });
    });
});
