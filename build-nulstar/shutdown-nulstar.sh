#!/bin/bash
cd `dirname $0`
KILL_WAIT_COUNT=120
modules=(%MODULES%)
stop(){
    pid=$1;
    kill $pid > /dev/null 2>&1
    COUNT=0
    while [ $COUNT -lt ${KILL_WAIT_COUNT} ]; do
        echo -e ".\c"
        sleep 1
        let COUNT=$COUNT+1
        PID_EXIST=`ps -f -p $pid | grep java`
        if [ -z "$PID_EXIST" ]; then
#            echo -e "\n"
#            echo "stop ${pid} success."
            return 0;
        fi
    done

    echo "stop ${pid} failure,dump and kill -9 it."
    kill -9 $pid > /dev/null 2>&1
}

APP_PID=`ps -ef|grep -w "Nulstar"|grep -v grep|awk '{print $2}'`
if [ -z "${APP_PID}" ]; then
 echo "not running"
        exit 0
fi
echo "stoping"
for pid in $APP_PID
do
   stop $pid
done
for module in ${module[@]}
do
    APP_PID=`ps -ef|grep -w ${module}|grep -v grep|awk '{print $2}'`
    if [ -z "${APP_PID}" ]; then
        echo "${module} not running"
    fi
    stop $APP_PID
done
echo ""
echo "shutdown success"