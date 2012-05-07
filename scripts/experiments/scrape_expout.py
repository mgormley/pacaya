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
    to_str, to_int
from experiments.core.scrape import Scraper

class DPScraper(Scraper):
    
    def __init__(self, options):
        Scraper.__init__(self, options.csv, options.google, options.remain, options.rproj)
    
    def get_exp_params_instance(self):
        return DPExpParams()
    
    def get_column_order(self):
        hs = "dataset maxNumSentences maxSentenceLength parser model formulation"
        hs += " deltaGenerator factor interval numPerSide"
        hs += " accuracy elapsed error iterations timeRemaining"
        return hs.split()
    
    def scrape_exp(self, exp, exp_dir, stdout_file):
        if not os.path.exists(stdout_file):
            return
        
        stdout_lines = self.read_stdout_lines(stdout_file)
        
        _, _, elapsed = get_time(stdout_lines)
        exp.update(elapsed = elapsed)
        
        numWords = to_int(get_following(stdout_lines, "Number of tokens: ", -1))
        exp.update(numWords = numWords)
        
        if "relaxOnly" in exp.keys():
            exp.update(accuracy = get_following(stdout_lines, "relaxBound: ", -1))
            exp.update(accuracy = get_following(stdout_lines, "relative: ", -1))
        else:
            exp.update(accuracy = get_following(stdout_lines, "Accuracy: ", -1))
            exp.update(logLikelihood = get_following(stdout_lines, "LogLikelihood: ", -1))
            exp.update(timeRemaining = get_following(stdout_lines, "Time remaining: ", -1))
        
        if exp.get("expname") == "corpus-size":
            tot_parse_times = get_all_following(stdout_lines, "Tot parse time: ")
            tot_parse_times = map(float, tot_parse_times)
            if len(tot_parse_times) > 1:
                exp.update(totalParseTimeFirst = tot_parse_times[0])
                exp.update(totalParseTimeLast = tot_parse_times[-1])
                exp.update(avgPerWordParseTimeFirst = tot_parse_times[0]/numWords)
                exp.update(avgPerWordParseTimeLast = tot_parse_times[-1]/numWords)
            elif len(tot_parse_times) > 0:
                exp.update(totalParseTime = tot_parse_times[0])
                exp.update(avgPerWordParseTime = tot_parse_times[0]/numWords)
         
if __name__ == "__main__":
    usage = "%prog [top_dir...]"

    parser = OptionParser(usage=usage)
    parser.add_option('--remain', action="store_true", help="Scrape for time remaining only")
    parser.add_option('--rproj', action="store_true", help="Print out for R-project")
    parser.add_option('--csv', action="store_true", help="Print out for CSV")
    parser.add_option('--google', action="store_true", help="Print out for Google Docs")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) < 2:
        parser.print_help()
        sys.exit(1)
    
    scraper = DPScraper(options)
    for top_dir in args[1:]:
        scraper.scrape(top_dir)
