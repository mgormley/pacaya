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

class Ptb2ConllX(experiment_runner.JavaExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.JavaExpParams.__init__(self,keywords)
            
    def get_initial_keys(self):
        return "dataSet model k s".split()
    
    def get_instance(self):
        return Ptb2ConllX()
    
    def create_experiment_script(self, exp_dir):
        script = ""
        #script += "export CLASSPATH=%s/classes:%s/lib/*\n" % (self.root_dir, self.root_dir)
        script += "echo 'CLASSPATH=$CLASSPATH'\n"
        cmd = "java -cp $CLASSPATH " + self.get_java_args() + " edu.jhu.PipelineRunner  %s \n" % (self.get_args())
        script += fancify_cmd(cmd)
        return script
    
    def get_java_args(self):
        # Allot the available memory to the JVM, ILP solver, and ZIMPL
        total_work_mem_megs = self.work_mem_megs
        if (self.get("parser").startswith("ilp-")):
            zimpl_mem = int(total_work_mem_megs * 0.5)
        else:
            zimpl_mem = 0
        java_mem = int((total_work_mem_megs - zimpl_mem) * 0.5)
        ilp_mem = total_work_mem_megs - java_mem - zimpl_mem
        # Subtract off some overhead for CPLEX
        ilp_mem -= 1024
        assert (zimpl_mem + java_mem + ilp_mem <= total_work_mem_megs)

        self.update(ilpWorkMemMegs=ilp_mem)
        
        # Create the JVM args
        java_args = self._get_java_args(java_mem)  
        if True: 
            # HACK: revert back to this if-clause after adding real parser for eval: self.get("ilpSolver") == "cplex":  
            mac_jlp = "/Users/mgormley/installed/IBM/ILOG/CPLEX_Studio125/cplex/bin/x86-64_darwin"
            coe_jlp = "/home/hltcoe/mgormley/installed/IBM/ILOG/CPLEX_Studio125/cplex/bin/x86-64_sles10_4.1"
            if os.path.exists(mac_jlp): jlp = mac_jlp
            elif os.path.exists(coe_jlp): jlp = coe_jlp
            else: raise Exception("Could not find java.library.path for CPLEX")
            java_args += " -Djava.library.path=%s " % (jlp)
        return java_args

class DepParseExpParamsRunner(ExpParamsRunner):
    
    def __init__(self, options):
        ExpParamsRunner.__init__(self, options.expname, options.queue, print_to_console=True)
        self.root_dir = os.path.abspath(get_root_dir())
        self.expname = options.expname
        if self.queue and not self.queue == "mem":
            print "WARN: Are you sure you don't want the mem queue?"
            
    def get_experiments(self):
        all = Ptb2ConllX()
        # PTB Data sets
        data_dir = os.path.join(self.root_dir, "data")
        wsj_00 = self.get_data(data_dir, "treebank_3_sym/wsj/02")
        wsj_dev = self.get_data(data_dir, "treebank_3_sym/wsj/22")
        wsj_test = self.get_data(data_dir, "treebank_3_sym/wsj/23")
        wsj_full = self.get_data(data_dir, "treebank_3_sym/wsj") # Only sections 2-21
        brown_cf = self.get_data(data_dir, "treebank_3/brown/cf")
        brown_full = self.get_data(data_dir, "treebank_3/brown")
        
        synth_alt_three = Ptb2ConllX(synthetic="alt-three")
        synth_alt_three.set("dataset", "alt-three", True, False)
        
        # Reducing tagset explicitly
        rt = reduceTags="%s/data/tag_maps/en-ptb-plus-aux.map" % (self.root_dir)
        
        # Printing synthetic data with fixed synthetic seed.
        for synthdata in [synth_alt_three]: 
            synthdata.update(printSentences="./data.txt",
                             syntheticSeed=123454321)
        
        # Default datasets.
        default_brown = brown + Ptb2ConllX(maxSentenceLength=10, 
                                            maxNumSentences=200)
        
        if self.expname == "viterbi-em":
            root = RootStage()            
            for ptbIn in [wsj_dev, wsj_test, wsj_full]:
                for maxSentenceLength in [10]:
                    msl = Ptb2ConllX(maxSentenceLength=maxSentenceLength)
                    experiment = all + ptbIn + msl
                    root.add_dependent(experiment)
                    root.add_dependent(all + ptbIn + msl + rt)
            return root
        
        return None

    def updateStagesForQsub(self, root_stage):
        '''Makes sure that the stage object specifies reasonable values for the 
        qsub parameters given its experimental parameters.
        '''
        for stage in self.get_stages_as_list(root_stage):
            if isinstance(stage, experiment_runner.ExpParams):
                # Update the thread count
                threads = stage.get("threads")
                if threads != None: 
                    stage.threads = threads
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
    parser.add_option('--expname',  help="Experiment name")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    runner = DepParseExpParamsRunner(options)
    root_stage = runner.get_experiments()
    root_stage = runner.updateStagesForQsub(root_stage)
    runner.run_pipeline(root_stage)


