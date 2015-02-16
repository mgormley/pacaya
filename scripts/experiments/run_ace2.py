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
from pypipeline.stages import get_oome_stages, StagePath
import multiprocessing
from experiments.exp_util import *
from experiments.path_defs import *
from experiments.param_defs import *
from experiments.srl_stages import ScrapeSrl, SrlExpParams, GobbleMemory
from hyperparams import *

class ReExpParams(experiment_runner.JavaExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.JavaExpParams.__init__(self,keywords)
            
    def get_initial_keys(self):
        return "".split()
    
    def get_instance(self):
        return ReExpParams()
    
    def create_experiment_script(self, exp_dir):
        script = "\n"
        cmd = "java " + self.get_java_args() + " edu.jhu.nlp.joint.JointNlpRunner  %s \n" % (self.get_args())
        script += fancify_cmd(cmd)
        return script
    
class ScrapeAce(experiment_runner.PythonExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.PythonExpParams.__init__(self,keywords)
        self.always_relaunch()

    def get_initial_keys(self):
        return "dataSet model k s".split()
    
    def get_instance(self):
        return ScrapeAce()
    
    def get_name(self):
        return "scrape_ace"
    
    def create_experiment_script(self, exp_dir):
        self.add_arg(os.path.dirname(exp_dir))
        script = ""
        script += "export PYTHONPATH=%s/scripts:$PYTHONPATH\n" % (self.root_dir)
        cmd = "python %s/scripts/experiments/scrape_ace.py %s\n" % (self.root_dir, self.get_args())
        script += fancify_cmd(cmd)
        return script

class HyperparamArgmax(experiment_runner.PythonExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.PythonExpParams.__init__(self,keywords)
        self.always_relaunch()

    def get_instance(self):
        return HyperparamArgmax()
    
    def get_name(self):
        return "hyperparam_argmax"
    
    def create_experiment_script(self, exp_dir):
        self.add_arg(os.path.dirname(exp_dir))
        script = ""
        cmd = "python -m experiments.hyperparam_argmax %s\n" % (self.get_args())
        script += fancify_cmd(cmd)
        return script

# ---------------------------- Experiments Creator Class ----------------------------------

def get_ace05_data(concrete_dir, prefix):
    return os.path.join(concrete_dir, prefix+"-comms.zip")

def get_annotation_as_train(comm):
    exp = ReExpParams(train=comm, trainType="RE_CONCRETE")
    exp.set('trainName', os.path.basename(comm), incl_arg=False)
    return exp

def get_annotation_as_dev(comm):
    exp = ReExpParams(dev=comm, devType="RE_CONCRETE")
    exp.set('devName', os.path.basename(comm), incl_arg=False)
    return exp

def get_annotation_as_test(comm):
    exp = ReExpParams(test=comm, testType="RE_CONCRETE")
    exp.set('testName', os.path.basename(comm), incl_arg=False)
    return exp

class SrlExpParamsRunner(ExpParamsRunner):
    
    # Class variables
    known_exps = (  "ace-pm13",
                    "ace-params",
                    "ace-lc",
                    "ace-opt",
                    "acl-feats",
                    )
    
    def __init__(self, options):
        if options.expname not in SrlExpParamsRunner.known_exps:
            sys.stderr.write("Unknown experiment setting.\n")
            parser.print_help()
            sys.exit()
        name = options.expname if not options.fast else "fast_" + options.expname 
        ExpParamsRunner.__init__(self, name, options.queue, print_to_console=True, dry_run=options.dry_run)
        self.root_dir = get_root_dir()
        self.fast = options.fast
        self.expname = options.expname
        self.hprof = options.hprof   
        self.big_machine = (multiprocessing.cpu_count() > 2)
        self.prm_defs = ParamDefinitions(options) 

    def get_experiments(self):
        # ------------------------ PARAMETERS --------------------------
        
        g, l, p = self.prm_defs.get_param_groups_and_lists_and_paths()
        g.defaults.remove("trainGoldOut")
        g.defaults.remove("trainPredOut")
        g.defaults.remove("devGoldOut")
        g.defaults.remove("devPredOut")
        g.defaults.remove("testGoldOut")
        g.defaults.remove("testPredOut")
        
        defaults = ReExpParams()        
        defaults.set("expname", self.expname, False, False)
        defaults.update(seed=random.getrandbits(63))
        defaults.set("timeoutSeconds", 48*60*60, incl_arg=False, incl_name=False)  
        if self.queue:
            threads = 7
            work_mem_megs = 15*1024
        elif self.big_machine:
            threads = 2
            work_mem_megs = 1.5*1024
        else:
            threads = 1
            work_mem_megs = 1.5*1024
        defaults.set("work_mem_megs", work_mem_megs, incl_arg=False, incl_name=False)
        defaults.set("threads", threads, incl_name=False)
        defaults.set_incl_name("train", False)
        defaults.set_incl_name("dev", False)
        defaults.set_incl_name("test", False)
        defaults.set_incl_name("modelIn", False)
        defaults.update(seed=random.getrandbits(63),
                   algebra="LOG",
                   propTrainAsDev=0.1,
                   featCountCutoff=0,
                   featureHashMod=-1,
                   includeUnsupportedFeatures=True,
                   l2variance=40000,
                   sgdBatchSize=7*4,
                   sgdNumPasses=20,
                   useRelationSubtype=False,
                   cacheType="MEMORY_STORE", # Store all the examples in memory.
                   predAts="REL_LABELS",
                   inference="BRUTE_FORCE",
                   trainTypeOut="SEMEVAL_2010",
                   devTypeOut="SEMEVAL_2010",
                   testTypeOut="SEMEVAL_2010",
                   printModel="./model.txt.gz",                      
                   trainPredOut="./train-pred.txt",
                   devPredOut="./dev-pred.txt",
                   testPredOut="./test-pred.txt",
                   trainGoldOut="./train-gold.txt",
                   devGoldOut="./dev-gold.txt",
                   testGoldOut="./test-gold.txt",
                   modelOut="./model.binary.gz",  
                   reportOut="./outparams.txt",
                   )
        
        defaults.update(propTrainAsDev=0.0,
                        useEmbeddingFeatures=True,
                        useZhou05Features=True,
                        entityTypeRepl="NONE")
        defaults.update(optimizer="ADAGRAD_COMID", adaGradEta=0.05, 
                        adaGradInitialSumSquares=1, adaGradConstantAddend=0,
                        sgdAutoSelectLr=True, regularizer="NONE")
        #defaults += g.lbfgs + ReExpParams()
        
        # Datasets
        
        # ACE 2005
        ace05_concrete_dir = get_first_that_exists(os.path.join(p.corpora_dir, "processed", "ace_05_concrete3.3"))
        
        # ACE 2005 full domains:  bc bn cts nw un wl
        ace05_bc = get_ace05_data(ace05_concrete_dir, "bc")
        ace05_bn = get_ace05_data(ace05_concrete_dir, "bn")
        ace05_cts = get_ace05_data(ace05_concrete_dir, "cts")
        ace05_nw = get_ace05_data(ace05_concrete_dir, "nw")
        ace05_un = get_ace05_data(ace05_concrete_dir, "un")
        ace05_wl = get_ace05_data(ace05_concrete_dir, "wl")
        
        # ACE 2005 other sets: bn_nw, bc_dev, bc_test
        # TODO: We need to make sure bc_dev and bc_test exactly match
        # the settings in Plank & Moschitti (2013). 
        ace05_bn_nw = get_ace05_data(ace05_concrete_dir, "bn+nw")
        ace05_bc_dev = get_ace05_data(ace05_concrete_dir, "bc_dev")
        ace05_bc_test = get_ace05_data(ace05_concrete_dir, "bc_test")
        
        # Collections
        ace05_domains = [ace05_bc, ace05_bn, ace05_cts, ace05_nw, ace05_un, ace05_wl]

        # Embeddings 
        embeddings_dir = os.path.join(p.corpora_dir, "processed", "embeddings")
        polyglot_en = ReExpParams(embeddingsFile=os.path.join(embeddings_dir, "polyglot-en.txt"),
                                  embedName="polyglot")
        polyglot_en_small_dstuned = ReExpParams(embeddingsFile=os.path.join(embeddings_dir, "polyglot-en.dstuned.txt"),
                                  embedName="polyglot-small-dstuned")
        polyglot_en_small_combined = ReExpParams(embeddingsFile=os.path.join(embeddings_dir, "polyglot-en.combined.txt"),
                                  embedName="polyglot-small-combined")
        polyglot_en_large_dstuned = ReExpParams(embeddingsFile=os.path.join(embeddings_dir, "polyglot-en.largedstuned.txt"),
                                  embedName="polyglot-large-dstuned")
        polyglot_en_large_combined = ReExpParams(embeddingsFile=os.path.join(embeddings_dir, "polyglot-en.largecombined.txt"),
                                  embedName="polyglot-large-combined")
        cbow_nyt_en = ReExpParams(embeddingsFile=os.path.join(embeddings_dir, "vectors.nyt.cbow.out.d200.txt"),
                                  embedName="cbow-nyt")
        cbow_nyt11_en = ReExpParams(embeddingsFile=os.path.join(embeddings_dir, "ace05.nyt2011.cbow.bin.filtered.txt"),
                                  embedName="cbow-nyt11")
        defaults.set_incl_name("embeddingsFile", False)
        defaults.set_incl_arg("embedName", False)
        
        # Brown clusters
        bc_bllip = ReExpParams(brownClusters=p.bllip_clusters, bcMaxTagLength=5)
        defaults.set_incl_name("brownClusters", False)
        defaults += bc_bllip

        # Models
        feats_head_only = ReExpParams(modelName="head",      useEmbeddingFeatures=True, embFeatType="HEAD_ONLY", useZhou05Features=False)
        feats_head_type = ReExpParams(modelName="head-type", useEmbeddingFeatures=True, embFeatType="HEAD_TYPE", useZhou05Features=False)
        
        feats_zhou  = ReExpParams(modelName="zhou",           useEmbeddingFeatures=False)
        feats_zhou_head_only = ReExpParams(modelName="zhou+head",      useEmbeddingFeatures=True, embFeatType="HEAD_ONLY")
        feats_zhou_head_type = ReExpParams(modelName="zhou+head-type", useEmbeddingFeatures=True, embFeatType="HEAD_TYPE")
        feats_zhou_full_noch = ReExpParams(modelName="zhou+full-noch", useEmbeddingFeatures=True, embFeatType="FULL_NO_CHUNKS")
        feats_zhou_full      = ReExpParams(modelName="zhou+full",      useEmbeddingFeatures=True, embFeatType="FULL")
        feats_emb_only_noch  = ReExpParams(modelName="full-noch", useEmbeddingFeatures=True, embFeatType="FULL_NO_CHUNKS", useZhou05Features=False)
        feats_emb_only  = ReExpParams(modelName="full",           useEmbeddingFeatures=True, embFeatType="FULL", useZhou05Features=False)
        defaults.set_incl_arg("modelName", False)
        
        # Evaluation settings
        eval_ng14    = ReExpParams(group="NG14", predictArgRoles=False, 
                                   maxInterveningEntities=3, removeEntityTypes=True,                                   
                                   useRelationSubtype=False)
        eval_pm13    = ReExpParams(group="PM13", predictArgRoles=True, 
                                   maxInterveningEntities=3, removeEntityTypes=True,                                   
                                   useRelationSubtype=False)
        eval_types7 = ReExpParams(group="TYPES7", predictArgRoles=False, 
                                   maxInterveningEntities=9999, removeEntityTypes=False,                                   
                                   useRelationSubtype=False)
        eval_types13 = ReExpParams(group="TYPES13", predictArgRoles=True, 
                                   maxInterveningEntities=9999, removeEntityTypes=False,                                   
                                   useRelationSubtype=False)            
        defaults.set_incl_arg("group", False)
        
        # Hyperparameters
        hyps=3
        if hyps==0:
            hyperparams = []
            for _ in range(10):
                l2variance = random.uniform(20000, 60000)
                embScalar = random.uniform(15, 25)
                adaGradEta = random.uniform(0.025, 0.050)
                hyperparams.append(ReExpParams(l2variance=l2variance, embScalar=embScalar, 
                                               adaGradEta=adaGradEta, sgdAutoSelectLr=False))
        elif hyps==1:            
            hyperparams = []
            for l2variance in [10000, 20000, 40000, 80000, 160000]:
                for embScalar in [8, 16, 32, 64]:
                    hyperparams.append(ReExpParams(l2variance=l2variance, embScalar=embScalar))            
        elif hyps==2:
            hyperparams = []
            for adaGradEta in [0.025, 0.05, 0.1, 0.2]:
                for embScalar in [8, 16, 32, 64]:
                    hyperparams.append(ReExpParams(adaGradEta=adaGradEta, embScalar=embScalar, sgdAutoSelectLr=False))          
        elif hyps==3:
            hyperparams = []
            for l2variance in [20000, 40000, 60000]:
                for embScalar in [15, 20, 25]:
                    for adaGradEta in [0.05, 0.1]:
                        hyperparams.append(ReExpParams(l2variance=l2variance, adaGradEta=adaGradEta, 
                                                       embScalar=embScalar, sgdAutoSelectLr=False))   
        elif hyps==4:                   
            hyperparams = []
            num_hyp = 12
            def uniform_range(minval, maxval, num):
                rate = float(maxval - minval) / (num - 1.0)
                return [minval + i*rate for i in range(num)]
            def loguniform_range(minval, maxval, num):
                minexp = math.log(minval)
                maxexp = math.log(maxval)
                return map(math.exp, uniform_range(minexp, maxexp, num))
            def combine(hyp_lists):
                num = len(hyp_lists[0])
                for hl in hyp_lists:
                    assert len(hl) == num
                stride = 1
                combo = None
                for i, hl in enumerate(hyp_lists):
                    if i == 0:
                        combo = hl
                    else:
                        cur = 0
                        for start in range(stride):
                            for j in range(start, len(hl), stride):
                                combo[j] += hl[cur]
                                cur += 1
                    stride += 1
                return combo
            hyp_lists = []
            hyp_lists.append([ReExpParams(l2variance=x) for x in loguniform_range(1000, 200000, num_hyp)])
            hyp_lists.append([ReExpParams(embScalar=x) for x in loguniform_range(8, 80, num_hyp)])
            hyp_lists.append([ReExpParams(adaGradEta=x) for x in loguniform_range(0.005, 0.01, num_hyp)])
            hyperparams = combine(hyp_lists)
        elif hyps == 5:
            # Use default settings.
            hyperparams = [ReExpParams()]
        for x in hyperparams: print x

        # ------------------------ EXPERIMENTS --------------------------
        
        
        if self.expname == "ace-pm13":
            '''Follows the two distinct test evaluations of Plank & Moschitti (2013) 
            and Nguyen & Grishman (2014) for ACE '05. 
            Train on the union of bn and nw, test on bc_test, and the other domains.
            '''
            root = RootStage()
            train = get_annotation_as_train(ace05_bn_nw)
            for evl in [eval_pm13 + ReExpParams(entityTypeRepl="NONE"), 
                        eval_types13,
                        eval_pm13 + ReExpParams(entityTypeRepl="BROWN"),
                        ]: # eval_ng14, eval_types7]:
                for embed in [cbow_nyt11_en]: #, polyglot_en]:
                    for feats in [feats_zhou, feats_zhou_head_only, feats_zhou_full_noch, feats_emb_only_noch]: 
                        # SKIPPING: feats_zhou_head_type, feats_zhou_full, feats_emb_only 
                        for hyperparam in hyperparams:
                            dev = get_annotation_as_dev(ace05_bc_dev)
                            # CTS 
                            train = get_annotation_as_train(ace05_bn_nw)
                            test = get_annotation_as_test(ace05_cts)                           
                            exp_cts = defaults + evl + train + dev + test + embed + feats + hyperparam
                            root.add_dependent(exp_cts)
                            # WL
                            train = ReExpParams(modelIn=StagePath(exp_cts, exp_cts.get("modelOut")))
                            test = get_annotation_as_test(ace05_wl)
                            exp_wl = defaults + evl + train + dev + test + embed + feats + hyperparam
                            exp_cts.add_dependent(exp_wl)
                            # BC_TEST
                            train = ReExpParams(modelIn=StagePath(exp_cts, exp_cts.get("modelOut")))
                            test = get_annotation_as_test(ace05_bc_test)
                            exp_bc = defaults + evl + train + dev + test + embed + feats + hyperparam
                            exp_cts.add_dependent(exp_bc)
                            # In-Domain
                            train = get_annotation_as_train(ace05_bn_nw)
                            dev = ReExpParams(propTrainAsDev=0.2)
                            exp_bnnw = defaults + evl + train + dev + embed + feats + hyperparam
                            root.add_dependent(exp_bnnw)
            # Scrape results.
            scrape = ScrapeAce(tsv_file="results.data", csv_file="results.csv")
            scrape.add_prereqs(root.dependents)
            hypmax = HyperparamArgmax(tsv_file="results.data", csv_file="results.csv",
                                      hyperparam_keys=",".join(list(experiment_runner.get_all_keys(hyperparams))+['modelIn']),
                                      argmax_key='devRelF1')
            hypmax.add_prereqs(root.dependents)
            return root
                
        elif self.expname == "ace-params":
            '''Comparse # of model parameters
            '''
            root = RootStage()
            train = get_annotation_as_train(ace05_bn_nw)
            for evl in [#eval_pm13 + ReExpParams(entityTypeRepl="BROWN"), 
                        eval_pm13 + ReExpParams(entityTypeRepl="NONE"), 
                        #eval_types13,
                        ]: # eval_ng14, eval_types7]:
                for embed in [cbow_nyt11_en]: #, polyglot_en]:
                    for feats in [feats_head_only, feats_head_type]:
                        dev = get_annotation_as_dev(ace05_bc_dev)
                        # CTS 
                        train = get_annotation_as_train(ace05_bn_nw)
                        test = get_annotation_as_test(ace05_cts)                           
                        exp_cts = defaults + evl + train + dev + test + embed + feats 
                        root.add_dependent(exp_cts)
            # Scrape results.
            scrape = ScrapeAce(tsv_file="results.data", csv_file="results.csv")
            scrape.add_prereqs(root.dependents)
            return root
                
        elif self.expname == "ace-lc":
            '''Learning curve experiment.
            '''
            root = RootStage()
            setup = get_annotation_as_train(ace05_bn_nw)            
            for dev, test in [(get_annotation_as_dev(ace05_bc_dev), get_annotation_as_test(ace05_cts)),
                              (get_annotation_as_dev(ace05_bc_dev), get_annotation_as_test(ace05_wl)),
                              #(get_annotation_as_dev(ace05_bc_dev), get_annotation_as_test(ace05_bc_test)),                              
                              #(ReExpParams(propTrainAsDev=0.2), ReExpParams()),
                              ]:
                for embed in [cbow_nyt11_en]: #, polyglot_en]:
                    for feats in [feats_emb_only]: 
                        for evl in [eval_pm13, eval_types13]:
                            for trainMaxNumSentences in [2000, 4000, 8000, 16000, 35990]:
                                setup.update(trainMaxNumSentences=trainMaxNumSentences)
                                setup.update(work_mem_megs=5000 + 10000. * (trainMaxNumSentences / 35990.))
                                setup.update(threads=int(2. + 5. * (trainMaxNumSentences / 35990.)))
                                print "se=%d mm=%f th=%d" % (trainMaxNumSentences, setup.get("work_mem_megs"), setup.get("threads"))
                                for hyperparam in hyperparams:
                                    experiment = defaults + setup + evl + dev + test + embed + feats + hyperparam
                                    root.add_dependent(experiment)

            if self.fast: root.dependents = root.dependents[:2]
            # Scrape results.
            scrape = ScrapeAce(tsv_file="results.data", csv_file="results.csv")
            scrape.add_prereqs(root.dependents)  
            hypmax = HyperparamArgmax(tsv_file="results.data", csv_file="results.csv",
                                      hyperparam_keys=",".join(experiment_runner.get_all_keys(hyperparams)),
                                      argmax_key='devRelF1')
            hypmax.add_prereqs(root.dependents)
            return root        
        
        elif self.expname == "ace-opt":
            '''Compares various methods of optimization for the simplest ACE log-linear model.'''
            root = RootStage()
            setup = get_annotation_as_train(ace05_bn_nw)
            setup += get_annotation_as_dev(ace05_bc_dev)
            setup += get_annotation_as_test(ace05_bc_test)
            setup += cbow_nyt11_en + eval_types13 + feats_emb_only_noch
            defaults.remove("printModel")
            defaults.remove("modelOut")
            
            setup.update(sgdAutoSelectLr=False, adaGradEta=0.1, embScalar=1)
            g.adagrad_comid = g.adagrad + ReExpParams(optimizer="ADAGRAD_COMID")
            g.adagrad_comid.update(regularizer="NONE")
            g.fobos.update(regularizer="NONE")
            g.adagrad.update(regularizer="NONE")
            optis = [g.adagrad_comid, g.adagrad_comid, g.adagrad]
            l2s = [10000, 40000, 0]
            for optimizer, l2variance in zip(optis, l2s):
                for adaGradConstant in [1e-9, 0.1, 1.0]:
                    for isAddend in [True, False]:
                        exp = defaults + optimizer + setup
                        exp += ReExpParams(l2variance=l2variance)
                        if isAddend: 
                            exp.update(adaGradConstantAddend=adaGradConstant, adaGradInitialSumSquares=0)
                        else:     
                            exp.update(adaGradConstantAddend=0, adaGradInitialSumSquares=adaGradConstant)
                        root.add_dependent(exp)
            # Scrape results.
            scrape = ScrapeAce(tsv_file="results.data", csv_file="results.csv")
            scrape.add_prereqs(root.dependents)
            hypmax = HyperparamArgmax(tsv_file="results.data", csv_file="results.csv",
                                      hyperparam_keys=",".join(experiment_runner.get_all_keys(hyperparams)),
                                      argmax_key='devRelF1')
            hypmax.add_prereqs(root.dependents)
            return root
        
        
        elif self.expname == "ace-feats":
            '''Development results for in-domain and out-of-domain experiments, 
            comparing different feature sets.
            '''
            root = RootStage()
            setup= ReExpParams(#DISABLED: maxInterveningEntities=3,
                               propTrainAsDev=0.0)
            # Nguyen & Grishman (2014) did not predict argument roles, Plank & Moschitti (2013) did.
            setup.update(predictArgRoles=False)
            # In-domain and Out-of-domain experiments
            for test in [get_annotation_as_test(ace05_bc_dev), ReExpParams(propTrainAsDev=0.0)]:
                defaults.set("group", "broad", incl_name=True, incl_arg=False)
                for useZhou05Features in [True, False]:
                    for removeEntityTypes in [True, False]:
                        for useEmbeddingFeatures in [True, False]:
                            feats = ReExpParams(useZhou05Features=useZhou05Features, 
                                                removeEntityTypes=removeEntityTypes,
                                                useEmbeddingFeatures=useEmbeddingFeatures)
                            train = get_annotation_as_train(ace05_bn_nw)
                            experiment = defaults + setup + train + test + feats
                            #SKIP FOR NOW: root.add_dependent(experiment)
                defaults.set("group", "embed", incl_name=True, incl_arg=False)
                for embed in [polyglot_en, polyglot_en_large_dstuned, polyglot_en_large_combined]:
                    for embTmplPath in [True, False]:
                        for embTmplType in [True, False]:
                            for embSlotPath in [True, False]:
                                for embSlotHead in [True, False]:
                                    feats = ReExpParams(embTmplPath=embTmplPath, 
                                                        embTmplType=embTmplType,
                                                        embSlotPath=embSlotPath,
                                                        embSlotHead=embSlotHead)
                                    train = get_annotation_as_train(ace05_bn_nw)
                                    experiment = defaults + setup + train + test + embed + feats
                                    root.add_dependent(experiment)
                defaults.set("group", "scaling", incl_name=True, incl_arg=False)
                for embed in [polyglot_en, polyglot_en_large_dstuned, polyglot_en_large_combined]:
                    for embNorm in ["L1_NORM", "L2_NORM", "STD_NORMAL"]:
                        for embScalar in [2.0, 4.0, 8.0, 16.0, 32.0]:
                            feats = ReExpParams(embTmplPath=True, 
                                                embTmplType=True,
                                                embSlotPath=False,
                                                embSlotHead=True,
                                                embNorm=embNorm,
                                                embScalar=embScalar)
                            train = get_annotation_as_train(ace05_bn_nw)
                            experiment = defaults + setup + train + test + embed + feats
                            root.add_dependent(experiment)
                
            # Scrape results.
            scrape = ScrapeAce(tsv_file="results.data", csv_file="results.csv")
            scrape.add_prereqs(root.dependents)
            return root
        
        else:
            raise Exception("Unknown expname: " + str(self.expname))
    
    def _get_pipeline_from_exps(self, exps):
        if self.fast and len(exps) > 4: exps = exps[:4]
        root = RootStage()            
        root.add_dependents(exps)
        scrape = ScrapeAce(csv_file="results.csv", tsv_file="results.data")
        scrape.add_prereqs(root.dependents)
        return root
    
    def update_stages(self, root_stage):
        for stage in self.get_stages_as_list(root_stage):
            # First make sure that the "fast" setting is actually fast.
            if isinstance(stage, ReExpParams) and self.fast:
                self.make_stage_fast(stage)
            if isinstance(stage, ReExpParams) and not self.big_machine and not self.dry_run:
                stage.update(work_mem_megs=1100, threads=1) 
            if isinstance(stage, experiment_runner.ExpParams):
                self.update_qsub_fields(stage)
            if self.hprof:
                if isinstance(stage, experiment_runner.JavaExpParams):
                    stage.hprof = self.hprof
        return root_stage
    
    def update_qsub_fields(self, stage):
        ''' Makes sure that the stage object specifies reasonable values for the 
            qsub parameters given its experimental parameters.
        '''
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
    
    def make_stage_fast(self, stage):       
        ''' Makes the stage run in a very short period of time (under 5 seconds).
        ''' 
        safe_update(stage, 
                    sgdNumPasses=3,
                    maxLbfgsIterations=3,
                     trainMaxNumSentences=10,
                     devMaxNumSentences=10,
                     testMaxNumSentences=10,
                     #trainMaxSentenceLength=7,
                     #devMaxSentenceLength=11,
                     #testMaxSentenceLength=7,
                     work_mem_megs=2000,
                     timeoutSeconds=20)

def safe_update(stage, **kwargs):
    for key, val in kwargs.iteritems():
        if stage.get(key) is not None:
            stage.set("non-fast:"+key, stage.get(key), incl_arg=False)        
        stage.params[key] = val

if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('-q', '--queue', help="Which SGE queue to use")
    parser.add_option('-f', '--fast', action="store_true", help="Run a fast version")
    parser.add_option('-e', '--expname',  help="Experiment name. [" + ", ".join(SrlExpParamsRunner.known_exps) + "]")
    parser.add_option(      '--hprof',  help="What type of profiling to use [cpu, heap]")
    parser.add_option('-n', '--dry_run',  action="store_true", help="Whether to just do a dry run.")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    runner = SrlExpParamsRunner(options)
    root_stage = runner.get_experiments()
    root_stage = runner.update_stages(root_stage)
    runner.run_pipeline(root_stage)


