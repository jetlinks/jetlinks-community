#!/usr/bin/env bash

java -jar -Dfile.encoding=UTF-8 device-simulator.jar \
  mqtt.limit=1 \
  mqtt.start=0 \
  mqtt.enableEvent=true \
  mqtt.eventLimit=1 \
  mqtt.eventRate=1000 \
  mqtt.scriptFile=./scripts/demo-device.js \
  mqtt.address=127.0.0.1 \
  mqtt.port=1883


