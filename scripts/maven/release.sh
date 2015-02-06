# Automated script for performing a release of prim, optimize, optimize-wrappers, and pacaya.
#
# This release script makes the following assumptions:
# - Master maintains tagged releases.
# - Develop contains the code which is about to be released.
#
# NOTE: For private releases we do NOT use the release branch.

set -e
	
# Function copied from http://stackoverflow.com/questions/3545292/how-to-get-maven-project-version-to-the-bash-command-line
# Advances the last number of the given version string by one.
function advance_version () {
    local v=$1
    # Get the last number. First remove any suffixes (such as '-SNAPSHOT').
    local cleaned=`echo $v | sed -e 's/[^0-9][^0-9]*$//'`
    local last_num=`echo $cleaned | sed -e 's/[0-9]*\.//g'`
    local next_num=$(($last_num+1))
    # Finally replace the last number in version string with the new one.
    echo $v | sed -e "s/[0-9][0-9]*\([^0-9]*\)$/$next_num/"
}

# Confirms whether or not to run a specific command.
function confirm( ) {
    echo "About to $@....";
    read -r -p "Are you sure? [Y/n]" response
    case "$response" in
        [yY][eE][sS]|[yY]) 
            "$@"
            ;;
        *)
            echo "Exiting..."
            exit
            ;;
    esac
}

# Asks whether to continue or not.
function ask_to_continue( ) {
    read -r -p "Continue? [Y/n]" response
    case "$response" in
        [yY][eE][sS]|[yY]) 
            echo "Continuing..."
            ;;
    *)
            echo "Exiting..."
            exit
            ;;
    esac
}

function export_version_number ( ) {
    local DIR=$1
    cd $DIR
    
    echo "About to checkout the develop branch in ${DIR} then pull"
    ask_to_continue
    git checkout develop
    git pull

    echo "Exporting the version numbers we'll be using."
    
    # Below, replace X.Y.Z with the release number.
    # This is the current version in the pom.xml without the -SNAPSHOT suffix.
    export CUR_VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -Ev '(^\[|Download\w+:)'`
    export RELEASE_VERSION=`echo ${CUR_VERSION} | sed -e 's/-SNAPSHOT//'`

    # This is the next version after this release. Don't include the -SNAPSHOT suffix.
    export NEXT_VERSION="$(advance_version ${RELEASE_VERSION})-SNAPSHOT"
}

function echo_version_number ( ) {
    echo CUR_VERSION=${CUR_VERSION}
    echo RELEASE_VERSION=${RELEASE_VERSION}
    echo NEXT_VERSION=${NEXT_VERSION}
}

function check_version_matches ( ) {
    local DIR=$1
    cd $DIR
    git checkout develop
    mvn versions:set -DoldVersion=${CUR_VERSION} -DnewVersion=${CUR_VERSION}
}

function private_release( ) {
    local DIR=$1
    cd $DIR

    echo "0. Perform a private release of each of this project's dependencies."
    echo "NOTE: You must have already done this manually before you proceed."
    ask_to_continue

    echo "1. Ensure th# at you're on the master branch, and merge the develop branch."
    git checkout develop
    git pull
    git checkout master
    git pull
    git merge --no-ff --no-commit develop
    confirm git commit

    echo "2. Cache the version numbers we'll be using."
    echo "The following version numbers will be used. If these are incorrect, please fix them and restart the release."
    echo_version_number
    continue    

    echo "3. Then change the Pacaya version number from X.Y.Z-SNAPSHOT to X.Y.Z."
    if [[ ${CHECK_CUR_VERSIONS} == "TRUE" ]]; then
        mvn versions:set -DoldVersion=${CUR_VERSION} -DnewVersion=${RELEASE_VERSION}
    else
        mvn versions:set -DnewVersion=${RELEASE_VERSION}
    fi

    echo "4. Open pom.xml and update the version numbers for Prim and Optimize to their non-SNAPSHOT versions from the release you just did."
    #${EDITOR:-emacs} pom.xml
    mvn versions:update-properties -DallowSnapshots=false

    echo "NOTE: All changes for this release should have been made. The next step is deploying/committing."
    ask_to_continue

    echo "5. Setup a tunnel to checker, the CLSP maven repository. Then deploy to the CLSP maven repository."
    confirm killall ssh
    if [ -f ~/bin/artifactory-tunnel-clsp]; then
        ~/bin/artifactory-tunnel-clsp
    else
        ssh -f -N -L 8081:checker:8081 -2 login.clsp.jhu.edu			
    fi
    mvn deploy -DskipTests -Pclsp 
    
    echo "6. Setup a tunnel to the COE maven repository. Then deploy to the COE maven repository. (Important Note: this requires that you have correctly configured you ~/.m2/settings.xml to include proper authentication in order to deploy. You must copy the <servers/> section from /export/common/tools/maven/conf/settings.xml.)"
    confirm killall ssh
    if [ -f ~/bin/artifactory-tunnel-coe]; then
        ~/bin/artifactory-tunnel-coe
    else
        ssh -f -N -L 8081:10.162.95.47:8081 -2 external.hltcoe.jhu.edu
    fi
    mvn deploy -DskipTests -Pcoe

    echo "7. Commit the non-SNAPSHOT release and tag it."
    git commit -a -m "Release ${RELEASE_VERSION}"
    git tag v${RELEASE_VERSION}

    echo "8. Open pom.xml, increment the version number to X.Y.Z+1, commit the change, and merge back to master."
    git checkout develop
    git merge --no-ff --no-commit master
    confirm git commit 
    if [[ ${CHECK_CUR_VERSIONS} == "TRUE" ]]; then
        mvn versions:set -DoldVersion=${RELEASE_VERSION} -DnewVersion=${NEXT_VERSION}
    else
        mvn versions:set -DnewVersion=${NEXT_VERSION}
    fi
    
    echo "9. Switch depedencies back to their latest SNAPSHOT version."
    mvn versions:update-properties -DallowSnapshots=true

    git commit -a -m "Updating version to ${NEXT_VERSION}"
    git push --tags
    git push origin master
    git push origin develop
}

export_version_number ~/research/pacaya2
echo_version_number

check_version_matches ~/research/prim
check_version_matches ~/research/optimize
check_version_matches ~/research/optimize-wrappers
check_version_matches ~/research/pacaya2

exit 1

CHECK_CUR_VERSIONS="TRUE"
private_release ~/research/prim
private_release ~/research/optimize
private_release ~/research/optimize-wrappers
private_release ~/research/pacaya2

