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

  <div class="broadcast-scroll-container">
    <div v-if="broadcastsLoading" class="broadcasts-loading">
      <div class="loading-spinner"></div>
      <p>라이브 방송을 불러오는 중...</p>
    </div>

    <div v-else-if="liveBroadcasts.length > 0" class="broadcast-list">
      <div
          v-for="broadcast in liveBroadcasts.slice(0, 10)"
          :key="broadcast.id"
          class="broadcast-card"
          @click="goToBroadcast(broadcast)"
      >
        <div class="broadcast-thumbnail">
          <img
              :src="getBroadcastThumbnail(broadcast)"
              :alt="broadcast.title"
              class="thumbnail-image"
              @error="handleImageError"
          />
          <div class="live-badge">
            <span class="live-dot"></span>
            LIVE
          </div>
        </div>

        <div class="broadcast-info">
          <h3 class="broadcast-title">{{ broadcast.title }}</h3>
          <div class="broadcaster-info">
<!--            <span class="broadcaster-name">{{ broadcast.broadcasterName || '방송자' }}</span>-->
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

    <div v-else class="no-broadcasts">
      <div class="no-broadcast-icon"></div>
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
      <div class="no-products-icon"></div>
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
import { useSmartImages } from '@/composables/useSmartImages'

const router = useRouter()
const { getProductImage, handleImageError } = useSmartImages()

const ChevronLeftIcon = () => '<'
const ChevronRightIcon = () => '>'

const currentIndex = ref(0)
const isAutoPlay = ref(true)
let autoPlayInterval = null

const selectedCategory = ref('ALL')
const categories = ref([])
const popularProducts = ref([])
const productsLoading = ref(false)
const liveBroadcasts = ref([])
const broadcastsLoading = ref(false)

const images = ref([
  { src: "/images/banners/1.jpg", alt: "특별 이벤트", title: "7/23일 행사" },
  { src: "/images/banners/2.jpg", alt: "신제품 출시", title: "피자 할인" },
  { src: "/images/banners/3.jpg", alt: "무료배송", title: "핫 이벤트" },
])

const prevIndex = computed(() => (currentIndex.value - 1 + images.value.length) % images.value.length)
const nextIndex = computed(() => (currentIndex.value + 1) % images.value.length)

const getBroadcastThumbnail = (broadcast) => {
  const thumbnailUrl = broadcast.thumbnailUrl || broadcast.thumbnail_url

  // 1. 썸네일이 있는 경우 - useSmartImages 활용
  if (thumbnailUrl && thumbnailUrl.trim() !== '') {
    // useSmartImages의 getProductImage 함수를 활용하여 경로 변환
    const thumbnailObject = {
      mainImage: thumbnailUrl,
      image: thumbnailUrl,
      imageUrl: thumbnailUrl,
      name: broadcast.title || '방송',
      title: broadcast.title || '방송'
    }

    // useSmartImages로 경로 변환 처리
    const convertedImage = getProductImage(thumbnailObject)
    return convertedImage
  }

  // 2. 방송에 연결된 상품 이미지 활용
  if (broadcast.products && broadcast.products.length > 0) {
    const firstProduct = broadcast.products[0]
    const productImage = getProductImage(firstProduct)
    return productImage
  }

  // 3. 최종 기본 이미지
  return '/images/banners/products/default-product.jpg'
}
const fetchLiveBroadcasts = async () => {
  try {
    broadcastsLoading.value = true
    const response = await apiClient.get('/api/broadcasts/live', {
      params: {
        broadcast_status: 'live',
        is_public: 1,
        limit: 10
      },
      withAuth: false
    })

    if (response.data && Array.isArray(response.data)) {
      liveBroadcasts.value = response.data.map(broadcast => ({
        id: broadcast.broadcast_id || broadcast.broadcastId,
        title: broadcast.title,
        thumbnailUrl: broadcast.thumbnail_url || broadcast.thumbnailUrl,
        broadcasterId: broadcast.broadcaster_id || broadcast.broadcasterId,
        broadcasterName: broadcast.broadcaster_name || broadcast.broadcasterName,
        currentViewers: broadcast.current_viewers || broadcast.currentViewers || 0,
        categoryName: broadcast.category_name || broadcast.categoryName,
        tags: broadcast.tags
      })).sort((a, b) => (b.currentViewers || 0) - (a.currentViewers || 0))
    } else {
      liveBroadcasts.value = []
    }
  } catch (error) {
    liveBroadcasts.value = []
  } finally {
    broadcastsLoading.value = false
  }
}

const formatTags = (tags) => {
  if (!tags) return ''
  return tags.split(',').slice(0, 2).join(', ')
}

const goToBroadcast = (broadcast) => {
  if (broadcast.id) {
    router.push({
      name: 'LiveBroadcastViewer',
      params: { broadcastId: String(broadcast.id) }
    })
  }
}

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
      const allCategory = { categoryId: 'ALL', name: '전체', icon: null }
      const serverCategories = response.data
          .filter(cat => cat.categoryUseYn === 'Y' && cat.categoryLevel === 1)
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .slice(0, 9)
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
      popularProducts.value = response.data.map((product, index) => ({
        id: product.productId,
        title: product.name || product.title,
        name: product.name || product.title,
        price: product.price || 0,
        salePrice: product.salePrice || product.price || 0,
        mainImage: product.mainImage,
        categoryId: product.categoryId || (index % 5) + 1
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

let broadcastRefreshInterval = null

const startBroadcastAutoRefresh = () => {
  broadcastRefreshInterval = setInterval(() => {
    fetchLiveBroadcasts()
  }, 30000)
}

const stopBroadcastAutoRefresh = () => {
  if (broadcastRefreshInterval) {
    clearInterval(broadcastRefreshInterval)
    broadcastRefreshInterval = null
  }
}

onMounted(async () => {
  if (isAutoPlay.value) startAutoPlay()

  await Promise.all([
    fetchCategories(),
    fetchPopularProducts(),
    fetchLiveBroadcasts()
  ])

  startBroadcastAutoRefresh()
})

onUnmounted(() => {
  stopAutoPlay()
  stopBroadcastAutoRefresh()
})
</script>

<style scoped src="@/assets/css/home.css"></style>