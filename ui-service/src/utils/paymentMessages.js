// utils/paymentMessages.js - 결제 메시지 공통 유틸리티 (아이콘 제거)

/**
 * 결제 관련 메시지 유틸리티 클래스
 */
export class PaymentMessageUtils {

    // 에러 코드별 친화적 메시지 (우선순위 높음)
    static ERROR_CODE_MESSAGES = {
        // 사용자 행동 관련 (긍정적 표현)
        'STOP_PAYMENT': '결제를 중단하셨습니다. 언제든 다시 시도해보세요!',
        'USER_CANCEL': '결제를 취소하셨네요. 괜찮습니다! 다시 시도하시면 됩니다',
        'PAYMENT_CANCEL': '결제를 취소하셨어요. 다시 필요하시면 언제든지!',
        'USER_CANCELLED': '결제를 취소하셨네요. 괜찮습니다! 다시 시도하시면 됩니다',

        // 카드/계좌 관련 (해결책 제시)
        'CARD_DECLINED': '카드 승인이 거절되었어요. 다른 카드로 시도하거나 카드사에 문의해보세요',
        'INSUFFICIENT_FUNDS': '잔액이 부족한 것 같아요. 충전 후 다시 시도해보세요',
        'INVALID_CARD': '카드 정보를 다시 확인해주세요. 카드번호나 유효기간을 다시 입력해보세요',
        'EXPIRED_CARD': '카드 유효기간이 만료되었어요. 다른 카드로 시도해보세요',
        'CARD_ERROR': '카드에 문제가 있는 것 같아요. 다른 카드로 시도해보세요',

        // 시스템 관련 (재시도 유도)
        'TIMEOUT': '결제 시간이 초과되었어요. 네트워크를 확인하고 다시 시도해보세요',
        'NETWORK_ERROR': '인터넷 연결을 확인하고 다시 시도해보세요',
        'SYSTEM_ERROR': '일시적인 오류가 발생했어요. 잠시 후 다시 시도해보세요',
        'SERVER_ERROR': '서버에 일시적인 문제가 있어요. 잠시 후 다시 시도해보세요',

        // PG사별 친화적 메시지
        'PG_ERROR': '결제 서비스에 일시적인 문제가 있어요. 다른 결제수단으로 시도해보세요',
        'PG_TIMEOUT': '결제 서비스 응답이 지연되고 있어요. 다시 시도해보세요',

        // 인증 관련
        'AUTH_FAILED': '본인인증에 실패했어요. 정보를 다시 확인해주세요',
        'SMS_FAILED': 'SMS 인증에 실패했어요. 휴대폰 번호를 확인해보세요',
        'OTP_FAILED': '인증번호가 맞지 않아요. 다시 확인해보세요',

        // 한도/제한 관련
        'DAILY_LIMIT_EXCEEDED': '일일 결제 한도를 초과했어요. 내일 다시 시도하거나 다른 카드를 사용해보세요',
        'MONTHLY_LIMIT_EXCEEDED': '월 결제 한도를 초과했어요. 다른 카드를 사용해보세요'
    }

    // 취소 관련 키워드 (우선 체크용)
    static CANCEL_KEYWORDS = [
        '취소', 'cancel', 'cancelled', 'canceled',
        '중단', 'stop', 'abort', '포기'
    ]

    // 에러 메시지별 친화적 변환 (에러 코드가 없을 때만 사용)
    static ERROR_MESSAGE_PATTERNS = {
        // 카드/결제 관련
        '카드 결제가 거절되었습니다': '카드 승인이 안 되었어요. 다른 카드로 시도해보세요',
        '카드 승인에 실패했습니다': '카드 승인이 안 되었어요. 카드사에 문의하거나 다른 카드로 시도해보세요',
        '잔액이 부족합니다': '잔액이 부족한 것 같아요. 충전 후 다시 시도해보세요',
        '결제 시간이 초과되었습니다': '결제 시간이 초과되었어요. 다시 시도해보세요',
        '네트워크 오류가 발생했습니다': '인터넷 연결을 확인하고 다시 시도해보세요',
        '서버 오류가 발생했습니다': '일시적인 서버 문제가 있어요. 잠시 후 다시 시도해보세요'
    }

    // 성공 메시지 템플릿
    static SUCCESS_MESSAGES = [
        (pgName, amount) => `${pgName} 결제가 완료되었어요!`,
        (pgName, amount) => `결제 성공! ${amount?.toLocaleString()}원이 결제되었습니다`,
        (pgName, amount) => `${pgName}로 결제가 완료되었습니다! 감사합니다`,
        (pgName, amount) => `결제 완료! 주문이 성공적으로 처리되었어요`,
        (pgName, amount) => `${amount?.toLocaleString()}원 결제가 완료되었어요! 주문해 주셔서 감사합니다`
    ]

    // PG사 표시 이름
    static PG_DISPLAY_NAMES = {
        'kakaopay.TC0ONETIME': '카카오페이',
        'html5_inicis': '일반카드',
        'tosspay.tosstest': '토스페이',
        'payco.PARTNERTEST': '페이코',
        'kcp.T0000': '컬리페이',
        'naverpay': '네이버페이',
        'smilepay': '스마일페이'
    }

