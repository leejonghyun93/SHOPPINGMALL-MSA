FROM nginx:1.25-alpine

RUN apk add --no-cache wget

RUN rm -f /etc/nginx/conf.d/default.conf

RUN mkdir -p /var/www/html/vue-app && \
    chown -R nginx:nginx /var/www/html && \
    chmod -R 755 /var/www/html

COPY vue-dist/ /var/www/html/vue-app/

COPY nginx.conf /etc/nginx/nginx.conf

RUN chmod 644 /etc/nginx/nginx.conf

RUN chown -R nginx:nginx /var/www/html/vue-app/ && \
    find /var/www/html/vue-app/ -type f -exec chmod 644 {} \; && \
    find /var/www/html/vue-app/ -type d -exec chmod 755 {} \;

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]