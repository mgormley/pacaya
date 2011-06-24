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
from experiments.core.scrape import Scraper

class DPScraper(Scraper):
    
    def get_exp_params_instance(self):
        return DPExpParams()
    
    def get_column_order(self):
        hs = "dataset parser model formulation accuracy elapsed iterations timeRemaining"
        return hs.split()
    
    def scrape_exp(self, exp, exp_dir, stdout_file):
        if not os.path.exists(stdout_file):
            return
        
        _, _, elapsed = get_time(stdout_file)
        exp.update(elapsed = elapsed)
        
        numWords = int(get_following(stdout_file, "Number of words: ", -1))
        exp.update(numWords = numWords)
        exp.update(accuracy = get_following(stdout_file, "Accuracy: ", -1))
        exp.update(timeRemaining = get_following(stdout_file, "Time remaining: ", -1))
        
        if exp.get("expname") == "corpus-size":
            tot_parse_times = get_all_following(stdout_file, "Tot parse time: ")
            tot_parse_times = map(float, tot_parse_times)
            if len(tot_parse_times) > 1:
                exp.update(totalParseTimeFirst = tot_parse_times[0])
                exp.update(totalParseTimeLast = tot_parse_times[-1])
                exp.update(avgPerWordParseTimeFirst = tot_parse_times[0]/numWords)
                exp.update(avgPerWordParseTimeLast = tot_parse_times[-1]/numWords)
         
if __name__ == "__main__":
    usage = "%s [top_dir...]" % (sys.argv[0])

    parser = OptionParser(usage=usage)
    #parser.add_option('-s', '--speedup', action="store_true", help="Scrape for speedup plot")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) < 2:
        print usage
        sys.exit(1)
    
    scraper = DPScraper()
    for top_dir in args[1:]:
        scraper.scrape(top_dir)