    /**
     * 취소 관련 메시지인지 확인
     * @param {string} errorCode - 에러 코드
     * @param {string} errorMsg - 에러 메시지
     * @returns {boolean} 취소 관련 여부
     */
    static isCancelRelated(errorCode, errorMsg) {
        // 에러 코드 체크
        const cancelCodes = ['STOP_PAYMENT', 'USER_CANCEL', 'PAYMENT_CANCEL', 'USER_CANCELLED']
        if (errorCode && cancelCodes.includes(errorCode)) {
            return true
        }

        // 메시지 키워드 체크
        if (errorMsg) {
            const lowerMsg = errorMsg.toLowerCase()
            return this.CANCEL_KEYWORDS.some(keyword =>
                lowerMsg.includes(keyword.toLowerCase())
            )
        }

        return false
    }

    /**
     * 에러 코드와 메시지를 친화적인 메시지로 변환 (중복 방지 개선)
     * @param {string} errorCode - 에러 코드
     * @param {string} errorMsg - 에러 메시지
     * @returns {string} 친화적인 메시지
     */
    static getFailureReason(errorCode, errorMsg) {
        console.log('에러 분석:', { errorCode, errorMsg })

        // 1. 에러 코드로 먼저 확인 (최우선)
        if (errorCode && this.ERROR_CODE_MESSAGES[errorCode]) {
            console.log('에러 코드 매칭:', errorCode)
            return this.ERROR_CODE_MESSAGES[errorCode]
        }

        // 2. 취소 관련 메시지 우선 처리 (중복 방지)
        if (this.isCancelRelated(errorCode, errorMsg)) {
            console.log('취소 관련 메시지 감지')
            return '결제를 취소하셨네요. 괜찮습니다! 다시 시도하시면 됩니다'
        }

        // 3. 에러 메시지 패턴 매칭 (취소 제외)
        if (errorMsg) {
            for (const [pattern, friendlyMsg] of Object.entries(this.ERROR_MESSAGE_PATTERNS)) {
                if (errorMsg.includes(pattern)) {
                    console.log('메시지 패턴 매칭:', pattern)
                    return friendlyMsg
                }
            }

            // 4. 부분 매칭으로 더 유연하게 처리 (취소 제외)
            const lowerErrorMsg = errorMsg.toLowerCase()

            if (lowerErrorMsg.includes('거절') || lowerErrorMsg.includes('declined')) {
                return '카드 승인이 안 되었어요. 다른 카드로 시도해보세요'
            }

            if (lowerErrorMsg.includes('잔액') || lowerErrorMsg.includes('insufficient')) {
                return '잔액이 부족한 것 같아요. 충전 후 다시 시도해보세요'
            }

            if (lowerErrorMsg.includes('시간') || lowerErrorMsg.includes('timeout')) {
                return '결제 시간이 초과되었어요. 다시 시도해보세요'
            }

            if (lowerErrorMsg.includes('네트워크') || lowerErrorMsg.includes('network')) {
                return '인터넷 연결을 확인하고 다시 시도해보세요'
            }
        }

        // 5. 기본 친화적 메시지
        console.log('기본 메시지 사용')
        return errorMsg || '결제 중 문제가 발생했어요. 다시 시도해보시거나 고객센터에 문의해주세요'
    }

    /**
     * 성공 메시지 생성
     * @param {string} pgProvider - PG사 코드
     * @param {number} amount - 결제 금액
     * @param {number} index - 메시지 인덱스 (선택사항)
     * @returns {string} 성공 메시지
     */
    static getSuccessMessage(pgProvider, amount, index = null) {
        const pgName = this.PG_DISPLAY_NAMES[pgProvider] || '결제서비스'

        if (index !== null && this.SUCCESS_MESSAGES[index]) {
            return this.SUCCESS_MESSAGES[index](pgName, amount)
        }

        // 랜덤 메시지 선택
        const randomIndex = Math.floor(Math.random() * this.SUCCESS_MESSAGES.length)
        return this.SUCCESS_MESSAGES[randomIndex](pgName, amount)
    }

    /**
     * PG사 표시 이름 가져오기
     * @param {string} pgProvider - PG사 코드
     * @returns {string} PG사 표시 이름
     */
    static getPgDisplayName(pgProvider) {
        return this.PG_DISPLAY_NAMES[pgProvider] || pgProvider
    }

    /**
     * 메시지 타입 판별 (정보성/에러)
     * @param {string} errorCode - 에러 코드
     * @param {string} errorMsg - 에러 메시지
     * @returns {string} 'info' | 'error' | 'warning'
     */
    static getMessageType(errorCode, errorMsg) {
        // 사용자 취소는 정보성
        if (this.isCancelRelated(errorCode, errorMsg)) {
            return 'info'
        }

        // 시스템 에러는 경고
        const systemCodes = ['TIMEOUT', 'NETWORK_ERROR', 'SYSTEM_ERROR', 'SERVER_ERROR']
        if (systemCodes.includes(errorCode)) {
            return 'warning'
        }

        // 나머지는 에러
        return 'error'
    }
}

// 간편 사용을 위한 기본 export
export const getFailureReason = PaymentMessageUtils.getFailureReason.bind(PaymentMessageUtils)
export const getSuccessMessage = PaymentMessageUtils.getSuccessMessage.bind(PaymentMessageUtils)
export const getPgDisplayName = PaymentMessageUtils.getPgDisplayName.bind(PaymentMessageUtils)
export const getMessageType = PaymentMessageUtils.getMessageType.bind(PaymentMessageUtils)

// 기본 export
export default PaymentMessageUtils