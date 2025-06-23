// src/utils/orderStatusUtils.js

/**
 * 주문 상태 관련 공통 유틸리티
 */

// 상태 매핑 정의
const STATUS_MAP = {
    // 영문 -> 한글
    'PENDING': '주문접수',
    'ORDER_COMPLETED': '주문완료',
    'PREPARING': '배송준비',
    'PAYMENT_COMPLETED': '결제완료',
    'SHIPPING': '배송중',
    'DELIVERED': '배송완료',
    'CANCELLED': '주문취소',
    'CANCELLED_BY_WITHDRAWAL': '탈퇴로 인한 취소',
    'RETURNED': '반품완료'
}

// CSS 클래스 매핑
const STATUS_CLASS_MAP = {
    // 영문 코드
    'PENDING': 'status-pending',
    'ORDER_COMPLETED': 'status-paid',
    'PREPARING': 'status-preparing',
    'PAYMENT_COMPLETED': 'status-paid',
    'SHIPPING': 'status-shipping',
    'DELIVERED': 'status-delivered',
    'CANCELLED': 'status-cancelled',
    'CANCELLED_BY_WITHDRAWAL': 'status-cancelled',
    'RETURNED': 'status-returned',

    // 한글도 지원
    '주문접수': 'status-pending',
    '주문완료': 'status-paid',
    '결제완료': 'status-paid',
    '배송준비': 'status-preparing',
    '배송중': 'status-shipping',
    '배송완료': 'status-delivered',
    '주문취소': 'status-cancelled',
    '반품완료': 'status-returned'
}

// 취소 가능한 상태들
const CANCELLABLE_STATUSES = [
    // 영문 코드
    'PENDING',
    'ORDER_COMPLETED',
    'PREPARING',
    'PAYMENT_COMPLETED',

    // 한글도 지원
    '주문접수',
    '주문완료',
    '결제완료',
    '배송준비'
]

/**
 * 상태 코드를 한글로 변환
 * @param {string} status - 상태 코드 (영문/한글)
 * @returns {string} 한글 상태명
 */
export const getStatusDisplayName = (status) => {
    return STATUS_MAP[status] || status
}

/**
 * 상태에 따른 CSS 클래스 반환
 * @param {string} status - 상태 코드 (영문/한글)
 * @returns {string} CSS 클래스명
 */
export const getStatusClass = (status) => {
    return STATUS_CLASS_MAP[status] || 'status-default'
}

/**
 * 주문 취소 가능 여부 확인
 * @param {string} status - 상태 코드 (영문/한글)
 * @returns {boolean} 취소 가능 여부
 */
export const canCancelOrder = (status) => {
    return CANCELLABLE_STATUSES.includes(status)
}

/**
 * 상태별 아이콘 반환
 * @param {string} status - 상태 코드 (영문/한글)
 * @returns {string} 이모지 아이콘
 */
export const getStatusIcon = (status) => {
    const iconMap = {
        'PENDING': '⏳',
        'ORDER_COMPLETED': '✅',
        'PREPARING': '📦',
        'PAYMENT_COMPLETED': '💳',
        'SHIPPING': '🚚',
        'DELIVERED': '📋',
        'CANCELLED': '❌',
        'CANCELLED_BY_WITHDRAWAL': '❌',
        'RETURNED': '↩️',

        // 한글
        '주문접수': '⏳',
        '주문완료': '✅',
        '결제완료': '💳',
        '배송준비': '📦',
        '배송중': '🚚',
        '배송완료': '📋',
        '주문취소': '❌',
        '반품완료': '↩️'
    }

    return iconMap[status] || '📦'
}

/**
 * 상태별 색상 반환
 * @param {string} status - 상태 코드 (영문/한글)
 * @returns {string} 색상 코드
 */
export const getStatusColor = (status) => {
    const colorMap = {
        'PENDING': '#FFA500',
        'ORDER_COMPLETED': '#28A745',
        'PREPARING': '#007BFF',
        'PAYMENT_COMPLETED': '#28A745',
        'SHIPPING': '#17A2B8',
        'DELIVERED': '#6C757D',
        'CANCELLED': '#DC3545',
        'CANCELLED_BY_WITHDRAWAL': '#DC3545',
        'RETURNED': '#FFC107'
    }

    // 한글은 영문으로 변환 후 색상 반환
    const englishStatus = Object.keys(STATUS_MAP).find(key => STATUS_MAP[key] === status) || status
    return colorMap[englishStatus] || '#6C757D'
}

/**
 * 모든 상태 목록 반환
 * @returns {Array} 상태 목록
 */
export const getAllStatuses = () => {
    return Object.entries(STATUS_MAP).map(([code, name]) => ({
        code,
        name,
        class: getStatusClass(code),
        icon: getStatusIcon(code),
        color: getStatusColor(code),
        cancellable: canCancelOrder(code)
    }))
}

export default {
    getStatusDisplayName,
    getStatusClass,
    canCancelOrder,
    getStatusIcon,
    getStatusColor,
    getAllStatuses
}