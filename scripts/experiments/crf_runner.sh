#!/bin/bash
#

#SRL_DIR=/Users/mgormley/research/srl
SRL_DIR=/home/hltcoe/mgormley/working/srl

# CPU: -agentlib:hprof=cpu=samples,depth=7,thread=y \
# HEAP: -agentlib:hprof=heap=sites,depth=7,thread=y \
java -ea -Xmx1G \
-agentlib:hprof=cpu=samples,depth=7,thread=y \
    edu.jhu.gm.data.CrfRunner \
    --featureFileIn=$SRL_DIR/train_test/typed.themes_split.no_link_factor.template \
    --trainType=ERMA \
    --train=$SRL_DIR/train_test/typed.themes_split.no_link_factor.train \
    --printModel=model.txt \
    --trainPredOut=train.predictions \

