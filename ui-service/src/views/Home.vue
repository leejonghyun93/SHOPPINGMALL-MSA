<template>
  <div class="carousel-container">
    <div class="background-zones">
      <div class="zone gray-zone"></div>
      <div class="zone white-zone"></div>
      <div class="zone white-zone"></div>
      <div class="zone white-zone"></div>
      <div class="zone gray-zone"></div>
    </div>

    <div
        class="carousel-content"
        @mouseenter="pauseAutoPlay"
        @mouseleave="resumeAutoPlay"
    >
      <div class="side-image left-side" @click="goToSlide(prevIndex)">
        <img :src="images[prevIndex].src" :alt="images[prevIndex].alt" class="side-img" />
        <div class="side-title">{{ images[prevIndex].title }}</div>
      </div>

      <transition name="fade">
        <div class="main-image" :key="currentIndex">
          <img
              :src="images[currentIndex].src"
              :alt="images[currentIndex].alt"
              class="main-img"
          />
          <div class="main-overlay">
            <h3 class="main-title">{{ images[currentIndex].title }}</h3>
            <p class="main-subtitle">{{ images[currentIndex].alt }}</p>
          </div>
          <div class="main-indicator">
            {{ currentIndex + 1 }} / {{ images.length }}
          </div>
        </div>
      </transition>

      <div class="side-image right-side" @click="goToSlide(nextIndex)">
        <img :src="images[nextIndex].src" :alt="images[nextIndex].alt" class="side-img" />
        <div class="side-title">{{ images[nextIndex].title }}</div>
      </div>

      <button class="nav-btn prev-btn" @click="prevSlide">
        <ChevronLeftIcon />
      </button>
      <button class="nav-btn next-btn" @click="nextSlide">
        <ChevronRightIcon />
      </button>
    </div>

    <div class="dot-indicators">
      <button
          v-for="(image, index) in images"
          :key="index"
          class="dot"
          :class="{ active: index === currentIndex }"
          @click="goToSlide(index)"
      ></button>
    </div>

    <button class="autoplay-toggle" @click="toggleAutoPlay">
      {{ isAutoPlay ? '일시정지' : '자동재생' }}
    </button>
  </div>

  <div class="category-container">
    <div class="category-list">
      <div
          v-for="category in categories"
          :key="category.categoryId"
          class="category-item"
          :class="{ active: selectedCategory === category.categoryId }"
          @click="goToCategory(category.categoryId)"
      >
        <div class="category-icon">
          <img v-if="category.icon" :src="category.icon" :alt="category.name" class="icon-image" />
          <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M9 11H15M9 15H15M17 21H7C5.89543 21 5 20.1046 5 19V5C5 3.89543 5.89543 3 7 3H12.5858C12.851 3 13.1054 3.10536 13.2929 3.29289L19.7071 9.70711C19.8946 9.89464 20 10.149 20 10.4142V19C20 20.1046 19.1046 21 18 21H17Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <span class="category-name">{{ category.name }}</span>
      </div>
    </div>
  </div>

  <div class="live-broadcast-container">
    <div class="section-header">
      <h2 class="section-title">바로 지금! 라이브 찬스</h2>
      <div class="live-count">
        <span class="count-badge">{{ liveBroadcasts.length }}개 방송 진행중</span>
      </div>
    </div>
  </div>

  <!-- 🔥 실제 데이터로 변경된 방송 리스트 -->
  <div class="broadcast-scroll-container">
    <!-- 로딩 상태 -->
    <div v-if="broadcastsLoading" class="broadcasts-loading">
      <div class="loading-spinner"></div>
      <p>라이브 방송을 불러오는 중...</p>
    </div>

    <!-- 방송 리스트 -->
    <div v-else-if="liveBroadcasts.length > 0" class="broadcast-list">
      <div
          v-for="broadcast in liveBroadcasts.slice(0, 10)"
          :key="broadcast.broadcastId || broadcast.broadcast_id"
          class="broadcast-card"
          @click="goToBroadcast(broadcast)"
      >
        <div class="broadcast-thumbnail">
          <img
              :src="broadcast.thumbnailUrl || getDefaultThumbnail(broadcast.broadcastId || broadcast.broadcast_id)"
              :alt="broadcast.title"
              class="thumbnail-image"
              @error="handleImageError"
          />

          <div class="live-badge">
            <span class="live-dot"></span>
            {{ getBroadcastStatusText(broadcast.broadcastStatus) }}
          </div>

