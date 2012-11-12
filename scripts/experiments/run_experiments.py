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

class ScrapeExpout(experiment_runner.PythonExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.PythonExpParams.__init__(self,keywords)
        self.always_relaunch()

    def get_initial_keys(self):
        return "dataSet model k s".split()
    
    def get_instance(self):
        return ScrapeExpout()
    
    def get_name(self):
        return "scrape_expout"
    
    def create_experiment_script(self, exp_dir):
        self.add_arg(os.path.dirname(exp_dir))
        script = ""
        script += "export PYTHONPATH=%s/scripts:$PYTHONPATH\n" % (self.root_dir)
        cmd = "python %s/scripts/experiments/scrape_expout.py %s\n" % (self.root_dir, self.get_args())
        script += fancify_cmd(cmd)
        return script

class ScrapeStatuses(experiment_runner.PythonExpParams):
    
    def __init__(self, stages_to_scrape, **keywords):
        experiment_runner.PythonExpParams.__init__(self,keywords)
        self.always_relaunch()
        self.stages_to_scrape = stages_to_scrape
        
    def get_initial_keys(self):
        return "dataSet model k s".split()
    
    def get_instance(self):
        return ScrapeStatuses()
    
    def get_name(self):
        return "scrape_statuses"
    
    def create_experiment_script(self, exp_dir):
        # Add directories to scrape.
        for stage in self.stages_to_scrape:
            self.add_arg(stage.cwd)
        script = ""
        script += "export PYTHONPATH=%s/scripts:$PYTHONPATH\n" % (self.root_dir)
        cmd = "python %s/scripts/experiments/scrape_statuses.py %s\n" % (self.root_dir, self.get_args())
        script += fancify_cmd(cmd)
        return script

class SvnCommitResults(pipeline.NamedStage):
    '''TODO: Move this to core.'''
    def __init__(self, expname):
        pipeline.NamedStage.__init__(self, "svn_commit_results")
        self.always_relaunch()
        self.expname = expname
        
    def get_instance(self):
        return SvnCommitResults()
        
    def create_stage_script(self, exp_dir):
        # TODO: check that all the experiments completed successfully 
        # before committing. 
        top_dir = os.path.dirname(exp_dir)
        results_exp_dir = "%s/results/%s" % (self.root_dir, self.expname)
        # Copy results files ending in .data to results/<expname>
        script = ""
        script += "mkdir %s\n" % (results_exp_dir)
        script += "find %s -name '*.data' "  % (top_dir)
        script += " | xargs cp -t %s/ \n" % (results_exp_dir)
        # Add all new results to svn
        script += "svn add --force %s/results\n" % (self.root_dir)
        # Commit the new results to svn
        script += "svn commit -m 'AUTOCOMMIT: Updates to results "
        script += " from %s' %s/results\n" % (os.path.basename(top_dir), self.root_dir)
        return script

class DPExpParams(experiment_runner.JavaExpParams):
    
    def __init__(self, **keywords):
        experiment_runner.JavaExpParams.__init__(self,keywords)
            
    def get_initial_keys(self):
        return "dataSet model k s".split()
    
    def get_instance(self):
        return DPExpParams()
    
    def create_experiment_script(self, exp_dir):
        script = ""
        #script += "export CLASSPATH=%s/classes:%s/lib/*\n" % (self.root_dir, self.root_dir)
        script += "echo 'CLASSPATH=$CLASSPATH'\n"
        cmd = "java -cp $CLASSPATH " + self.get_java_args() + " edu.jhu.hltcoe.PipelineRunner  %s \n" % (self.get_args())
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
            mac_jlp = "/Users/mgormley/installed/ILOG/CPLEX_Studio_AcademicResearch122/cplex/bin/x86-64_darwin9_gcc4.0"
            coe_jlp = "/home/hltcoe/mgormley/installed/ILOG/CPLEX_Studio_AcademicResearch122/cplex/bin/x86-64_sles10_4.1"
            if os.path.exists(mac_jlp): jlp = mac_jlp
            elif os.path.exists(coe_jlp): jlp = coe_jlp
            else: raise Exception("Could not find java.library.path for CPLEX")
            java_args += " -Djava.library.path=%s " % (jlp)
        return java_args


class HProfCpuExpParams(DPExpParams):

    def __init__(self):
        DPExpParams.__init__(self)
        self.set("hprof","cpu-samples",True,False)
        
    def get_instance(self):
        return HProfCpuExpParams()
    
    def get_java_args(self):
        # Default interval is 10ms
        return DPExpParams.get_java_args(self) + " -agentlib:hprof=cpu=samples,depth=7,interval=2 "
    
class HProfHeapExpParams(DPExpParams):

    def __init__(self):
        DPExpParams.__init__(self)
        self.set("hprof","heap-sites",True,False)
        
    def get_instance(self):
        return HProfHeapExpParams()
    
    def get_java_args(self):
        return DPExpParams.get_java_args(self) + " -agentlib:hprof=heap=sites "
    

class DepParseExpParamsRunner(ExpParamsRunner):
    
    def __init__(self, options):
        ExpParamsRunner.__init__(self, options.expname, options.queue)
        self.root_dir = os.path.abspath(get_root_dir())
        self.fast = options.fast
        self.expname = options.expname
        self.data = options.data
        if options.test:
            self.get_data = get_test_data
        else:
            self.get_data = get_dev_data
            
        if self.queue and not self.queue == "mem":
            print "WARN: Are you sure you don't want the mem queue?"
            
    def get_experiments(self):
        all = DPExpParams()
        all.set("expname", self.expname, False, False)
        all.update(threads=self.threads)
        all.update(formulation="deptree-flow-nonproj",
                   parser="cky",
                   model="dmv",
                   algorithm="viterbi",
                   ilpSolver="cplex",
                   convergenceRatio=0.99999,
                   epsilon=0.1,
                   varSelection="regret",
                   varSplit="half-prob",
                   maxDwIterations=3, 
                   maxSetSizeToConstrain=3, 
                   maxCutRounds=1, 
                   minSumForCuts=1.01, 
                   initWeights="uniform",
                   nodeOrder="bfs")
        all.set("lambda",0.1)
        all.update(printModel="./model.txt")
        # Only keeping sentences that contain a verb
        all.update(mustContainVerb=None)
                
        dgFixedInterval = DPExpParams(deltaGenerator="fixed-interval",interval=0.01,numPerSide=2)
        dgFactor = DPExpParams(deltaGenerator="factor",factor=1.1,numPerSide=2)
        
        if self.fast:       all.update(iterations=1,
                                       maxSentenceLength=7,
                                       maxNumSentences=3,
                                       numRestarts=1,
                                       timeoutSeconds=10,   
                                       bnbTimeoutSeconds=3)
        else:               all.update(iterations=25,
                                       numRestarts=10,
                                       bnbTimeoutSeconds=100)
        
        # Data sets
        data_dir = os.path.join(self.root_dir, "data")
        wsj_00 = self.get_data(data_dir, "treebank_3/wsj/00")
        wsj_full = self.get_data(data_dir, "treebank_3/wsj")
        brown_cf = self.get_data(data_dir, "treebank_3/brown/cf")
        brown_full = self.get_data(data_dir, "treebank_3/brown")
        synth_alt_three = DPExpParams(synthetic="alt-three")

        wsj = wsj_full if not self.fast else wsj_00
        brown = brown_full if not self.fast else brown_cf
        
        # Reducing tagset explicitly
        for ptbdata in [wsj_00, wsj_full, brown_cf, brown_full]:
            ptbdata.update(reduceTags="%s/data/universal_pos_tags.1.02/en-ptb.map" % (self.root_dir))
        
        if self.data == "synthetic": 
            datasets = [synth_alt_three]
            all.update(printSentences="./data.txt",
                       syntheticSeed=123454321)
        elif self.fast:       datasets = [brown_cf]
        else:               datasets = [brown_full]
        
        experiments = []
        if self.expname == "viterbi-em":
            root = RootStage()
            setup = brown
            setup.update(maxSentenceLength=10, maxNumSentences=100000000)
            setup.update(algorithm="viterbi", parser="cky", numRestarts=0, iterations=1000, convergenceRatio=0.99999)
            setup.set("lambda", 1)
            for initWeights in ["uniform", "random"]:
                setup.update(initWeights=initWeights)
                for randomRestartId in range(100):
                    setup.set("randomRestartId", randomRestartId, True, False)
                    experiment = all + setup + DPExpParams()
                    root.add_dependent(experiment)
            scrape = ScrapeExpout(rproj=None, out_file="results.data")
            scrape.add_prereqs(root.dependents)
            svnco = SvnCommitResults(self.expname)
            svnco.add_prereq(scrape)
            return root
        elif self.expname == "viterbi-vs-bnb":
            for dataset in datasets:
                for maxSentenceLength in [3,5]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                    for maxNumSentences in [10,100]:
                        mns = DPExpParams(maxNumSentences=maxNumSentences)
                        for algorithm in ["viterbi", "bnb"]:
                            for varSelection in ["regret"]:
                                experiments.append(all + dataset + msl + mns + DPExpParams(algorithm=algorithm, varSelection=varSelection))
        elif self.expname == "bnb":
            all.update(algorithm="bnb")
            for dataset in datasets:
                for maxSentenceLength, maxNumSentences, timeoutSeconds in [(-1, 10, 6*60*60), (-1, 100, 6*60*60)]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                    mns = DPExpParams(maxNumSentences=maxNumSentences)
                    if not self.fast:
                        # Run for some fixed amount of time.                
                        all.update(numRestarts=1000000000)
                        all.update(timeoutSeconds=timeoutSeconds)
                    for varSelection in ["regret", "rand-uniform", "rand-weighted", "full", "pseudocost"]:
                        experiments.append(all + dataset + msl + mns + DPExpParams(varSelection=varSelection))
        elif self.expname == "bnb-semi":
            root = RootStage()
            all.update(algorithm="bnb",
                       initBounds="viterbi-em",                    
                       varSelection="regret")
            dataset = brown
            for maxSentenceLength, maxNumSentences, timeoutSeconds in [(5, 100, 1*60*60), (10, 300, 1*60*60)]:
                msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                mns = DPExpParams(maxNumSentences=maxNumSentences)
                if not self.fast:
                    # Run for some fixed amount of time.                
                    all.update(numRestarts=1000000000, epsilon=0.0)
                    all.update(timeoutSeconds=timeoutSeconds)
                for varSplit in ["half-prob", "half-logprob"]:
                    for offsetProb in [0.05, 0.1, 0.2, 0.5, 1.0]: #TODO: frange(10e-13, 0.21,0.05):
                        for propSupervised in frange(0.0, 1.0, 0.1):
                            algo = DPExpParams(varSplit=varSplit, offsetProb=offsetProb, 
                                               propSupervised=propSupervised)
                            experiment = all + dataset + msl + mns + algo
                            root.add_dependent(experiment)
            # Scrape all results.
            scrape = ScrapeExpout(rproj=None, out_file="results.data")
            scrape.add_prereqs(root.dependents)
            #Scrape status information from a subset of the experiments.
            subset = get_subset(root.dependents, offsetProb=1.0, maxSentenceLength=10, maxNumSentences=300)
            scrape_stat = ScrapeStatuses(subset, rproj=None, out_file="bnb-status.data", type="bnb")
            scrape_stat.add_prereqs(subset)
            # Commit results to svn
            svnco = SvnCommitResults(self.expname)
            svnco.add_prereqs([scrape, scrape_stat])
            return root
        elif self.expname == "bnb-semi-synth":
            all.update(algorithm="bnb",
                       initBounds="gold",
                       initWeights="gold",
                       varSelection="regret")
            dataset = synth_alt_three
            for maxNumSentences, timeoutSeconds in [(300, 20*60)]:
                mns = DPExpParams(maxNumSentences=maxNumSentences)
                if not self.fast:
                    # Run for some fixed amount of time.                
                    all.update(numRestarts=1000000000, epsilon=0.0)
                    all.update(timeoutSeconds=timeoutSeconds)
                for varSplit in ["half-prob", "half-logprob"]:
                    for varSelection in ["regret", "pseudocost", "full"]:
                        for offsetProb in [0.05, 0.1, 0.2, 0.5, 1.0]:
                            for propSupervised in frange(0.0, 1.0, 0.1):
                                algo = DPExpParams(varSplit=varSplit, offsetProb=offsetProb, 
                                                   propSupervised=propSupervised, varSelection=varSelection)
                                experiments.append(all + dataset + mns + algo)
        elif self.expname == "bnb-supervised":
            root = RootStage()
            all.update(algorithm="bnb",
                       initBounds="viterbi-em",                    
                       varSelection="regret",
                       offsetProb=1.0, 
                       varSplit="half-prob",
                       propSupervised=1.0,
                       maxSimplexIterations=1000000000,
                       maxDwIterations=1000000000,
                       maxCutRounds=1000000000,
                       minSumForCuts=1.00001,
                       maxSentenceLength=10,
                       maxNumSentences=300,)
            all.set("lambda", 0.0)
            # Run for some fixed amount of time.                
            all.update(numRestarts=1000000000, epsilon=0.0,
                       timeoutSeconds=2*60)
            dataset = brown
            for relaxation in ["dw", "dw-res"]:
                experiment = all + dataset + DPExpParams(relaxation=relaxation)
                root.add_dependent(experiment)
            return root
        elif self.expname == "bnb-hprof":
            all.update(algorithm="bnb")
            for dataset in datasets:
                for maxSentenceLength in [3,5]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                    for maxNumSentences in [10,100]:
                        mns = DPExpParams(maxNumSentences=maxNumSentences)
                        for varSelection in ["regret", "rand-uniform", "rand-weighted", "full"]:
                            experiments.append(all + dataset + msl + mns + DPExpParams(varSelection=varSelection) + HProfCpuExpParams())
        elif self.expname == "bnb-expanding-boxes":
            # Fixed seed
            all.update(algorithm="bnb", seed=112233)
            for dataset in datasets:
                for maxSentenceLength, maxNumSentences, timeoutSeconds in [(5, 50, 1*60*60), (10, 300, 4*60*60)]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                    mns = DPExpParams(maxNumSentences=maxNumSentences)
                    if not self.fast:
                        # Run for some fixed amount of time.                
                        all.update(numRestarts=1000000000)
                        all.update(timeoutSeconds=timeoutSeconds)
                    for varSelection in ["regret"]:
                        for initBounds in ["viterbi-em"]: #TODO: , "random", "uniform"]: # TODO: "gold"
                            for offsetProb in [0.05, 0.1, 0.2, 0.5, 1.0]: #TODO: frange(10e-13, 0.21,0.05):
                                for probOfSkipCm in [0.0]: #TODO: frange(0.0, 0.21, 0.05):
                                    algo = DPExpParams(varSelection=varSelection,initBounds=initBounds,offsetProb=offsetProb, probOfSkipCm=probOfSkipCm)
                                    experiments.append(all + dataset + msl + mns + algo)
        elif self.expname == "viterbi-bnb":
            root = RootStage()
            # Fixed seed
            all.update(seed=112233)
            for dataset in datasets:
                for maxSentenceLength, maxNumSentences, timeoutSeconds in [(10, 300, 1*60*60)]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                    mns = DPExpParams(maxNumSentences=maxNumSentences)
                    if not self.fast:
                        # Run for some fixed amount of time.
                        all.update(numRestarts=1000000000)
                        all.update(timeoutSeconds=timeoutSeconds)
                    for algorithm in ["viterbi", "viterbi-bnb", "bnb"]:
                        algo = DPExpParams(algorithm=algorithm)
                        if algorithm == "viterbi-bnb":
                            if not self.fast:
                                algo.update(bnbTimeoutSeconds=maxNumSentences/3)
                            for offsetProb in frange(0.05, 0.21, 0.05):
                                algo.update(offsetProb=offsetProb)
                                root.add_dependent(all + dataset + msl + mns + algo)
                        else:
                            root.add_dependent(all + dataset + msl + mns + algo)
            # Scrape all results.
            scrape = ScrapeExpout(rproj=None, out_file="results.data")
            scrape.add_prereqs(root.dependents)
            #Scrape status information from a subset of the experiments.
            #TODO: maybe a subset? #get_subset(root.dependents, offsetProb=1.0, maxSentenceLength=10, maxNumSentences=300) 
            subset = root.dependents
            scrape_stat = ScrapeStatuses(subset, rproj=None, out_file="incumbent-status.data", type="incumbent")
            scrape_stat.add_prereqs(subset)
            # Commit results to svn
            svnco = SvnCommitResults(self.expname)
            svnco.add_prereqs([scrape, scrape_stat])
            return root
        elif self.expname == "relax-percent-pruned":
            for dataset in datasets:
                for maxSentenceLength in [10]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                    for maxNumSentences in [300]:
                        mns = DPExpParams(maxNumSentences=maxNumSentences)
                        experiments.append(all + dataset + msl + mns + DPExpParams(algorithm="viterbi",parser="cky"))
                        for i in range(0,100):
                            for initBounds in ["random"]:
                                for offsetProb in frange(10e-13, 1.001,0.05):
                                    experiments.append(all + dataset + msl + mns + DPExpParams(initBounds=initBounds,offsetProb=offsetProb, seed=random.getrandbits(63), relaxOnly=None))
        elif self.expname == "relax-quality":
            # Fixed seed
            all.update(seed=112233)
            for dataset in datasets:
                for maxSentenceLength in [10]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                    for maxNumSentences in [100,300]:
                        mns = DPExpParams(maxNumSentences=maxNumSentences)
                        for initBounds in ["viterbi-em", "random", "uniform"]: # TODO: "gold"
                            for offsetProb in frange(10e-13, 1.001,0.05):
                                for probOfSkipCm in frange(0.0, 0.2, 0.05):
                                    experiments.append(all + dataset + msl + mns + DPExpParams(initBounds=initBounds,offsetProb=offsetProb,probOfSkipCm=probOfSkipCm, relaxOnly=None))
        elif self.expname == "relax-compare":
            # Fixed seed
            all.update(relaxOnly=None, seed=112233)
            for dataset in datasets:
                for maxSentenceLength in [10]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                    for maxNumSentences in [300]:
                        mns = DPExpParams(maxNumSentences=maxNumSentences)
                        for initBounds in ["random"]: # TODO: "gold"
                            for offsetProb in frange(10e-13, 1.001,0.2):
                                #for probOfSkipCm in frange(0.0, 0.2, 0.05):
                                for relaxation in ["dw", "dw-res"]:
                                    for maxDwIterations in [1,2,10,100]:
                                        for maxSimplexIterations in [10,100,1000]:
                                            for maxSetSizeToConstrain in [0,2,3]:
                                                for minSumForCuts in [1.001, 1.01, 1.1]:
                                                    for maxCutRounds in [1,10,100]:
                                                        p1 = DPExpParams(initBounds=initBounds,offsetProb=offsetProb)
                                                        #p1.update(probOfSkipCm=probOfSkipCm)
                                                        p1.update(relaxation=relaxation, maxDwIterations=maxDwIterations, maxSimplexIterations=maxSimplexIterations)
                                                        p1.update(maxSetSizeToConstrain=maxSetSizeToConstrain, minSumForCuts=minSumForCuts, maxCutRounds=maxCutRounds)
                                                        if (relaxation != "dw" and maxSetSizeToConstrain > 0 and minSumForCuts > 1.001 and maxCutRounds > 1):
                                                            pass
                                                        else:
                                                            experiments.append(all + dataset + msl + mns + p1)
        elif self.expname == "formulations":
            all.update(parser="ilp-corpus")
            for dataset in datasets:
                formulations = ["deptree-dp-proj", "deptree-explicit-proj", "deptree-flow-nonproj", "deptree-flow-proj", "deptree-multiflow-nonproj", "deptree-multiflow-proj" ]
                for formulation in formulations:
                    ilpform = DPExpParams(formulation=formulation)
                    experiments.append(all + dataset + ilpform)
        elif self.expname == "corpus-size":
            # For ilp-corpus testing:
            #  all.update(iterations=1)
            all.set("lambda",0.0)
            # For SIMPLEX testing
            #  all.update(formulation="deptree-flow-nonproj-lprelax")
            for parser in ["cky"]: #["ilp-corpus","ilp-sentence"]:
                par = DPExpParams(parser=parser)
                for dataset in datasets:
                    for maxSentenceLength in [7,10,20]:
                        msl = DPExpParams(maxSentenceLength=maxSentenceLength)
                        # Limiting to max  7 words/sent there are 2394 sentences
                        # Limiting to max 10 words/sent there are 5040 sentences
                        # Limiting to max 20 words/sent there are 20570 sentences
                        if (maxSentenceLength == 7):
                            mns_list = range(200,2200,200)
                        elif (maxSentenceLength == 10):
                            mns_list = range(100,5040,100)
                        else: # for 20
                            mns_list = range(1000,20570,1000)
                        for maxNumSentences in mns_list:
                            mns = DPExpParams(maxNumSentences=maxNumSentences)
                            experiments.append(all + dataset + msl + par + mns)
        elif self.expname == "deltas":
            for dataset in datasets:
                for maxSentenceLength in [5,7]:
                    msl = DPExpParams(maxSentenceLength=maxSentenceLength)
#                    if (maxSentenceLength == 5):
#                        mns_list = range(100,1500,100)
#                    elif (maxSentenceLength == 7):
#                        mns_list = range(100,2200,100)
                    mns_list = [2,4,8,16,32,64,128,256,512,1024]
                    for maxNumSentences in mns_list:
                        mns = DPExpParams(maxNumSentences=maxNumSentences)
                        # It doesn't make sense to do D-W for ilp-corpus, because there are no coupling constraints
                        experiments.append(all + dataset + msl + mns + DPExpParams(parser="ilp-corpus"))
                        for dataGen in [dgFixedInterval, dgFactor]:
                            for ilpSolver in ["cplex","dip-milpblock-cpm","dip-milpblock-pc"]:
                                experiments.append(all + dataset + msl + mns + DPExpParams(parser="ilp-deltas", ilpSolver=ilpSolver) + dataGen)
                                if ilpSolver == "cplex":
                                    experiments.append(all + dataset + msl + mns + DPExpParams(parser="ilp-deltas-init", ilpSolver=ilpSolver) + dataGen)

        else:
            raise Exception("Unknown expname: " + str(self.expname))
                
        print "Number of experiments:",len(experiments)
        print "Number of experiments: %d" % (len(experiments))
        root_stage = RootStage()
        root_stage.add_dependents(experiments)
        return root_stage
  
if __name__ == "__main__":
    usage = "%prog "

    parser = OptionParser(usage=usage)
    parser.add_option('-q', '--queue', help="Which SGE queue to use")
    parser.add_option('-f', '--fast', action="store_true", help="Run a fast version")
    parser.add_option('--test', action="store_true", help="Use test data")
    parser.add_option('--expname',  help="Experiment name")
    parser.add_option('--data',  help="Dataset to use")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)
    
    runner = DepParseExpParamsRunner(options)
    root_stage = runner.get_experiments()
    runner.run_pipeline(root_stage)


