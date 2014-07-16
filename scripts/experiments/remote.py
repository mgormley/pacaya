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
from glob import glob
from pypipeline.util import get_all_following, get_following, get_time, get_following_literal,\
    to_str, to_int, get_group1, head, get_match
import shlex

from fabric.state import env
from fabric.api import local, settings, abort, run, cd, lcd
from fabric.contrib.console import confirm
from fabric.context_managers import prefix

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;


def prep_project_git(name, install_cmd, check_local=True):
    with lcd("~/research/%s" % (name)):
        local("git push")
        if "nothing to commit, working directory clean" not in local("git status", capture=True):
            print "\nERROR: project requires git commit/git push: %s\n" % name
            if check_local:
                sys.exit(1)    
    with cd("~/working/%s" % (name)):
        if "Already up-to-date" not in run("git pull"):
            run(install_cmd)

def prep_project_mvn(name, mvn_cmd, check_local=True):
    install_cmd = "mvn %s -DskipTests" % (mvn_cmd)
    prep_project_git(name, install_cmd, check_local)

def prep_project_py(name, check_local=True):
    install_cmd = "python setup.py develop --user"
    prep_project_git(name, install_cmd, check_local)

def run_command(name, argv):
    args = " ".join(argv[1:])
    with cd("~/working/%s" % (name)):
        with prefix("source setupenv.sh"):
            run("%s" % (args))
        
def remote_command(argv):
    env.gateway = "%s:%s" % ("external.hltcoe.jhu.edu", "22")
    env.host_string = "%s:%s" % ("test2", "22")
    run("uname -a")
    prep_project_mvn("prim", "install", True)
    #prep_project_mvn("erma", "install", True)
    prep_project_mvn("optimize", "install", True)
    prep_project_py("pypipeline", True)
    prep_project_mvn("pacaya", "compile", False)
    run_command("pacaya", argv)
    
if __name__ == "__main__":
    usage = "%prog [top_dir...]"

    #parser = OptionParser(usage=usage)
    #scrape.add_options(parser)
    #(options, args) = parser.parse_args(sys.argv)
    
    remote_command(sys.argv)