<!--          <div class="viewer-count">-->
<!--            <i class="fas fa-users viewer-icon"></i>-->
<!--            {{ formatViewerCount(broadcast.currentViewers) }}-->
<!--          </div>-->

        </div>

        <div class="broadcast-info">
          <h3 class="broadcast-title">{{ broadcast.title }}</h3>
<!--          <p class="broadcast-description">{{ broadcast.description || '방송 설명이 없습니다.' }}</p>-->

          <div class="broadcaster-info">
            <div class="broadcaster-avatar">
              <img
                  :src="getBroadcasterAvatar(broadcast.broadcasterId)"
                  :alt="broadcast.broadcasterName"
                  class="avatar-image"
                  @error="handleAvatarError"
              />
            </div>
            <span class="broadcaster-name">{{ broadcast.broadcasterName || '방송자' }}</span>
          </div>

          <div class="broadcast-tags">
            <span class="category-tag">{{ broadcast.categoryName || '일반' }}</span>
            <span v-if="broadcast.tags" class="tags">
              {{ formatTags(broadcast.tags) }}
            </span>
          </div>

        </div>
      </div>
    </div>

    <!-- 방송이 없을 때 -->
    <div v-else class="no-broadcasts">
      <div class="no-broadcast-icon">📺</div>
      <h3>현재 진행 중인 라이브 방송이 없습니다</h3>
      <p>잠시 후 다시 확인해주세요!</p>
    </div>
  </div>

  <div class="popular-products-container">
    <div class="section-header">
      <h2 class="section-title">지금 인기 상품</h2>
      <button class="view-all-btn" @click="goToCategory('ALL')">
        전체보기 →
      </button>
    </div>

    <div v-if="productsLoading" class="products-loading">
      <div class="loading-spinner"></div>
      <p>인기 상품을 불러오는 중...</p>
    </div>

    <div v-else-if="!popularProducts || popularProducts.length === 0" class="no-products">
      <div class="no-products-icon">📦</div>
      <h3>현재 표시할 상품이 없습니다</h3>
      <p>잠시 후 다시 확인해주세요!</p>
    </div>

    <div v-else class="products-grid">
      <div
          v-for="(product, index) in popularProducts.slice(0, 8)"
          :key="product.id || index"
          class="product-card"
          @click="goToProduct(product)"
      >
<!--        <div class="rank-badge">{{ index + 1 }}</div>-->

        <div class="product-image">
          <img
              :src="getProductImage(product)"
              :alt="product.title || product.name || '상품'"
              @error="handleImageError"
              loading="lazy"
          />
        </div>

        <div class="product-info">
          <h3 class="product-title">{{ product.title || product.name || '상품명 없음' }}</h3>

          <div class="product-pricing">
            <div v-if="product.discountRate && product.discountRate > 0" class="discount-info">
            </div>
            <div class="final-price">
              {{ formatPrice(product.salePrice || product.price) }}원
            </div>
          </div>

          <div class="product-meta">
            <span class="shipping-info">무료배송</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/api/axiosInstance'

const router = useRouter()

const ChevronLeftIcon = () => '<'
const ChevronRightIcon = () => '>'

const currentIndex = ref(0)
const isAutoPlay = ref(true)
let autoPlayInterval = null

const selectedCategory = ref('ALL')
const categories = ref([])

const popularProducts = ref([])
const productsLoading = ref(false)

// 🔥 실제 방송 데이터 관련 상태
const liveBroadcasts = ref([])
const broadcastsLoading = ref(false)

const images = ref([
  { src: "/images/banners/1.jpg", alt: "특별 이벤트", title: "7/23일 행사" },
  { src: "/images/banners/2.jpg", alt: "신제품 출시", title: "피자 할인" },
  { src: "/images/banners/3.jpg", alt: "무료배송", title: "핫 이벤트" },
])

const prevIndex = computed(() => (currentIndex.value - 1 + images.value.length) % images.value.length)
const nextIndex = computed(() => (currentIndex.value + 1) % images.value.length)

