import psycopg2
from flask import Flask
from flask import g


# The Flask app object
app = Flask(__name__)


def initialize_app():
    """Initialize the app with registered views.

    NOTE: This is a complete hack to register the views to avoid cyclic imports.

    Returns:
        Flask app object
    """
    # Register the views
    from myfecviz.views.fec import *  # noqa

    return app


def connect_to_database():
    """Return a new connected database cursor.

    Returns:
        Pscopg2 database cursor
    """
    conn = psycopg2.connect(database='fec', user='vagrant')
    conn.set_session(readonly=True)
    return conn.cursor()


def get_db():
    """Return a db cursor from global context or create a new connection.

    Returns:
        Psycopg2 database cursor
    """
    db = getattr(g, '_database', None)
    if db is None:
        db = g._database = connect_to_database()
    return db


@app.teardown_appcontext
def teardown_db(exception):
    """Tear down the database connection if it exists."""
    db = getattr(g, '_database', None)
    if db is not None:
        db.close()


# Instantiate views
initialize_app()
