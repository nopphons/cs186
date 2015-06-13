import unittest


class FlaskClientTestCase(unittest.TestCase):

    def setUp(self):
        from myfecviz import app  # noqa
        self.app = app
        self.app_context = self.app.app_context()
        self.app_context.push()
        self.client = self.app.test_client(use_cookies=True)

    def tearDown(self):
        self.app_context.pop()
