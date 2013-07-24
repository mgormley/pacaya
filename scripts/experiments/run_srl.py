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
from experiments.core.util import get_new_file, sweep_mult, fancify_cmd, frange
from experiments.core.util import head_sentences
import platform
from glob import glob
from experiments.core.experiment_runner import ExpParamsRunner, get_subset
from experiments.core import experiment_runner
from experiments.core import pipeline
import re
import random
from experiments.core.pipeline import write_script, RootStage, Stage

def get_root_dir():
    scripts_dir =  os.path.abspath(sys.path[0])
    root_dir =  os.path.dirname(os.path.dirname(scripts_dir))
    print "Using root_dir: " + root_dir
    return root_dir;

class SrlExpParams(experiment_runner.JavaExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.JavaExpParams.__init__(self,keywords)
            
    def get_initial_keys(self):
        return "dataset".split()
    
    def get_instance(self):
        return SrlExpParams()
    
    def create_experiment_script(self, exp_dir):
        script = ""
        script += "echo 'CLASSPATH=$CLASSPATH'\n"
        cmd = "java " + self.get_java_args() + " edu.jhu.srl.SrlRunner  %s \n" % (self.get_args())
        script += fancify_cmd(cmd)
        return script
    

class SrlExpParamsRunner(ExpParamsRunner):
    
    def __init__(self, options):
        ExpParamsRunner.__init__(self, options.expname, options.queue, print_to_console=True)
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
        self.expname = options.expname
        
        if self.queue and not self.queue == "mem":
            print "WARN: Are you sure you don't want the mem queue?"
            
    def get_experiments(self):
        all = SrlExpParams()
        all.set("expname", self.expname, False, False)
        all.update(seed=random.getrandbits(63))
        all.update(printModel="./model.txt",
                   trainPredOut="./train-srl.txt",
                   testPredOut="./test-srl.txt",
                   modelOut="./model.binary.gz"
                   )
        all.set("timeoutSeconds", 8*60*60, incl_arg=False, incl_name=False)
        all.set("work_mem_megs", 1*1024, incl_arg=False, incl_name=False)
        all.set("dataset", "", incl_arg=False)
        all.set("train", "", incl_name=False)
        all.set("test", "", incl_name=False)
        
        datasets_train = {}
        datasets_test = {}

            
        exp_dir = "/home/hltcoe/mgormley/working/parsing/exp"
        if not os.path.exists(exp_dir):
            exp_dir = "/Users/mgormley/research/parsing/exp"
            
        # ------------- Sentences of length <= 20 ---------------
        # --- Gold POS tags output of grammar induction ---
        prefix = exp_dir + "/vem-conll_001"
            
        # Gold trees: HEAD column.
        datasets_train['pos-gold'] = prefix + "/dmv_conll09-sp-dev_20_28800_True/train-parses.txt"
        datasets_test['pos-gold'] = prefix + "/dmv_conll09-sp-dev_20_28800_True/test-parses.txt"
        
        # Supervised parser output: PHEAD column.
        datasets_train['pos-sup'] = prefix + "/dmv_conll09-sp-dev_20_28800_True_SUPERVISED/train-parses.txt"
        datasets_test['pos-sup'] = prefix + "/dmv_conll09-sp-dev_20_28800_True_SUPERVISED/test-parses.txt"
        
        # Semi-supervised parser output: PHEAD column.
        datasets_train['pos-semi'] = prefix + "/dmv_conll09-sp-dev_20_28800_True/train-parses.txt"
        datasets_test['pos-semi'] = prefix + "/dmv_conll09-sp-dev_20_28800_True/test-parses.txt"
        
        # Unsupervised parser output: PHEAD column.
        datasets_train['pos-unsup'] = prefix + "/dmv_conll09-sp-dev_20_28800_False/train-parses.txt"
        datasets_test['pos-unsup'] = prefix + "/dmv_conll09-sp-dev_20_28800_False/test-parses.txt"
        
        # --- Brown cluster tagged output of grammar induction: ---
        prefix = "/home/hltcoe/mgormley/working/parsing/exp/vem-conll_002"

        # Semi-supervised parser output: PHEAD column.
        datasets_train['brown-semi'] = prefix + "/dmv_conll09-sp-dev_20_28800_True/train-parses.txt"
        datasets_test['brown-semi'] = prefix + "/dmv_conll09-sp-dev_20_28800_True/test-parses.txt"
        
        # Unsupervised parser output: PHEAD column.
        datasets_train['brown-unsup'] = prefix + "/dmv_conll09-sp-dev_20_28800_False/train-parses.txt"
        datasets_test['brown-unsup'] = prefix + "/dmv_conll09-sp-dev_20_28800_False/test-parses.txt"
        
        if self.expname == "srl-dev20":
            root = RootStage()
            setup = SrlExpParams()
            # Full length test sentences.
            setup.update(maxNumSentences=100000000, maxSentenceLength=1000)
            setup.update(timeoutSeconds=48*60*60,
                         work_mem_megs=180*1024)
            exps = []
            for dataset in datasets_train:                        
                train_file = datasets_train[dataset]
                test_file = datasets_train[dataset]
                data = SrlExpParams(dataset=dataset, 
                                    train=train_file, trainType='CONLL_2009', 
                                    test=test_file, testType='CONLL_2009')
#                for roleStructure in ['ALL_PAIRS', 'PREDS_GIVEN']:
#                    setup.update(roleStructure=roleStructure)
#                    for makeUnknownPredRolesLatent in [True, False]:
#                        setup.update(makeUnknownPredRolesLatent=makeUnknownPredRolesLatent)
                for useProjDepTreeFactor in [True, False]:
                    setup.update(useProjDepTreeFactor=useProjDepTreeFactor)
                    exp = all + setup + data 
                    exps.append(exp)
            # Drop all but 3 experiments for a fast run.
            if self.fast: exps = exps[:4]
            root.add_dependents(exps)
            return root
        else:
            raise Exception("Unknown expname: " + str(self.expname))
                

    def updateStagesForQsub(self, root_stage):
        '''Makes sure that the stage object specifies reasonable values for the 
        qsub parameters given its experimental parameters.
        '''
        for stage in self.get_stages_as_list(root_stage):
            # First make sure that the "fast" setting is actually fast.
            if isinstance(stage, SrlExpParams) and self.fast:
                stage.update(maxLbfgsIterations=3,
                             maxSentenceLength=7,
                             maxNumSentences=3,
                             work_mem_megs=1000,
                             timeoutSeconds=20)
            if isinstance(stage, experiment_runner.ExpParams):
                # Update the thread count
                threads = stage.get("threads")
                if threads != None: 
                    # Add an extra thread just as a precaution.
                    stage.threads = threads + 1
                work_mem_megs = stage.get("work_mem_megs")
                if work_mem_megs != None:
                    stage.work_mem_megs = work_mem_megs
                # Update the runtime
                timeoutSeconds = stage.get("timeoutSeconds")
                if timeoutSeconds != None:
                    stage.minutes = (timeoutSeconds / 60.0)
                    # Add some extra time in case some other part of the experiment
                    # (e.g. evaluation) takes excessively long.
                    stage.minutes = (stage.minutes * 2.0) + 10
                    
        return root_stage

if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('-q', '--queue', help="Which SGE queue to use")
    parser.add_option('-f', '--fast', action="store_true", help="Run a fast version")
    parser.add_option('--test', action="store_true", help="Use test data")
    parser.add_option('--expname',  help="Experiment name")
    # DISABLED: parser.add_option('--hprof',  help="What type of profiling to use [cpu, heap]")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    runner = SrlExpParamsRunner(options)
    root_stage = runner.get_experiments()
    root_stage = runner.updateStagesForQsub(root_stage)
    runner.run_pipeline(root_stage)


