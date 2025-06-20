// router/index.js 수정

import {createRouter, createWebHistory} from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/user/Login.vue'
import Register from '../views/user/Register.vue'
import Category from '../views/main/Category.vue'
import FindId from "../views/user/FindId.vue"
import FindPassword from "../views/user/FindPassword.vue"
import BoardList from "../views/board/BoardList.vue"
import ProfileEdit from '@/views/user/UserEdit.vue'
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
        path: '/mypage',
        name: 'mypage',
        component: () => import('@/views/user/MyPage.vue'),
        beforeEnter: requireAuth,
        children: [
            {
                path: 'orders',
                name: 'mypage-orders',
                component: () => import('@/views/user/MyPageOrders.vue')
            },
            // {
            //     path: 'returns',
            //     name: 'mypage-returns',
            //     component: () => import('@/views/MyPageReturns.vue')
            // },
            // {
            //     path: 'coupons',
            //     name: 'mypage-coupons',
            //     component: () => import('@/views/MyPageCoupons.vue')
            // },
            // {
            //     path: 'wishlist',
            //     name: 'mypage-wishlist',
            //     component: () => import('@/views/MyPageWishlist.vue')
            // },
            {
                path: 'profile',
                name: 'mypage-profile',
                component: () => import('@/views/user/MyPageProfile.vue')
            },
            {
                path: '/mypage/profile',
                name: 'profile-edit',
                component: ProfileEdit,
                meta: { requiresAuth: true }  // 인증 필요
            },
            // {
            //     path: 'shipping',
            //     name: 'mypage-shipping',
            //     component: () => import('@/views/MyPageShipping.vue')
            // },
            // {
            //     path: 'colorstyle',
            //     name: 'mypage-colorstyle',
            //     component: () => import('@/views/MyPageColorStyle.vue')
            // },
            // {
            //     path: 'reviews',
            //     name: 'mypage-reviews',
            //     component: () => import('@/views/MyPageReviews.vue')
            // },
            // {
            //     path: 'inquiries',
            //     name: 'mypage-inquiries',
            //     component: () => import('@/views/MyPageInquiries.vue')
            // },
            // {
            //     path: 'frequent',
            //     name: 'mypage-frequent',
            //     component: () => import('@/views/MyPageFrequent.vue')
            // },
            // {
            //     path: 'vip',
            //     name: 'mypage-vip',
            //     component: () => import('@/views/MyPageVip.vue')
            // },
            {
                path: '',
                redirect: 'orders'
            }
        ]
    },
    {
        path: '/category',
        name: 'Category', // ⚠️ 수정: 소문자 'category'에서 'Category'로 통일
        component: Category
    },
    {
        path: '/category/:categoryId',
        name: 'CategoryDetail', // ⚠️ 수정: name이 중복되어서 변경
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
    // 장바구니 관련 라우트
    {
        path: '/cart',
        name: 'Cart',
        component: () => import('@/views/product/Cart.vue')
    },
    {
        path: '/checkout',
        name: 'Checkout',
        component: () => import('@/views/order/Checkout.vue')
    },
    {
        path: '/order-complete',
        name: 'OrderComplete',
        component: () => import('@/views/order/OrderComplete.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router