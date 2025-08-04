// src/config/websocket.js
const SERVER_IP = '192.168.4.126'; // 테스트에서 성공한 IP
const GATEWAY_PORT = 8080;

export const getWebSocketUrl = () => {
    const hostname = window.location.hostname;

    console.log(' WebSocket URL 생성 - 현재 호스트:', hostname);

    // 로컬 개발 환경 (같은 PC에서 개발)
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        const url = `http://localhost:${GATEWAY_PORT}/ws-chat`;
        console.log(' 로컬 환경 WebSocket:', url);
        return url;
    }

    // 다른 PC에서 접속 (테스트에서 성공한 설정)
    const url = `http://${SERVER_IP}:${GATEWAY_PORT}/ws-chat`;
    console.log('원격 환경 WebSocket:', url);
    return url;
};

export const getApiBaseUrl = () => {
    const hostname = window.location.hostname;

    console.log('API URL 생성 - 현재 호스트:', hostname);

    // 로컬 개발 환경
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        const url = `http://localhost:${GATEWAY_PORT}/api`;
        console.log('로컬 환경 API:', url);
        return url;
    }

    // 다른 PC에서 접속
    const url = `http://${SERVER_IP}:${GATEWAY_PORT}/api`;
    console.log(' 원격 환경 API:', url);
    return url;
};

// 환경 감지
export const isLocalEnvironment = () => {
    const hostname = window.location.hostname;
    return hostname === 'localhost' || hostname === '127.0.0.1';
};

// 연결 상태 확인용 함수
export const testConnection = async () => {
    const wsUrl = getWebSocketUrl();
    const apiUrl = getApiBaseUrl();

    console.log('연결 테스트:');
    console.log('WebSocket URL:', wsUrl);
    console.log('API Base URL:', apiUrl);

    return {
        websocketUrl: wsUrl,
        apiBaseUrl: apiUrl,
        isLocal: isLocalEnvironment()
    };
};

// 디버깅용 (개발 환경에서만)
if (typeof window !== 'undefined') {
    window.chatConfig = {
        test: testConnection,
        getWebSocketUrl,
        getApiBaseUrl,
        isLocal: isLocalEnvironment
    };
}