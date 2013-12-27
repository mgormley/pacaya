# Example argument: 
# 
# ../other_lib/mate-tools-read-only/srl/featuresets/spa/a*.feats

FEATURESETS="../other_lib/mate-tools-read-only/srl/featuresets"

createFeats() {
    
    LANGSHORT=$1
    PREFIX=$2
    FEAT_FILES=$FEATURESETS/$LANGSHORT/$PREFIX*.feats
    OUT_FILE=tmp.out.$LANGSHORT.$PREFIX
    
    # Create original list: 
    cat $FEAT_FILES | awk '{print $1;}' | sort | uniq > tmp.feat
    
    # Make a sed script:
    cat src/main/resources/edu/jhu/featurize/bjorkelund-arg-feats.txt | perl -pe "s/# (.*)\n/s\/\1\//" | perl -pe "s/$/\//" | tail -n 31 > tmp.sed
 
    # Run sed to produce final output:
    grep -v "#" tmp.feat | sed -f ./tmp.sed | perl -pe "s/\s*\+\s*/ + /g" > $OUT_FILE

}

createFeats "spa" "a"
createFeats "spa" "pd"