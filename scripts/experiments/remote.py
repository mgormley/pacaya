#!/opt/local/bin/python
from __future__ import with_statement

import re
import sys
import os
import getopt
import math
import tempfile
import stat
import subprocess
from optparse import OptionParser
from experiments.run_srl import SrlExpParams
from glob import glob
from experiments.core.util import get_all_following, get_following, get_time, get_following_literal,\
    to_str, to_int, get_group1, head, get_match
from experiments.core.scrape import Scraper
from experiments.core.util import tail
from experiments import scrape_statuses
import shlex
from experiments.core import scrape

from fabric.state import env
from fabric.api import local, settings, abort, run, cd, lcd
from fabric.contrib.console import confirm
from fabric.context_managers import prefix

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;

def prep_project(name, mvn_command, check_local=True):
    with lcd("~/research/%s" % (name)):
        local("git push")
        if "nothing to commit, working directory clean" not in local("git status", capture=True):
            print "\nERROR: prject requires git commit/git push: %s\n" % name
            if check_local:
                sys.exit(1)
    with cd("~/working/%s" % (name)):
        if "Already up-to-date" not in run("git pull"):
            run("mvn %s -DskipTests" % (mvn_command))

def run_srl(name, argv):
    args = " ".join(argv[1:])
    with cd("~/working/%s" % (name)):
        with prefix("source setupenv.sh"):
            run("%s" % (args))
        
def remote_srl(argv):
    env.gateway = "%s:%s" % ("external.hltcoe.jhu.edu", "22")
    env.host_string = "%s:%s" % ("test4", "22")
    run("uname -a")
    prep_project("prim", "install", True)
    #prep_project("erma", "install", True)
    prep_project("parsing", "compile", False)
    run_srl("parsing", argv)
    
if __name__ == "__main__":
    usage = "%prog [top_dir...]"

    #parser = OptionParser(usage=usage)
    #scrape.add_options(parser)
    #(options, args) = parser.parse_args(sys.argv)
    
    remote_srl(sys.argv)
