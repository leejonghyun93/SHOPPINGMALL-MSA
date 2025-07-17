// src/config/environment.js
export const getEnvironmentConfig = () => {
    const isDevelopment = process.env.NODE_ENV === 'development'
    const isProduction = process.env.NODE_ENV === 'production'

    return {
        // API 설정
        apiBaseUrl: process.env.VUE_APP_API_BASE_URL ||
            (isDevelopment ? 'http://localhost:8080' : 'http://13.209.253.241:8080'),

        // NGINX 스트리밍 서버 설정
        nginxHost: process.env.VUE_APP_NGINX_HOST ||
            (isDevelopment ? 'localhost' : '13.209.253.241'),
        nginxPort: process.env.VUE_APP_NGINX_PORT || '8080',

        // 프론트엔드 URL
        frontendUrl: process.env.VUE_APP_FRONTEND_URL ||
            (isDevelopment ? 'http://localhost:5173' : 'https://shopmall.com'),

        // 관리자 URL
        adminUrl: process.env.VUE_APP_ADMIN_URL ||
            (isDevelopment ? 'http://localhost:3000' : 'https://admin.shopmall.com'),

        // 환경 정보
        isDevelopment,
        isProduction,
        nodeEnv: process.env.NODE_ENV
    }
}

// 스트림 URL 생성 헬퍼 함수
export const generateStreamUrl = (streamKey, broadcast = {}) => {
    const host = broadcast.nginx_host || process.env.VUE_APP_NGINX_HOST || 'localhost'
    const port = broadcast.nginx_port || process.env.VUE_APP_NGINX_PORT || '8080'

    return `http://${host}:${port}/hls/${streamKey}/index.m3u8`
}
// CORS 허용 Origin 생성 함수
export const getAllowedOrigins = () => {
    const config = getEnvironmentConfig()

    const origins = [
        config.frontendUrl,
        config.adminUrl
    ]

    // 개발 환경에서만 localhost 추가
    if (config.isDevelopment) {
        origins.push(
            'http://localhost:5173',
            'http://localhost:3000',
            'http://localhost:8080',
            'http://127.0.0.1:5173',
            'http://127.0.0.1:3000'
        )
    }

    return origins
}

export default getEnvironmentConfig