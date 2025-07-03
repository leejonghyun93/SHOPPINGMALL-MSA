import axios from "axios"
import { jwtDecode } from "jwt-decode"

const instance = axios.create({
    // Vite 프록시를 사용하므로 baseURL 제거하거나 상대경로 사용
    timeout: 10000,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 요청 인터셉터
instance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("token")

        if (token) {
            try {
                const decoded = jwtDecode(token)
                const now = Date.now() / 1000

                if (decoded.exp < now) {
                    alert("토큰이 만료되었습니다. 다시 로그인해주세요.")
                    localStorage.clear()
                    window.location.href = "/login"
                    return Promise.reject(new Error("토큰 만료"))
                }

                config.headers["Authorization"] = `Bearer ${token}`
            } catch (error) {
                console.error('토큰 디코딩 에러:', error)
                localStorage.removeItem("token")
            }
        }

        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 응답 인터셉터
instance.interceptors.response.use(
    (response) => {
        return response
    },
    (error) => {
        if (error.response?.status === 401) {
            localStorage.clear()
            window.location.href = "/login"
        }
        return Promise.reject(error)
    }
)

export default instance