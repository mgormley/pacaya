
import sys
import os
import re
import getopt
import math
import tempfile
import stat
import shlex
import subprocess
from subprocess import Popen
from optparse import OptionParser
import platform
from glob import glob
from ilp.gurobi2zimpl import get_var_map
import traceback

# Node  Left     Objective  IInf  Best Integer     Best Node    ItCnt     Gap         Variable B NodeID Parent  Depth
class CplexStatus:
    
    def __init__(self, line):
        splits = line.split()
        if splits[0] == "*":
            splits = splits[1:]
            
        # Check whether the node was fathomed     
        try:
            float(splits[2])
            self.is_fathomed = False
        except ValueError:
            if (splits[2] != "integral"):
                splits.insert(2, splits[2])
            self.fathomtype = splits[2]
            self.is_fathomed = True
            
        if len(splits) < 13:
            self.is_branching_node = False
            return
        self.is_branching_node = True
        self.variable = splits[8]
        self.bdirection = splits[9]
        self.nodeid = splits[10]
        self.parent = splits[11]
        self.depth = int(splits[12])
        
def get_cplex_status_list(cplex_log_file):
    status_list = []
    reading = False
    for line in open(cplex_log_file, 'r'):
        if line.find("Node  Left     Objective") != -1:
            reading = True
            continue
        if line.find("Elapsed real time") != -1 or line.find("Nodefile size") != -1:
            continue
        if line.find("GUB cover cuts applied") != -1:
            reading = False
            break
        if line.isspace():
            continue
        if reading:
            try:
                status_list.append(CplexStatus(line))
            except Exception:
                traceback.print_exc()
                print "Bad status line:",line
                sys.exit(1)
    return status_list
        
def parse_cplex_log(cplex_log_file, zimpl_tbl_file):
    # Map from short var names to zimpl var names
    var_map = get_var_map(zimpl_tbl_file)
    status_list = get_cplex_status_list(cplex_log_file)
    for status in status_list:
        if status.is_branching_node:
            status.variable = var_map[status.variable]
    
    # Print counts of each branching variable type
    print "Fathoming histogram: (var fathomtype depth-rounded-to-nearest-10)"
    type_count_map = {}
    for status in status_list:
        if status.is_branching_node and status.is_fathomed:
            type = re.split("[#$]", status.variable)[0]
            type += " %s %d" % (status.fathomtype, int(round(status.depth / 10.0) * 10))
            count = type_count_map.get(type,0)
            type_count_map[type] = count+1
    histogram = []
    for type,count in type_count_map.items():
        histogram.append((count,type))
    for count,type in reversed(sorted(histogram)):
        print "%s\t%d" % (type,count)
        
    # Print counts of each branching variable type
    print "Branching var histogram:"
    type_count_map = {}
    for status in status_list:
        if status.is_branching_node:
            type = re.split("[#$]", status.variable)[0]
            count = type_count_map.get(type,0)
            type_count_map[type] = count+1
    histogram = []
    for type,count in type_count_map.items():
        histogram.append((count,type))
    for count,type in sorted(histogram):
        print "%s\t%d" % (type,count)
        
    # Print counts of each zimpl var
    print "Zimpl var histogram:"
    type_count_map = {}
    for short_var,zimpl_var in var_map.items():
        type = re.split("[#$]", zimpl_var)[0]
        count = type_count_map.get(type,0)
        type_count_map[type] = count+1
    histogram = []
    for type,count in type_count_map.items():
        histogram.append((count,type))
    for count,type in sorted(histogram):
        print type,count
            
if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('-l', '--cplex_log', help="CPLEX log file")
    parser.add_option('-t', '--zimpl_tbl', help="Zimpl tbl file")
    (options, args) = parser.parse_args(sys.argv)
    
    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
        
    cplex_log_file = options.cplex_log
    zimpl_tbl_file = options.zimpl_tbl
    parse_cplex_log(cplex_log_file, zimpl_tbl_file)
    
    