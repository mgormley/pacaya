echo "Setting CLASSPATH from maven."
export CLASSPATH=`mvn exec:exec -q -Dexec.executable="echo" -Dexec.args="%classpath"`


