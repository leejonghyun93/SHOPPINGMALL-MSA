<template>
  <div class="live-broadcast-viewer">
    <!-- 로딩 상태 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>방송 정보를 불러오는 중...</p>
    </div>

    <!-- 에러 상태 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">⚠️</div>
      <h3>방송을 불러오는데 실패했습니다</h3>
      <p>{{ error }}</p>
      <button @click="loadBroadcastData" class="retry-button">다시 시도</button>
    </div>

    <!-- 방송 컨텐츠 -->
    <div v-else-if="broadcast" class="broadcast-content">
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
              <span class="duration">{{ getBroadcastDuration(broadcast.actual_start_time) }}</span>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <button class="action-btn" @click="scrollToProducts">
            상품목록
          </button>
          <button class="action-btn" @click="showBroadcastInfo">
            라이브 소개
          </button>
          <button class="action-btn" @click="likeBroadcast">
            좋아요 ({{ broadcast.like_count }})
          </button>
          <button class="action-btn" @click="shareBroadcast">
            공유
          </button>
        </div>
      </div>

      <div class="main-content">
        <!-- 왼쪽: 방송 영상 -->
        <div class="video-section">
          <!-- 실제 HLS 비디오 플레이어 -->
          <div class="video-player">
            <video
                ref="videoPlayer"
                class="video-element"
                :poster="broadcast.thumbnail_url"
                controls
                autoplay
                muted
                playsinline
                @loadstart="onVideoLoadStart"
                @canplay="onVideoCanPlay"
                @error="onVideoError"
                @play="onPlay"
                @pause="onPause"
            >
              <source v-if="hlsUrl" :src="hlsUrl" type="application/x-mpegURL">
              브라우저가 비디오를 지원하지 않습니다.
            </video>

            <!-- 스트림 정보 표시 -->
            <div class="stream-info" v-if="streamInfo.url">
              <div class="stream-status">
                <span class="status-indicator" :class="streamInfo.status"></span>
                <span class="status-text">{{ getStreamStatusText() }}</span>
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
                    <span class="original-price">정가 {{ formatPrice(featuredProduct.price) }}원</span>
                  </div>
                  <div class="special-offer">
                    <span class="live-special">라이브 특가 {{ featuredProduct.getDiscountPercent() }}% 할인</span>
                    <span class="special-price">{{ formatPrice(featuredProduct.salePrice) }}원</span>
                  </div>
                  <div class="final-price">
                    <span class="final-label">최대혜택가</span>
                    <span class="final-amount">{{ formatPrice(featuredProduct.getFinalPrice()) }}원</span>
                  </div>
                </div>
              </div>
              <div class="product-actions">
                <button class="heart-btn" @click="likeProduct(featuredProduct.productId)">
                  ❤️ {{ featuredProduct.viewCount || 0 }}
                </button>
                <button class="share-btn" @click="shareProduct(featuredProduct)">
                  공유
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 🔥 오른쪽: 사이드바 (상품목록 + 채팅) -->
        <div class="sidebar">
          <!-- 🔥 상품 목록 - 사이드바 상단 -->
          <div class="product-list" ref="productList">
            <div class="section-header">
              <h3>방송 상품 ({{ products.length }}개)</h3>
            </div>

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

          <!-- 🔥 채팅 컨테이너 - 사이드바 하단 (sidebar 안으로 이동) -->
          <div class="chat-container">
            <!-- 공지 영역 -->
            <div class="notice-banner" :class="{ expanded: isNoticeExpanded }">
              <div class="notice-text" :class="{ expanded: isNoticeExpanded }">
                📢 {{ displayNotice }}
              </div>
              <button
                  v-if="shouldShowMoreBtn"
                  class="notice-toggle-btn"
                  @click="toggleNotice"
              >
                {{ isNoticeExpanded ? '접기' : '더보기' }}
              </button>
            </div>

            <!-- 메시지 + 입력창 묶음 -->
            <div class="chat-main">
              <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll">
                <div
                    v-for="(msg, index) in messages"
                    :key="index"
                    :class="['chat-message', msg.systemOnly ? 'system-message' : (isMyMessage(msg) ? 'my-message' : 'other-message')]"
                >
                  <template v-if="msg.systemOnly">
                    <div class="system-box">{{ msg.text }}</div>
                  </template>
                  <template v-else>
                    <div class="chat-line">
                      <template v-if="!isMyMessage(msg)">
                        <div class="nickname">{{ msg.from }}</div>
                      </template>
                      <div class="bubble">
                        <img v-if="msg.type === 'sticker'" :src="stickerMap[msg.text]" class="chat-sticker" />
                        <span v-else class="chat-content">{{ msg.text }}</span>
                      </div>
                    </div>
                  </template>
                </div>
              </div>

              <!-- 최근 메시지로 이동 -->
              <div v-if="showScrollToBottom" class="scroll-to-bottom" @click="scrollToBottom">
                최근 메시지로 이동
              </div>

              <!-- 입력창 -->
              <div class="chat-input">
                <input
                    ref="inputRef"
                    v-model="newMessage"
                    @focus="handleInputFocus"
                    @keyup.enter="sendMessage"
                    :placeholder="isLoggedIn ? '메시지를 입력하세요' : '로그인 후 사용가능'"
                />
                <button @click="sendMessage">전송</button>
                <button @click="toggleTools" class="tools-toggle">😎</button>
              </div>

              <!-- 도구창 -->
              <div v-if="showTools" class="chat-tools">
                <div class="tools-header">
                  <div class="tab-buttons">
                    <button :class="{ active: activeTab === 'bear' }" @click="activeTab = 'bear'">🐻</button>
                    <button :class="{ active: activeTab === 'rabbit' }" @click="activeTab = 'rabbit'">🐰</button>
                  </div>
                  <button class="close-tools" @click="showTools = false">✖</button>
                </div>
                <div class="sticker-list">
                  <img
                      v-for="(src, key) in filteredStickers"
                      :key="key"
                      :src="src"
                      class="sticker-item"
                      @click="() => sendSticker(key)"
                  />
                </div>
              </div>
            </div>

            <!-- 로그인 안내 -->
            <div v-if="showLoginModal" class="login-popup-overlay">
              <div class="login-popup">
                <p>로그인 후 채팅이 가능합니다.</p>
                <div class="popup-buttons">
                  <button @click="goToLogin">로그인 하고 채팅 참여하기</button>
                  <button @click="showLoginModal = false">로그인 없이 방송 시청하기</button>
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
import axios from 'axios'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'
import { stickerMap } from './chat/EmojiMap'
import { userState } from './chat/UserState'
import { getWebSocketUrl, getApiBaseUrl } from '@/config/websocket' // 🔥 추가
const route = useRoute()
const router = useRouter()

