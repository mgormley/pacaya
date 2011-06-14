#!/usr/local/bin/python

import re
import sys
import os
import getopt
import math
import tempfile
import stat
import subprocess
from optparse import OptionParser
from experiments.run_experiments import DPExpParams
from glob import glob
from experiments.core.util import get_all_following, get_following, get_time,\
    to_str
    
def scrape(top_dir):
    sep = ","
    exp_dirs = [os.path.join(top_dir,f) for f in os.listdir(top_dir) 
                if os.path.isdir(os.path.join(top_dir, f)) and f != ".svn"]
    print top_dir
    # Read README
    readme = os.path.join(top_dir, "README")
    if os.path.exists(readme):
        lines = open(readme, 'r').readlines()
        lines = " ".join(lines)
        print '"' + lines.replace("\n"," ") + '"'
        
    # Read experiment directories
    exp_list = []
    for exp_dir in sorted(exp_dirs):
        try:
            # Read name
            name = os.path.basename(exp_dir)
            sys.stderr.write("Reading %s\n" % (name))
            
            # Read experiment parameters
            exp = DPExpParams()
            exp_list.append(exp)
            exp.read(os.path.join(exp_dir, "expparams.txt"))
            
            # Read stdout
            stdout_file = os.path.join(exp_dir,"stdout")

            if not os.path.exists(stdout_file):
                continue
            _, _, elapsed = get_time(stdout_file)
            exp.update(elapsed=elapsed)
            
            numWords = int(get_following(stdout_file, "Number of words: ", -1))
            exp.update(numWords=numWords)
            exp.update(accuracy=get_following(stdout_file, "Accuracy: ", -1))
            exp.update(timeRemaining=get_following(stdout_file, "Time remaining: ", -1))
            
            if exp.get("expname") == "corpus-size":
                tot_parse_times = get_all_following(stdout_file, "Tot parse time: ")
                tot_parse_times = map(float, tot_parse_times)
                if len(tot_parse_times) > 1:
                    exp.update(totalParseTimeFirst=tot_parse_times[0])
                    exp.update(totalParseTimeLast=tot_parse_times[-1])
                    exp.update(avgPerWordParseTimeFirst=tot_parse_times[0]/numWords)
                    exp.update(avgPerWordParseTimeLast=tot_parse_times[-1]/numWords)
                
        except Exception, e:
            print sep.join(map(to_str,[exp_dir,"ERROR"]))
            import traceback
            sys.stderr.write(str(e) + '\n')
            traceback.print_exc()
    
    # Choose column header order
    exp_orderer = DPExpParams()
    for exp in exp_list:
        exp_orderer = exp_orderer.concat(exp)
    exp_orderer.get_initial_keys = lambda : "dataset parser model formulation accuracy elapsed iterations timeRemaining".split() 
    key_order = exp_orderer.get_name_key_order()

    # Print exp_list
    def csv_to_str(x):
        x = exp_orderer._get_as_str(x)
        if x.find(",") != -1:
            x = '"%s"' % x
        return x
    print sep.join(map(csv_to_str, key_order))
    for exp in exp_list:
        values = []
        for key in key_order:
            values.append(exp.get(key))
        print sep.join(map(csv_to_str, values))
    print ""

if __name__ == "__main__":
    usage = "%s [top_dir...]" % (sys.argv[0])

    parser = OptionParser(usage=usage)
    #parser.add_option('-s', '--speedup', action="store_true", help="Scrape for speedup plot")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) < 2:
        print usage
        sys.exit(1)
    
    for top_dir in args[1:]:
        scrape(top_dir)
