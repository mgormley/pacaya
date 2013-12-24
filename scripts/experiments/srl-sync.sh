# Syncs the remote results with a local copy of them.
#


LOCAL_COPY=./remote_exp
SERVER=external.hltcoe.jhu.edu
REMOTE_EXP=$SERVER:/export/common/SCALE13/Text/u/mgormley/active/working--parsing--exp
#REMOTE_EXP=~/working/parsing/exp/

SUB_DIR=srl-all-sup-lat_003
#LOCAL_COPY=$LOCAL_COPY/$SUB_DIR
#REMOTE_EXP=$REMOTE_EXP/$SUB_DIR

echo "LOCAL_COPY=$LOCAL_COPY"
echo "REMOTE_EXP=$REMOTE_EXP"
echo ""

echo "Syncing results..."
rsync -av $* $REMOTE_EXP/ $LOCAL_COPY \
    --include="/*/" \
    --include="scrape*/" \
    --include="*.csv" \
    --include="results.csv" \
    --include="results.tsv" \
    --include="results.data" \
    --exclude="*" 
echo ""

echo "Syncing grammar induction output..."
rsync -av $* $REMOTE_EXP/vem-conll_005/ $LOCAL_COPY/vem-conll_005 \
    --include="/*/" \
    --include="*parses.txt" \
    --exclude="*" 
echo "Adding symbolic link to vem-conll_005"
ln -s `pwd`/remote_exp/vem-conll_005/ ./exp/vem-conll_005
