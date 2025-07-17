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
                @loadedmetadata="onVideoLoadedMetadata"
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
        </div>

        <!-- 오른쪽: 사이드바 (상품목록 + 채팅) -->
        <div class="sidebar">
          <!--  상품 목록 - 사이드바 상단 -->
          <div class="product-list" ref="productList">
            <div class="section-header">
              <h3>방송 상품 ({{ products.length }}개)</h3>
            </div>

            <!--  상품 아이템 - 클릭 이벤트 분리 -->
            <div
                class="product-item"
                v-for="product in products"
                :key="product.productId"
                @click="goToProductDetail(product)"
                style="cursor: pointer;"
            >
              <!-- 할인 배지 -->
              <div v-if="product.getDiscountPercent() > 0" class="product-badge">
                {{ product.getDiscountPercent() }}%
              </div>

              <!-- 상품 정보 - selectProduct 함수 제거하고 전체 클릭으로 상세페이지 이동 -->
              <div class="product-info">
                <h4 class="product-name">{{ product.name }}</h4>
                <div class="product-price">
                  <span class="price">{{ formatPrice(product.getFinalPrice()) }}원</span>
                </div>
              </div>
            </div>

            <!-- 전체 상품 보기 버튼 -->
            <button v-if="products.length > 0" class="view-more-btn" @click="goToAllProducts">
              상품 {{ products.length }}개 전체 보기
            </button>
          </div>

          <!-- 채팅 컨테이너 -->
          <ChatCommon :broadcast-id="broadcastId" />
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
import ChatCommon from '@/views/live/chat/ChatCommon.vue';
import { generateStreamUrl } from '@/config/environment'
const route = useRoute()
const router = useRouter()
const broadcastId = Number(route.params.broadcastId)
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

// 계산된 속성
const featuredProduct = computed(() => {
  return products.value.find(p => p.isFeatured) || products.value[0] || null
})

// 환경별 설정 함수
const getServiceConfig = () => {
  return {
    // 게이트웨이가 모든 라우팅을 처리하므로 단순하게 설정
    useRouter: true,        // 항상 라우터 사용
    openInNewTab: false,    // 같은 탭에서 이동
    apiBaseUrl: ''          // 상대 경로 사용 (게이트웨이가 처리)
  }
}

// 상품 관련 함수들
const selectProduct = (product) => {
  const index = products.value.findIndex(p => p.productId === product.productId)
  if (index > -1) {
    products.value.forEach(p => p.isFeatured = false)
    products.value[index].isFeatured = true
  }
}

const goToProductDetail = (product) => {
  if (!product || !product.productId) {
    console.error('상품 정보가 없습니다:', product)
    alert('상품 정보를 찾을 수 없습니다.')
    return
  }

  console.log('상품 상세페이지로 이동:', product.productId)

  try {
    // ✅ 게이트웨이가 자동으로 커머스 서비스로 라우팅
    router.push(`/product/${product.productId}`)
  } catch (error) {
    console.error('라우터 이동 실패:', error)
    // 폴백: 직접 URL 이동
    window.location.href = `/product/${product.productId}`
  }
}

const goToAllProducts = () => {
  try {
    router.push('/products')
  } catch (error) {
    console.error('전체 상품 페이지 이동 실패:', error)
    window.location.href = '/products'
  }
}

const likeProduct = async (productId) => {
  const token = localStorage.getItem('jwt') || sessionStorage.getItem('jwt')
  if (!token) {
    alert('로그인이 필요합니다.')
    return
  }

  try {
    //  게이트웨이가 자동으로 커머스 서비스로 라우팅
    const response = await axios.post(`/api/products/${productId}/like`, {}, {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      timeout: 5000
    })

    if (response.data.success) {
      // 상품 목록에서 해당 상품의 좋아요 수 업데이트
      const productIndex = products.value.findIndex(p => p.productId === productId)
      if (productIndex > -1) {
        products.value[productIndex].viewCount = (products.value[productIndex].viewCount || 0) + 1
      }

      alert('상품을 좋아요 했습니다! ❤️')
    } else {
      alert('좋아요 처리에 실패했습니다.')
    }
  } catch (error) {
    console.error('상품 좋아요 실패:', error)
    alert('좋아요 처리 중 오류가 발생했습니다.')
  }
}

const handleProductImageError = (event, product) => {
  if (event.target.dataset.errorHandled) return
  event.target.dataset.errorHandled = 'true'

  // 기본 이미지로 대체
  event.target.src = getDefaultProductImage(product.productId)

  // 기본 이미지도 실패하면 플레이스홀더 표시
  event.target.onerror = () => {
    event.target.style.display = 'none'
    const placeholder = document.createElement('div')
    placeholder.className = 'product-image-placeholder'
    placeholder.style.cssText = `
      width: 100%;
      height: 150px;
      background: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #999;
      font-size: 14px;
      border: 1px solid #ddd;
      border-radius: 4px;
    `
    placeholder.innerHTML = '이미지 없음'
    event.target.parentNode.appendChild(placeholder)
  }
}

const scrollToProducts = () => {
  const productList = document.querySelector('.product-list')
  if (productList) {
    productList.scrollIntoView({ behavior: 'smooth' })
  }
}
const onVideoLoadedMetadata = () => {
  if (videoPlayer.value) {
    const video = videoPlayer.value
    const aspectRatio = video.videoWidth / video.videoHeight

    // 비율에 따라 세로/가로 구분 (0.75 기준)
    if (aspectRatio < 0.75) {
      videoAspectRatio.value = 'vertical'
    } else {
      videoAspectRatio.value = 'horizontal'
    }

    // 비디오 플레이어 컨테이너에 클래스 추가
    const playerContainer = video.closest('.video-player')
    if (playerContainer) {
      // 기존 클래스 제거
      playerContainer.classList.remove('vertical-video', 'horizontal-video')
      // 새 클래스 추가
      playerContainer.classList.add(`${videoAspectRatio.value}-video`)
    }

    streamInfo.value.status = 'connected'
  }
}
// 스트림 URL 생성 함수
const generateStreamUrls = (broadcast) => {
  if (!broadcast) return

  if (broadcast.streamUrl) {
    hlsUrl.value = broadcast.streamUrl
    streamInfo.value.url = broadcast.streamUrl
  } else {
    const streamKey = broadcast.stream_key || broadcast.broadcast_id

    // 🔥 환경 설정 유틸리티 사용
    hlsUrl.value = generateStreamUrl(streamKey, broadcast)
    streamInfo.value.url = hlsUrl.value

    console.log('스트림 URL 생성:', hlsUrl.value)
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

// 비디오 이벤트 핸들러들
const onVideoLoadStart = () => {
  streamInfo.value.status = 'connecting'
}

const onVideoCanPlay = () => {
  streamInfo.value.status = 'connected'
  // 메타데이터가 로드되지 않았다면 다시 체크
  if (videoAspectRatio.value === 'unknown') {
    onVideoLoadedMetadata()
  }
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
})
</script>

<style scoped src="@/assets/css/broadcastViewer.css"></style>