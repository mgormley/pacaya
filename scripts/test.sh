#!/bin/bash

#DATA=./data/treebank_3/wsj/00
DATA=./data/small

time java edu.jhu.PipelineRunner --train $DATA --formulation deptree-flow-nonproj
time java edu.jhu.PipelineRunner --train $DATA --formulation deptree-explicit-proj
time java edu.jhu.PipelineRunner --train $DATA --formulation deptree-dp-proj
