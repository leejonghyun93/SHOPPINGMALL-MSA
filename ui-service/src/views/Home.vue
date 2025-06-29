<template>
  <div class="carousel-container">
    <!-- 3구역 배경 -->
    <div class="background-zones">
      <div class="zone gray-zone"></div>
      <div class="zone white-zone"></div>
      <div class="zone white-zone"></div>
      <div class="zone white-zone"></div>
      <div class="zone gray-zone"></div>
    </div>

    <!-- 캐러셀 메인 컨테이너 -->
    <div
        class="carousel-content"
        @mouseenter="pauseAutoPlay"
        @mouseleave="resumeAutoPlay"
    >
      <!-- 왼쪽 사이드 이미지 -->
      <div class="side-image left-side" @click="goToSlide(prevIndex)">
        <img :src="images[prevIndex].src" :alt="images[prevIndex].alt" class="side-img" />
        <div class="side-title">{{ images[prevIndex].title }}</div>
      </div>

      <!-- 메인 중앙 이미지 -->
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

      <!-- 오른쪽 사이드 이미지 -->
      <div class="side-image right-side" @click="goToSlide(nextIndex)">
        <img :src="images[nextIndex].src" :alt="images[nextIndex].alt" class="side-img" />
        <div class="side-title">{{ images[nextIndex].title }}</div>
      </div>

      <!-- 네비게이션 버튼 -->
      <button class="nav-btn prev-btn" @click="prevSlide">
        <ChevronLeftIcon />
      </button>
      <button class="nav-btn next-btn" @click="nextSlide">
        <ChevronRightIcon />
      </button>
    </div>

    <!-- 하단 도트 인디케이터 -->
    <div class="dot-indicators">
      <button
          v-for="(image, index) in images"
          :key="index"
          class="dot"
          :class="{ active: index === currentIndex }"
          @click="goToSlide(index)"
      ></button>
    </div>

    <!-- 자동재생 토글 -->
    <button class="autoplay-toggle" @click="toggleAutoPlay">
      {{ isAutoPlay ? '일시정지' : '자동재생' }}
    </button>
  </div>

  <!-- 🔥 수정된 카테고리 섹션 (Category.vue와 동일한 아이콘 처리) -->
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
          <!-- 🔥 SVG 아이콘으로 변경 (Category.vue와 동일) -->
          <img v-if="category.icon" :src="category.icon" :alt="category.name" class="icon-image" />
          <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M9 11H15M9 15H15M17 21H7C5.89543 21 5 20.1046 5 19V5C5 3.89543 5.89543 3 7 3H12.5858C12.851 3 13.1054 3.10536 13.2929 3.29289L19.7071 9.70711C19.8946 9.89464 20 10.149 20 10.4142V19C20 20.1046 19.1046 21 18 21H17Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <span class="category-name">{{ category.name }}</span>
      </div>
    </div>
  </div>

  <!-- 라이브 방송 섹션 -->
  <div class="live-broadcast-container">
    <div class="section-header">
      <h2 class="section-title">🔴 바로 지금! 라이브 찬스</h2>
      <div class="live-count">
        <span class="count-badge">{{ liveBroadcasts.length }}개 방송 진행중</span>
      </div>
    </div>
  </div>

  <!-- 라이브 방송 카드 리스트 -->
  <div class="broadcast-scroll-container">
    <div class="broadcast-list">
      <div
          v-for="broadcast in liveBroadcasts.slice(0, 10)"
          :key="broadcast.broadcast_id"
          class="broadcast-card"
          @click="goToBroadcast(broadcast.broadcast_id)"
      >
        <!-- 방송 썸네일 -->
        <div class="broadcast-thumbnail">
          <img
              :src="broadcast.thumbnail_url || `https://picsum.photos/seed/${broadcast.broadcast_id}/300/200`"
              :alt="broadcast.title"
              class="thumbnail-image"
          />

          <!-- 라이브 배지 -->
          <div class="live-badge">
            <span class="live-dot"></span>
            LIVE
          </div>

          <!-- 시청자 수 -->
          <div class="viewer-count">
            <span class="viewer-icon">👥</span>
            {{ formatViewerCount(broadcast.current_viewers) }}
          </div>

          <!-- 방송 시간 -->
          <div class="broadcast-time">
            {{ getBroadcastDuration(broadcast.actual_start_time) }}
          </div>
        </div>

        <!-- 방송 정보 -->
        <div class="broadcast-info">
          <h3 class="broadcast-title">{{ broadcast.title }}</h3>
          <p class="broadcast-description">{{ broadcast.description }}</p>

          <!-- 방송자 정보 -->
          <div class="broadcaster-info">
            <div class="broadcaster-avatar">
              <img
                  :src="`https://picsum.photos/seed/user${broadcast.broadcaster_id}/40/40`"
                  :alt="broadcast.broadcaster_name"
                  class="avatar-image"
              />
            </div>
            <span class="broadcaster-name">{{ broadcast.broadcaster_name }}</span>
          </div>

          <!-- 카테고리 태그 -->
          <div class="broadcast-tags">
            <span class="category-tag">{{ broadcast.category }}</span>
            <span v-if="broadcast.tags" class="tags">
            {{ broadcast.tags.split(',').slice(0, 2).join(', ') }}
          </span>
          </div>

          <!-- 좋아요 수 -->
          <div class="broadcast-stats">
          <span class="like-count">
            <span class="heart-icon">❤️</span>
            {{ broadcast.like_count }}
          </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 방송이 없을 때 -->
    <div v-if="liveBroadcasts.length === 0" class="no-broadcasts">
      <div class="no-broadcast-icon">📺</div>
      <h3>현재 진행 중인 라이브 방송이 없습니다</h3>
      <p>잠시 후 다시 확인해주세요!</p>
    </div>
  </div>

  <!-- 인기 상품 섹션 -->
  <div class="popular-products-container">
    <div class="section-header">
      <h2 class="section-title">🔥 지금 인기 상품</h2>
      <button class="view-all-btn" @click="goToCategory('ALL')">
        전체보기 →
      </button>
    </div>

    <!-- 로딩 상태 -->
    <div v-if="productsLoading" class="products-loading">
      <div class="loading-spinner"></div>
      <p>인기 상품을 불러오는 중...</p>
    </div>

    <!-- 상품이 없을 때 -->
    <div v-else-if="!popularProducts || popularProducts.length === 0" class="no-products">
      <div class="no-products-icon">📦</div>
      <h3>현재 표시할 상품이 없습니다</h3>
      <p>잠시 후 다시 확인해주세요!</p>
    </div>

    <!-- 상품 그리드 4x2 -->
    <div v-else class="products-grid">
      <div
          v-for="(product, index) in popularProducts.slice(0, 8)"
          :key="product.id || index"
          class="product-card"
          @click="goToProduct(product)"
      >
        <!-- 순위 배지 -->
        <div class="rank-badge">{{ index + 1 }}</div>

        <!-- 상품 이미지 -->
        <div class="product-image">
          <img
              :src="getProductImage(product)"
              :alt="product.title || product.name || '상품'"
              @error="handleImageError"
              loading="lazy"
          />
        </div>

        <!-- 상품 정보 -->
        <div class="product-info">
          <h3 class="product-title">{{ product.title || product.name || '상품명 없음' }}</h3>

          <!-- 가격 정보 -->
          <div class="product-pricing">
            <div v-if="product.discountRate && product.discountRate > 0" class="discount-info">
              <span class="discount-rate">{{ product.discountRate }}%</span>
              <span class="original-price">{{ formatPrice(product.price) }}원</span>
            </div>
            <div class="final-price">
              {{ formatPrice(product.salePrice || product.price) }}원
            </div>
          </div>

          <!-- 배송 정보 -->
          <div class="product-meta">
            <span class="shipping-info">🚚 무료배송</span>
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

