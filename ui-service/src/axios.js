import axios from "axios";
import jwtDecode from "jwt-decode";

const instance = axios.create({
    baseURL: "http://localhost:8080", // gateway 주소
});

instance.interceptors.request.use((config) => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
        const decoded = jwtDecode(token);
        const now = Date.now() / 1000;

        if (decoded.exp < now) {
            alert("토큰이 만료되었습니다. 다시 로그인해주세요.");
            localStorage.clear();
            window.location.href = "/login";
            return Promise.reject("토큰 만료");
        }

        config.headers["Authorization"] = `Bearer ${token}`;
    }

    return config;
});

export default instance;