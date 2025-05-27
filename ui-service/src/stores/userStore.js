import { reactive } from "vue";
import { jwtDecode } from "jwt-decode"; // ✅ 변경

export const user = reactive({
    id: null,
    name: null,
    role: null,
});

export function setUserFromToken(token) {
    try {
        const payload = jwtDecode(token); // ✅ 변경
        console.log('decoded payload:', payload);
        user.id = payload.sub;
        user.name = payload.name || payload.sub;
        user.role = payload.role;
    } catch (e) {
        user.id = null;
        user.name = null;
        user.role = null;
    }
}
