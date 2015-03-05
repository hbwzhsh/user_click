#!/bin/bash

source /etc/profile

yesterday(){
        day=$1
        ns=`date -d $day +%s`
        ts=$[ $ns - 86400]
        echo `date -d @$ts +%Y-%m-%d`
}

yesterday2(){
        day=$1
        ns=`date -d $day +%s`
        ts=$[ $ns - 86400]
        echo `date -d @$ts +%Y%m%d`
}

d=`date +%Y-%m-%d`
echo $d
yesd=`yesterday $d`
echo $yesd
yesd2=`yesterday2 $d`
echo $yesd2

mkdir parsedLogs/$yesd2
mv parsedLogs/parsedInfo_$yesd*.log parsedLogs/$yesd2