const getIconForCategory = (category) => {
  if (category.iconUrl && category.iconUrl.trim() !== '') {
    return category.iconUrl.trim();
  }

  if (category.icon && category.icon.trim() !== '') {
    return category.icon.trim();
  }

  if (category.categoryIcon && category.categoryIcon.trim() !== '') {
    const iconMap = {
      'vegetables': 'vegetables.svg',
      'canned': 'canned-food.svg',
      'meal': 'meal-box.svg',
      'bread': 'bread.svg',
      'milk': 'milk.svg',
      'medicine': 'medicine.svg',
      'cooking': 'cooking.svg',
      'tissue': 'tissue.svg',
      'baby': 'baby-bottle.svg'
    };

    const iconFile = iconMap[category.categoryIcon] || category.categoryIcon + '.svg';
    const iconUrl = `/icons/${iconFile}`;
    return iconUrl;
  }

  return null;
};

// 🔥 실제 라이브 방송 데이터 가져오기
const fetchLiveBroadcasts = async () => {
  try {
    broadcastsLoading.value = true

    // 라이브 스트리밍 서비스 API 호출
    const response = await apiClient.get('/api/broadcasts/live', {
      params: {
        broadcast_status: 'live',
        is_public: 1,
        limit: 10
      },
      withAuth: false
    })

    if (response.data && Array.isArray(response.data)) {
      // 시청자 수 높은 순으로 정렬 (이미 백엔드에서 정렬되어 오지만 확실히 하기 위해)
      liveBroadcasts.value = response.data
          .sort((a, b) => (b.currentViewers || 0) - (a.currentViewers || 0))
          .slice(0, 10) // 상위 10개만

      console.log('라이브 방송 데이터 로드 완료:', liveBroadcasts.value.length, '개')
    } else {
      liveBroadcasts.value = []
    }
  } catch (error) {
    console.error('라이브 방송 데이터 로드 실패:', error)
    liveBroadcasts.value = []
  } finally {
    broadcastsLoading.value = false
  }
}

// 🔥 방송 관련 유틸리티 함수들
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

const getDefaultThumbnail = (broadcastId) => {
  return `https://picsum.photos/seed/${broadcastId}/300/200`
}

const getBroadcasterAvatar = (broadcasterId) => {
  return `https://picsum.photos/seed/user${broadcasterId}/40/40`
}

const formatTags = (tags) => {
  if (!tags) return ''
  return tags.split(',').slice(0, 2).join(', ')
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

const goToBroadcast = (broadcast) => {
  const broadcastId = broadcast.broadcastId || broadcast.broadcast_id
  if (broadcastId) {
    router.push({
      name: 'LiveBroadcastViewer',
      params: { broadcastId: String(broadcastId) }
    })
  } else {
    alert('방송 정보를 찾을 수 없습니다.')
  }
}

// 캐러셀 관련 함수들
const nextSlide = () => {
  currentIndex.value = nextIndex.value
}

const prevSlide = () => {
  currentIndex.value = prevIndex.value
}

const goToSlide = (index) => {
  currentIndex.value = index
}

const startAutoPlay = () => {
  stopAutoPlay()
  if (!isAutoPlay.value) return
  autoPlayInterval = setInterval(() => {
    nextSlide()
  }, 4000)
}

const stopAutoPlay = () => {
  if (autoPlayInterval) {
    clearInterval(autoPlayInterval)
    autoPlayInterval = null
  }
}

const toggleAutoPlay = () => {
  isAutoPlay.value = !isAutoPlay.value
  if (isAutoPlay.value) {
    startAutoPlay()
  } else {
    stopAutoPlay()
  }
}

const pauseAutoPlay = () => {
  stopAutoPlay()
}

const resumeAutoPlay = () => {
  if (isAutoPlay.value) {
    startAutoPlay()
  }
}

const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/api/categories/main', { withAuth: false })

    if (response.data && response.data.length > 0) {
      const allCategory = { categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0 }

      const serverCategories = response.data
          .filter(cat => cat.categoryUseYn === 'Y' && cat.categoryLevel === 1)
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .slice(0, 9)
          .map(cat => {
            const processedCategory = {
              categoryId: cat.categoryId,
              name: cat.name,
              icon: getIconForCategory(cat),
              categoryDisplayOrder: cat.categoryDisplayOrder,
              categoryIcon: cat.categoryIcon,
              iconUrl: cat.iconUrl
            };
            return processedCategory;
          })

      categories.value = [allCategory, ...serverCategories]
    }
  } catch (error) {
    categories.value = [
      { categoryId: 'ALL', name: '전체', icon: null },
      { categoryId: '1', name: '신선식품', icon: null },
      { categoryId: '2', name: '가공식품', icon: null },
      { categoryId: '3', name: '간편식', icon: null },
      { categoryId: '4', name: '베이커리', icon: null },
      { categoryId: '5', name: '유제품', icon: null }
    ]
  }
}

