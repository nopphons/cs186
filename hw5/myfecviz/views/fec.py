import ujson
from flask import render_template, Response

from myfecviz import app
from myfecviz.services.fec import (
    get_all_transaction_amounts,
    get_total_transaction_amounts_by_state,
)


@app.route('/')
def home():
    """Return the home page."""
    return render_template('index.html')

@app.route('/test')
def run_js_tests():
    """Return the BDD testing page."""
    return render_template('test.html')


@app.route('/fec/summed_transactions')
def summed_transactions():
    """Return a JSON response containing the summed state transactions."""
    return Response(
        ujson.dumps({'txn_totals': get_total_transaction_amounts_by_state()}),
        mimetype='application/json'
    )


@app.route('/fec/all_transaction_amounts')
def all_transaction_amounts():
    """Return a JSON response containing all the postitive contributions."""
    return Response(
        ujson.dumps({'txn_amounts': get_all_transaction_amounts()}),
        mimetype='application/json'
    )