// 상태 관리
const loading = ref(true)
const error = ref(null)
const broadcast = ref(null)
const products = ref([])

// 비디오 및 스트림 상태
const videoPlayer = ref(null)
const isPlaying = ref(false)
const streamInfo = ref({
  url: null,
  status: 'disconnected',
})

// HLS 관련
const hlsUrl = ref(null)
let hlsPlayer = null

// 인터벌 참조
let statusInterval = null

// 채팅 관련 상태
const isLoggedIn = ref(false)
const showLoginModal = ref(false)
const messages = ref([])
const newMessage = ref('')
const messagesContainer = ref(null)
const inputRef = ref(null)
const showTools = ref(false)
const showScrollToBottom = ref(false)
const activeTab = ref('bear')
const noticeMessage = ref('')
const isNoticeExpanded = ref(false)

// 계산된 속성
const featuredProduct = computed(() => {
  return products.value.find(p => p.isFeatured) || products.value[0] || null
})

const filteredStickers = computed(() => {
  return Object.fromEntries(
      Object.entries(stickerMap).filter(([key]) => key.startsWith(activeTab.value))
  );
})

const shouldShowMoreBtn = computed(() => {
  return noticeMessage.value.length > 10;
})

const displayNotice = computed(() => {
  return noticeMessage.value.trim() !== '' ? noticeMessage.value : '등록된 공지사항이 없습니다.';
})

