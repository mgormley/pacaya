#!/usr/bin/env python

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
from pypipeline.util import get_new_file, sweep_mult, fancify_cmd, frange
from pypipeline.util import head_sentences
import platform
from glob import glob
from pypipeline.experiment_runner import ExpParamsRunner, get_subset
from pypipeline import experiment_runner
from pypipeline import pipeline
import re
import random
from pypipeline.pipeline import write_script, RootStage, Stage
from pypipeline.stages import get_oome_stages
import multiprocessing
from experiments.exp_util import *
from experiments.path_defs import *
from experiments.param_defs import *
from experiments.srl_stages import ScrapeSrl, SrlExpParams, GobbleMemory

# ---------------------------- Experiments Creator Class ----------------------------------

class SrlExpParamsRunner(ExpParamsRunner):
    
    # Class variables
    known_exps = (  "dp-conllx",
                    "dp-conllx-tmp",
                    "dp-conllx-tune",
                    "dp-pruning",
                    "gobble-memory",
                    "dp-aware",
                    "dp-aware-small",
                    "dp-erma",
                    "dp-erma-tune",
                    )
    
    def __init__(self, options):
        if options.expname not in SrlExpParamsRunner.known_exps:
            sys.stderr.write("Unknown experiment setting.\n")
            parser.print_help()
            sys.exit()
        #if options.fast: name = 
        name = options.expname if not options.fast else "fast_" + options.expname 
        ExpParamsRunner.__init__(self, name, options.queue, print_to_console=True, dry_run=options.dry_run)
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
        self.expname = options.expname
        self.hprof = options.hprof   
        self.eval = options.eval
        self.big_machine = (multiprocessing.cpu_count() > 2)
        self.prm_defs = ParamDefinitions(options) 

    def get_experiments(self):
        # ------------------------ PARAMETERS --------------------------
        
        g, l, p = self.prm_defs.get_param_groups_and_lists_and_paths()
        
        g.defaults += g.feat_mcdonald
        g.defaults += g.adagrad
        g.defaults.update(includeSrl=False, featureSelection=False, useGoldSyntax=True, 
                          adaGradEta=0.05, featureHashMod=10000000, sgdNumPasses=10, l2variance=10000,
                          sgdAutoSelecFreq=2, sgdAutoSelectLr=True, pruneByDist=True,
                          useLogAddTable=False, acl14DepFeats=False, normalizeMessages=True,
                          logDomain=False,
                          algebra="LOG_SIGN")
        g.defaults.set_incl_name("pruneByModel", False)
        g.defaults.set_incl_name("siblingFactors", False)
        g.defaults.set_incl_name("grandparentFactors", False)
        
        g.first_order = SrlExpParams(useProjDepTreeFactor=True, linkVarType="PREDICTED", predAts="DEP_TREE", 
                                   removeAts="DEPREL", tagger_parser="1st", pruneByModel=False)
                                   #bpUpdateOrder="SEQUENTIAL", bpSchedule="TREE_LIKE", bpMaxIterations=1)
        g.second_order = g.first_order + SrlExpParams(grandparentFactors=True, siblingFactors=True, tagger_parser="2nd", 
                                                  #bpUpdateOrder="SEQUENTIAL", bpSchedule="RANDOM", bpMaxIterations=5, 
                                                  bpUpdateOrder="PARALLEL", bpMaxIterations=10, 
                                                  useMseForValue=True)
        g.second_grand = g.second_order + SrlExpParams(grandparentFactors=True, siblingFactors=False, tagger_parser="2nd-gra")
        g.second_sib = g.second_order + SrlExpParams(grandparentFactors=False, siblingFactors=True, tagger_parser="2nd-sib")
        g.unpruned_parsers = [g.second_sib, g.first_order, g.second_order, g.second_grand]
        g.pruned_parsers = [x + SrlExpParams(pruneByModel=True,tagger_parser=x.get("tagger_parser")+"-pr") for x in g.unpruned_parsers]
        g.parsers = g.pruned_parsers + g.unpruned_parsers
        
        g.erma_dp = SrlExpParams(trainer="ERMA", dpLoss="DP_DECODE_LOSS", dpStartTemp=10, dpEndTemp=.1, dpAnnealMse=True)
        g.erma_mse = SrlExpParams(trainer="ERMA", dpLoss="MSE")
        g.erma_er = SrlExpParams(trainer="ERMA", dpLoss="EXPECTED_RECALL")
        g.cll = SrlExpParams(trainer="CLL")
        
        models_dir = get_first_that_exists(os.path.join(self.root_dir, "exp", "models", "dp-conllx_FAST"), # This is a fast model locally.
                                           os.path.join(self.root_dir, "exp", "models", "dp-pruning_000"),
                                           os.path.join(self.root_dir, "exp", "models", "dp-pruning_001"),
                                           os.path.join(self.root_dir, "remote_exp", "models", "dp-conllx_005"))
        
        # Language specific parameters
        p.cx_langs_with_phead = ["bg", "en", "de", "es"]

        # This is a map from language to number of sentences.
        l2var_map = {"ar" : 1500, "zh" : 57000, "cs" : 72700, "da" : 5200, "nl" : 13300,
                     "de" : 39200, "ja" : 17000, "pt" : 9100, "sl" : 1500, "es" : 3300,
                     "sv" : 11000, "tr" : 5000, "bg" : 12800, "en" : 40000}

        for lang_short in p.cx_lang_short_names:
            gl = g.langs[lang_short]
            pl = p.langs[lang_short]
            gl.pruneModel = os.path.join(models_dir, "1st_"+lang_short, "model.binary.gz")
            gl.cx_data = SrlExpParams(train=pl.cx_train, trainType="CONLL_X", devType="CONLL_X",
                                      test=pl.cx_test, testType="CONLL_X", 
                                      language=lang_short, trainUseCoNLLXPhead=True)        
            if lang_short == "en":
                gl.cx_data += SrlExpParams(dev=pl.cx_dev)
            else:
                gl.cx_data += SrlExpParams(propTrainAsDev=0.10) 
            
        # ------------------------ EXPERIMENTS --------------------------
        
        if self.expname == "dp-conllx":
            # CoNLL-X experiments.
            exps = []
            # Note: "ar" has a PHEAD column, but it includes multiple roots per sentence.
            for lang_short in ["bg", "es", "en"]:
                gl = g.langs[lang_short]
                pl = p.langs[lang_short]
                for parser in g.parsers:
                    data = gl.cx_data
                    data.update(l2variance=l2var_map[lang_short],
                                pruneModel=gl.pruneModel,
                                propTrainAsDev=0)  # TODO: Set to zero for final experiments.
                    exp = g.defaults + data + parser
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    if parser in [g.second_order, g.second_grand, g.second_sib]:
                        exps += get_oome_stages(exp)
                    else:
                        exps.append(exp)
            exps = [x for x in exps if x.get("language") == "en"]
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "dp-pruning":            
            # Trains the pruning models for the CoNLL-X languages.
            exps = []
            g.defaults += g.feat_mcdonald_basic
            g.defaults.update(pruneByDist=False) # TODO: Consider changing this.
            for lang_short in ["bg", "es", "en"]:
                gl = g.langs[lang_short]
                pl = p.langs[lang_short]
                data = gl.cx_data
                data.update(l2variance=l2var_map[lang_short],
                            propTrainAsDev=0) # TODO: Set to zero for final experiments.
                exp = g.defaults + data + g.first_order
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "dp-aware":
            # Comparison of CLL and ERMA training with varying models and iterations.
            exps = []
            for trainer in [g.erma_mse, g.cll]:
                for bpMaxIterations in [2, 3, 5, 10]:
                    for lang_short in ["bg", "es", "en"]:
                        gl = g.langs[lang_short]
                        pl = p.langs[lang_short]
                        for parser in g.parsers:
                            data = gl.cx_data
                            data.update(l2variance=l2var_map[lang_short],
                                        pruneModel=gl.pruneModel,
                                        propTrainAsDev=0.0)  # TODO: Set to zero for final experiments.
                            exp = g.defaults + data + parser + trainer + SrlExpParams(bpMaxIterations=bpMaxIterations)
                            exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                            if parser in [g.second_order, g.second_grand, g.second_sib]:
                                exps += get_oome_stages(exp)
                            else:
                                exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        
        elif self.expname == "dp-aware-small":
            # Comparison of CLL and ERMA training with varying models and iterations.
            # Here we use a small dataset and no pruning.
            exps = []
            overrides = SrlExpParams(trainMaxNumSentences=2000,
                              trainMaxSentenceLength=15,
                              pruneByDist=False,
                              pruneByModel=False)
            for trainer in [g.erma_mse, g.cll]:
                for bpMaxIterations in [2, 3, 4, 5, 6, 7, 8, 9, 10]:
                    for lang_short in ['en']: #["bg", "es", "en"]:
                        gl = g.langs[lang_short]
                        pl = p.langs[lang_short]
                        for parser in [g.second_order, g.second_sib, g.first_order, g.second_grand]:
                            data = gl.cx_data
                            data.update(l2variance=l2var_map[lang_short],
                                        pruneModel=gl.pruneModel,
                                        propTrainAsDev=0.5,
                                        bpUpdateOrder="PARALLEL",
                                        useMseForValue=True)
                            data.remove("test")
                            data.remove("testType")
                            data.remove("dev")
                            exp = g.defaults + data + parser + trainer + overrides + SrlExpParams(bpMaxIterations=bpMaxIterations)
                            exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                            exps.append(exp)
            return self._get_pipeline_from_exps(exps)
         
        elif self.expname == "dp-erma":
            # Comparison of CLL and ERMA training with varying models and iterations.
            exps = []
            g.defaults += g.erma_mse #TODO: Change to DEP_PARSE_DECODE_LOSS
            g.defaults.set_incl_name("l2variance", False)
            # Train a first-order pruning model for each language
            prune_exps = {}
            languages = p.cx_lang_short_names
            for lang_short in languages:
                # Include the full first order model, just for comparison with prior work.
                for feats in [g.feat_mcdonald_basic, g.feat_mcdonald]:
                    gl = g.langs[lang_short]
                    pl = p.langs[lang_short]
                    data = gl.cx_data
                    data.update(l2variance=l2var_map[lang_short],
                                propTrainAsDev=0,
                                trainUseCoNLLXPhead=False) # TODO: Set to zero for final experiments.
                    exp = g.defaults + data + g.first_order + feats
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    prune_exps[lang_short] = exp
                    exps.append(exp)
            # Train the other models for each language
            parser = g.second_order 
            parser += SrlExpParams(pruneByModel=True,
                                   tagger_parser=g.second_order.get("tagger_parser")+"-pr")
            for lang_short in languages:
                gl = g.langs[lang_short]
                pl = p.langs[lang_short]
                data = gl.cx_data
                data.update(l2variance=l2var_map[lang_short],
                            # TODO: This method of setting the pruneModel path is very unstable.
                            pruneModel="../1st_tpl_mcdonald_basic_%s/model.binary.gz" % (lang_short),
                            propTrainAsDev=0,
                            trainUseCoNLLXPhead=False)  # TODO: Set to zero for final experiments.
                exp = g.defaults + data + parser
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                if parser in [g.second_order, g.second_grand, g.second_sib]:
                    exps += get_oome_stages(exp)
                else:
                    exps.append(exp)
                exp.add_prereq(prune_exps[lang_short])
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "dp-erma-tune":
            # Tuning optimization for gradients computed by ERMA.
            exps = []
            #g.defaults.update(trainMaxNumSentences=)
            g.defaults.set_incl_name("l2variance", False)
            # With a short number of sentences prune_byDist is causing trouble.
            g.defaults.update(pruneByDist=False) # TODO: Consider changing this.
            # Train a first-order pruning model for each language
            languages = p.cx_lang_short_names
            for trainMaxNumSentences in [100, 1000, 10000]:
                for optimizer in [g.sgd, g.adagrad, g.lbfgs]:
                    for trainer in [g.erma_dp, g.erma_mse, g.cll, g.erma_er]:
                        for lang_short in ["bg"]:
                            gl = g.langs[lang_short]
                            pl = p.langs[lang_short]
                            data = gl.cx_data
                            data.update(l2variance=l2var_map[lang_short],
                                        trainMaxNumSentences=trainMaxNumSentences)
                            exp = g.defaults + data + g.first_order + g.feat_mcdonald_basic + optimizer + trainer
                            exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))                
                            exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "dp-conllx-tune":
            # CoNLL-X experiments.
            exps = []
            g.defaults.update(seed=123456789) # NOTE THE FIXED SEED
            for lang_short in ["es", "bg"]:
                gl = g.langs[lang_short]      
                pl = p.langs[lang_short]      
                for parser in [g.second_order, g.first_order]:
                    for adaGradEta in [ 0.05, 0.01, 0.1, 0.001, 1.0]:
                        for l2variance in [10000, 1000, 100000, 100,]:
                            for sgdNumPasses in [3, 5]:
                                hyper = SrlExpParams(sgdNumPasses=sgdNumPasses, adaGradEta=adaGradEta, 
                                                     l2variance=l2variance)
                                exp = g.defaults + gl.cx_data + parser + hyper
                                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                                exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "dp-conllx-tmp":
            # Temporary CoNLL-X experiment setup (currently testing why we can't overfit train).
            exps = []
            g.defaults += g.feat_mcdonald #tpl_narad 
            g.defaults.update(sgdNumPasses=2, sgdAutoSelectLr=False)
            if not self.big_machine:
                g.defaults.update(maxEntriesInMemory=1, sgdBatchSize=2)
            for trainMaxNumSentences in [100, 500, 1000, 2000, 9999999]:
                for lang_short in ["bg"]: #, "es"]:
                    gl = g.langs[lang_short]
                    pl = p.langs[lang_short]
                    for parser in g.parsers:
                        data = gl.cx_data
                        data.update(l2variance=l2var_map[lang_short],
                                    pruneModel=gl.pruneModel,
                                    trainMaxNumSentences=trainMaxNumSentences)
                        data.remove("test")
                        exp = g.defaults + data + parser
                        exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                        exps.append(exp)
            return self._get_pipeline_from_exps(exps)
            
        elif self.expname == "gobble-memory":
            exps = []
            stage = GobbleMemory(megsToGobble = 300, work_mem_megs=100)
            stage.set_incl_arg("work_mem_megs", False)
            exps += get_oome_stages(stage)
            return self._get_pipeline_from_exps(exps)
        
        else:
            raise Exception("Unknown expname: " + str(self.expname))
    
    def _get_pipeline_from_exps(self, exps):
        if self.fast and len(exps) > 4: exps = exps[:4]
        root = RootStage()            
        root.add_dependents(exps)    
        scrape = ScrapeSrl(csv_file="results.csv", tsv_file="results.data")
        scrape.add_prereqs(root.dependents)
        return root
    
    def update_stages_for_qsub(self, root_stage):
        ''' Makes sure that the stage object specifies reasonable values for the 
            qsub parameters given its experimental parameters.
        '''
        for stage in self.get_stages_as_list(root_stage):
            # First make sure that the "fast" setting is actually fast.
            if isinstance(stage, SrlExpParams) and self.fast:
                self.make_stage_fast(stage)
                # Uncomment next line for multiple threads on a fast run: 
                # stage.update(threads=2)
            if isinstance(stage, SrlExpParams) and not self.big_machine and not self.dry_run:
                stage.update(work_mem_megs=1100, threads=1) 
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
    
    def make_stage_fast(self, stage):       
        ''' Makes the stage run in a very short period of time (under 5 seconds).
        ''' 
        stage.update(maxLbfgsIterations=3,
                     trainMaxSentenceLength=7,
                     trainMaxNumSentences=3,
                     devMaxSentenceLength=11,
                     devMaxNumSentences=3,
                     testMaxSentenceLength=7,
                     testMaxNumSentences=3,
                     work_mem_megs=2000,
                     timeoutSeconds=20)

if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('-q', '--queue', help="Which SGE queue to use")
    parser.add_option('-f', '--fast', action="store_true", help="Run a fast version")
    parser.add_option('-e', '--expname',  help="Experiment name. [" + ", ".join(SrlExpParamsRunner.known_exps) + "]")
    parser.add_option('--hprof',  help="What type of profiling to use [cpu, heap]")
    parser.add_option('-n', '--dry_run',  action="store_true", help="Whether to just do a dry run.")
    parser.add_option('-v', '--eval', help="Experiment directory to use as input for eval")
    parser.add_option('-r', '--remote',  action="store_true", help="Whether to run remotely.")
    (options, args) = parser.parse_args(sys.argv)
    # TODO: Above, we still want to list the experiment names in the usage printout, but we should
    # somehow pull them from SrlExpParamsRunner so that they are less likely to get stale.

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    runner = SrlExpParamsRunner(options)
    root_stage = runner.get_experiments()
    root_stage = runner.update_stages_for_qsub(root_stage)
    runner.run_pipeline(root_stage)


