import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'

import Login from '../views/user/Login.vue'
import Register from '../views/user/Register.vue'
import BoardList from "../views/board/BoardList.vue";

const routes = [
    {
        path: '/',
        name: 'Home',
        component: Home
    },
    {
        path: '/login',
        name: 'Login',
        component: Login
    },
    {
        path: '/register',
        name: 'Register',
        component: Register
    },
    {
        path: '/boardList',
        name: 'BoardList',
        component: BoardList
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router