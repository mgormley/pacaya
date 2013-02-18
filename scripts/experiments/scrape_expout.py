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
from experiments import scrape_statuses
import shlex
from experiments.core import scrape

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;

class DPScraper(Scraper):
    
    def __init__(self, options):
        Scraper.__init__(self, options)
        self.root_dir = os.path.abspath(get_root_dir())

    def get_exp_params_instance(self):
        return DPExpParams()
    
    def get_column_order(self):
        hs = "dataset maxNumSentences maxSentenceLength propSupervised parser model formulation"
        hs += " algorithm varSelection relaxation envelopeOnly rltInitProp rltCutProp"
        hs += " accuracy elapsed error iterations timeRemaining"
        hs += " avgNodeTime estNumNodes estNumNodesStddev estBnbHours estNumSamples"
        hs += " deltaGenerator factor interval numPerSide"
        return hs.split()
    
    def scrape_exp(self, exp, exp_dir, stdout_file):
        if not os.path.exists(stdout_file):
            return
        
        if exp.get("expname") == "bnb-semi" or exp.get("expname") == "bnb-depth-test":
            stdout_lines = head(stdout_file, window=500)
            stdout_lines += tail(stdout_file, window=500)
        else:
            stdout_lines = self.read_stdout_lines(stdout_file)
        
        _, _, elapsed = get_time(stdout_lines)
        exp.update(elapsed = elapsed)
        exp.update(hours = to_int(get_group1(stdout_lines, "^(\d+) +[A-Z][A-Z]+", -1)) / 1000.0 / 60.0 / 60.0)
        
        numWords = to_int(get_following_literal(stdout_lines, "Number of train tokens: ", -1))
        exp.update(numWords = numWords)

        if "relaxOnly" in exp.keys():
            exp.update(relaxTime = get_following_literal(stdout_lines, "relaxTime(ms): ", -1))
            exp.update(relaxBound = get_following_literal(stdout_lines, "relaxBound: ", -1))
            exp.update(relative = get_following_literal(stdout_lines, "relative: ", -1))
            exp.update(projLogLikelihood = get_following_literal(stdout_lines, "projLogLikelihood: ", -1))
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
            exp.update(avgNodeTime = get_following_literal(stdout_lines, "Avg time(ms) per node: ", -1))
            exp.update(avgRelaxTime = get_following_literal(stdout_lines, "Avg relax time(ms) per node: ", -1))
        if exp.get("algorithm").find("bnb-rand-walk") != -1:
            exp.update(estNumNodes = get_following_literal(stdout_lines, "Node count estimate mean: ", -1))
            exp.update(estNumNodesStddev = get_following_literal(stdout_lines, "Node count estimate stddev: ", -1))
            exp.update(estBnbHours = get_following_literal(stdout_lines, "Solution time (ms) estimate mean: ", -1))
            exp.update(estBnbHoursStddev = get_following_literal(stdout_lines, "Solution time (ms) estimate stddev: ", -1))  
            exp.update(estNumSamples = get_following_literal(stdout_lines, "Num samples for estimates: ", -1))
             
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
        if exp.get("disableFathoming") == "True" and exp.get("expname") != "viterbi-vs-bnb":
            # Scrape stdout to create a curnode_status.data file.
            status_file = os.path.join(exp_dir, "curnode-status.data")
            if not os.path.exists(status_file):
                _, options, _ = scrape_statuses.parse_options([])
                options.type = "curnode"
                options.tsv_file = status_file
                exp_dirs = [exp_dir]
                print "Creating status file", options, exp_dirs
                scraper = scrape_statuses.DpSingleScraper(options)
                scraper.scrape_exp_dirs(exp_dirs)
            # Run bnb-time-estimate.R 
            estimate_file = os.path.join(exp_dir, "bnb-time-estimate.out")
            cmd = 'bash -c "Rscript %s/scripts/plot/bnb-time-estimate.R %s &> %s"' % (self.root_dir, status_file, estimate_file)
            print "Running:", cmd
            subprocess.check_call(shlex.split(cmd))
            # Get the estimated time to complete a full run of branch and bound.
            estimate_lines = self.read_stdout_lines(estimate_file)
            exp.update(estNumNodes = get_following_literal(estimate_lines, "Estimated number of nodes: ", -1))
            exp.update(estNumNodesStddev = get_following_literal(estimate_lines, "Standard deviation: ", -1))
            exp.update(estBnbHours = get_following_literal(estimate_lines, "Estimated number of B&B hours: ", -1))
            exp.update(estNumSamples = get_following_literal(estimate_lines, "Number of samples: ", -1))
            
if __name__ == "__main__":
    usage = "%prog [top_dir...]"

    parser = OptionParser(usage=usage)
    scrape.add_options(parser)
    (options, args) = parser.parse_args(sys.argv)

    if len(args) < 2:
        parser.print_help()
        sys.exit(1)
    
    scraper = DPScraper(options)
    for top_dir in args[1:]:
        scraper.scrape(top_dir)
