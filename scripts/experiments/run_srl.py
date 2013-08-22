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
        script = "\n"
        #script += 'echo "CLASSPATH=$CLASSPATH"\n'
        cmd = "java " + self.get_java_args() + " edu.jhu.srl.SrlRunner  %s \n" % (self.get_args())
        script += fancify_cmd(cmd)
        
        script += self.get_eval_script("train")
        script += self.get_eval_script("test")
        
        return script
    
    def get_eval_script(self, data_name):    
        script = "\n"
        script += 'echo "Evaluating %s"\n' % (data_name)
        eval_args = "" 
        if self.get("normalizeRoles") is not None:
            #eval_args += "--normalizeRoles %s" % (self.get("normalizeRoles"))
            pass
        eval_args += " -g " + self.get(data_name + "GoldOut") + " -s " + self.get(data_name + "PredOut")
        eval_out = data_name + "-eval.out"
        script += "perl %s/scripts/eval/eval09-no_sense.pl %s &> %s\n" % (self.root_dir, eval_args, eval_out)
        script += 'grep --after-context 11 "SEMANTIC SCORES:" %s' % (eval_out)
        return script
    
    def get_java_args(self):
        return self._get_java_args(0.75 * self.work_mem_megs)


class SrlExpParamsRunner(ExpParamsRunner):
    
    def __init__(self, options):
        ExpParamsRunner.__init__(self, options.expname, options.queue, print_to_console=True)
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
        self.expname = options.expname
        self.hprof = options.hprof
        
        if self.queue and not self.queue == "mem":
            print "WARN: Are you sure you don't want the mem queue?"
            
    def get_experiments(self):
        all = SrlExpParams()
        all.set("expname", self.expname, False, False)
        all.update(seed=random.getrandbits(63))
        all.update(printModel="./model.txt",
                   trainPredOut="./train-pred.txt",
                   testPredOut="./test-pred.txt",
                   trainGoldOut="./train-gold.txt",
                   testGoldOut="./test-gold.txt",
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
        conll09_sp_dir = os.path.abspath(os.path.join("data", "conll2009", "CoNLL2009-ST-Spanish"))
        prefix = exp_dir + "/vem-conll_005"
            
        # Gold trees: HEAD column.
        pos_gold = SrlExpParams(dataset = 'pos-gold', 
                                train = conll09_sp_dir + "/CoNLL2009-ST-Spanish-train.txt",
                                test = conll09_sp_dir + "/CoNLL2009-ST-Spanish-development.txt")
        
        # --- Predicted POS tags for of grammar induction ---

        # Supervised parser output: PHEAD column.
        pos_sup = SrlExpParams(dataset = 'pos-sup',
                               train = conll09_sp_dir + "/CoNLL2009-ST-Spanish-train.txt",
                               test = conll09_sp_dir + "/CoNLL2009-ST-Spanish-development.txt")
        
        # Semi-supervised parser output: PHEAD column.
        pos_semi = SrlExpParams(dataset = 'pos-semi',
                                train = prefix + "/dmv_conll09-sp-train_20_True/test-parses.txt",
                                test = prefix + "/dmv_conll09-sp-dev_20_True/test-parses.txt")
        
        # Unsupervised parser output: PHEAD column.
        pos_unsup = SrlExpParams(dataset = 'pos-unsup',
                                 train = prefix + "/dmv_conll09-sp-train_20_False/test-parses.txt",
                                 test = prefix + "/dmv_conll09-sp-dev_20_False/test-parses.txt")
        
        # --- Brown cluster tagged output of grammar induction: ---

        # Semi-supervised parser output: PHEAD column.
        brown_semi = SrlExpParams(dataset = 'brown-semi',
                                  train = prefix + "/dmv_conll09-sp-brown-train_20_True/test-parses.txt",
                                  test = prefix + "/dmv_conll09-sp-brown-dev_20_True/test-parses.txt")
        
        # Unsupervised parser output: PHEAD column.
        brown_unsup = SrlExpParams(dataset = 'brown-unsup',
                                   train = prefix + "/dmv_conll09-sp-brown-train_20_False/test-parses.txt",
                                   test = prefix + "/dmv_conll09-sp-brown-dev_20_False/test-parses.txt")
            
        datas = [pos_gold, pos_sup, pos_semi, pos_unsup, brown_semi, brown_unsup]
        
        # TODO: This removal of DEPREL and DEPREL should happen prior to these SRL experiments.
        for data in datas:
            data.update(trainType='CONLL_2009', testType='CONLL_2009')
            if data.get("dataset").find("-unsup") != -1 or data.get("dataset").find("-semi") != -1:
                data.set("removeDeprel", True, incl_name=False)
            else:
                data.set("removeDeprel", False, incl_name=False)
            if data.get("dataset").find("-gold") != -1:
                data.set("useGoldSyntax", True, incl_name=False)
            else:
                data.set("useGoldSyntax", False, incl_name=False)
                    
        if self.expname == "srl-dev20" or self.expname == "srl-biasonly":
            root = RootStage()
            setup = SrlExpParams()
            # Full length test sentences.
                #trainMaxNumSentences=3000,
            setup.update(
                         featureHashMod=-1,
                         alwaysIncludeLinkVars=True,
                         linkVarType="OBSERVED",
                         unaryFactors=True,
                         useSimpleFeats=False,
                         useNaradFeats=True,
                         useZhaoFeats=False,
                         useDepPathFeats=False,
                         featCountCutoff=4,
                         )
            setup.update(timeoutSeconds=48*60*60,
                         work_mem_megs=2*1024)
            if self.expname == "srl-biasonly":
                setup.update(biasOnly=True, work_mem_megs=2*1024)
            exps = []
            for data in datas:
                dataset = data.get("dataset")
#                for roleStructure in ['ALL_PAIRS', 'PREDS_GIVEN']:
#                    setup.update(roleStructure=roleStructure)
                for normalizeRoleNames in [True, False]:
                    setup.update(normalizeRoleNames=normalizeRoleNames)
                    for useProjDepTreeFactor in [True, False]:
                        if useProjDepTreeFactor and not (dataset == 'pos-unsup' or dataset == 'brown-unsup'):
                            # We only need to run this on one of the input datasets.
                            continue
                        if useProjDepTreeFactor: setup.update(linkVarType="LATENT")
                        else: setup.update(linkVarType="OBSERVED")
                        setup.update(useProjDepTreeFactor=useProjDepTreeFactor)
                        exp = all + setup + data
                        if exp.get("biasOnly") != True and os.uname()[1].find("Gormley") == -1:
                            # 2500 of len <= 20 fit in 1G, with  8 roles, and global factor on.
                            # 2700 of len <= 20 fit in 1G, with 37 roles, and global factor off.
                            # 1500 of len <= 20 fit in 1G, with 37 roles, and global factor on.
                            # So, increasing to 37 roles should require a 5x increase (though we see a 2x).
                            # Adding the global factor should require a 5x increase.
                            if not normalizeRoleNames and useProjDepTreeFactor:
                                base_work_mem_megs = 5*3*3*1024
                            elif useProjDepTreeFactor:
                                base_work_mem_megs = 5*3*3*1024
                            elif not normalizeRoleNames:
                                base_work_mem_megs = 5*3*1024
                            else:
                                base_work_mem_megs = 5*1024
                            base_work_mem_megs = 200*1024
                            exp += SrlExpParams(work_mem_megs=base_work_mem_megs)
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
                             trainMaxSentenceLength=7,
                             trainMaxNumSentences=3,
                             testMaxSentenceLength=7,
                             testMaxNumSentences=3,
                             work_mem_megs=2000,
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
            if self.hprof:
                if isinstance(stage, experiment_runner.JavaExpParams):
                    stage.hprof = self.hprof
        return root_stage

if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('-q', '--queue', help="Which SGE queue to use")
    parser.add_option('-f', '--fast', action="store_true", help="Run a fast version")
    parser.add_option('--test', action="store_true", help="Use test data")
    parser.add_option('--expname',  help="Experiment name")
    parser.add_option('--hprof',  help="What type of profiling to use [cpu, heap]")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    runner = SrlExpParamsRunner(options)
    root_stage = runner.get_experiments()
    root_stage = runner.updateStagesForQsub(root_stage)
    runner.run_pipeline(root_stage)


