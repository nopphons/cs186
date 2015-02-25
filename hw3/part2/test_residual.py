#!/usr/bin/env python
import unittest
import maxflow as mf

class TestResidualFlow(unittest.TestCase):

  def test4_max_flow_edges(self):
    mf.setup("test4.csv")
    mf.maxflow()
    mf.final_flow()
    mf.db.execute("SELECT flow FROM flow "
                  "WHERE src = 7 AND dst = 8;")
    capacity1 = int(mf.db.fetchone()[0])
    self.assertEquals(capacity1, 10)

    mf.db.execute("SELECT flow FROM flow "
                  "WHERE src = 4 AND dst = 8;")
    capacity2 = int(mf.db.fetchone()[0])
    self.assertEquals(capacity2, 10)

  def test4_total_flow(self):
    mf.setup("test4.csv")
    mf.maxflow()
    self.assertEquals(mf.final_flow(), 20)

if __name__ == '__main__':
    unittest.main(verbosity=3)
