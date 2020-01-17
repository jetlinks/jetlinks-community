FROM nginx
ADD nginx.conf /etc/nginx/conf.d/default.conf
ADD docker-entrypoint.sh /docker-entrypoint.sh

CMD ["sh","/docker-entrypoint.sh"]
#ADD oauth2 /usr/share/nginx/html/oauth2