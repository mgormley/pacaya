#!/usr/local/bin/python

import re
import sys
import os
import getopt
import math
import tempfile
import stat
import subprocess
import string
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

        stdout_file_bak = stdout_file + ".bak"
        os.system("cp %s %s" % (stdout_file, stdout_file_bak))
        stdout_lines = self.read_stdout_lines(stdout_file_bak)
        
        ilist = range(0,len(stdout_lines))
        ilist.reverse()
        for i in ilist:
            if stdout_lines[i].find("PipelineRunner  - relaxBound:") != -1:
                line = stdout_lines[i-1]
                line = line.replace("edu.jhu.PipelineRunner  - ", "edu.jhu.PipelineRunner  - relaxTime(ms): ")
                stdout_lines[i-1] = line
                break

        out = open(stdout_file, "w")
        for line in stdout_lines:
            out.write(line)
        out.close()
         
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
