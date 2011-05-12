#!/usr/bin/python

import sys
import os
import getopt
import math
import tempfile
import stat
import shlex
import subprocess
from subprocess import Popen
from optparse import OptionParser
from experiments.core.pipeline import ExperimentRunner
from experiments.core.util import get_new_file, sweep_mult, fancify_cmd
from experiments.core.util import head_sentences
import platform
from glob import glob
from experiments.core.experiment_runner import ExpParamsRunner
from experiments.core import experiment_runner

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;

def get_dev_data(data_dir, file_prefix, name=None):
    return get_some_data(data_dir, file_prefix, name, "dev")

def get_test_data(data_dir, file_prefix, name=None):
    return get_some_data(data_dir, file_prefix, name, "test")

def get_some_data(data_dir, file_prefix, name, test_suffix): 
    data = DPExpParams()
    if name == None:
        name = file_prefix.replace("/", "-")
    data.set("dataset", name,True,False)
    data.set("train","%s/%s" % (data_dir, file_prefix),False,True)
    #data.set("dev","%s/%s.%s" % (data_dir, file_prefix, test_suffix),False,True)
    return data

class DPExpParams(experiment_runner.ExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.ExpParams.__init__(self,keywords)
        
    def get_name_key_order(self):
        key_order = []
        initial_keys = self.get_initial_keys()
        all_keys = sorted(self.params.keys())
        for key in initial_keys:
            if key in all_keys:
                key_order.append(key)
        for key in all_keys:
            if key not in initial_keys:
                key_order.append(key)
        return key_order
    
    def get_initial_keys(self):
        return "dataSet model k s".split()
    
    def get_instance(self):
        return DPExpParams()
    
    def create_experiment_script(self, exp_dir, eprunner):
        script = ""
        script += "export CLASSPATH=%s/classes:%s/lib/*\n" % (eprunner.root_dir, eprunner.root_dir)
        cmd = "java -cp $CLASSPATH " + eprunner.java_args + " edu.jhu.hltcoe.PipelineRunner  %s \n" % (self.get_args())
        script += fancify_cmd(cmd)
        return script

class DepParseExpParamsRunner(ExpParamsRunner):
    
    def __init__(self, options):
        ExpParamsRunner.__init__(self, "dp", options.queue)
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
        self.expname = options.expname
        if options.test:
            self.get_data = get_test_data
        else:
            self.get_data = get_dev_data
            
    def get_experiments(self):
        all = DPExpParams()
        all.set("expname", self.expname, False, False)
        all.update(formulation="deptree-flow-nonproj",
                   parser="ilp-corpus",
                   model="dmv",
                   algorithm="viterbi")
        
        if self.fast:       all.update(iterations=1,
                                       maxSentenceLength=10,
                                       maxNumSentences=2)
        else:               all.update(iterations=10)
        
        # Data sets
        data_dir = os.path.join(self.root_dir, "data")
        wsj_00 = self.get_data(data_dir, "treebank_3/wsj/00")
            
        if self.fast:       datasets = [wsj_00]
        else:               datasets = [wsj_00]
        
        experiments = []
        if self.expname == "formulations":
            for dataset in datasets:
                formulations = ["deptree-dp-proj", "deptree-explicit-proj", "deptree-flow-nonproj", "deptree-flow-proj", "deptree-multiflow-nonproj", "deptree-multiflow-proj" ]
                for formulation in formulations:
                    ilpform = DPExpParams(formulation=formulation)
                    experiments.append(all + dataset + ilpform)
        elif self.expname == "corpus-size":
            if not self.fast:
                all.update(iterations=2,
                           maxSentenceLength=7)
            for dataset in datasets:
                for maxNumSentences in range(10,110,10):
                    mns = DPExpParams(maxNumSentences=maxNumSentences)
                    experiments.append(all + dataset + mns)
        else:
            raise Exception("Unknown expname: " + self.expname)
                
        return experiments

    def create_post_processing_script(self, top_dir, exp_tuples):
        #return "python %s/scripts/experiments/pp_scrape_expout.py %s" % (self.tagging_dir, top_dir)
        return None;

  
if __name__ == "__main__":
    usage = "%s " % (sys.argv[0])

    parser = OptionParser(usage=usage)
    parser.add_option('-q', '--queue', action="store_true", help="Which SGE queue to use")
    parser.add_option('-f', '--fast', action="store_true", help="Run a fast version")
    parser.add_option('--test', action="store_true", help="Use test data")
    parser.add_option('--expname',  help="Experiment name")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        print usage
        sys.exit(1)
    
    runner = DepParseExpParamsRunner(options)
    experiments = runner.get_experiments()
    runner.run_experiments(experiments)


