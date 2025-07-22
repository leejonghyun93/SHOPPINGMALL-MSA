// router/index.js

import { createRouter, createWebHistory } from 'vue-router'
import ResetPassword from '../views/user/FindPassword.vue'
// 컴포넌트 imports
import Home from '../views/Home.vue'
import Login from '../views/user/Login.vue'
import Register from '../views/user/Register.vue'
import FindId from '../views/user/FindId.vue'
import FindPassword from '../views/user/FindPassword.vue'
import Category from '../views/main/Category.vue'

// User 관련 컴포넌트
import MyPage from '@/views/user/MyPage.vue'
import MyPageOrders from '@/views/user/MyPageOrders.vue'
import MyPageProfile from '@/views/user/MyPageProfile.vue'
import UserEdit from '@/views/user/UserEdit.vue'

// Product 관련 컴포넌트
import ProductDetail from '@/views/product/ProductDetail.vue'
import Cart from '@/views/product/Cart.vue'

// Order 관련 컴포넌트
import Checkout from '@/views/order/Checkout.vue'
import OrderComplete from '@/views/order/OrderComplete.vue'

// 방송 관련 컴포넌트
import BroadcastList from "@/views/live/BroadcastList.vue"
import LiveBroadcastViewer from '@/views/live/BroadcastViewer.vue'

// 방송 예약 컴포넌트
import BroadcastCalendar from "@/views/live/Calendar.vue"

// 에러 페이지 컴포넌트 추가
import ErrorPage from '@/views/ErrorPage.vue'

// 소셜 로그인 콜백 처리 컴포넌트
import SocialCallback from '@/views/auth/SocialCallback.vue'

import ChatTest from '@/views/live/chat/ChatTest.vue'

