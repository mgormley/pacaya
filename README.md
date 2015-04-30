# Summary

Pacaya is a library for joint modeling with graphical models, structured factors,
neural networks, and hypergraphs.

# Setup

## Install Dependencies

This project has several dependencies. 
* Prim: a Java primitives library
* Optimize: a Java optimization library
* Optimize Wrappers (only for unit tests): wrappers for various optimizers that are 
  not included in the standard Optimize library.

Currently, these are accessible from the COE's internal maven repository, or they 
can be installed locally. You can use the COE maven repository if you are installing a tagged 
release of Pacaya. If you are trying to build a development branch,
you should install the dependencies locally.

### Using the COE's internal maven repository

If you are installing on the COE grid, you can skip the rest of this 
section.

If you are installing somewhere other than the COE grid, set up an ssh 
tunnel to the COE maven repository and update your settings.xml file 
to point to localhost. 

### Installing dependencies locally

Currently you must request permission to access these private
repositories. Email mrg@cs.jhu.edu for access with your GitLab username.

1. Checkout and install Prim locally
	git clone https://gitlab.hltcoe.jhu.edu/mgormley/prim.git
	cd prim
	mvn install -DskipTests
2. Checkout and install Optimize locally
	git clone https://gitlab.hltcoe.jhu.edu/mgormley/optimize.git
	cd optimize
	mvn install -DskipTests
3. Checkout and install Optimize Wrappers locally
	git clone https://gitlab.hltcoe.jhu.edu/mgormley/optimize-wrappers.git
	cd optimize-wrappers
	mvn install -DskipTests

## Build:

* Compile the code from the command line:

    mvn compile

* To build a single jar with all the dependencies included:

    mvn compile assembly:single

* To set the classpath using maven:
	
	source setupenv.sh

## Eclipse setup:

* Create local versions of the .project and .classpath files for Eclipse:

    mvn eclipse:eclipse

* Add M2_REPO environment variable to
  Eclipse. http://maven.apache.org/guides/mini/guide-ide-eclipse.html
  Open the Preferences and navigate to 'Java --> Build Path -->
  Classpath Variables'. Add a new classpath variable M2_REPO with the
  path to your local repository (e.g. ~/.m2/repository).

* To make the project Git aware, right click on the project and select Team -> Git... 
    
# Maven Releases

The workflow for releasing pacaya and its dependencies involves both maven and git.

## Private Releases

For private releases, we do NOT use the release branch.

1. Perform a private release of Prim and Optimize.
2. Cache the version numbers we'll be using.
		
		# Below, replace X.Y.Z with the release number.
		# This is the current version in the pom.xml without the -SNAPSHOT suffix.
		RELEASE_VERSION=X.Y.Z
		
		# This is the next version after this release. Don't include the -SNAPSHOT suffix.
		NEXT_VERSION=X.Y.Z+1		

3. Ensure that you're on the master branch. Then change the Pacaya version number from X.Y.Z-SNAPSHOT to X.Y.Z. 

		git checkout master	
		mvn version:set -DoldVersion=${RELEASE_VERSION}-SNAPSHOT -DnewVersion=${RELEASE_VERSION}
		
4. Open pom.xml and update the version numbers for Prim and Optimize to their non-SNAPSHOT versions from the release you just did.
5. Setup a tunnel to checker, the CLSP maven repository. Then deploy to the CLSP maven repository.

		ssh -f -N -L 8081:checker:8081 -2 login.clsp.jhu.edu			
		mvn deploy -DskipTests -Pclsp 
  	
6. Setup a tunnel to the COE maven repository. Then deploy to the COE maven repository. (Important Note: this requires that you have correctly configured you ~/.m2/settings.xml to include proper authentication in order to deploy. You must copy the <servers/> section from /export/common/tools/maven/conf/settings.xml.)

		ssh -f -N -L 8081:10.162.95.47:8081 -2 external.hltcoe.jhu.edu
		mvn deploy -DskipTests -Pcoe

7. Commit the non-SNAPSHOT release and tag it.
	
		git commit -m "Release ${RELEASE_VERSION}"
		git tag pacaya-${RELEASE_VERSION}

8. Open pom.xml, increment the version number to X.Y.Z+1, commit the change, and merge back to master.
		
		mvn version:set -DoldVersion=${RELEASE_VERSION} -DnewVersion=${NEXT_VERSION}-SNAPSHOT
		git commit -m "Updating version to ${NEXT_VERSION}-SNAPSHOT"
		git push --tags
		git push

## Public (GitHub) Releases

1. Perform a private release of Prim and Optimize.

2. Merge the latest changes from master to the release branch.
		
		git checkout release
		git merge master
		
3. Cache the version numbers we'll be using.
		
		# Below, replace X.Y.Z with the release number.
		# This is the current version in the pom.xml without the -SNAPSHOT suffix.
		RELEASE_VERSION=X.Y.Z
		
		# This is the next version after this release. Don't include the -SNAPSHOT suffix.
		NEXT_VERSION=X.Y.Z+1		

4. Change the Pacaya version number from X.Y.Z-SNAPSHOT to X.Y.Z. 
		
		mvn version:set -DoldVersion=${RELEASE_VERSION}-SNAPSHOT -DnewVersion=${RELEASE_VERSION}
		
5. Open pom.xml and update the version numbers for Prim and Optimize to their non-SNAPSHOT versions from the release you just did.
6. Deploy to Maven Central. (First, ensure that the password is correctly set in ~/.m2/settings.xml.) 

		mvn deploy -DskipTests -Prelease 

7. Commit the non-SNAPSHOT release and tag it.
	
		git commit -m "Release ${RELEASE_VERSION}"
		git tag pacaya-${RELEASE_VERSION}

8. Open pom.xml, increment the version number to X.Y.Z+1, commit the change, and merge back to master.
		
		mvn version:set -DoldVersion=${RELEASE_VERSION} -DnewVersion=${NEXT_VERSION}-SNAPSHOT
		git commit -m "Updating version to ${NEXT_VERSION}-SNAPSHOT"
		git checkout master
		
		# Only if it is safe, should we merge the changes back to the master branch.
		# Otherwise, we may just need to cherry-pick the appropriate commits.
		# git merge release



