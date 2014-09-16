#!/usr/bin/env python

import re
import sys
import os
import getopt
import math
import tempfile
import stat
import subprocess
from optparse import OptionParser
from experiments.run_ace import ReExpParams
from glob import glob
from pypipeline.util import get_all_following, get_following, get_time, get_following_literal,\
    to_str, to_int, get_group1, head
from pypipeline.scrape import Scraper
from pypipeline.util import tail
import shlex
from pypipeline import scrape

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;

class ReScraper(Scraper):
    
    def __init__(self, options):
        Scraper.__init__(self, options)
        self.root_dir = os.path.abspath(get_root_dir())

    def get_exp_params_instance(self):
        return ReExpParams()
    
    def get_column_order(self, exp_list):
        hs = ""
        hs += " dataset trainDataset testDataset maxNumSentences maxSentenceLength propTrain "
        hs += " elapsed "
        hs += " trainPrecision trainRecall trainF1 "
        hs += " devPrecision devRecall devF1 "
        hs += " testPrecision testRecall testF1 "
        hs += " trainNumSentences trainNumInstances trainTruePositives "
        hs += " devNumSentences devNumInstances devTruePositives "
        hs += " testNumSentences testNumInstances testTruePositives "
        hs += " trainNumFeatures trainNumLabels "
        return hs.split()
    
    def scrape_exp(self, exp, exp_dir, stdout_file):
        if not os.path.exists(stdout_file):
            return
        
        stdout_lines = self.read_stdout_lines(stdout_file)
        
        _, _, elapsed = get_time(stdout_lines)
        exp.update(elapsed = elapsed)
        ## exp.update(hours = to_int(get_group1(stdout_lines, "^(\d+) \[main\]", -1)) / 1000.0 / 60.0 / 60.0)

        # Get stats about train/dev/test datasets.
        exp.update(trainNumSentences = to_int(get_following_literal(stdout_lines, "Number of train sentences: ", -1)))
        exp.update(testNumSentences = to_int(get_following_literal(stdout_lines, "Number of test sentences: ", -1)))
        
        exp.update(trainNumInstances = to_int(get_following_literal(stdout_lines, "Number of train instances: ", -1)))
        exp.update(devNumInstances = to_int(get_following_literal(stdout_lines, "Number of dev instances: ", -1)))
        exp.update(testNumInstances = to_int(get_following_literal(stdout_lines, "Number of test instances: ", -1)))

        exp.update(trainNumFeatures = to_int(get_following_literal(stdout_lines, "Number of train features after thresholding: ", -1)))
        exp.update(trainNumLabels = to_int(get_following_literal(stdout_lines, "Number of train labels: ", -1)))
        
        # Get train/dev/test results.
        exp.update(trainTruePositives = to_int(get_following_literal(stdout_lines, "Num true positives on train: ", -1)))
        exp.update(trainPrecision = get_following_literal(stdout_lines, "Precision on train: ", -1))
        exp.update(trainRecall = get_following_literal(stdout_lines, "Recall on train: ", -1))
        exp.update(trainF1 = get_following_literal(stdout_lines, "F1 on train: ", -1))
        
        exp.update(devTruePositives = to_int(get_following_literal(stdout_lines, "Num true positives on dev: ", -1)))
        exp.update(devPrecision = get_following_literal(stdout_lines, "Precision on dev: ", -1))
        exp.update(devRecall = get_following_literal(stdout_lines, "Recall on dev: ", -1))
        exp.update(devF1 = get_following_literal(stdout_lines, "F1 on dev: ", -1))
                
        exp.update(testTruePositives = to_int(get_following_literal(stdout_lines, "Num true positives on test: ", -1)))
        exp.update(testPrecision = get_following_literal(stdout_lines, "Precision on test: ", -1))
        exp.update(testRecall = get_following_literal(stdout_lines, "Recall on test: ", -1))
        exp.update(testF1 = get_following_literal(stdout_lines, "F1 on test: ", -1))

        
if __name__ == "__main__":
    usage = "%prog [top_dir...]"

    parser = OptionParser(usage=usage)
    scrape.add_options(parser)
    (options, args) = parser.parse_args(sys.argv)

    if len(args) < 2:
        parser.print_help()
        sys.exit(1)
    
    scraper = ReScraper(options)
    for top_dir in args[1:]:
        scraper.scrape(top_dir)
