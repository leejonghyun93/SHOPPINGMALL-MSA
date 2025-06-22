import axios from 'axios'

// 상품 API 클라이언트 (포트 8080)
const apiClient = axios.create({
    baseURL: 'http://localhost:8080',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    }
})

// 요청 인터셉터 - 단순화
apiClient.interceptors.request.use(
    (config) => {
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 응답 인터셉터 - 데이터 변환 방지
apiClient.interceptors.response.use(
    (response) => {
        //  원본 응답 그대로 반환
        return response
    },
    (error) => {

        // withAuth: false인 경우 특별 처리
        if (error.config?.withAuth === false) {
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
        // 원본 응답 그대로 반환
        return response
    },
    (error) => {
        return Promise.reject(error)
    }
)
export default apiClient