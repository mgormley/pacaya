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
from experiments.core.scrape import Scraper, RprojResultsWriter,\
    GoogleResultsWriter, CsvResultsWriter
from experiments.core.util import tail
from random import sample


_re_stat_elem = re.compile('\S+=(\S+)')
_re_logging_time = re.compile('^(\d+)\s')


class Status(DPExpParams):
        
    def __init__(self, status):
        DPExpParams.__init__(self)
        self.update(time = _re_logging_time.search(status).group(1))
        matches = [x for x in _re_stat_elem.finditer(status)]
        self.update(upBound = float(matches[0].group(1)),
                    lowBound = float(matches[1].group(1)),
                    relativeDiff = float(matches[2].group(1)),
                    numLeaves = int(matches[3].group(1)),
                    numFathom = int(matches[4].group(1)),
                    numPrune = int(matches[5].group(1)),
                    numInfeasible = int(matches[6].group(1)),
                    numSeen = int(matches[7].group(1)))
        
def get_status_list(stdout_lines): 
    status_list = get_all_following(stdout_lines, ".*LazyBranchAndBoundSolver  - Summary: ", True)
    if status_list == None:
        return None
    # Downsample the summaries if there are too many
    if len(status_list) > 500:
        status_list = sample(status_list, 500)
    return map(lambda x: Status(x), status_list)


class DpSingleScraper(Scraper):
    
    def __init__(self, options):
        Scraper.__init__(self, options.csv, options.google, options.remain, options.rproj)

    def get_exp_params_instance(self):
        return DPExpParams()
    
    def get_column_order(self):
        hs = "dataset maxNumSentences maxSentenceLength parser model formulation"
        hs += " deltaGenerator factor interval numPerSide"
        hs += " accuracy elapsed error iterations timeRemaining"
        return hs.split()
    
    def scrape_exp_statuses(self, exp, exp_dir, stdout_file):
        stdout_lines = self.read_stdout_lines(stdout_file)
        return [exp + status for status in get_status_list(stdout_lines)]


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
    
    exp_dirs = args[1:]
    scraper = DpSingleScraper(options)
    scraper.scrape_exp_dirs(exp_dirs)
