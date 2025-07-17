import axios from "axios"
import { jwtDecode } from "jwt-decode"

//  환경변수 기반 설정
const API_BASE_URL = import.meta.env.VITE_API_URL ||
    (import.meta.env.DEV ? 'http://localhost:8080' : 'http://13.209.253.241:8080')

const instance = axios.create({
    baseURL: API_BASE_URL,
    timeout: 10000,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 요청 인터셉터
instance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("jwt")

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
                localStorage.removeItem("jwt")
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