const fetchPopularProducts = async () => {
  try {
    productsLoading.value = true

    const response = await apiClient.get('/api/products/filter', {
      params: {
        categoryId: 'ALL',
        limit: 8,
        sort: 'popular'
      },
      withAuth: false
    })

    if (response.data && Array.isArray(response.data)) {
      popularProducts.value = response.data.map(product => ({
        id: product.productId,
        title: product.name || product.title,
        name: product.name || product.title,
        price: product.price || 0,
        salePrice: product.salePrice || product.price || 0,
        discountRate: product.discountRate || 0,
        mainImage: product.mainImage,
        image: product.image,
        images: product.images,
        mainImageUrl: product.mainImageUrl
      }))
    } else {
      popularProducts.value = []
    }
  } catch (error) {
    popularProducts.value = []
  } finally {
    productsLoading.value = false
  }
}

const getProductImage = (product) => {
  const defaultImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjNmNGY2Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzZiNzI4MCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPk5vIEltYWdlPC90ZXh0Pjwvc3ZnPg=='

  const imagePath = product.mainImage ||
      product.image ||
      (product.images && product.images[0]) ||
      product.mainImageUrl

  if (imagePath && imagePath.trim() !== '') {
    if (imagePath.startsWith('http')) {
      return imagePath
    }

    if (imagePath.startsWith('/api/')) {
      return `http://localhost:8080${imagePath}`
    }

    return `http://localhost:8080/api/images/products/${imagePath}`
  }

  return defaultImage
}

const handleImageError = (event) => {
  const img = event.target
  if (img.dataset.errorHandled) return

  img.dataset.errorHandled = 'true'
  img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjNmNGY2Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzZiNzI4MCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPkltYWdlIEVycm9yPC90ZXh0Pjwvc3ZnPg=='
}

const handleAvatarError = (event) => {
  event.target.src = '/default-avatar.jpg'
}

const goToCategory = (categoryId) => {
  if (categoryId === 'ALL') {
    router.push('/category/')
  } else {
    router.push(`/category/${categoryId}`)
  }
}

const goToProduct = (product) => {
  router.push(`/product/${product.id}`)
}

const formatPrice = (price) => {
  return (price || 0).toLocaleString()
}

const formatViewerCount = (count) => {
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return count?.toString() || '0'
}

// 🔥 자동 새로고침 추가 (방송 데이터용)
let broadcastRefreshInterval = null

const startBroadcastAutoRefresh = () => {
  broadcastRefreshInterval = setInterval(() => {
    fetchLiveBroadcasts()
  }, 30000) // 30초마다 방송 데이터 새로고침
}

const stopBroadcastAutoRefresh = () => {
  if (broadcastRefreshInterval) {
    clearInterval(broadcastRefreshInterval)
    broadcastRefreshInterval = null
  }
}

onMounted(async () => {
  if (isAutoPlay.value) startAutoPlay()

  // 🔥 모든 데이터 로드 (방송 데이터 추가)
  await Promise.all([
    fetchCategories(),
    fetchPopularProducts(),
    fetchLiveBroadcasts() // 실제 방송 데이터 로드
  ])

  // 🔥 방송 데이터 자동 새로고침 시작
  startBroadcastAutoRefresh()
})

onUnmounted(() => {
  stopAutoPlay()
  stopBroadcastAutoRefresh() // 🔥 방송 자동 새로고침 정리
})
</script>

<style scoped src="@/assets/css/home.css"></style>