// 채팅 유틸리티 함수
const normalize = str => String(str || '').trim()
const isMyMessage = msg => normalize(msg.from) === normalize(userState.currentUser)

// WebSocket 연결 설정
const socket = new SockJS('http://localhost:8080/ws-chat');
const stompClient = new Client({
  webSocketFactory: () => socket,
  reconnectDelay: 5000,
  onConnect: () => {
    messages.value.push({ text: '채팅방에 입장하셨습니다.', systemOnly: true })

    stompClient.subscribe('/topic/public', msg => {
      const received = JSON.parse(msg.body)

      if (received.type === 'notice') {
        noticeMessage.value = received.text.trim() || ''
        return
      }

      messages.value.push(received)

      nextTick(() => {
        isScrolledToBottom() ? scrollToBottom() : (showScrollToBottom.value = true)
      })
    })
  },
  onDisconnect: () => {
    // 연결 해제
  },
  onStompError: (frame) => {
    // 에러 처리
  }
})

// 스트림 URL 생성 함수
const generateStreamUrls = (broadcast) => {
  if (!broadcast) return

  if (broadcast.streamUrl) {
    hlsUrl.value = broadcast.streamUrl
    streamInfo.value.url = broadcast.streamUrl
  } else {
    const streamKey = broadcast.stream_key || broadcast.broadcast_id
    const nginxHost = broadcast.nginx_host || 'localhost'
    hlsUrl.value = `http://${nginxHost}:8080/hls/${streamKey}/index.m3u8`
    streamInfo.value.url = hlsUrl.value
  }

  tryInitializeHls()
}

const tryInitializeHls = async () => {
  if (!videoPlayer.value) {
    await nextTick()
    if (!videoPlayer.value) {
      setTimeout(() => {
        tryInitializeHls()
      }, 100)
      return
    }
  }

  if (!hlsUrl.value) return

  await initHlsPlayer()
}

// HLS.js 플레이어 초기화
const initHlsPlayer = async () => {
  if (!hlsUrl.value || !videoPlayer.value) return

  try {
    if (window.Hls) {
      if (window.Hls.isSupported()) {
        hlsPlayer = new window.Hls({
          enableWorker: true,
          lowLatencyMode: true,
          backBufferLength: 90,
          debug: false
        })

        hlsPlayer.on(window.Hls.Events.MANIFEST_PARSED, () => {
          streamInfo.value.status = 'connected'
          videoPlayer.value.play().catch(() => {
            // 자동 재생 실패는 무시
          })
        })

        hlsPlayer.on(window.Hls.Events.ERROR, (event, data) => {
          streamInfo.value.status = 'error'
          if (data.fatal) {
            handleStreamError(data)
          }
        })

        hlsPlayer.loadSource(hlsUrl.value)
        hlsPlayer.attachMedia(videoPlayer.value)
      } else {
        tryNativeHls()
      }
    } else {
      tryNativeHls()
    }
  } catch (err) {
    streamInfo.value.status = 'error'
  }
}

const tryNativeHls = () => {
  if (videoPlayer.value.canPlayType('application/vnd.apple.mpegurl')) {
    videoPlayer.value.src = hlsUrl.value
    streamInfo.value.status = 'connected'
    videoPlayer.value.play().catch(() => {
      // 자동 재생 실패는 무시
    })
  } else {
    streamInfo.value.status = 'error'
    videoPlayer.value.src = hlsUrl.value
  }
}

// 스트림 에러 처리
const handleStreamError = (errorData) => {
  if (errorData.type === 'networkError') {
    setTimeout(() => {
      initHlsPlayer()
    }, 3000)
  } else if (errorData.type === 'mediaError') {
    if (hlsPlayer) {
      hlsPlayer.recoverMediaError()
    }
  }
}

