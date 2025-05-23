import { createApp } from 'vue'
import App from './App.vue'
import router from './router'       // 반드시 먼저 import

import '@/assets/css/main.css'         // 중복 제거
import 'bootstrap/dist/css/bootstrap.min.css';
import { setUserFromToken } from '@/stores/userStore';

const token = localStorage.getItem('jwtToken');
if (token) {
    setUserFromToken(token);
}
createApp(App)
    .use(router)
    .mount('#app')