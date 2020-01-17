#!/usr/bin/env bash

API_BASE_PATH=$API_BASE_PATH;

if [ -z "$API_BASE_PATH" ]; then
    API_BASE_PATH="http://jetlinks:8844/";
fi

apiUrl="proxy_pass  $API_BASE_PATH;"

sed -i '18c '"$apiUrl"'' /etc/nginx/conf.d/default.conf

nginx -g "daemon off;"