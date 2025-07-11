<!-- ErrorPage.vue -->
<template>
  <div class="error-page">
    <div class="error-container">
      <div class="error-icon">
        <span class="icon">{{ errorConfig.icon }}</span>
      </div>

      <div class="error-code">
        {{ errorCode }}
      </div>

      <h1 class="error-title">
        {{ errorConfig.title }}
      </h1>

      <p class="error-description">
        {{ errorConfig.description }}
      </p>

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

const isDev = process.env.NODE_ENV === 'development'

const errorConfigs = {
  400: {
    icon: '',
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
    icon: '',
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
    icon: '',
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
    icon: '',
    title: '페이지를 찾을 수 없습니다',
    description: '요청하신 페이지가 존재하지 않거나 이동되었습니다.\nURL을 다시 확인하거나 홈페이지에서 원하는 정보를 찾아보세요.',
    helpText: '찾으시는 페이지가 있다면 검색을 이용해보세요.',
    supportLink: '/support',
    actions: [
      { key: 'home', label: '홈으로 이동', type: 'primary' }
    ]
  },
  422: {
    icon: '',
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
    icon: '',
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
    icon: '',
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
    icon: '',
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
    icon: '',
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

const errorConfig = computed(() => {
  const code = String(props.errorCode)
  return errorConfigs[code] || errorConfigs['500']
})

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
      break
  }
}

onMounted(() => {
  if (props.errorCode >= 500) {
    // 실제 환경에서는 에러 로깅 서비스로 전송
    // sendErrorLog(errorData)
  }
})
</script>

<style scoped src="@/assets/css/errorPage.css"></style>
