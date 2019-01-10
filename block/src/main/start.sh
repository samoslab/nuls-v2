#!/bin/sh

mvn clean -DskipTests=true package

cd target

if [ ! -d "logs" ]; then
    mkdir logs
fi
status=$?
if [ $status -ne 0 ]; then
    echo "start the Server failed."
    exit $status
fi

JAVA_OPTS="-Xms3072m -Xmx3072m -Xss160k -XX:NewRatio=1 -XX:SurvivorRatio=9 -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=5 -XX:MaxTenuringThreshold=15"

CLASS_PATH="./:./API-Server-1.0-SNAPSHOT.jar"

echo "now starting the API-Server..."

# (java -Dfile.encoding=UTF-8 $JAVA_OPTS -classpath $CLASS_PATH io.nuls.main.Main &)
nohup java -Dfile.encoding=UTF-8 $JAVA_OPTS -classpath $CLASS_PATH io.nuls.main.Main >./logs/starting.log 2>&1 &

if [ $? -ne 0 ]; then
    echo "start failed."
else
    echo "start completed."
fi
exit 0



#!/bin/sh
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

SOURCE="$0"
while [ -h "$SOURCE"  ]; do # resolve $SOURCE until the file is no longer a symlink
    DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"
    SOURCE="$(readlink "$SOURCE")"
    [[ ${SOURCE} != /*  ]] && SOURCE=${DIR}/${SOURCE} # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done

SERVER_HOME="$( cd -P "$( dirname "$SOURCE"  )" && cd .. && pwd  )"

export logdir=${SERVER_HOME}/logs

CLASSPATH=${SERVER_HOME}
# add conf to classpath
if [ -d ${SERVER_HOME}/conf ]; then
  CLASSPATH=${CLASSPATH}:${SERVER_HOME}/conf
fi

# add jar to CLASSPATH
for file in ${SERVER_HOME}/*.jar; do
  CLASSPATH=${CLASSPATH}:${file};
done


# add libs to CLASSPATH
for file in ${SERVER_HOME}/libs/*.jar; do
  CLASSPATH=${CLASSPATH}:${file};
done

# Get standard environment variables
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms1024m -Xmx4096m -XX:NewSize=256m -XX:MaxNewSize=256m -XX:MaxDirectMemorySize=256M -XX:PermSize=128M -XX:MaxPermSize=128M"

MAIN_CLASS=io.nuls.client.Bootstrap

if [ ! -d ${logdir} ]; then
  mkdir ${logdir}
fi

# check jre exist
if  [ -x ${SERVER_HOME}/jre/bin/java ]; then
  nohup ${SERVER_HOME}/jre/bin/java ${JAVA_OPTS} -classpath ${CLASSPATH} ${MAIN_CLASS} 1>${SERVER_HOME}/logs/stdout.log 0>${SERVER_HOME}/logs/stderr.log 2>&1 &
  exit 0
fi

JAVA_BIN=`which java`
# try to use JAVA_HOME jre
if [ -x ${JAVA_BIN} ]; then
  nohup ${JAVA_BIN} ${JAVA_OPTS} -classpath ${CLASSPATH} ${MAIN_CLASS} 1>${SERVER_HOME}/logs/stdout.log 0>${SERVER_HOME}/logs/stderr.log 2>&1 &
  exit 0
fi

echo "The JDK required to start NULS was not found."
exit 1