// 인증 가드
const requireAuth = (to, from, next) => {
    const token = localStorage.getItem('jwt')
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
        component: Home,
        meta: {
            title: '홈페이지'
        }
    },
    {
        path: '/login',
        name: 'Login',
        component: Login,
        meta: {
            title: '로그인'
        }
    },
    {
        path: '/auth/callback',
        name: 'SocialCallback',
        component: SocialCallback,
        meta: {
            title: '로그인 처리 중...',
            hideNavigation: true
        }
    },
    {
        path: '/auth/kakao/callback',
        name: 'KakaoCallback',
        component: SocialCallback,
        meta: {
            title: '카카오 로그인 처리 중...',
            hideNavigation: true
        }
    },
    {
        path: '/auth/naver/callback',
        name: 'NaverCallback',
        component: SocialCallback,
        meta: {
            title: '네이버 로그인 처리 중...',
            hideNavigation: true
        }
    },
    {
        path: '/register',
        name: 'Register',
        component: Register,
        meta: {
            title: '회원가입'
        }
    },
    {
        path: '/findId',
        name: 'FindId',
        component: FindId,
        meta: {
            title: '아이디 찾기'
        }
    },
    {
        path: '/findPassword',
        name: 'FindPassword',
        component: FindPassword,
        meta: {
            title: '비밀번호 찾기'
        }
    },
    {
        path: '/resetPassword',
        name: 'ResetPassword',
        component: ResetPassword,
        meta: {
            title: '비밀번호 재설정'
        }
    },
    {
        path: '/mypage',
        name: 'MyPage',
        component: MyPage,
        redirect: '/mypage/orders',
        children: [
            {
                path: '',
                name: 'MyPageDefault',
                redirect: '/mypage/orders'
            },
            {
                path: 'orders',
                name: 'MyPageOrders',
                component: MyPageOrders
            },
            {
                path: 'profile',
                name: 'MyPageProfile',
                component: MyPageProfile,
                meta: {
                    title: '프로필 관리'
                }
            },
            {
                path: 'edit',
                name: 'ProfileEdit',
                component: UserEdit,
                meta: {
                    requiresAuth: true,
                    title: '프로필 수정'
                }
            }
        ]
    },
    {
        path: '/category',
        name: 'Category',
        component: Category,
        meta: {
            title: '카테고리'
        }
    },
    {
        path: '/category/:categoryId',
        name: 'CategoryDetail',
        component: Category,
        meta: {
            title: '카테고리 상품'
        }
    },
    {
        path: '/broadcasts/category/:categoryId?',  // ? 는 선택적 매개변수
        name: 'BroadcastCategory',
        component: BroadcastList,
        props: true,
        meta: {
            title: '라이브 방송'
        }
    },
    {
        path: '/broadcasts/calendar',
        name: 'BroadcastCalendar',
        component: BroadcastCalendar,
        meta: {
            title: '방송 일정'
        }
    },
    {
        path: '/live/:broadcastId',
        name: 'LiveBroadcastViewer',
        component: LiveBroadcastViewer,
        props: true,
        meta: {
            title: '라이브 방송 시청',
            requiresAuth: false
        }
    },
    {
        path: '/product/:id',
        name: 'ProductDetail',
        component: ProductDetail,
        meta: {
            title: '상품 상세'
        }
    },
    {
        path: '/users/edit/:userid',
        name: 'UserEdit',
        component: UserEdit,
        props: true,
        meta: {
            title: '사용자 정보 수정',
            requiresAuth: true
        }
    },
    {
        path: '/cart',
        name: 'Cart',
        component: Cart,
        meta: {
            title: '장바구니'
        }
    },
    {
        path: '/checkout',
        name: 'Checkout',
        component: Checkout,
        meta: {
            title: '주문서',
            requiresAuth: true
        }
    },
    {
        path: '/order-complete',
        name: 'OrderComplete',
        component: OrderComplete,
        meta: {
            title: '주문 완료',
            requiresAuth: true
        }
    },

    // 에러 페이지 라우트 추가
    {
        path: '/error/:code',
        name: 'ErrorPage',
        component: ErrorPage,
        props: route => ({
            errorCode: route.params.code,
            errorMessage: route.query.message || '',
            errorDetails: route.query.details || ''
        }),
        meta: {
            title: '에러 페이지',
            hideNavigation: true // 네비게이션 숨김 (선택사항)
        }
    },

    {
        path: '/:pathMatch(.*)*',
        redirect: to => {
            return {
                name: 'ErrorPage',
                params: { code: '404' },
                query: {
                    message: `페이지를 찾을 수 없습니다: ${to.path}`,
                    details: JSON.stringify({
                        requestedPath: to.path,
                        requestedQuery: to.query,
                        timestamp: new Date().toISOString(),
                        referrer: document.referrer || 'direct'
                    }, null, 2)
                }
            }
        }
    },
    {
        path: '/chat-test/:broadcastId/:role?',
        name: 'ChatTest',
        component: ChatTest,
        props: true
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes,
    //  스크롤 동작 설정 (선택사항)
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition
        } else {
            return { top: 0 }
        }
    }
})

//  네비게이션 가드 추가 (선택사항)
router.beforeEach((to, from, next) => {
    // 페이지 타이틀 설정
    if (to.meta.title) {
        document.title = `${to.meta.title} - 트라이마켓`
    } else {
        document.title = '트라이마켓'
    }

    // 인증이 필요한 페이지 체크
    if (to.meta.requiresAuth) {
        const token = localStorage.getItem('jwt')
        if (!token) {
            // 로그인 페이지로 리다이렉트하되, 원래 가려던 페이지 정보 저장
            next({
                name: 'Login',
                query: { redirect: to.fullPath }
            })
            return
        }
    }

    next()
})

//  에러 처리 (선택사항)
router.onError((error) => {
    console.error('라우터 에러:', error)

    // 라우터 에러도 에러 페이지로 처리
    router.push({
        name: 'ErrorPage',
        params: { code: '500' },
        query: {
            message: '페이지 로딩 중 오류가 발생했습니다',
            details: JSON.stringify({
                error: error.message,
                stack: error.stack,
                timestamp: new Date().toISOString()
            }, null, 2)
        }
    })
})

export default router