import { reactive } from "vue";

export const user = reactive({
    id: null,
    name: null,
});

export function setUserFromToken(token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        user.id = payload.sub;
        user.name = payload.name || payload.sub;
        user.role = payload.role;

    } catch (e) {
        console.error("❌ 토큰 파싱 실패", e);
        user.id = null;
        user.name = null;
    }
}