// 라우터
const router = useRouter()

// 아이콘 컴포넌트 (실제 프로젝트에서는 라이브러리 사용)
const ChevronLeftIcon = () => '<'
const ChevronRightIcon = () => '>'

// 캐러셀 관련 상태
const currentIndex = ref(0)
const isAutoPlay = ref(true)
let autoPlayInterval = null

// 카테고리 관련 상태
const selectedCategory = ref('ALL')
const categories = ref([])

// 상품 관련 상태
const popularProducts = ref([])
const productsLoading = ref(false)

// 이미지 배열
const images = ref([
  { src: "https://picsum.photos/seed/1/800/400", alt: "특별 이벤트", title: "6/13 라이브" },
  { src: "https://picsum.photos/seed/2/800/400", alt: "신제품 출시", title: "다우니 신제품" },
  { src: "https://picsum.photos/seed/3/800/400", alt: "무료배송", title: "무료배송 이벤트" },
  { src: "https://picsum.photos/seed/4/800/400", alt: "회원 혜택", title: "회원 전용 할인" },
  { src: "https://picsum.photos/seed/5/800/400", alt: "할인 상품", title: "타임세일" },
])

// 라이브 방송 데이터 (기존과 동일)
const liveBroadcasts = ref([
  {
    broadcast_id: 1,
    broadcaster_id: 101,
    broadcaster_name: "라이프 쇼핑",
    title: "삼에서 먹어도 제맛! 전복죽 달인",
    description: "전복죽 만들기의 모든 것! 지금 특가로 만나보세요",
    broadcast_status: "live",
    actual_start_time: "2025-06-24T14:30:00",
    current_viewers: 134,
    like_count: 89,
    category: "푸드",
    tags: "전복죽,간편식,건강식",
    thumbnail_url: null
  },
  {
    broadcast_id: 2,
    broadcaster_id: 102,
    broadcaster_name: "닥터안에그",
    title: "[1+1]닥터안에그 무황성계 30구",
    description: "신선한 계란을 특가로! 지금 주문하면 1+1 혜택",
    broadcast_status: "live",
    actual_start_time: "2025-06-24T15:00:00",
    current_viewers: 89,
    like_count: 45,
    category: "신선식품",
    tags: "계란,1+1,특가",
    thumbnail_url: null
  },
  {
    broadcast_id: 3,
    broadcaster_id: 103,
    broadcaster_name: "라이브 특가",
    title: "엄마 손맛 그 자체! 순두부찌개",
    description: "집에서 쉽게 만드는 순두부찌개 레시피와 재료 세트",
    broadcast_status: "live",
    actual_start_time: "2025-06-24T13:45:00",
    current_viewers: 71,
    like_count: 112,
    category: "간편식",
    tags: "순두부찌개,집밥,간편식",
    thumbnail_url: null
  },
  {
    broadcast_id: 4,
    broadcaster_id: 104,
    broadcaster_name: "글램핑 한돈깔비",
    title: "글램핑 한돈깔비❤️ 특가방송",
    description: "프리미엄 한돈으로 만든 깔비! 글램핑 분위기까지",
    broadcast_status: "live",
    actual_start_time: "2025-06-24T16:15:00",
    current_viewers: 156,
    like_count: 203,
    category: "정육",
    tags: "한돈,깔비,글램핑,특가",
    thumbnail_url: null
  },
  {
    broadcast_id: 5,
    broadcaster_id: 105,
    broadcaster_name: "쪽쪽쪽주",
    title: "[쪽쪽쪽주] 우욱짬 인기상품",
    description: "인기 상품들을 한번에! 쪽쪽쪽주 스페셜 라이브",
    broadcast_status: "live",
    actual_start_time: "2025-06-24T14:00:00",
    current_viewers: 267,
    like_count: 89,
    category: "종합",
    tags: "인기상품,스페셜,할인",
    thumbnail_url: null
  }
])

