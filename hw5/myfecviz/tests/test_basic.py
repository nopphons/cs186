from unittest import TestCase

from myfecviz.tests import FlaskClientTestCase
from myfecviz import get_db


class BasicTest(TestCase):

    def test_basic_sanity(self):
        """Verify tests are working."""
        self.assertEquals(10, 10)


class ClientBasicTest(FlaskClientTestCase):

    def test_basic_query(self):
        """Verify basic query works."""
        db = get_db()
        db.execute('SELECT 1;')
        results = db.fetchall()
        self.assertEquals(1, results[0][0])
