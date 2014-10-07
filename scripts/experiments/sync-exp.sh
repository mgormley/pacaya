# Syncs the remote results with a local copy of them.
#

RSYNC=(rsync -azv -e "ssh external.hltcoe.jhu.edu ssh")
LOCAL_COPY=./remote_exp
SERVER=test2
REMOTE_EXP=$SERVER:/export/projects/mgormley/exp_dirs/working--pacaya--exp
#Old Remote exp directory: REMOTE_EXP=$SERVER:/export/common/SCALE13/Text/u/mgormley/active/working--parsing--exp
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

