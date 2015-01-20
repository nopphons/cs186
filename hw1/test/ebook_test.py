#!/usr/bin/env python

import unittest
import locale, resource, subprocess, filecmp, os

class TestEbook(unittest.TestCase):

  def setUp(self):
    # ensure sane sort orders from UNIX sort
    os.environ["LC_ALL"] = "C"

  def test_sanity(self):
    self.diff_files("ebooks_tiny.txt")

  def test_full(self):
    self.diff_files("ebooks_full.txt")

  def diff_files(self, subpath):
    # cap virtual memory usage to ~size of largest ebook to detect memory hogs
    rsrc = resource.RLIMIT_STACK
    soft, hard = resource.getrlimit(rsrc)
    resource.setrlimit(rsrc, (170000, hard))

    # run hw1.sh script and make sure it succeeds
    try:
      subprocess.check_call(['./hw1.sh', subpath])
    except subprocess.CalledProcessError:
      self.fail("hw1.sh did not return with exit code 0, may have failed")

    # uncap virtual memory usage  
    resource.setrlimit(rsrc, (soft, hard))

    for fname in ['ebook', 'tokens', 'token_counts', 'name_counts']:
      try: 
        subprocess.check_call("sort " + fname + ".csv > " + fname + "-sorted.csv", shell=True) 
      except subprocess.CalledProcessError:
        self.fail("Sorting " + fname + ".csv failed.")
      # compare each of the resulting csv files with reference versions
      self.assertEqual(filecmp.cmp(fname + "-sorted.csv", "test/"+ subpath + ".out/" +fname+ "-sorted.csv"), True,
                       fname + "-sorted.csv did not match sorted reference file.") 

      try: 
        subprocess.check_call("rm " + fname + "-sorted.csv", shell=True) 
      except subprocess.CalledProcessError:
        self.fail("Removing " + fname + "-sorted.csv failed.")

if __name__ == '__main__':
    unittest.main()