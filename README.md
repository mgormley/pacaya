
# Setup

## Install Dependencies

This project has several dependencies. Currently, these are accessible
from CLSP's internal maven repository, or they can be installed
locally.
* Prim: a Java primitives library
* Optimize: a Java optimization library

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
	git clone git@bitbucket.org:minyans/optimize.git
	cd optimize
	mvn install -DskipTests

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

