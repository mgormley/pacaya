# Syncs the remote results with a local copy of them.
#

RSYNC=(rsync -azv -e "ssh external.hltcoe.jhu.edu ssh")
LOCAL_COPY=./remote_exp
SERVER=test4
REMOTE_EXP=$SERVER:/export/common/SCALE13/Text/u/mgormley/active/working--parsing--exp
#SERVER=external.hltcoe.jhu.edu
#REMOTE_EXP=~/working/parsing/exp/

echo "RSYNC=$RSYNC"
echo "LOCAL_COPY=$LOCAL_COPY"
echo "REMOTE_EXP=$REMOTE_EXP"
echo ""

echo "Syncing results..."
"${RSYNC[@]}" $REMOTE_EXP/ $LOCAL_COPY \
    --include="/*/" \
    --include="scrape*/" \
    --include="*.csv" \
    --include="README" \
    --include="results.csv" \
    --include="results.tsv" \
    --include="results.data" \
    --exclude="*" 
echo ""

echo "Syncing grammar induction output..."
"${RSYNC[@]}" $REMOTE_EXP/vem-conll_005/ $LOCAL_COPY/vem-conll_005 \
    --include="/*/" \
    --include="*parses.txt" \
    --exclude="*" 
"${RSYNC[@]}" $REMOTE_EXP/vem-conll_006/ $LOCAL_COPY/vem-conll_006 \
    --include="/*20*/" \
    --include="*parses.txt" \
    --exclude="*" 

echo "Syncing Brown cluster output..."
"${RSYNC[@]}" $SERVER:/home/hltcoe/mgormley/working/word_embeddings/bc_out_256/ ./data/bc_out_256 \
    --include="/*/" \
    --include="paths.cutoff" \
    --exclude="*"
"${RSYNC[@]}" $SERVER:/home/hltcoe/mgormley/working/word_embeddings/bc_out_1000/ ./data/bc_out_1000 \
    --include="/**/" \
    --include="paths" \
    --exclude="*"
head -n 10 data/bc_out_1000/full.txt_*/bc/paths | grep -P "^0" > data/bc_out_1000/paths.tiny

echo "Adding symbolic link to vem-conll_00{5,6}"
ln -s `pwd`/remote_exp/vem-conll_005/ ./exp/vem-conll_005
ln -s `pwd`/remote_exp/vem-conll_006/ ./exp/vem-conll_006


# ------------------------ Trash ----------------------------------------
# Normally would include -f to force to background, but instead using eval.
#eval "ssh -N -L localhost:8910:test4:22 -2 external.hltcoe.jhu.edu &"
#TUNNELPID=$!
#echo "Killing ssh tunnel with pid $TUNNELPID"
#kill $TUNNELPID
