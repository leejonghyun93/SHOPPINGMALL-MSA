# Dockerfile (사용자 서비스용)
FROM nginx:alpine

# 기본 nginx 설정 제거
RUN rm /etc/nginx/conf.d/default.conf

# 커스텀 nginx 설정 복사
COPY nginx.conf /etc/nginx/nginx.conf

# 사용자 Vue 빌드 파일만 복사
COPY vue-dist/ /var/www/html/vue-app/

# 포트 노출
EXPOSE 80

# nginx 시작
CMD ["nginx", "-g", "daemon off;"]