// 이전/다음 인덱스 계산
const prevIndex = computed(() => (currentIndex.value - 1 + images.value.length) % images.value.length)
const nextIndex = computed(() => (currentIndex.value + 1) % images.value.length)

// 🔥 Category.vue와 동일한 아이콘 처리 로직
const getIconForCategory = (category) => {
  console.log('🔍 홈 카테고리 아이콘 처리:', category); // 디버깅용

  // 1. 서버에서 제공하는 iconUrl 우선 사용
  if (category.iconUrl && category.iconUrl.trim() !== '') {
    console.log('✅ iconUrl 사용:', category.iconUrl);
    return category.iconUrl.trim();
  }

  // 2. icon 필드 사용
  if (category.icon && category.icon.trim() !== '') {
    console.log('✅ icon 사용:', category.icon);
    return category.icon.trim();
  }

  // 3. categoryIcon으로 로컬 경로 생성
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
    console.log('✅ categoryIcon 매핑:', category.categoryIcon, '->', iconUrl);
    return iconUrl;
  }

  console.log('❌ 아이콘을 찾을 수 없음:', category);
  return null;
};

// 캐러셀 함수들
const nextSlide = () => {
  currentIndex.value = nextIndex.value
}

const prevSlide = () => {
  currentIndex.value = prevIndex.value
}

