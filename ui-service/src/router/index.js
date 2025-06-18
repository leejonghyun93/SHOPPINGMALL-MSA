// router/index.js 수정

import {createRouter, createWebHistory} from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/user/Login.vue'
import Register from '../views/user/Register.vue'
import Category from '../views/main/Category.vue'
import FindId from "../views/user/FindId.vue"
import FindPassword from "../views/user/FindPassword.vue"
import BoardList from "../views/board/BoardList.vue"

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
        path: '/mypage',
        name: 'MyPage',
        component: () => import('@/views/user/MyPage.vue')
    },
    {
        path: '/category',
        name: 'category',
        component: Category
    },
    {
        path: '/category/:categoryId',
        name: 'Category',
        component: Category
    },
    {
        path: '/product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/product/ProductDetail.vue')
    },
    {
        path: '/users/:userid',
        name: 'UserDetail',
        component: () => import('@/views/user/UserDetail.vue')
    },
    {
        path: '/users/edit/:userid',
        name: 'UserEdit',
        component: () => import('@/views/user/UserEdit.vue'),
        props: true
    },
    {
        path: "/find-id",
        name: "FindId",
        component: FindId,
    },
    {
        path: "/find-password",
        name: "FindPassword",
        component: FindPassword,
    },
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
    },
    // 장바구니 관련 라우트 추가
    {
        path: '/cart',
        name: 'Cart',
        component: () => import('@/views/product/Cart.vue')
    },
    {
        path: '/checkout',
        name: 'Checkout',
        component: () => import('@/views/order/Checkout.vue')
    }
    // 주문 관련 라우트
    // {
    //     path: '/order/complete',
    //     name: 'OrderComplete',
    //     component: () => import('@/views/order/OrderComplete.vue')
    // },
    // {
    //     path: '/orders',
    //     name: 'OrderList',
    //     component: () => import('@/views/order/OrderList.vue')
    // },
    // {
    //     path: '/order/:orderId',
    //     name: 'OrderDetail',
    //     component: () => import('@/views/order/OrderDetail.vue')
    // }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router