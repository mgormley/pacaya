#!/usr/bin/env python
'''
Utility methods for running experiments.
'''

import sys
import os

# ---------------------------- Handy Functions ----------------------------------

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir

def get_first_that_exists(*paths):
    for path in paths:
        if os.path.exists(path):
            return path
    raise Exception("ERROR - None of the required paths exist: " + str(paths))

def require_path_exists(*paths):
    for path in paths:
        if not os.path.exists(path):
            raise Exception("ERROR - Required path does not exist: " + path)

def safe_join(*parts):
    for part in parts:
        if part is None:
            return None
    return os.path.join(*parts)
        
def combine_pairs(list1, list2):
        '''Creates a new list of groups by combining each pair of groups in these lists.'''
        new_list = []
        for x1 in list1:
            for x2 in list2:
                exp = x1 + x2
                new_list.append(exp)
        return new_list

