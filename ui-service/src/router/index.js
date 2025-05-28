import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'

import Login from '../views/user/Login.vue'
import Register from '../views/user/Register.vue'
import MemberList from "../views/user/MemberList.vue";
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
        path: '/members',
        name: 'memberList',
        component: MemberList
    },
    {
        path: '/users/:userid',
        name: 'UserDetail',
        component: () => import('@/views/user/UserDetail.vue')  // 혹은 UserDetailView.vue
    },
    { path: '/users/edit/:userid', name: 'UserEdit', component: () => import('@/views/user/UserEdit.vue'), props: true },
    {
        path: '/boardList',
        name: 'BoardList',
        component: BoardList
    },
    {
        path: '/board/write',
        name: 'BoardWrite',
        component: () => import('@/views/board/BoardWrite.vue')
    },
    {
        path: '/board/:bno',
        name: 'BoardDetail',
        component: () => import('@/views/board/BoardDetail.vue')
    },
    {
        path: '/board/edit/:bno',
        name: 'BoardEdit',
        component: () => import('@/views/board/BoardEdit.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router