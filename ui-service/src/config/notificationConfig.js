// src/config/notificationConfig.js - 환경 독립적 설정

export const NOTIFICATION_CONFIG = {
    //  환경변수 기반 BASE_URL 설정
    BASE_URL: import.meta.env.VITE_NOTIFICATION_URL ||
        (import.meta.env.DEV ? 'http://localhost:8096/api/notifications' : 'http://13.209.253.241:8096/api/notifications'),

    ENDPOINTS: {
        HEALTH: '/health',
        BROADCASTS_SCHEDULE: '/broadcasts/schedule',
        SUBSCRIPTIONS_USER: '/subscriptions/user',
        SUBSCRIBE_BROADCAST: '/subscriptions',
        SUBSCRIPTIONS: '/subscriptions'
    },

    getNotificationBaseUrl() {
        return this.BASE_URL
    }
}

export const subscribeBroadcastStart = async (userIdentifier, broadcastId) => {
    if (!userIdentifier || !broadcastId) {
        throw new Error('필수 파라미터가 누락되었습니다.')
    }

    const endpoint = `/subscriptions/broadcast-start?userId=${userIdentifier}&broadcastId=${broadcastId}`

    const response = await notificationApiCall(endpoint, {
        method: 'POST'
    })

    if (!response.ok) {
        const errorText = await response.text()
        let errorData

        try {
            errorData = JSON.parse(errorText)
        } catch (e) {
            errorData = { message: errorText }
        }

        const error = new Error(errorData.message || `HTTP ${response.status}`)
        error.response = { status: response.status, data: errorData }
        throw error
    }

    return await response.json()
}

export const unsubscribeBroadcast = async (userIdentifier, broadcastId) => {
    if (!userIdentifier || !broadcastId) {
        throw new Error('필수 파라미터가 누락되었습니다.')
    }

    const endpoint = `/subscriptions?userId=${userIdentifier}&broadcastId=${broadcastId}&type=BROADCAST_START`

    const response = await notificationApiCall(endpoint, {
        method: 'DELETE'
    })

    if (!response.ok) {
        const errorText = await response.text()
        let errorData

        try {
            errorData = JSON.parse(errorText)
        } catch (e) {
            errorData = { message: errorText }
        }

        const error = new Error(errorData.message || `HTTP ${response.status}`)
        error.response = { status: response.status, data: errorData }
        throw error
    }
}

const getCurrentUser = () => {
    const token = localStorage.getItem('jwt')

    if (!token || !isTokenValid(token)) {
        return { id: null, username: 'guest', identifier: null }
    }

    try {
        const parts = token.split('.')
        let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
        while (base64.length % 4) {
            base64 += '='
        }

        const payload = JSON.parse(atob(base64))

        let userId = null
        let userIdentifier = null

        const potentialIds = [payload.userId, payload.sub, payload.id]
        for (const id of potentialIds) {
            if (id && !isNaN(Number(id)) && Number(id) > 0) {
                userId = Number(id)
                userIdentifier = String(id)
                break
            }
        }

        if (!userId) {
            userIdentifier = payload.sub || payload.username || payload.id
        }

        const username = payload.username || payload.name || userIdentifier || 'user'

        return {
            id: userId,
            identifier: userIdentifier,
            username: username
        }

    } catch (error) {
        return { id: null, username: 'guest', identifier: null }
    }
}

const isTokenValid = (token) => {
    if (!token) return false

    try {
        const parts = token.split('.')
        if (parts.length !== 3) return false

        let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
        while (base64.length % 4) {
            base64 += '='
        }

        const payloadStr = atob(base64)
        const payload = JSON.parse(payloadStr)
        const currentTime = Math.floor(Date.now() / 1000)

        if (payload.exp && payload.exp < (currentTime + 30)) {
            return false
        }

        return true
    } catch (error) {
        return false
    }
}

const getAuthHeaders = () => {
    const token = localStorage.getItem('jwt')

    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    }

    if (token) {
        headers['Authorization'] = `Bearer ${token}`
    }

    return headers
}

export const notificationApiCall = async (endpoint, options = {}) => {
    let url

    if (endpoint.startsWith('http://') || endpoint.startsWith('https://')) {
        url = endpoint
    } else if (endpoint.startsWith('/')) {
        url = `${NOTIFICATION_CONFIG.BASE_URL}${endpoint}`
    } else {
        url = `${NOTIFICATION_CONFIG.BASE_URL}/${endpoint}`
    }

    try {
        const response = await fetch(url, {
            method: options.method || 'GET',
            mode: 'cors',
            headers: {
                ...getAuthHeaders(),
                ...options.headers
            },
            ...options
        })

        return response

    } catch (error) {
        return {
            ok: false,
            status: 0,
            json: async () => ({}),
            text: async () => ''
        }
    }
}

export const notificationHelpers = {

    async getUnreadCount(userId) {
        try {
            const response = await notificationApiCall(`/unread-count?userId=${userId}`);
            if (response && response.ok) {
                const data = await response.json();
                return data.count || 0;
            }
            return 0;
        } catch (error) {
            return 0;
        }
    },

    async getRecentNotifications(userId, limit = 10) {
        try {
            const response = await notificationApiCall(`/recent?userId=${userId}&limit=${limit}`);
            if (response && response.ok) {
                return await response.json();
            }
            return [];
        } catch (error) {
            return [];
        }
    },

    async markAllAsRead(userId) {
        try {
            const response = await notificationApiCall(`/mark-all-read?userId=${userId}`, {
                method: 'PATCH'
            });
            return response && response.ok;
        } catch (error) {
            return false;
        }
    },

    async markAsRead(notificationId, userId) {
        try {
            const response = await notificationApiCall(`/${notificationId}/read?userId=${userId}`, {
                method: 'PATCH'
            });
            return response && response.ok;
        } catch (error) {
            return false;
        }
    },

    formatTime(timeString) {
        const now = new Date();
        const time = new Date(timeString);
        const diffInMinutes = Math.floor((now - time) / (1000 * 60));

        if (diffInMinutes < 1) return '방금 전';
        if (diffInMinutes < 60) return `${diffInMinutes}분 전`;
        if (diffInMinutes < 1440) return `${Math.floor(diffInMinutes / 60)}시간 전`;

        const diffInDays = Math.floor(diffInMinutes / 1440);
        if (diffInDays < 7) return `${diffInDays}일 전`;

        return time.toLocaleDateString('ko-KR');
    },

    getNotificationIcon(type) {
        const iconMap = {
            'BROADCAST_START': 'live',
            'BROADCAST_END': 'stop',
            'BROADCAST_REMINDER': 'bell',
            'BROADCAST_CANCEL': 'x',
            'GENERAL': 'info'
        };
        return iconMap[type] || 'info';
    },

    getPriorityColor(priority) {
        const colorMap = {
            'LOW': '#6c757d',
            'NORMAL': '#007bff',
            'HIGH': '#fd7e14',
            'URGENT': '#dc3545'
        };
        return colorMap[priority] || '#007bff';
    }
};

export { getCurrentUser }