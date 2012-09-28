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
from experiments.core.util import get_all_following, get_following, get_time, get_following_literal,\
    to_str, to_int, get_group1, head
from experiments.core.scrape import Scraper
from experiments.core.util import tail

class DPScraper(Scraper):
    
    def __init__(self, options):
        out = options.out if options.out else sys.stdout            
        Scraper.__init__(self, options.csv, options.google, options.remain, options.rproj, out)
    
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
        
        if exp.get("expname") == "bnb-semi":
            stdout_lines = head(stdout_file, window=500)
            stdout_lines += tail(stdout_file, window=500)
        else:
            stdout_lines = self.read_stdout_lines(stdout_file)
        
        _, _, elapsed = get_time(stdout_lines)
        exp.update(elapsed = elapsed)
        exp.update(hours = to_int(get_group1(stdout_lines, "^(\d+) \[main\]", -1)) / 1000.0 / 60.0 / 60.0)
        
        numWords = to_int(get_following_literal(stdout_lines, "Number of train tokens: ", -1))
        exp.update(numWords = numWords)
        
        if "relaxOnly" in exp.keys():
            exp.update(relaxTime = get_following_literal(stdout_lines, "relaxTime(ms): ", -1))
            exp.update(relaxBound = get_following_literal(stdout_lines, "relaxBound: ", -1))
            exp.update(relative = get_following_literal(stdout_lines, "relative: ", -1))
        else:
            exp.update(trainAccuracy = get_following_literal(stdout_lines, "Accuracy on train: ", -1))
            exp.update(trainLogLikelihood = get_following_literal(stdout_lines, "LogLikelihood on train: ", -1))
            exp.update(trainPerTokCrossEnt = get_following_literal(stdout_lines, "Per token cross entropy on train: ", -1))
            exp.update(testAccuracy = get_following_literal(stdout_lines, "Accuracy on test: ", -1))
            exp.update(testLogLikelihood = get_following_literal(stdout_lines, "LogLikelihood on test: ", -1))
            exp.update(testPerTokCrossEnt = get_following_literal(stdout_lines, "Per token cross entropy on test: ", -1))
            exp.update(timeRemaining = get_following_literal(stdout_lines, "Time remaining: ", -1))
        
        if exp.get("algorithm").find("bnb") != -1:
            exp.update(relativeDiff = get_group1(stdout_lines, "relativeDiff=(\S+)", -1))
            exp.update(lowBound = get_group1(stdout_lines, "lowBound=(\S+)", -1))
            exp.update(upBound = get_group1(stdout_lines, "upBound=(\S+)", -1))
            exp.update(numLeaves = get_group1(stdout_lines, "#leaves=(\S+)", -1))
            exp.update(numFathom = get_group1(stdout_lines, "#fathom=(\S+)", -1))
            exp.update(numSeen = get_group1(stdout_lines, "#seen=(\S+)", -1))
            exp.update(propRootSpaceRemain = get_following_literal(stdout_lines, "Proportion of root space remaining: ", -1))
            exp.update(bnbStatus = get_following_literal(stdout_lines, "B&B search status: ", -1))
             
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
    parser.add_option('--out_file', help="Output file [optional, defaults to stdout]")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) < 2:
        parser.print_help()
        sys.exit(1)
    
    scraper = DPScraper(options)
    for top_dir in args[1:]:
        scraper.scrape(top_dir)