const goToSlide = (index) => {
  currentIndex.value = index
}

// 자동재생 컨트롤
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

// 🔥 수정된 카테고리 데이터 가져오기 (Category.vue와 동일한 로직)
const fetchCategories = async () => {
  try {
    const response = await apiClient.get('/api/categories/main', { withAuth: false })
    console.log('🔍 홈 카테고리 API 응답:', response.data); // 디버깅용

    if (response.data && response.data.length > 0) {
      const allCategory = { categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0 }

      const serverCategories = response.data
          .filter(cat => cat.categoryUseYn === 'Y' && cat.categoryLevel === 1)
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .slice(0, 9) // 홈에서는 최대 9개만 표시
          .map(cat => {
            const processedCategory = {
              categoryId: cat.categoryId,
              name: cat.name,
              icon: getIconForCategory(cat), // 🔥 수정된 함수 사용
              categoryDisplayOrder: cat.categoryDisplayOrder,
              categoryIcon: cat.categoryIcon,
              iconUrl: cat.iconUrl
            };
            console.log('🔍 홈 처리된 카테고리:', processedCategory); // 디버깅용
            return processedCategory;
          })

      categories.value = [allCategory, ...serverCategories]
      console.log('✅ 홈 최종 카테고리 목록:', categories.value); // 디버깅용
    }
  } catch (error) {
    console.error('❌ 홈 카테고리 조회 실패:', error)
    // 기본 카테고리 설정 (아이콘 없이)
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

// 인기 상품 데이터 가져오기
const fetchPopularProducts = async () => {
  try {
    productsLoading.value = true

    const response = await apiClient.get('/api/products/filter', {
      params: {
        categoryId: 'ALL',
        limit: 8,
        sort: 'popular' // 인기순 정렬
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
    console.error('인기 상품 로드 실패:', error)
    popularProducts.value = []
  } finally {
    productsLoading.value = false
  }
}

// 상품 이미지 URL 생성
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

// 이미지 에러 처리
const handleImageError = (event) => {
  const img = event.target
  if (img.dataset.errorHandled) return

  img.dataset.errorHandled = 'true'
  img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjNmNGY2Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxNCIgZmlsbD0iIzZiNzI4MCIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPkltYWdlIEVycm9yPC90ZXh0Pjwvc3ZnPg=='
}

// 네비게이션 함수들
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

const goToBroadcast = (broadcastId) => {
  console.log('방송 페이지로 이동:', broadcastId)
  // router.push(`/live/${broadcastId}`)
}

// 유틸리티 함수들
const formatPrice = (price) => {
  return (price || 0).toLocaleString()
}

const formatViewerCount = (count) => {
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return count.toString()
}

const getBroadcastDuration = (startTime) => {
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

// 컴포넌트 마운트
onMounted(async () => {
  // 자동재생 시작
  if (isAutoPlay.value) startAutoPlay()

  // 데이터 로드
  await Promise.all([
    fetchCategories(),
    fetchPopularProducts()
  ])
})

onUnmounted(() => {
  stopAutoPlay()
})
</script>

<style scoped src="@/assets/css/home.css"></style>