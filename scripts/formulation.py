import os
import sys

formulations = ["deptree-dp-proj", "deptree-explicit-proj", "deptree-flow-nonproj", "deptree-flow-proj", "deptree-multiflow-nonproj", "deptree-multiflow-proj" ]
for formulation in formulations:
    print formulation
    cmd = "java edu.jhu.hltcoe.PipelineRunner --train ../data/treebank_3/wsj/00 --formulation %s --maxSentenceLength 10 --maxNumSentences 2 --iterations 1" % (formulation)
    os.system(cmd)
    