// 메인 데이터 로드 함수
const loadBroadcastData = async () => {
  try {
    loading.value = true
    error.value = null

    const broadcastId = route.params.broadcastId

    // 방송 정보 조회
    const broadcastResponse = await apiClient.get(`/api/broadcast/${broadcastId}`)
    broadcast.value = broadcastResponse.data

    if (!broadcast.value.streamUrl) {
      error.value = '스트림 URL이 설정되지 않았습니다.'
      return
    }

    await nextTick()
    generateStreamUrls(broadcast.value)

    // 상품 목록 조회
    const productsResponse = await apiClient.get(`/api/broadcast/${broadcastId}/products`)
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

    // 시청자 수 증가
    await increaseViewerCount()

    // 자동 새로고침 시작
    startAutoRefresh()

    // 채팅 초기화
    await initializeChat()

  } catch (err) {
    if (err.response?.status === 401) {
      // 인증되지 않은 사용자 - 방송 시청만 가능
    } else {
      error.value = err.response?.data?.message || '방송 정보를 불러오는데 실패했습니다'
    }
  } finally {
    loading.value = false
  }
}

// 채팅 초기화
const initializeChat = async () => {
  try {
    stompClient.activate()
  } catch (err) {
    // 에러 처리
  }

  // 채팅 히스토리는 빈 상태로 시작
  messages.value = []
  noticeMessage.value = ''

  // 로그인 유저 정보 확인
  const token = localStorage.getItem('jwt') || sessionStorage.getItem('jwt')
  if (token) {
    try {
      const res = await axios.get('/api/users/profile', {
        headers: { Authorization: `Bearer ${token}` },
      })

      if (res.data && res.data.success && res.data.data) {
        const userData = res.data.data
        userState.currentUser = userData.nickname
        userState.userId = userData.userId
        isLoggedIn.value = true
      } else {
        localStorage.removeItem('jwt')
        sessionStorage.removeItem('jwt')
      }
    } catch (err) {
      localStorage.removeItem('jwt')
      sessionStorage.removeItem('jwt')
    }
  }

  scrollToBottom()
}

// 비디오 이벤트 핸들러들
const onVideoLoadStart = () => {
  streamInfo.value.status = 'connecting'
}

const onVideoCanPlay = () => {
  streamInfo.value.status = 'connected'
}

const onVideoError = (event) => {
  streamInfo.value.status = 'error'
}

const onPlay = () => {
  isPlaying.value = true
}

const onPause = () => {
  isPlaying.value = false
}

// 기타 함수들
const increaseViewerCount = async () => {
  try {
    const broadcastId = route.params.broadcastId
    await apiClient.post(`/api/broadcast/${broadcastId}/view`, {})
  } catch (err) {
    // 실패 무시
  }
}

const likeBroadcast = async () => {
  try {
    const broadcastId = route.params.broadcastId
    const response = await apiClient.post(`/api/broadcast/${broadcastId}/like`, {})

    if (response.data.success && broadcast.value) {
      broadcast.value.like_count = response.data.likeCount
    }
  } catch (err) {
    // 실패 무시
  }
}

const startAutoRefresh = () => {
  statusInterval = setInterval(async () => {
    try {
      const broadcastId = route.params.broadcastId
      const response = await apiClient.get(`/api/broadcast/${broadcastId}/status`)

      if (response.data && broadcast.value) {
        broadcast.value.current_viewers = response.data.currentViewers
        broadcast.value.like_count = response.data.likeCount
        broadcast.value.broadcast_status = response.data.broadcastStatus
      }
    } catch (err) {
      // 실패해도 계속 진행
    }
  }, 10000)
}

const stopAutoRefresh = () => {
  if (statusInterval) {
    clearInterval(statusInterval)
    statusInterval = null
  }
}

