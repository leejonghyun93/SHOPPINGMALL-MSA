// 🔥 axiosInstance.js 수정 - 인터셉터 문제 해결

import axios from 'axios'

// 상품 API 클라이언트 (포트 8080)
const apiClient = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    }
})

// 🔥 요청 인터셉터 - 단순화
apiClient.interceptors.request.use(
    (config) => {
        console.log('🚀 API 요청:', config.method?.toUpperCase(), config.url, config.params)
        return config
    },
    (error) => {
        console.error('❌ 요청 에러:', error)
        return Promise.reject(error)
    }
)

// 🔥 응답 인터셉터 - 데이터 변환 방지
apiClient.interceptors.response.use(
    (response) => {
        console.log('✅ API 응답:', response.status, response.config.url)
        console.log('📊 응답 데이터:', response.data)

        // 🚨 문제가 될 수 있는 부분 - 응답을 변조하는 경우
        // 만약 이런 코드가 있다면 제거하세요:

        // return {
        //   success: true,
        //   message: "성공",
        //   data: response.data || []
        // }

        // 🔥 올바른 방식 - 원본 응답 그대로 반환
        return response
    },
    (error) => {
        console.error('🚨 API 에러:', error.response?.status || error.message, error.config?.url)

        // withAuth: false인 경우 특별 처리
        if (error.config?.withAuth === false) {
            console.log('🔓 인증 없는 요청 - 빈 배열 반환')
            return Promise.resolve({
                data: [],
                status: 200,
                statusText: 'OK'
            })
        }

        return Promise.reject(error)
    }
)
apiClient.interceptors.response.use(
    (response) => {
        console.log('✅ API 응답:', response.status, response.config.url)
        console.log('📊 응답 데이터:', response.data)

        // 원본 응답 그대로 반환
        return response
    },
    (error) => {
        console.error('🚨 API 에러:', error.response?.status || error.message, error.config?.url)

        // 🔥 withAuth: false여도 실제 에러는 그대로 전달
        return Promise.reject(error)
    }
)
export default apiClient