#!/bin/bash

docker-compose -f ap-bigstock-compose.yaml down

docker images | grep "ap-bigstock" | awk '{print $3}' | xargs docker rmi -f


cd ../../../

mvn -s ./settings.xml clean install -DskipTests=true -Denv=dev

cp ./auth/target/bigstock-auth.jar ./auth/docker/bigstock-auth.jar
cp ./biz/target/bigstock-biz.jar ./biz/docker/bigstock-biz.jar
cp ./gateway/target/bigstock-gateway.jar ./gateway/docker/bigstock-gateway.jar
cp ./schedule/target/bigstock-schedule.jar ./schedule/docker/bigstock-schedule.jar


cd ./docker-compose-folder/init/ap/

docker-compose -f ap-bigstock-compose.yaml up -d