// 상품 관련
const selectProduct = (product) => {
  const index = products.value.findIndex(p => p.productId === product.productId)
  if (index > -1) {
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
    navigator.clipboard.writeText(window.location.href)
    alert('링크가 클립보드에 복사되었습니다!')
  }
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

// 채팅 관련 함수들
const sendMessage = () => {
  if (!isLoggedIn.value || newMessage.value.trim() === '' || !stompClient.connected) {
    return
  }

  const payload = {
    from: userState.currentUser,
    text: newMessage.value,
    type: 'text',
    broadcastId: route.params.broadcastId,
    userId: userState.userId
  }

  stompClient.publish({ destination: '/app/sendMessage', body: JSON.stringify(payload) })
  newMessage.value = ''
  focusInput()
  scrollToBottom()
}

const sendSticker = key => {
  if (!isLoggedIn.value || !stompClient.connected) return
  const payload = {
    from: userState.currentUser,
    type: 'sticker',
    text: key,
    broadcastId: route.params.broadcastId,
    userId: userState.userId
  }
  stompClient.publish({ destination: '/app/sendMessage', body: JSON.stringify(payload) })
  focusInput()
  scrollToBottom()
}

const focusInput = () => nextTick(() => inputRef.value?.focus())

const scrollToBottom = () => {
  nextTick(() => {
    const el = messagesContainer.value
    if (el) {
      el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
      showScrollToBottom.value = false
    }
  })
}

const isScrolledToBottom = (threshold = 200) => {
  const el = messagesContainer.value
  return !el || el.scrollHeight - el.scrollTop - el.clientHeight < threshold
}

const handleScroll = () => {
  showScrollToBottom.value = !isScrolledToBottom(200)
}

const toggleTools = () => {
  showTools.value = !showTools.value
  focusInput()
  if (showTools.value) {
    scrollToBottom()
  }
}

const goToLogin = () => router.push('/login')

const handleInputFocus = e => {
  if (!isLoggedIn.value) {
    e.target.blur()
    showLoginModal.value = true
  }
}

const toggleNotice = () => {
  isNoticeExpanded.value = !isNoticeExpanded.value
}

// 스트림 상태 텍스트
const getStreamStatusText = () => {
  const statusMap = {
    'connected': '연결됨',
    'connecting': '연결 중...',
    'disconnected': '연결 안됨',
    'error': '오류 발생'
  }
  return statusMap[streamInfo.value.status] || '알 수 없음'
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

const getBroadcastDuration = (startTime) => {
  if (!startTime) return '진행 중'

  const start = new Date(startTime)
  const now = new Date()
  const diffMinutes = Math.floor((now - start) / (1000 * 60))

  if (diffMinutes < 60) {
    return `${diffMinutes}분`
  } else {
    const hours = Math.floor(diffMinutes / 60)
    const minutes = diffMinutes % 60
    return `${hours}시간 ${minutes}분`
  }
}

const getBroadcasterAvatar = (broadcasterId) => {
  return `https://picsum.photos/seed/user${broadcasterId}/40/40`
}

const getDefaultProductImage = (productId) => {
  return `https://picsum.photos/seed/product${productId}/200/150`
}

// HLS.js 동적 로드
const loadHlsJs = () => {
  return new Promise((resolve, reject) => {
    if (window.Hls) {
      resolve(window.Hls)
      return
    }

    const script = document.createElement('script')
    script.src = 'https://cdnjs.cloudflare.com/ajax/libs/hls.js/1.4.10/hls.min.js'
    script.onload = () => resolve(window.Hls)
    script.onerror = reject
    document.head.appendChild(script)
  })
}

// 라이프사이클
onMounted(async () => {
  try {
    await loadHlsJs()
  } catch (err) {
    // HLS.js 로드 실패는 무시
  }

  await loadBroadcastData()

  window.addEventListener('beforeunload', () => {
    if (hlsPlayer) {
      hlsPlayer.destroy()
    }
  })
})

onUnmounted(() => {
  if (hlsPlayer) {
    hlsPlayer.destroy()
    hlsPlayer = null
  }

  stopAutoRefresh()

  // WebSocket 연결 해제
  if (stompClient.connected) {
    stompClient.deactivate()
  }
})
</script>



<style scoped src="@/assets/css/broadcastViewer.css"></style>
