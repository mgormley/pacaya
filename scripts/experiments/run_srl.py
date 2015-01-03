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
from pypipeline import StagePath
import multiprocessing
from experiments.exp_util import *
from experiments.path_defs import *
from experiments.param_defs import *
from experiments.srl_stages import ScrapeSrl, SrlExpParams, AnnoPipelineRunner

# ---------------------------- Experiments Creator Class ----------------------------------

class SrlExpParamsRunner(ExpParamsRunner):
    
    # Class variables
    known_exps = (  "srl-narad-dev20",
                    "srl-narad",
                    "srl-all",
                    "srl-all-nosup",
                    "srl-all-sup-lat",
                    "srl-agiga2",
                    "srl-predpos",
                    "srl-conll09",
                    "srl-conll09-bjork",
                    "srl-conll09-zhao",
                    "srl-conll09-coarse",
                    "srl-conll08",
                    "srl-subtraction",
                    "srl-lc-sem",
                    "srl-lc-syn",
                    "srl-mem",
                    "srl-opt",
                    "srl-benchmark",
                    "srl-feats",
                    "srl-feat-settings",
                    "srl-feat-reg",
                    "srl-feat-ig",
                    "srl-eval",
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
        g, l, p = self.prm_defs.get_param_groups_and_lists_and_paths()
        
        # Default language-specific groups.
        gl = g.langs["es"]
        ll = l.langs["es"]
                
        g.defaults.update(acl14DepFeats=True)
        
        if self.expname == "srl-narad-dev20":
            g.defaults += g.feat_tpl_narad
            g.defaults.update(trainMaxSentenceLength=20)
            return self._get_default_pipeline(g, l, gl, ll)
        
        elif self.expname == "srl-narad":
            g.defaults += g.feat_tpl_narad
            return self._get_default_pipeline(g, l, gl, ll)
        
        elif self.expname == "srl-all":
            g.defaults += SrlExpParams(trainMaxNumSentences=100,
                                       devMaxNumSentences=100,
                                       testMaxNumSentences=0,
                                       threads=1)
            g.defaults += g.feat_all
            return self._get_default_pipeline(g, l, gl, ll)

        elif self.expname == "srl-all-sup-lat":
            # Experiment on all languages with supervised and latent parses.
            g.defaults += g.feat_all
            exps = []
            for parser_srl in l.all_parse_and_srl_sup_lat:
                exp = g.defaults + parser_srl
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
            return self._get_pipeline_from_exps(exps)
                
        elif self.expname == "srl-agiga2":    
            '''Train SRL model for Annotated Gigaword 2.0. Test on some simple Concrete files.'''            
            root = RootStage()
            lang_short = "en"
            gl = g.langs[lang_short]
            pl = p.langs[lang_short]      
            
            # Annotate CoNLL-2009 data with my dependency parser.
            dp_pipe = get_first_that_exists("/home/hltcoe/mgormley/working/pacaya2/exp/dp-agiga2_000/1st_APW_ENG_19961005.0334_APW_ENG_19961005.0334/pipe.binary.gz",
                                            os.path.abspath("./exp/dp-agiga2_021/1st_APW_ENG_19961005.0334_APW_ENG_19961005.0334_CONCRETE/pipe.binary.gz"))
            apr_defaults = AnnoPipelineRunner(threads=g.defaults.get("threads"), testPredOut="./test-pred.txt",
                                              concreteDepParseTool="Pacaya")
            apr_defaults.set_incl_arg("group", False)
            apr_defaults.set_incl_name("pipeIn", False)
            apr_defaults.set_incl_name("test", False)
            apr_defaults.set_incl_name("testPredOut", False)
            g.defaults.update(concreteDepParseTool="Pacaya")
            c09_stages = {}
            for c09,name in [(pl.pos_gold_train, "train"),
                        (pl.pos_gold_dev, "dev"),
                        (pl.pos_gold_eval, "eval")]:
                c09_name = os.path.basename(c09)
                exp = apr_defaults + AnnoPipelineRunner(pipeIn=dp_pipe, predAts="DEP_TREE", 
                                            test=c09, testType="CONLL_2009", group=c09_name)
                c09_stages[name] = exp
                root.add_dependent(exp)
            
            # Train on the annotated CoNLL-2009 data.
            pos_sup = SrlExpParams(tagger_parser = 'pos-sup', 
                            train = StagePath(c09_stages['train'], c09_stages['train'].get("testPredOut")), trainType = "CONLL_2009",
                            dev =   StagePath(c09_stages['dev'],   c09_stages['dev'].get("testPredOut")),   devType = "CONLL_2009",
                            useGoldSyntax = False, language = lang_short)
            g.defaults += g.feat_tpl_coarse1 + pos_sup + g.model_ap_obs_tree_predpos
            g.defaults.update(predictSense=True, featureSelection=True, removeAts="DEPREL,MORPHO")
            train = g.defaults + SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))            
            comm = glob(p.concrete380 + "/*")[0]
            comm_name = os.path.basename(comm)
            train.update(pipeOut="pipe.binary.gz") 
            #train.update(test=comm, testType="CONCRETE", group=comm_name, evalTest=False)
            train.add_prereqs(root.dependents)
            
            if False: # Enable for quick local run.
                for exp in self.get_stages_as_list(root):
                    if isinstance(exp, RootStage): continue
                    exp.update(trainMaxNumSentences=3, devMaxNumSentences=3, testMaxNumSentences=3,
                                 trainMaxSentenceLength=7, devMaxSentenceLength=7, testMaxSentenceLength=7)
                                 #featureHashMod=1000, sgdNumPasses=2)
                                 
            # Test on a few Concrete files.
            for comm in glob(p.concrete380 + "/*"):
                comm_name = os.path.basename(comm)
                exp1 = apr_defaults + AnnoPipelineRunner(pipeIn=dp_pipe, predAts="DEP_TREE", 
                                            test=comm, testType="CONCRETE", group=comm_name)
                train.add_dependent(exp1)
                exp2 = apr_defaults + AnnoPipelineRunner(pipeIn=StagePath(train, train.get("pipeOut")), predAts="SRL",
                                            test=StagePath(exp1, exp1.get("testPredOut")), testType="CONCRETE", group=comm_name)
                exp1.add_dependent(exp2)
            return root
            
        elif self.expname == "srl-predpos":    
            '''Train SRL model which predicts the predicate position.'''            
            root = RootStage()
            lang_short = "en"
            gl = g.langs[lang_short]
            pl = p.langs[lang_short]                  
            # Train on the English CoNLL-2009 data. We include a few other models for comparison.
            for model in [g.model_ap_obs_tree_predpos, g.model_pg_obs_tree]:
                for predictSense in [True, False]:
                    exp = g.defaults + g.feat_tpl_coarse1 + gl.pos_sup + model
                    exp.update(predictSense=predictSense, featureSelection=True, removeAts="DEPREL,MORPHO")
                               #trainMaxNumSentences=100, trainMaxSentenceLength=10,
                               #devMaxNumSentences=10, devMaxSentenceLength=10,
                               #testMaxNumSentences=10, testMaxSentenceLength=10)
                    exp = exp + SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    root.add_dependent(exp)
            return root
                    
        elif self.expname == "srl-conll09":    
            # Experiment on CoNLL'2009 Shared Task.
            # Evaluates gold, supervised, semi-supervised, and unsupervised syntax in a pipelined model,
            # and marginalized syntax in a joint model.
            # We only include grammar induction run on brown clusters.
            exps = []
            g.defaults += g.feat_tpl_coarse1
            g.defaults.update(predictSense=True)
            feats_list = [ g.feat_tpl_coarse1 + SrlExpParams(featureSelection=True),
                      g.feat_tpl_bjork + SrlExpParams(featureSelection=True),
                      g.feat_tpl_zhao + SrlExpParams(featureSelection=False),
                      "bjork_ls"
                      ]
            for feats in feats_list:
                for lang_short in p.c09_lang_short_names:
                    gl = g.langs[lang_short]
                    ll = l.langs[lang_short]
                    if feats == "bjork_ls": 
                        feats_ls = gl.feat_tpl_bjork_ls + SrlExpParams(featureSelection=False)
                    else:
                        feats_ls = feats
                    parser_srl_list = combine_pairs([gl.pos_gold, gl.pos_sup, gl.brown_semi, gl.brown_unsup], [g.model_pg_obs_tree]) + \
                                       combine_pairs([gl.pos_sup], [g.model_pg_lat_tree])
                    for parser_srl in parser_srl_list:
                        exp = g.defaults + feats_ls + parser_srl 
                        if lang_short == "en":
                            exp.update(allowPredArgSelfLoops=True)
                        exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                        exps.append(exp)                        
            # Filter to just English
            exps = [x for x in exps if x.get("language") == "en" and x.get("feature_set").find("zhao") == -1]
            return self._get_pipeline_from_exps(exps)

        elif self.expname == "srl-conll09-bjork":    
            # This experiment is identical to srl-conll09 except that
            # it uses the Bjork features and a subset of the languages.
            exps = []
            g.defaults += g.feat_all
            g.defaults.update(predictSense=True, featureSelection=False)
            for lang_short in ['es', 'de', 'en', 'zh']:
                gl = g.langs[lang_short]
                ll = l.langs[lang_short]
                # SKIPPING: gl.brown_semi, gl.brown_unsup
                parser_srl_list = combine_pairs([gl.brown_semi], [g.model_pg_obs_tree])
                for parser_srl in parser_srl_list:
                    exp = g.defaults + parser_srl + gl.feat_tpl_bjork_ls
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-conll09-zhao":    
            # This experiment is identical to srl-conll09 except that
            # it uses the Bjork features and a subset of the languages.
            exps = []
            g.defaults += g.feat_tpl_zhao
            g.defaults.update(predictSense=True, featureSelection=False)
            for lang_short in ['ca']: #p.c09_lang_short_names:
                gl = g.langs[lang_short]
                ll = l.langs[lang_short]
                # SKIPPING: gl.brown_semi, gl.brown_unsup,  gl.pos_sup for OBS and LAT.
                parser_srl_list = combine_pairs([gl.pos_sup], [g.model_pg_obs_tree]) + \
                                   combine_pairs([gl.pos_sup], [g.model_pg_lat_tree])
                for parser_srl in parser_srl_list:
                    exp = g.defaults + parser_srl
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
                 
        elif self.expname == "srl-conll09-coarse":    
            # Experiment on CoNLL'2009 Shared Task.
            # Evaluates gold, supervised, semi-supervised, and unsupervised syntax in a pipelined model,
            # and marginalized syntax in a joint model.
            # We only include grammar induction run on brown clusters.
            exps = []
            g.defaults += g.feat_tpl_coarse1 + SrlExpParams(featureSelection=True)
            g.defaults.update(predictSense=True)
            for lang_short in p.c09_lang_short_names:
                gl = g.langs[lang_short]
                ll = l.langs[lang_short]
                parser_srl_list = combine_pairs([gl.pos_gold, gl.pos_sup, gl.brown_semi, gl.brown_unsup], [g.model_pg_obs_tree]) + \
                                       combine_pairs([gl.pos_sup], [g.model_pg_lat_tree])
                for parser_srl in parser_srl_list:
                    exp = g.defaults + parser_srl
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    if exp.get("language") == "cs":
                        exp.update(sgdNumPasses=2)
                        if exp.get("tagger_parser") == "pos-sup":
                            exp.update(work_mem_megs=60*1000)
                    exps.append(exp)
            # Filter to just Czech
            exps = [x for x in exps if x.get("language") == "cs"]
            return self._get_pipeline_from_exps(exps)
       
        elif self.expname == "srl-conll08":
            exps = []
            #g.defaults += g.feat_all
            g.defaults.update(predictSense=False)
            # Eval only. TODO
            g.defaults.set_incl_arg("trainName", False)
            # Train and test.
            for train in [SrlExpParams(train=p.c08_pos_gold_train_simplified, trainName="trainSimplified")]:
                for feats in [g.feat_all, g.feat_tpl_coarse1 + SrlExpParams(featureSelection=True)]:
                    pos_sup = SrlExpParams(tagger_parser = 'pos-sup', 
                                    trainType = "CONLL_2008",
                                    dev = p.c08_pos_gold_test_wsj_missing, devType = "CONLL_2008",
                                    test = p.c08_pos_gold_test_wsj_simplified, testType = "CONLL_2008",
                                    useGoldSyntax = False, language = 'en')
                    exp = g.defaults + feats + pos_sup + g.model_pg_lat_tree + train 
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-subtraction":
            exps = []
            g.defaults += g.feat_tpl_coarse1 + SrlExpParams(featureSelection=True)
            g.defaults.update(predictSense=False)
            g.defaults.set_incl_name('removeAts', True)
            removeAtsList = ["DEP_TREE,DEPREL", "MORPHO", "POS", "LEMMA", "SRL_PRED_IDX"]
            for lang_short in ['ca', 'de', 'es']: #p.c09_lang_short_names:
                gl = g.langs[lang_short]
                ll = l.langs[lang_short]
                # Add observed trees experiment.
                parser_srl = gl.pos_sup + g.model_pg_obs_tree
                exp = g.defaults + parser_srl
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
                # Add latent trees experiments.
                parser_srl = gl.pos_sup + g.model_pg_lat_tree
                for i in reversed(range(len(removeAtsList))):
                    removeAts = ",".join(removeAtsList[:i+1])
                    if removeAtsList[i] == "SRL_PRED_IDX":
                        exp = g.defaults + gl.pos_sup + g.model_ap_lat_tree_predpos + SrlExpParams(removeAts=removeAts)
                    else:
                        exp = g.defaults + parser_srl + SrlExpParams(removeAts=removeAts)
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-lc-sem": 
            # Learning curve experiment to evaluate the quality of SRL
            # with no other supervision, when different quantities of training
            # sentences are given.
            cl_map = {"ca":13200, "cs":38727, "de":36020, "en":39279, "es":14329, "zh":22277}
            exps = []
            g.defaults += g.feat_tpl_coarse1 + SrlExpParams(featureSelection=True)
            g.defaults.update(predictSense=False)
            g.defaults.set_incl_name('removeAts', True)
            for lang_short in ['ca', 'de', 'es']: #p.c09_lang_short_names:
                gl = g.langs[lang_short]
                ll = l.langs[lang_short]
                #parser_srl = gl.pos_sup + g.model_pg_lat_tree + SrlExpParams(removeAts="DEP_TREE,DEPREL,MORPHO,POS,LEMMA")
                parser_srl = gl.brown_semi + g.model_pg_obs_tree + SrlExpParams(removeAts="DEPREL,MORPHO,POS,LEMMA")
                for trainMaxNumSentences in [1000, 2000, 4000, 8000, 16000, 32000, 64000]:
                    if trainMaxNumSentences/2 >= cl_map[lang_short]:
                        break
                    exp = g.defaults + parser_srl + SrlExpParams(trainMaxNumSentences=trainMaxNumSentences,
                                                                 l2variance=min(trainMaxNumSentences, cl_map[lang_short]))
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-lc-syn":            
            # Learning curve experiment to evaluate the quality of high-resource SRL
            # where the amount of syntactic supervision is varied.
            # Note that this is a joint model, not a pipelined version, so it provides a spectrum 
            # of training settings between the marginalized model and the full joint model.
            #
            cl_map = {"ca":13200, "cs":38727, "de":36020, "en":39279, "es":14329, "zh":22277}
            exps = []
            g.defaults += g.feat_tpl_coarse1 + SrlExpParams(featureSelection=False)
            g.defaults.update(predictSense=False)
            g.defaults.set_incl_name('removeAts', True)
            g.defaults.update(removeAts="DEP_TREE,DEPREL,MORPHO,POS,LEMMA")
            for lang_short in p.c09_lang_short_names:
                gl = g.langs[lang_short]
                ll = l.langs[lang_short]
                parser_srl = gl.pos_sup + g.model_pg_prd_tree
                for trainMaxNumDepParses in [1000, 2000, 4000, 8000, 16000, 32000, 64000]:
                    if trainMaxNumDepParses/2 >= cl_map[lang_short]:
                        break
                    exp = g.defaults + parser_srl + SrlExpParams(trainMaxNumDepParses=trainMaxNumDepParses)
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-mem":
            exps = []
            g.defaults += g.feat_tpl_coarse1 + SrlExpParams(featureSelection=False)
            g.defaults.update(trainMaxNumSentences=100)
            g.defaults.set_incl_name('threads', True)
            g.defaults.set_incl_name('work_mem_megs', True)
            g.defaults.set_incl_arg('ts', False)
            g.defaults.set_incl_arg('wmm', False)
            gl = g.langs['en']
            for heap_gigs in [0.25,0.5,1,1.5,2]:
                for threads in [1,2,3,4,5]:
                    work_mem_megs = heap_gigs*1000 + (512 + 128) # We will subtract this off for overhead / PermGen.
                    exp = g.defaults + gl.pos_sup + g.model_pg_lat_tree 
                    exp += SrlExpParams(threads=threads, work_mem_megs=work_mem_megs, ts=threads, wmm=heap_gigs*1000)
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-opt":
            # Experiment to do grid search over parameters for optimization.
            
            # All experiments here use the PREDS_GIVEN, observed tree model, on supervised parser output.
            exps = []
            g.defaults.set_incl_arg("group", False)
            data_settings = SrlExpParams(trainMaxNumSentences=1000,
                                         devMaxNumSentences=500,
                                         testMaxNumSentences=0)
            
            # Best so far...
            g.defaults.update(group="tuneAdaDelta")            
            for adaDeltaDecayRate in [0.90, 0.95, 0.99]:
                for adaDeltaConstantAddend in [-2., -4., -6., -8.]:
                    adaDeltaConstantAddend = math.exp(adaDeltaConstantAddend)
                    exp = g.defaults + g.model_pg_obs_tree + gl.pos_sup + data_settings \
                            + g.adadelta + SrlExpParams(adaDeltaDecayRate=adaDeltaDecayRate, 
                                                        adaDeltaConstantAddend=adaDeltaConstantAddend)
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
                    
            # Best so far is adaGradEta = 0.1
            g.defaults.update(group="tuneAdaGrad")            
            for adaGradEta in [0.001, 0.01, 0.05, 0.1, 0.5, 1.0, 10.0, 100.0]:
                exp = g.defaults + g.model_pg_obs_tree + gl.pos_sup + data_settings + g.adagrad + SrlExpParams(adaGradEta=adaGradEta)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
                
            # Best so far is sgdInitialLr = 0.1     
            g.defaults.update(group="tuneSGD")
            for sgdInitialLr in [0.001, 0.01, 0.05, 0.1, 0.5, 1.0, 10.0, 100.0]:
                exp = g.defaults + g.model_pg_obs_tree + gl.pos_sup + data_settings + g.sgd + SrlExpParams(sgdInitialLr=sgdInitialLr)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp) 
                
            # Best so far is l2variance = 500 
            g.defaults.update(group="tuneL2")
            for l2variance in [0.01, 0.1, 1., 10., 100., 250., 500., 750., 1000., 10000.]:
                exp = g.defaults + g.model_pg_obs_tree + gl.pos_sup + data_settings + g.lbfgs + SrlExpParams(l2variance=l2variance)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
                
            g.defaults.update(group="compare")
            for trainMaxNumSentences in [250, 500, 1000, 2000]:
                data_settings.update(trainMaxNumSentences=trainMaxNumSentences)
                for optimizer in l.optimizers:
                    exp = g.defaults + g.model_pg_obs_tree + gl.pos_sup + data_settings + optimizer
                    exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                    exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-benchmark":
            # Experiment to do grid search over parameters for caching computation.
            
            # All experiments here use the PREDS_GIVEN, observed tree model, on supervised parser output.
            exps = []
            data_settings = SrlExpParams(trainMaxNumSentences=500,
                                         devMaxNumSentences=1,
                                         testMaxNumSentences=1)
            g.defaults.update(sgdNumPasses=1)            
            cacheSettings = [           # Number of sents: 100      500 
                             ("DISK_STORE", False, 1),    # 0.10    0.71
                             ("DISK_STORE", False, -1),   # 0.05    0.27
                             ("MEMORY_STORE", False, -1), # 0.06    0.23
                             ("NONE", False, -1),         # 0.17    0.78 
                             ("CACHE", True, 1000000),    # 0.62    2.87
                             ]
            for cacheType, gzipCache, maxEntriesInMemory in cacheSettings:
                exp = g.defaults + g.model_pg_obs_tree + gl.pos_sup + data_settings \
                        + SrlExpParams(cacheType=cacheType, 
                                       gzipCache=gzipCache,
                                       maxEntriesInMemory=maxEntriesInMemory)
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exp.remove("dev")
                exp.remove("test")
                exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feats":
            # Experiment to compare various feature sets.
            # Test 1: for testing correctness of feature sets.
            exps = []
            g.defaults.update(trainMaxNumSentences=100,
                              devMaxNumSentences=500,
                              testMaxNumSentences=0,
                              threads=6,
                              work_mem_megs=5*1024)
            feature_sets = [g.feat_tpl_coarse1, g.feat_tpl_narad, g.feat_tpl_zhao, g.feat_tpl_bjork, 
                            g.feat_narad, g.feat_zhao, g.feat_bjork, 
                            g.feat_mcdonald, g.feat_koo_basic, g.feat_koo_hybrid,
                            g.feat_tpl_bjork_es, g.feat_tpl_bjork_ig]
            g.defaults.set_incl_name('featureSelection', True)
            g.defaults.update(featureSelection=False)
            for feature_set in feature_sets:
                # Spanish, observed/supervised dep parse and POS tags.                    
                parser_srl = g.model_pg_obs_tree + gl.pos_sup
                exp = g.defaults + parser_srl + feature_set
                #exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feat-settings":
            # Experiment to compare various feature sets.
            # Test 2: for testing bjork_es feature set and effect of feature hashing / feature count cutoffs.
            # Doubles a test of the quality of bjorkelund spanish feats.
            exps = []
            feature_sets = [g.feat_tpl_bjork_es]
            for feature_set in feature_sets:
                for featCountCutoff in [0, 2, 4]:
                    for featureHashMod in [-1, 500000, 1000000, 10000000]:
                        # Spanish, observed/supervised dep parse and POS tags.
                        parser_srl = g.model_pg_obs_tree + gl.pos_sup
                        exp = g.defaults + parser_srl + feature_set + SrlExpParams(featCountCutoff=featCountCutoff, featureHashMod=featureHashMod)
                        exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                        exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feat-reg":
            # Test 3: for comparing regularization weights across feature sets.
            # Findings:
            # - Best l2variance for 1000 sentences is 250.
            # - Best l2variance (observed) for 15000 sentences is 10000 (haven't tried higher).
            exps = []
            #g.defaults.update(testMaxNumSentences=500)
            feature_sets = [g.feat_tpl_coarse1] # g.feat_tpl_bjork_es, g.feat_tpl_bjork_ig]
            for trainMaxNumSentences in [15000]: #[500, 1000, 2000, 4000, 15000]:
                for feature_set in feature_sets:
                    for l2variance in [1000, 5000, 10000, 50000, 100000]: #[0.01, 0.1, 1., 10., 100., 250., 500., 750., 1000., 10000., 100000]:
                        for sgdNumPasses in [4,8,10,12,14]:
                            # Spanish, observed/supervised dep parse and POS tags.
                            parser_srl = g.model_pg_obs_tree + gl.pos_sup + SrlExpParams(l2variance=l2variance, 
                                                                                         trainMaxNumSentences=trainMaxNumSentences,
                                                                                         sgdNumPasses=sgdNumPasses)
                            exp = g.defaults + parser_srl + feature_set
                            if exp.get("trainMaxNumSentences") == 15000:
                                exp += SrlExpParams(threads=20, work_mem_megs=50*1024)
                                exp.remove("devMaxNumSentences")
                                exp.remove("testMaxNumSentences")
                            else:
                                exp += SrlExpParams(threads=6, work_mem_megs=5*1024)                              
                            #exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                            exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feat-ig":
            exps = []
            g.defaults.update(trainMaxNumSentences=1000,
                              devMaxNumSentences=500,
                              testMaxNumSentences=0,
                              threads=6,
                              work_mem_megs=5*1024,
                              featureHashMod=1000000,
                              l2variance="500.0")
            feature_sets = [
                            g.feat_tpl_coarse1, 
                            g.feat_tpl_coarse2, 
                            g.feat_lluis_koo,
                            g.feat_tpl_bjork, 
                            g.feat_lluis, 
                            g.feat_tpl_narad, g.feat_tpl_zhao, g.feat_tpl_bjork_es,
                            # Dependency parsing
                            g.feat_mcdonald, g.feat_koo_basic, g.feat_koo_hybrid,
                            # Combos
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse2, g.feat_tpl_zhao),
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse2, g.feat_lluis_koo),
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse1, g.feat_tpl_zhao),
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse1, g.feat_tpl_bjork_es),
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse1, g.feat_koo_hybrid),
                            ]
            for lang_short in ["es", "en"]:
                gl = g.langs[lang_short]
                ll = l.langs[lang_short]
                for feature_set in feature_sets:
                    for featureSelection in [True, False]:
                        for model in [g.model_pg_lat_tree]: #TODO: , g.model_pg_obs_tree]:
                            parser_srl = model + gl.pos_sup 
                            exp = g.defaults + parser_srl + feature_set + SrlExpParams(featureSelection=featureSelection)
                            exp.set_incl_name('featureSelection', True)
                            #exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                            exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-feat-ig":
            # TODO: Finish this exp.
            exps = []
            g.defaults.update(trainMaxNumSentences=1000,
                              devMaxNumSentences=500,
                              testMaxNumSentences=0,
                              threads=6,
                              work_mem_megs=5*1024,
                              featureHashMod=1000000)
            feature_sets = [
                            #g.feat_tpl_coarse1, 
                            g.feat_tpl_coarse2, 
                            g.feat_lluis_koo,                            
                            #g.feat_tpl_bjork, 
                            #g.feat_tpl_zhao,
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse2, g.feat_tpl_zhao),
                            self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse2, g.feat_lluis_koo),
                            #g.feat_lluis, 
                            #g.feat_tpl_narad, g.feat_tpl_zhao, g.feat_tpl_bjork_es,
                            #g.feat_mcdonald, g.feat_koo_basic, g.feat_koo_hybrid,
                            #self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse1, g.feat_tpl_zhao),
                            #self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse1, g.feat_tpl_bjork_es),
                            #self.prm_defs.combine_feat_tpls(g.feat_tpl_coarse1, g.feat_koo_hybrid),
                            ]
            for lang_short in ["es", "en"]:
                gl = g.langs[lang_short]
                ll = l.langs[lang_short]
                for feature_set in feature_sets:
                    for featureSelection in [True, False]:
                        for model in [g.model_pg_obs_tree]: #TODO: , g.model_pg_lat_tree]:
                            parser_srl = model + gl.pos_sup 
                            exp = g.defaults + parser_srl + feature_set + SrlExpParams(featureSelection=featureSelection)
                            exp.set_incl_name('featureSelection', True)
                            #exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                            exps.append(exp)
            return self._get_pipeline_from_exps(exps)
        
        elif self.expname == "srl-eval":
            # Experiment to evaluate trained models from a specified directory using test data.
            if not self.eval:
                raise Exception("--eval value required")
            exps = []
            train_exp_dir = os.path.abspath(self.eval)
            for exp_dir in glob(train_exp_dir + "/*"):
                name = os.path.basename(exp_dir)                    
                # Skip files
                if not os.path.isdir(exp_dir): continue
                # Read old parameters
                old_params = SrlExpParams()
                old_params.read(os.path.join(exp_dir, "expparams.txt"))
                # Copy over old parameters for viewing in Excel
                old_params_for_record = SrlExpParams()
                for key in old_params.keys():
                    old_params_for_record.set("old:"+key, old_params.get(key), False, False)
                old_params_for_record.set("old:tagger_parser", old_params.get("tagger_parser"), incl_name=True, incl_arg=False)
                # Create experiment (used to include g.defaults)
                new_params = old_params_for_record + old_params + SrlExpParams()
                # -- remove irrelevant params
                keys_to_remove = [ "train", "trainType", "trainPredOut",
                                   "trainGoldOut", "trainMaxSentenceLength",
                                   "trainMaxNumSentences", 
                                   "dev", "devType", "devMaxSentenceLength",
                                   "devMaxNumSentences", 
                                   "test", "testType", "testMaxSentenceLength",
                                   "testMaxNumSentences", 
                                   "modelIn", "modelOut", "printModel", "seed",
                                   "senseFeatTplsOut", "argFeatTplsOut", 
                                   ]
                for key in keys_to_remove: 
                    new_params.remove(key)
                # -- add new params
                modelIn = old_params.get("modelOut")
                #   - skip non-experiment dirs.
                if modelIn is None: continue
                #   - prepend the experiment directory to relative paths.
                if modelIn.startswith("."):
                    modelIn = os.path.join(exp_dir, modelIn)
                #   - skip failed experiments.
                if not os.path.exists(modelIn): continue                
                # -- compose the parameters
                new_params.set("modelIn", modelIn, incl_name=False, incl_arg=True)
                new_params.set("oldName", name, incl_name=True, incl_arg=False)
                evalGroup = old_params.get("eval") # For several revisions we stored the test file in eval.
                if evalGroup is None and old_params.get("expname") == "srl-all-sup-lat":      
                    # For backwards compatibility.
                    lang_short = old_params.get("language")
                    if lang_short == "sp": lang_short = "es"                              
                    evalGroup = g.langs[lang_short].pos_sup.get("test")
                    # TODO: Hack for backwards compat with srl-all-sup-lat_004.
                    #        new_params.update(featureHashMod='1000000')
                new_params.update(test=evalGroup, 
                                  testType=old_params.get("testType"))
                # Reduce to these get on the grid quickly.                                           
                new_params.update(threads=6, work_mem_megs=7*1024)
                new_params.set('oldJar', os.path.join(train_exp_dir, "jar-with-deps.jar"), False, False)
                exps.append(new_params)
            return self._get_pipeline_from_exps(exps)
        
        else:
            raise Exception("Unknown expname: " + str(self.expname))
    
    def _get_default_experiments(self, g, l, gl, ll):
        exps = []
        data_settings = SrlExpParams()                    
        for normalizeRoleNames in [True, False]:
            data_settings.update(normalizeRoleNames=normalizeRoleNames)
            for parser_srl in ll.parse_and_srl:
                exp = g.defaults + data_settings + parser_srl
                exp += SrlExpParams(work_mem_megs=self.prm_defs.get_srl_work_mem_megs(exp))
                exps.append(exp)
        return exps
    
    def _get_default_pipeline(self, g, l, gl, ll):
        exps = self._get_default_experiments(g, l, gl, ll)
        return self._get_pipeline_from_exps(exps)
    
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
            if isinstance(stage, SrlExpParams) and not self.big_machine:
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


