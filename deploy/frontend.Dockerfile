FROM nginx:1.28-alpine
COPY --chmod=0644 deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY front/dist/ /usr/share/nginx/html/
RUN find /usr/share/nginx/html -type d -exec chmod 0755 {} + \
    && find /usr/share/nginx/html -type f -exec chmod 0644 {} +
EXPOSE 80
