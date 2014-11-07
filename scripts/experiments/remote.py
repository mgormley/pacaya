#!/usr/bin/env python

import sys
from pypipeline.remote_prep import init, prep_project_mvn, prep_project_py, run_command

def remote_command(argv):
    init()
    prep_project_mvn("prim", "install", True)
    #prep_project_mvn("erma", "install", True)
    prep_project_mvn("optimize", "install", True)
    prep_project_py("pypipeline", True)
    prep_project_mvn("pacaya", "compile", False)
    run_command("pacaya", argv)
    
if __name__ == "__main__":
    remote_command(sys.argv)
