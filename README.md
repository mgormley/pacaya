
# Setup

## Install Dependencies

This project has several dependencies. Currently, these are accessible
from CLSP's internal maven repository, or they can be installed
locally.
* Prim: a Java primitives library
* Optimize: a Java optimization library
* Erma: used only for the ERMA file format
* [optional] PyPipeline: for running experiments

### Using CLSP's internal maven repository

1. Copy the settings.xml from this repository to your .m2
   directory. Caution: this may overwrite your existing settings.

	cp ~/.m2/settings{,.bak}
	cp ./scripts/maven/settings.xml ~/.m2/settings.xml

2. Set up a tunnel to the maven repository.
    ssh -f -N -L 8081:checker:8081 -2 login.clsp.jhu.edu

### Installing dependencies locally

Currently you must request permission to access these private
repositories. Email mrg@cs.jhu.edu for access with your GitLab and
Bitbucket usernames.

1. Checkout and install Prim locally
	git clone https://gitlab.hltcoe.jhu.edu/mgormley/prim.git
	cd prim
	mvn install -DskipTests
2. Checkout and install Optimize locally
	git clone https://gitlab.hltcoe.jhu.edu/mgormley/optimize.git
	cd optimize
	mvn install -DskipTests
3. Checkout and install Erma locally
	git clone git@github.com:jeisner/erma.git
	cd erma
	mvn install -DskipTests
4. Checkout and install PyPipeline locally
	git clone https://gitlab.hltcoe.jhu.edu/mgormley/pypipeline.git
	cd pypipeline
	python setup.py develop --user

## Build:

* Compile the code from the command line:

    mvn compile

* To build a single jar with all the dependencies included:

    mvn compile assembly:single

## Eclipse setup:

* Create local versions of the .project and .classpath files for Eclipse:

    mvn eclipse:eclipse

* Add M2_REPO environment variable to
  Eclipse. http://maven.apache.org/guides/mini/guide-ide-eclipse.html
  Open the Preferences and navigate to 'Java --> Build Path -->
  Classpath Variables'. Add a new classpath variable M2_REPO with the
  path to your local repository (e.g. ~/.m2/repository).

* To make the project Git aware, right click on the project and select Team -> Git... 

* If using PyDev, there is an outstanding bug
  (http://sourceforge.net/p/pydev/bugs/1286/) which might not make the
  egg-link visible to PyPipeline within Eclipse. In this case, the
  pypipeline project directory can be explicitly added to the
  PYTHONPATH within the Eclipse project.


## Running ACE Relation Extraction Experiments

Ensure that you have properly installed PyPipeline by running "python"
followed by "import pypipeline.pipeline".

The following experiment scripts should work fine on the CLSP grid
except for two big issues: 

1. My scripts, namely param_defs.py will be looking for the corpus
   files at paths that don't exist. For example, it will expect to
   find the LDC directories in /export/common/data/corpora/LDC 

2. The definitions for how to run jobs on the CLSP grid on line 20 of
   qsub.py might be out of date. For example, I can't remember if
   we're still supposed to use the "-pe smp" flag.

If you can fix these two issues, everything should work seamlessly.

Run a "fast" version of the experiments (should complete in about a
minute) with output printed to stdout:
    
    mkdir ./exp/
    source setupenv.sh
    run_ace2.py -e ace-subtypes -f

Create all the experiments directories, but don't actually run them on
the grid:

    source setupenv.sh
    run_ace2.py -e ace-subtypes -n

The above command should create a subdirectory named
exp/ace-subtypes_000/ which contains a directory for each
experiment. The contents of each directory will be a qsub_script.sh
that launches that experiment on the grid, a qdel_script.sh that stops
the experiment if already running on the grid, an experiment_script.sh
that actually runs the experiment locally (and is called by qsub_script.sh).
    
Run the full set of experiments on the grid:
    
    source setupenv.sh
    run_ace2.py -e ace-subtypes -q clsp

The above command will be just like the -n version except that it will
create the "next" experiment subdirectory in exp/ace-subtypes_001/ and
then run each of the experiments on the grid by calling each
qsub_script.sh in the appropriate topological order. The output of
each experiment will be in the file stdout in the experiment
directory. There will be two special directories exp/scape_ace/ and
exp/hyperparam_argmax/. These will each contain a results.csv file
that summarizes the relevant results. Notably, the
exp/hyperparam_argmax/ file will have done the appropriate
hyperparameter optimization and lists only the best run (as selected
by dev F1) for each experimental setting.

Finally, if you want to run the original experiments from our TACL
submission, you would do the following which invokes the same script
with a different named experiment "ace-pm13".

    source setupenv.sh
    run_ace2.py -e ace-pm13 -q clsp

## Running ACL '14 Experiments

Ensure that you have properly installed PyPipeline by running "python"
followed by "import pypipeline.pipeline".

The following experiment scripts should work fine on the COE grid
where the corpus files can be found in the appropriate places.

Run a "fast" version of the experiments (should complete in about a
minute) with output printed to stdout:
    
    source setupenv.sh
    run_srl.py -e srl-conll09 -f

Run the full set of experiments on the grid:
    
    source setupenv.sh
    run_srl.py -e srl-conll09 -q mem
