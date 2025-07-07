<template>
  <div class="live-broadcast-viewer">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>방송 정보를 불러오는 중...</p>
    </div>

    <!-- 에러 상태 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon"><i class="fas fa-exclamation-triangle"></i></div>
      <h3>방송을 불러오는데 실패했습니다</h3>
      <p>{{ error }}</p>
      <button @click="loadBroadcastData" class="retry-button">다시 시도</button>
    </div>

    <!-- 방송 컨텐츠 -->
    <div v-else-if="broadcast">
      <!-- 상단 헤더 -->
      <div class="broadcast-header">
        <div class="channel-info">
          <div class="channel-logo">
            <img :src="getBroadcasterAvatar(broadcast.broadcaster_id)" :alt="broadcast.broadcaster_name" />
          </div>
          <div class="channel-details">
            <h1 class="broadcast-title">{{ broadcast.title }}</h1>
            <div class="channel-stats">
              <span class="live-badge">{{ getBroadcastStatusText(broadcast.broadcast_status) }}</span>
              <span class="viewer-count">{{ formatViewerCount(broadcast.current_viewers) }} 시청</span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <button class="action-btn" @click="scrollToProducts">
            <i class="fas fa-shopping-bag"></i> 상품목록
          </button>
          <button class="action-btn" @click="showBroadcastInfo">
            <i class="fas fa-info-circle"></i> 라이브 소개
          </button>
          <button class="action-btn" @click="likeBroadcast">
            <i class="fas fa-heart"></i> 좋아요 ({{ broadcast.like_count }})
          </button>
          <button class="action-btn" @click="shareBroadcast">
            <i class="fas fa-share"></i> 공유
          </button>
        </div>
      </div>

      <div class="main-content">
        <!-- 왼쪽: 방송 영상 -->
        <div class="video-section">
          <!-- 비디오 플레이어 -->
          <div class="video-player">
            <div class="video-overlay">
              <div class="control-overlay">
                <!-- 중앙 재생 버튼 -->
                <button class="play-btn-center" @click="togglePlay">
                  <i v-if="!isPlaying" class="fas fa-play play-icon"></i>
                  <i v-else class="fas fa-pause pause-icon"></i>
                </button>

                <!-- 상품 소개 팝업 -->
                <div v-if="featuredProduct" class="product-popup" @click="showProductDetail(featuredProduct)">
                  <div class="popup-header">
                    <i class="fas fa-shopping-cart popup-icon"></i>
                    <span class="popup-text">{{ featuredProduct.name }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 비디오 하단 컨트롤 -->
            <div class="video-controls">
              <div class="controls-left">
                <button class="control-btn" @click="togglePlay">
                  <i v-if="!isPlaying" class="fas fa-play"></i>
                  <i v-else class="fas fa-pause"></i>
                </button>
                <span class="time-display">{{ getFormattedDuration() }}</span>
              </div>
              <div class="controls-center">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: progress + '%' }"></div>
                </div>
              </div>
              <div class="controls-right">
                <button class="control-btn" @click="toggleMute">
                  <i v-if="!isMuted" class="fas fa-volume-up"></i>
                  <i v-else class="fas fa-volume-mute"></i>
                </button>
                <button class="control-btn">
                  <i class="fas fa-cog"></i>
                </button>
                <button class="control-btn" @click="toggleFullscreen">
                  <i class="fas fa-expand"></i>
                </button>
              </div>
            </div>
          </div>

          <!-- 상품 정보 섹션 -->
          <div v-if="featuredProduct" class="product-info-section">
            <div class="product-card">
              <img
                  :src="featuredProduct.mainImage || getDefaultProductImage(featuredProduct.productId)"
                  :alt="featuredProduct.name"
                  class="product-image"
              />
              <div class="product-details">
                <h3 class="product-title">{{ featuredProduct.name }}</h3>
                <div class="product-pricing">
                  <div class="discount-info">
                    <span class="original-price">정가</span>
                    <span class="discount-price">{{ formatPrice(featuredProduct.price) }}원</span>
                  </div>
                  <div class="special-offer">
                    <span class="live-special">라이브 특가 {{ featuredProduct.getDiscountPercent() }}% 할인</span>
                    <span class="special-price">{{ formatPrice(featuredProduct.salePrice) }}원</span>
                  </div>
                  <div class="final-price">
                    <span class="final-label">최대혜택가</span>
                    <span class="final-amount">{{ formatPrice(featuredProduct.getFinalPrice()) }}원</span>
                  </div>
                  <div class="sub-info">
                    <span class="payment-info">무이자카드 할인 + 적립금 포함 시</span>
                  </div>
                </div>
              </div>
              <div class="product-actions">
                <button class="heart-btn" @click="likeProduct(featuredProduct.productId)">
                  <i class="fas fa-heart heart-icon"></i>
                  <span class="heart-count">{{ featuredProduct.viewCount || 0 }}</span>
                </button>
                <button class="share-btn" @click="shareProduct(featuredProduct)">
                  <i class="fas fa-share-alt"></i>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 오른쪽: 상품 목록 & 채팅 -->
        <div class="sidebar">
          <!-- 상품 목록 -->
          <div class="product-list" ref="productList">
            <div class="product-item" v-for="product in products" :key="product.productId" @click="selectProduct(product)">
              <div v-if="product.getDiscountPercent() > 0" class="product-badge">{{ product.getDiscountPercent() }}%</div>
              <img
                  :src="product.mainImage || getDefaultProductImage(product.productId)"
                  :alt="product.name"
                  class="product-thumb"
              />
              <div class="product-info">
                <h4 class="product-name">{{ product.name }}</h4>
                <div class="product-price">
                  <span class="discount-rate">{{ product.getDiscountPercent() }}%</span>
                  <span class="price">{{ formatPrice(product.getFinalPrice()) }}원</span>
                </div>
                <div class="shipping-info">무료배송</div>
              </div>
            </div>

            <button v-if="products.length > 0" class="view-more-btn">
              상품 {{ products.length }}개 전체 보기
            </button>
          </div>

          <!-- 실시간 채팅 -->
          <div class="chat-section">
            <div class="chat-header">
              <h3><i class="fas fa-comments"></i> 실시간 채팅</h3>
              <button class="chat-toggle" @click="toggleChat">
                <i v-if="chatExpanded" class="fas fa-chevron-down"></i>
                <i v-else class="fas fa-chevron-up"></i>
              </button>
            </div>

            <div v-if="chatExpanded" class="chat-container">
              <!-- 채팅 메시지 목록 -->
              <div class="chat-messages" ref="chatMessages">
                <div
                    v-for="message in chatMessages"
                    :key="message.id"
                    class="chat-message"
                    :class="{ 'my-message': message.isMine }"
                >
                  <div class="message-content">
                    <span class="username">{{ message.username }}</span>
                    <span class="message-text">{{ message.message }}</span>
                  </div>
                  <span class="message-time">{{ message.timestamp }}</span>
                </div>
              </div>

              <!-- 채팅 입력 -->
              <div class="chat-input-container">
                <input
                    type="text"
                    v-model="newMessage"
                    @keypress.enter="sendMessage"
                    placeholder="채팅을 입력하세요..."
                    class="chat-input"
                />
                <button @click="sendMessage" class="send-btn">
                  <i class="fas fa-paper-plane"></i>
                </button>
              </div>
            </div>

            <!-- 자주 묻는 질문 -->
            <div class="faq-section">
              <div class="faq-header">
                <h3><i class="fas fa-question-circle"></i> 자주 묻는 질문</h3>
                <button class="faq-toggle" @click="toggleFaq">
                  <i v-if="faqExpanded" class="fas fa-chevron-down"></i>
                  <i v-else class="fas fa-chevron-up"></i>
                </button>
              </div>

              <div v-if="faqExpanded" class="faq-list">
                <div class="faq-item" v-for="faq in faqs" :key="faq.id">
                  <div class="faq-question" @click="toggleFaqItem(faq.id)">
                    <span>{{ faq.question }}</span>
                    <i :class="faq.expanded ? 'fas fa-chevron-down' : 'fas fa-chevron-right'" class="faq-arrow"></i>
                  </div>
                  <div v-if="faq.expanded" class="faq-answer">
                    {{ faq.answer }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiClient from '@/api/axiosInstance.js'

const route = useRoute()
const router = useRouter()

// 상태 관리
const loading = ref(true)
const error = ref(null)
const broadcast = ref(null)
const products = ref([])
const chatMessages = ref([])

// 비디오 컨트롤 상태
const isPlaying = ref(false)
const isMuted = ref(false)
const progress = ref(0)

// 채팅 상태
const chatExpanded = ref(true)
const faqExpanded = ref(false)
const newMessage = ref('')

// FAQ 데이터
const faqs = ref([
  {
    id: 1,
    question: '배송은 언제 가능한가요?',
    answer: '주문 완료 후 영업일 기준 2-3일 내 배송됩니다.',
    expanded: false
  },
  {
    id: 2,
    question: '설치 서비스가 포함되나요?',
    answer: '대형 가전 제품의 경우 무료 설치 서비스가 포함됩니다.',
    expanded: false
  },
  {
    id: 3,
    question: '환불이나 교환이 가능한가요?',
    answer: '구매 후 7일 이내 미사용 제품에 한해 교환/환불이 가능합니다.',
    expanded: false
  }
])

// 인터벌 참조
let statusInterval = null
let chatInterval = null
let progressInterval = null

// 계산된 속성
const featuredProduct = computed(() => {
  return products.value.find(p => p.isFeatured) || products.value[0] || null
})

// 메서드들
const loadBroadcastData = async () => {
  try {
    loading.value = true
    error.value = null

    const broadcastId = route.params.broadcastId

    // 방송 정보 조회
    const broadcastResponse = await apiClient.get(`/api/broadcast/${broadcastId}`, { withAuth: false })
    broadcast.value = broadcastResponse.data

    // 상품 목록 조회
    const productsResponse = await apiClient.get(`/api/broadcast/${broadcastId}/products`, { withAuth: false })
    products.value = productsResponse.data.map(product => ({
      ...product,
      getDiscountPercent: () => {
        if (product.price && product.salePrice && product.price > 0) {
          return Math.round((product.price - product.salePrice) / product.price * 100)
        }
        return 0
      },
      getFinalPrice: () => {
        return product.specialPrice || product.salePrice
      }
    }))

    // 채팅 메시지 조회
    const chatResponse = await apiClient.get(`/api/broadcast/${broadcastId}/chat`, { withAuth: false })
    chatMessages.value = chatResponse.data

    // 시청자 수 증가
    await increaseViewerCount()

    // 자동 새로고침 시작
    startAutoRefresh()

  } catch (err) {
    error.value = err.response?.data?.message || '방송 정보를 불러오는데 실패했습니다'
  } finally {
    loading.value = false
  }
}

const increaseViewerCount = async () => {
  try {
    const broadcastId = route.params.broadcastId
    await apiClient.post(`/api/broadcast/${broadcastId}/view`, {}, { withAuth: false })
  } catch (err) {
    // 시청자 수 증가 실패는 무시
  }
}

const likeBroadcast = async () => {
  try {
    const broadcastId = route.params.broadcastId
    const response = await apiClient.post(`/api/broadcast/${broadcastId}/like`, {}, { withAuth: false })

    if (response.data.success && broadcast.value) {
      broadcast.value.like_count = response.data.likeCount
    }
  } catch (err) {
    // 좋아요 실패 처리
  }
}

const sendMessage = async () => {
  if (!newMessage.value.trim()) return

  try {
    const broadcastId = route.params.broadcastId
    const messageData = {
      message: newMessage.value,
      username: '시청자'
    }

    await apiClient.post(`/api/broadcast/${broadcastId}/chat`, messageData, { withAuth: false })

    // 내 메시지를 즉시 추가
    const now = new Date()
    const timeStr = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`

    chatMessages.value.push({
      id: Date.now(),
      username: '나',
      message: newMessage.value,
      timestamp: timeStr,
      isMine: true
    })

    newMessage.value = ''

    // 채팅 스크롤을 맨 아래로
    nextTick(() => {
      scrollToBottom()
    })

  } catch (err) {
    // 채팅 전송 실패 처리
  }
}

const startAutoRefresh = () => {
  // 방송 상태 주기적 업데이트 (10초마다)
  statusInterval = setInterval(async () => {
    try {
      const broadcastId = route.params.broadcastId
      const response = await apiClient.get(`/api/broadcast/${broadcastId}/status`, { withAuth: false })

      if (response.data && broadcast.value) {
        broadcast.value.current_viewers = response.data.currentViewers
        broadcast.value.like_count = response.data.likeCount
        broadcast.value.broadcast_status = response.data.broadcastStatus
      }
    } catch (err) {
      // 상태 업데이트 실패는 무시
    }
  }, 10000)

  // 채팅 메시지 주기적 업데이트 (5초마다)
  chatInterval = setInterval(async () => {
    try {
      const broadcastId = route.params.broadcastId
      const response = await apiClient.get(`/api/broadcast/${broadcastId}/chat`, { withAuth: false })

      // 새로운 메시지가 있으면 추가
      const newMessages = response.data.filter(msg =>
          !chatMessages.value.some(existing => existing.id === msg.id)
      )

      chatMessages.value.push(...newMessages)

      // 메시지가 너무 많으면 오래된 것 제거
      if (chatMessages.value.length > 100) {
        chatMessages.value = chatMessages.value.slice(-50)
      }

      if (newMessages.length > 0) {
        nextTick(() => {
          scrollToBottom()
        })
      }
    } catch (err) {
      // 채팅 업데이트 실패는 무시
    }
  }, 5000)

  // 진행률 업데이트 (재생 중일 때만)
  progressInterval = setInterval(() => {
    if (isPlaying.value && progress.value < 100) {
      progress.value = Math.min(progress.value + 0.1, 100)
    }
  }, 1000)
}

const stopAutoRefresh = () => {
  if (statusInterval) {
    clearInterval(statusInterval)
    statusInterval = null
  }
  if (chatInterval) {
    clearInterval(chatInterval)
    chatInterval = null
  }
  if (progressInterval) {
    clearInterval(progressInterval)
    progressInterval = null
  }
}

// 비디오 컨트롤
const togglePlay = () => {
  isPlaying.value = !isPlaying.value
}

const toggleMute = () => {
  isMuted.value = !isMuted.value
}

const toggleFullscreen = () => {
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    document.documentElement.requestFullscreen()
  }
}

// 채팅 관련
const toggleChat = () => {
  chatExpanded.value = !chatExpanded.value
}

const toggleFaq = () => {
  faqExpanded.value = !faqExpanded.value
}

const toggleFaqItem = (id) => {
  const faq = faqs.value.find(f => f.id === id)
  if (faq) {
    faq.expanded = !faq.expanded
  }
}

const scrollToBottom = () => {
  const chatContainer = document.querySelector('.chat-messages')
  if (chatContainer) {
    chatContainer.scrollTop = chatContainer.scrollHeight
  }
}

// 상품 관련
const selectProduct = (product) => {
  // 선택된 상품을 메인으로 표시
  const index = products.value.findIndex(p => p.productId === product.productId)
  if (index > -1) {
    // 첫 번째 상품을 featured로 설정
    products.value.forEach(p => p.isFeatured = false)
    products.value[index].isFeatured = true
  }
}

const scrollToProducts = () => {
  const productList = document.querySelector('.product-list')
  if (productList) {
    productList.scrollIntoView({ behavior: 'smooth' })
  }
}

const likeProduct = (productId) => {
  alert(`상품 ${productId} 좋아요!`)
}

const shareProduct = (product) => {
  if (navigator.share) {
    navigator.share({
      title: product.name,
      text: `${product.name} - 라이브 특가!`,
      url: window.location.href
    })
  } else {
    // 클립보드 복사
    navigator.clipboard.writeText(window.location.href)
    alert('링크가 클립보드에 복사되었습니다!')
  }
}

const showProductDetail = (product) => {
  alert(`상품 상세 정보: ${product.name}`)
}

const showBroadcastInfo = () => {
  if (broadcast.value) {
    alert(`방송 정보:\n제목: ${broadcast.value.title}\n설명: ${broadcast.value.description || '설명 없음'}\n방송자: ${broadcast.value.broadcaster_name}`)
  }
}

const shareBroadcast = () => {
  if (navigator.share) {
    navigator.share({
      title: broadcast.value?.title || '라이브 방송',
      text: `${broadcast.value?.broadcaster_name || '방송자'}의 라이브 방송을 시청하세요!`,
      url: window.location.href
    })
  } else {
    navigator.clipboard.writeText(window.location.href)
    alert('방송 링크가 클립보드에 복사되었습니다!')
  }
}

// 유틸리티 함수들
const getBroadcastStatusText = (status) => {
  const statusMap = {
    'live': 'LIVE',
    'starting': '시작중',
    'paused': '일시정지',
    'scheduled': '예정',
    'ended': '종료',
    'cancelled': '취소'
  }
  return statusMap[status] || 'LIVE'
}

const formatViewerCount = (count) => {
  if (!count) return '0'
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return count.toString()
}

const formatPrice = (price) => {
  if (!price) return '0'
  return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const getFormattedDuration = () => {
  if (!broadcast.value?.actual_start_time) return '00:00'

  const start = new Date(broadcast.value.actual_start_time)
  const now = new Date()
  const diffMinutes = Math.floor((now - start) / (1000 * 60))

  const hours = Math.floor(diffMinutes / 60)
  const minutes = diffMinutes % 60

  return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`
}

const getBroadcasterAvatar = (broadcasterId) => {
  return `https://picsum.photos/seed/user${broadcasterId}/40/40`
}

const getDefaultProductImage = (productId) => {
  return `https://picsum.photos/seed/product${productId}/200/150`
}

// 라이프사이클
onMounted(async () => {
  await loadBroadcastData()

  // 페이지 떠날 때 시청자 수 감소 (옵션)
  window.addEventListener('beforeunload', () => {
    // 실제로는 시청자 수 감소 API 호출
  })
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped src="@/assets/css/broadcastViewer.css"></style>