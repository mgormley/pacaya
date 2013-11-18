# Installs the 3rd-party jars to your local maven install.
#

set -e

CPLEX=lib/cplex.jar
GUROBI=lib/gurobi.jar
JOPTIMIZER=lib/joptimizer-1.3.0.jar


for JARFILE in $CPLEX $GUROBI $JOPTIMIZER
do
    if [ ! -f $JARFILE ]; then
	echo "File not found: $JARFILE"
	exit 1
    fi
done

mvn install:install-file -Dfile=$CPLEX -DgroupId=cplex \
    -DartifactId=cplex -Dversion=12.5 -Dpackaging=jar

mvn install:install-file -Dfile=$GUROBI -DgroupId=gurobi \
    -DartifactId=gurobi -Dversion=3.0.1 -Dpackaging=jar

mvn install:install-file -Dfile=$JOPTIMIZER -DgroupId=joptimizer \
    -DartifactId=joptimizer -Dversion=1.3.0 -Dpackaging=jar

exit 0

