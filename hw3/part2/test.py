#!/usr/bin/env python
import unittest
import maxflow as mf

class TestMaxFlow(unittest.TestCase):

  def test1_computed_flow(self):
    mf.setup("test1.csv")
    mf.maxflow()
    self.assertEquals(mf.final_flow(), 2)

  def test1_bfs_one_level(self):
    mf.setup("test1.csv")
    mf.maxflow(bfs_max_iterations=1)
    mf.db.execute("SELECT * FROM terminated_paths;")
    paths = mf.db.fetchall()
    self.assertEquals(len(paths), 2)
    self.assertIn(([1, 4], [1, 2, 4]), paths)
    self.assertIn(([2, 5], [1, 3, 4]), paths)

  def test1_one_flow(self):
    mf.setup("test1.csv")
    mf.maxflow(flow_max_iterations=1)
    mf.final_flow()
    mf.db.execute("SELECT id FROM flow WHERE flow > 0;")
    flows = mf.db.fetchall()
    self.assertEquals(len(flows), 2)
    self.assertIn((1,), flows)
    self.assertIn((4,), flows)

  def test1_edge_flows(self):
    mf.setup("test1.csv")
    mf.maxflow()
    mf.final_flow()
    mf.db.execute("SELECT flow FROM flow ORDER BY id;")
    assigned_flows = mf.db.fetchall()
    expected_flows = [1, 1, 0, 1, 1]

    for i, f in enumerate(expected_flows):
      expected = assigned_flows[i][0]
      self.assertEquals(f, expected,
                        "Flow {}: expected:{} found:{}".format(i, f, expected))
      
  def test2_computed_flow(self):
    """
    Test 2 is based on
    http://en.wikipedia.org/wiki/Ford%E2%80%93Fulkerson_algorithm#Integral_example
    """
    mf.setup("test2.csv")
    mf.maxflow()
    self.assertEquals(mf.final_flow(), 2000)

  def test3_computed_flow(self):
    """
    Test 3 is based on
    http://en.wikipedia.org/wiki/Edmonds%E2%80%93Karp_algorithm#Example
    """
    mf.setup("test3.csv")
    mf.maxflow()
    self.assertEquals(mf.final_flow(), 5)

if __name__ == '__main__':
    unittest.main(verbosity=3)
