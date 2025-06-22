// router/index.js

import { createRouter, createWebHistory } from 'vue-router'

// 컴포넌트 imports
import Home from '../views/Home.vue'
import Login from '../views/user/Login.vue'
import Register from '../views/user/Register.vue'
import FindId from '../views/user/FindId.vue'
import FindPassword from '../views/user/FindPassword.vue'
import Category from '../views/main/Category.vue'
import BoardList from '../views/board/BoardList.vue'

// User 관련 컴포넌트
import MyPage from '@/views/user/MyPage.vue'
import MyPageOrders from '@/views/user/MyPageOrders.vue'
import MyPageProfile from '@/views/user/MyPageProfile.vue'
import UserDetail from '@/views/user/UserDetail.vue'
import UserEdit from '@/views/user/UserEdit.vue'

// Product 관련 컴포넌트
import ProductDetail from '@/views/product/ProductDetail.vue'
import Cart from '@/views/product/Cart.vue'

// Board 관련 컴포넌트
import BoardWrite from '@/views/board/BoardWrite.vue'
import BoardDetail from '@/views/board/BoardDetail.vue'
import BoardEdit from '@/views/board/BoardEdit.vue'

// Order 관련 컴포넌트
import Checkout from '@/views/order/Checkout.vue'
import OrderComplete from '@/views/order/OrderComplete.vue'

// 인증 가드
const requireAuth = (to, from, next) => {
    const token = localStorage.getItem('token')
    if (token) {
        next()
    } else {
        next('/login')
    }
}

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
        path: '/find-id',
        name: 'FindId',
        component: FindId
    },
    {
        path: '/find-password',
        name: 'FindPassword',
        component: FindPassword
    },
    {
        path: '/mypage',
        name: 'MyPage',
        component: MyPage,
        beforeEnter: requireAuth,
        children: [
            {
                path: 'orders',
                name: 'MyPageOrders',
                component: MyPageOrders
            },
            {
                path: 'profile',
                name: 'MyPageProfile',
                component: MyPageProfile
            },
            {
                path: 'edit',
                name: 'ProfileEdit',
                component: UserEdit,
                meta: { requiresAuth: true }
            },
            {
                path: '',
                redirect: 'orders'
            }
        ]
    },
    {
        path: '/category',
        name: 'Category',
        component: Category
    },
    {
        path: '/category/:categoryId',
        name: 'CategoryDetail',
        component: Category
    },
    {
        path: '/product/:id',
        name: 'ProductDetail',
        component: ProductDetail
    },
    {
        path: '/users/:userid',
        name: 'UserDetail',
        component: UserDetail
    },
    {
        path: '/users/edit/:userid',
        name: 'UserEdit',
        component: UserEdit,
        props: true
    },
    {
        path: '/boardList',
        name: 'BoardList',
        component: BoardList
    },
    {
        path: '/board/write',
        name: 'BoardWrite',
        component: BoardWrite
    },
    {
        path: '/board/:bno',
        name: 'BoardDetail',
        component: BoardDetail
    },
    {
        path: '/board/edit/:bno',
        name: 'BoardEdit',
        component: BoardEdit
    },
    {
        path: '/cart',
        name: 'Cart',
        component: Cart
    },
    {
        path: '/checkout',
        name: 'Checkout',
        component: Checkout
    },
    {
        path: '/order-complete',
        name: 'OrderComplete',
        component: OrderComplete
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router