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
    to_str, to_int, get_group1, head, get_all_matches
from experiments.core.scrape import Scraper, RprojResultsWriter,\
    GoogleResultsWriter, CsvResultsWriter
from experiments.core.util import tail
from random import sample


_re_stat_elem = re.compile('\S+=(\S+)')
_re_logging_time = re.compile('^(\d+)\s')


class BnbStatus(DPExpParams):
        
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
        
        
def get_bnb_status_list(stdout_lines):
    '''Gets a list of BnbStatus objects from summary lines in stdout''' 
    status_list = get_all_following(stdout_lines, ".*LazyBranchAndBoundSolver  - Summary: ", True)
    if status_list == None:
        return None
    # Downsample the summaries if there are too many
    if len(status_list) > 500:
        status_list = sample(status_list, 500)
    return map(lambda x: BnbStatus(x), status_list)

    
def get_incumbent_status_list(stdout_lines):
    '''Gets a list of incumbent statuses from summary lines in stdout''' 
    ll_list = get_all_matches(stdout_lines, "(\d+).*Incumbent logLikelihood: (.*)")
    acc_list = get_all_matches(stdout_lines, "(\d+).*Incumbent accuracy: (.*)")
    if ll_list == None or acc_list == None:
        return None
    assert len(ll_list) == len(acc_list)
    
    # Use the timestamp from the log-likelihood.
    # TODO: depending on the timestamp from the Logger in this way is very brittle.
    status_list = []
    for ll_match, acc_match in zip(ll_list, acc_list):
        ll_time_ms = int(ll_match.group(1))
        ll = float(ll_match.group(2))
        acc_time_ms = int(acc_match.group(1))
        acc = float(acc_match.group(2))
        
        # Double check that the times are at least close (within 3 sec).
        assert abs(ll_time_ms - acc_time_ms) < 3000
        
        status_list.append(DPExpParams(time=ll_time_ms, 
                                       incumbentLogLikelihood=ll, 
                                       incumbentAccuracy=acc))
    return status_list


class DpSingleScraper(Scraper):
    
    def __init__(self, options):
        Scraper.__init__(self, options.csv, options.google, options.remain, options.rproj)
        self.type = options.type

    def get_exp_params_instance(self):
        return DPExpParams()
    
    def get_column_order(self):
        hs = "dataset maxNumSentences maxSentenceLength parser model formulation"
        hs += " deltaGenerator factor interval numPerSide"
        hs += " accuracy elapsed error iterations timeRemaining"
        return hs.split()
    
    def scrape_exp_statuses(self, exp, exp_dir, stdout_file):
        stdout_lines = self.read_stdout_lines(stdout_file)
        
        # Get the status list of the appropriate type.
        status_list = None
        if self.type == "incumbent":
            status_list = get_incumbent_status_list(stdout_lines)
        elif self.type == "bnb":
            status_list = get_bnb_status_list(stdout_lines)

        # Combine the status objects with the experiment definition. 
        return [exp + status for status in status_list]


if __name__ == "__main__":
    usage = "%prog [top_dir...]"

    parser = OptionParser(usage=usage)
    parser.add_option('--remain', action="store_true", help="Scrape for time remaining only")
    parser.add_option('--rproj', action="store_true", help="Print out for R-project")
    parser.add_option('--csv', action="store_true", help="Print out for CSV")
    parser.add_option('--google', action="store_true", help="Print out for Google Docs")
    parser.add_option('--type', help="The type of status info to scrape [bnb, incumbent]")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) < 2 or options.type is None:
        parser.print_help()
        sys.exit(1)
    
    exp_dirs = args[1:]
    scraper = DpSingleScraper(options)
    scraper.scrape_exp_dirs(exp_dirs)
