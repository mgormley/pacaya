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
    assert argmax_key not in train_keys
    for key in hyperparam_keys: assert key in train_keys
    
    # Convert to a DataFrame
    df = exp_list_to_df(exp_list)
    
    # Group by the experiment parameters excluding the hyperparameters.
    group_keys = list(set(train_keys) - set(hyperparam_keys))
    grp = df.groupby(group_keys)

    # Argmax over argmax_key
    df = df.ix[grp[argmax_key].idxmax(),:]

    return df_to_exp_list(df)

class HyperParamArgmaxScraper(Scraper):
    
    def __init__(self, options):
        Scraper.__init__(self, options)
        self.options = options

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
