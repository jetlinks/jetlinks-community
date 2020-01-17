#!/usr/bin/env bash

./mvnw clean package -Dmaven.test.skip=true
if [ $? -ne 0 ];then
    echo "构建失败!"
else
  cd ./jetlinks-standalone || exit
  ../mvnw docker:build && docker push registry.cn-shenzhen.aliyuncs.com/jetlinks/jetlinks-standalone
fi