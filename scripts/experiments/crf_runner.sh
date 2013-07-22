#!/bin/bash
#

# CPU: -agentlib:hprof=cpu=samples,depth=7,thread=y \
# HEAP: -agentlib:hprof=heap=sites,depth=7,thread=y \
java -ea -Xmx1G \
-agentlib:hprof=cpu=samples,depth=7,thread=y \
    edu.jhu.gm.data.CrfRunner \
    --featureFileIn=/Users/mgormley/research/srl/train_test/typed.themes_split.no_link_factor.template \
    --trainType=ERMA \
    --train=/Users/mgormley/research/srl/train_test/typed.themes_split.no_link_factor.train \
    --printModel=model.txt \
    --trainPredOut=train.predictions \

