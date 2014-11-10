#!/usr/bin/env python

import re
import sys
import os
import getopt
import math
import tempfile
import stat
import subprocess
import optparse
from glob import glob
from pypipeline.util import get_all_following, get_following, get_time, get_following_literal,\
    to_str, to_int, get_group1, head
from pypipeline.scrape import Scraper
from pypipeline.util import tail
import shlex
from pypipeline import scrape
from pypipeline import experiment_runner
from pypipeline.experiment_runner import get_all_keys

import pandas as pd

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;

def exp_to_series(exp):
    '''Converts an ExpParams object to a Pandas Series.'''
    return pd.Series(exp.params)

def series_to_exp(s):
    '''Converts a Panda Series object to an ExpParams.'''
    return experiment_runner.ExpParams(s.to_dict())

def exp_list_to_df(exp_list):
    '''Converts a list of ExpParams objects to a Pandas DataFrame.'''
    return pd.DataFrame(map(lambda exp: pd.Series(exp.params), exp_list))

def df_to_exp_list(df):
    return [experiment_runner.ExpParams(row.to_dict()) for _, row in df.iterrows()]

def hyperparam_argmax(exp_list, train_keys, hyperparam_keys, argmax_key):
    '''Groups by all train_keys except for the hyperparam_keys. 
    Then takes the argmax of each group over argmax_key.
    '''    

    # Check the keys for correctness
    all_keys = get_all_keys(exp_list)
    assert argmax_key not in train_keys
    assert argmax_key in all_keys
    for key in hyperparam_keys: assert key in train_keys

    # Convert to a DataFrame
    df = exp_list_to_df(exp_list)

    # Fill train columns with NA string.
    for key in all_keys:
        df[key].fillna("MISSING", inplace=True)
        if key != argmax_key:
            df[key] = df[key].astype(str)
    # Group by the experiment parameters excluding the hyperparameters.
    group_keys = list(set(train_keys) - set(hyperparam_keys))
    grp = df.groupby(group_keys)
    
    # Argmax over argmax_key
    dfam = df.ix[grp[argmax_key].idxmax(),:]

    print "Groups:"
    for g in grp.groups:
        print "\t",str(g)
    print "Train keys:" 
    for key in train_keys: print "\t",key
    print "Group keys:" 
    for key in group_keys: print "\t",key
    print "Group keys with unique values:"
    for key in group_keys:
        if len(df[key]) == len(set(df[key])):
            print "\t",key
    print "Hyperparam keys:"
    for key in hyperparam_keys: print "\t",key
    print "Argmax key:"
    print "\t",argmax_key
    print "Num exps = %d Num groups = %d Num in df = %d" % (len(exp_list), len(grp.groups), len(dfam))

    #import ipdb; ipdb.set_trace() # drop into an IPython session
    #from IPython import embed; embed() # drop into an IPython session.
    
    return df_to_exp_list(dfam)

class HyperParamArgmaxScraper(Scraper):
    
    def __init__(self, options):
        Scraper.__init__(self, options)
        self.options = options

    def scrape_exp(self, exp, exp_dir, stdout_file):
        '''TODO: Remove this method.'''
        if not os.path.exists(stdout_file):
            return
        
        stdout_lines = self.read_stdout_lines(stdout_file)
        
        _, _, elapsed = get_time(stdout_lines)
        exp.update(elapsed = elapsed)
        ## exp.update(hours = to_int(get_group1(stdout_lines, "^(\d+) \[main\]", -1)) / 1000.0 / 60.0 / 60.0)

        # Get stats about train/dev/test datasets.
        exp.update(trainNumSentences = to_int(get_following_literal(stdout_lines, "Num train sentences: ", -1)))
        exp.update(devNumSentences = to_int(get_following_literal(stdout_lines, "Num dev sentences: ", -1)))
        exp.update(testNumSentences = to_int(get_following_literal(stdout_lines, "Num test sentences: ", -1)))
        
        exp.update(trainNumInstances = to_int(get_following_literal(stdout_lines, "Num train instances: ", -1)))
        exp.update(devNumInstances = to_int(get_following_literal(stdout_lines, "Num dev instances: ", -1)))
        exp.update(testNumInstances = to_int(get_following_literal(stdout_lines, "Num test instances: ", -1)))

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

    def process_all(self, orig_list, exp_list):
        train_keys = set(get_all_keys(orig_list)) - set()
        hyperparam_keys = options.hyperparam_keys.split(",")
        return hyperparam_argmax(exp_list, train_keys, hyperparam_keys, options.argmax_key)
                
if __name__ == "__main__":
    usage = "%prog [top_dir...]"

    parser = optparse.OptionParser(usage=usage)
    scrape.add_options(parser)
    parser.add_option('--hyperparam_keys', help="Comma separated list of hyperparameter keys")
    parser.add_option('--argmax_key', help="Key over which to argmax")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) < 2:
        parser.print_help()
        sys.exit(1)
    
    scraper = HyperParamArgmaxScraper(options)
    for top_dir in args[1:]:
        scraper.scrape(top_dir)
