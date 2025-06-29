<!-- ErrorPage.vue -->
<template>
  <div class="error-page">
    <div class="error-container">
      <!-- 에러 아이콘 -->
      <div class="error-icon">
        <span class="icon">{{ errorConfig.icon }}</span>
      </div>

      <!-- 에러 코드 -->
      <div class="error-code">
        {{ errorCode }}
      </div>

      <!-- 에러 제목 -->
      <h1 class="error-title">
        {{ errorConfig.title }}
      </h1>

      <!-- 에러 설명 -->
      <p class="error-description">
        {{ errorConfig.description }}
      </p>

<!--      &lt;!&ndash; 추가 정보 (개발환경에서만) &ndash;&gt;-->
<!--      <div v-if="isDev && errorDetails" class="error-details">-->
<!--        <details>-->
<!--          <summary>에러 상세 정보 (개발용)</summary>-->
<!--          <pre>{{ errorDetails }}</pre>-->
<!--        </details>-->
<!--      </div>-->

      <!-- 액션 버튼들 -->
      <div class="error-actions">
        <button
            v-for="action in errorConfig.actions"
            :key="action.key"
            :class="['action-btn', action.type]"
            @click="handleAction(action.key)"
        >
          {{ action.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// Props
const props = defineProps({
  errorCode: {
    type: [String, Number],
    default: 500
  },
  errorMessage: {
    type: String,
    default: ''
  },
  errorDetails: {
    type: String,
    default: ''
  }
})

// 개발환경 체크
const isDev = process.env.NODE_ENV === 'development'

// 에러 설정
const errorConfigs = {
  400: {
    icon: '⚠️',
    title: '잘못된 요청입니다',
    description: '요청하신 정보가 올바르지 않습니다.\n입력 내용을 다시 확인해주세요.',
    helpText: '문제가 계속 발생하면 고객센터로 문의해주세요.',
    supportLink: '/support',
    actions: [
      { key: 'back', label: '이전 페이지로', type: 'secondary' },
      { key: 'home', label: '홈으로 이동', type: 'primary' }
    ]
  },
  401: {
    icon: '🔐',
    title: '로그인이 필요합니다',
    description: '해당 페이지에 접근하려면 로그인이 필요합니다.\n로그인 후 다시 이용해주세요.',
    helpText: '계정이 없으시다면 회원가입을 진행해주세요.',
    supportLink: '/support',
    actions: [
      { key: 'login', label: '로그인하기', type: 'primary' },
      { key: 'signup', label: '회원가입', type: 'secondary' },
      { key: 'home', label: '홈으로 이동', type: 'outline' }
    ]
  },
  403: {
    icon: '🚫',
    title: '접근 권한이 없습니다',
    description: '요청하신 페이지에 접근할 권한이 없습니다.\n관리자에게 문의하거나 다른 페이지를 이용해주세요.',
    helpText: '권한이 필요한 경우 관리자에게 문의해주세요.',
    supportLink: '/support',
    actions: [
      { key: 'back', label: '이전 페이지로', type: 'secondary' },
      { key: 'home', label: '홈으로 이동', type: 'primary' }
    ]
  },
  404: {
    icon: '🔍',
    title: '페이지를 찾을 수 없습니다',
    description: '요청하신 페이지가 존재하지 않거나 이동되었습니다.\nURL을 다시 확인하거나 홈페이지에서 원하는 정보를 찾아보세요.',
    helpText: '찾으시는 페이지가 있다면 검색을 이용해보세요.',
    supportLink: '/support',
    actions: [
      { key: 'home', label: '홈으로 이동', type: 'primary' }
    ]
  },
  422: {
    icon: '📝',
    title: '입력 정보가 올바르지 않습니다',
    description: '제출하신 정보에 오류가 있습니다.\n각 항목을 다시 확인하고 올바르게 입력해주세요.',
    helpText: '입력 형식을 확인하고 다시 시도해주세요.',
    supportLink: '/support',
    actions: [
      { key: 'back', label: '다시 시도', type: 'primary' },
      { key: 'home', label: '홈으로 이동', type: 'secondary' }
    ]
  },
  429: {
    icon: '⏱️',
    title: '요청이 너무 많습니다',
    description: '잠시 후 다시 시도해주세요.\n서비스 안정성을 위해 일시적으로 요청을 제한하고 있습니다.',
    helpText: '5분 후에 다시 시도해보세요.',
    supportLink: '/support',
    actions: [
      { key: 'retry', label: '다시 시도', type: 'primary' },
      { key: 'home', label: '홈으로 이동', type: 'secondary' }
    ]
  },
  500: {
    icon: '🔧',
    title: '서버 오류가 발생했습니다',
    description: '일시적인 서버 오류로 인해 요청을 처리할 수 없습니다.\n잠시 후 다시 시도해주세요.',
    helpText: '문제가 지속되면 고객센터로 문의해주세요.',
    supportLink: '/support',
    actions: [
      { key: 'retry', label: '새로고침', type: 'primary' },
      { key: 'home', label: '홈으로 이동', type: 'secondary' }
    ]
  },
  502: {
    icon: '🌐',
    title: '게이트웨이 오류',
    description: '서비스 연결에 문제가 발생했습니다.\n네트워크 상태를 확인하고 잠시 후 다시 시도해주세요.',
    helpText: '네트워크 연결을 확인해주세요.',
    supportLink: '/support',
    actions: [
      { key: 'retry', label: '다시 시도', type: 'primary' },
      { key: 'home', label: '홈으로 이동', type: 'secondary' }
    ]
  },
  503: {
    icon: '🔄',
    title: '서비스를 이용할 수 없습니다',
    description: '서비스가 일시적으로 중단되었습니다.\n시스템 점검이나 과부하로 인한 일시적 현상일 수 있습니다.',
    helpText: '서비스 복구까지 잠시만 기다려주세요.',
    supportLink: '/support',
    actions: [
      { key: 'retry', label: '다시 시도', type: 'primary' },
      { key: 'status', label: '서비스 상태 확인', type: 'secondary' },
      { key: 'home', label: '홈으로 이동', type: 'outline' }
    ]
  }
}

// 현재 에러 설정
const errorConfig = computed(() => {
  const code = String(props.errorCode)
  return errorConfigs[code] || errorConfigs['500']
})

// 액션 처리
const handleAction = (actionKey) => {
  switch (actionKey) {
    case 'back':
      router.go(-1)
      break
    case 'home':
      router.push('/')
      break
    case 'login':
      const currentPath = route.fullPath
      router.push(`/login?redirect=${encodeURIComponent(currentPath)}`)
      break
    case 'signup':
      router.push('/signup')
      break
    case 'search':
      router.push('/search')
      break
    case 'retry':
      window.location.reload()
      break
    case 'status':
      window.open('https://status.yourservice.com', '_blank')
      break
    default:
      console.log('Unknown action:', actionKey)
  }
}

// 에러 로깅 (실제 환경에서는 서버로 전송)
onMounted(() => {
  if (props.errorCode >= 500) {
    console.error('Server Error:', {
      code: props.errorCode,
      message: props.errorMessage,
      details: props.errorDetails,
      url: window.location.href,
      userAgent: navigator.userAgent,
      timestamp: new Date().toISOString()
    })

    // 실제 환경에서는 에러 로깅 서비스로 전송
    // sendErrorLog(errorData)
  }
})
</script>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  padding: 2rem;
}

.error-container {
  max-width: 600px;
  width: 100%;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  padding: 3rem;
  text-align: center;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.error-icon {
  margin-bottom: 1.5rem;
}

.error-icon .icon {
  font-size: 4rem;
  display: inline-block;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

.error-code {
  font-size: 2rem;
  font-weight: 800;
  color: #667eea;
  margin-bottom: 1rem;
  letter-spacing: 2px;
}

.error-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #2d3748;
  margin-bottom: 1rem;
  line-height: 1.2;
}

.error-description {
  font-size: 1.1rem;
  color: #4a5568;
  line-height: 1.6;
  margin-bottom: 2rem;
  white-space: pre-line;
}

.error-details {
  margin-bottom: 2rem;
  text-align: left;
}

.error-details details {
  background: #f7fafc;
  border-radius: 8px;
  padding: 1rem;
}

.error-details summary {
  cursor: pointer;
  font-weight: 600;
  color: #667eea;
  margin-bottom: 0.5rem;
}

.error-details pre {
  background: #1a202c;
  color: #e2e8f0;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.875rem;
  margin-top: 0.5rem;
}

.error-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 2rem;
}

.action-btn {
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: 1rem;
  min-width: 120px;
}

.action-btn.primary {
  background: #667eea;
  color: white;
}

.action-btn.primary:hover {
  background: #5a67d8;
  transform: translateY(-2px);
}

.action-btn.secondary {
  background: #e2e8f0;
  color: #4a5568;
}

.action-btn.secondary:hover {
  background: #cbd5e0;
  transform: translateY(-2px);
}

.action-btn.outline {
  background: transparent;
  color: #667eea;
  border: 2px solid #667eea;
}

.action-btn.outline:hover {
  background: #667eea;
  color: white;
  transform: translateY(-2px);
}

.help-section {
  border-top: 1px solid #e2e8f0;
  padding-top: 1.5rem;
}

.help-text {
  color: #718096;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.support-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  font-size: 0.9rem;
}

.support-link:hover {
  text-decoration: underline;
}

/* 모바일 반응형 */
@media (max-width: 768px) {
  .error-page {
    padding: 1rem;
  }

  .error-container {
    padding: 2rem;
  }

  .error-code {
    font-size: 1.5rem;
  }

  .error-title {
    font-size: 1.5rem;
  }

  .error-description {
    font-size: 1rem;
  }

  .error-actions {
    flex-direction: column;
    align-items: center;
  }

  .action-btn {
    width: 100%;
    max-width: 280px;
  }
}
</style>
