# Example argument: 
# 
# ../other_lib/mate-tools-read-only/srl/featuresets/spa/a*.feats

FEATURESETS="../other_lib/mate-tools-read-only/srl/featuresets"
OUT_DIR="./src/main/resources/edu/jhu/featurize"

createFeats() {
    
    LANGSHORT=$1
    PREFIX=$2
    LANGTWO=$3
    TYPE=$4
    FEAT_FILES=$FEATURESETS/$LANGSHORT/$PREFIX*.feats
    OUT_FILE=$OUT_DIR/bjorkelund-$LANGTWO-$TYPE-feats.txt
    
    # Create original list: 
    cat $FEAT_FILES | awk '{print $1;}' | sort | uniq | grep -v -P "^[VN]$" > tmp.feat
    
    # Make a sed script:
    cat src/main/resources/edu/jhu/featurize/bjorkelund-arg-feats.txt | perl -pe "s/# (.*)\n/s\/\1\//" | perl -pe "s/$/\//" | tail -n 31 > tmp.sed
 
    # Print a header 
    echo "# Features selected as in Bjorkelund et al. (2009) for '$TYPE' language '$LANGTWO'" > $OUT_FILE
    # Run sed to produce final output:
    grep -v "#" tmp.feat | sed -f ./tmp.sed | perl -pe "s/\s*\+\s*/ + /g" \
	| perl -pe "s/^(.*lemmaSense.*)$/# \1 ### NOT SUPPORTED/g" >> $OUT_FILE

}

#createFeats "spa" "a" "es" "arg"
#createFeats "spa" "pd" "es" "sense"

createFeats "eng" "a" "en" "arg"
createFeats "eng" "pd" "en" "sense"

createFeats "chi" "a" "zh" "arg"
createFeats "chi" "pd" "zh" "sense"

createFeats "ger" "a" "de" "arg"
createFeats "ger" "pd" "de" "sense"

grep "lemmaSense" $OUT_DIR/bjorkelund-*-*.txt