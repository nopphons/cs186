from myfecviz.tests import FlaskClientTestCase


class FECViewTest(FlaskClientTestCase):

    def test_get_home(self):
        """Verify home page loads."""
        res = self.client.get('/')
        self.assertEqual(200, res.status_code)

    def test_summed_transactions(self):
        """Verify summed transaction view works."""
        res = self.client.get('/fec/summed_transactions')
        self.assertEqual(200, res.status_code)
        self.assertEqual('application/json', res.mimetype)

    def test_all_transaction_amounts(self):
        """Verify all transaction amount view works."""
        res = self.client.get('/fec/all_transaction_amounts')
        self.assertEqual(200, res.status_code)
        self.assertEqual('application/json', res.mimetype)
