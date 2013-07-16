alias runhist='history | grep run_experiments.py'

ROOT_DIR=`pwd`
#ROOT_DIR=`dirname $0`

echo "Setting CLASSPATH by running mvn-classpath."
export CLASSPATH=$ROOT_DIR/target/classes:$ROOT_DIR/lib/*
export CLASSPATH=$CLASSPATH:`$ROOT_DIR/scripts/experiments/mvn-classpath`
# The old way of setting the class path is below, we simply used the
# output classes and the lib directory. Yet, we now have maven dependencies
# that are not included in the lib directory.


echo "Setting PYTHONPATH."
export PYTHONPATH=$ROOT_DIR/scripts:$PYTHONPATH:/Library/Python/2.6/site-packages/
export PYTHONPATH=$PYTHONPATH:/home/hltcoe/mgormley/installed/gdata-2.0.14/src

echo "Adding to PATH."
export PATH=$ROOT_DIR/bin:$ROOT_DIR/dip_parse:$PATH
export PATH=$ROOT_DIR/scripts/experiments:$ROOT_DIR/scripts/experiments/core:$PATH
