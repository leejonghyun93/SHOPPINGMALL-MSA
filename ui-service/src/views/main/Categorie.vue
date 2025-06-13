<template>
  <div class="main-container">
    <!-- 카테고리 섹션 -->
    <div class="category-container">
      <div class="category-list">
        <div
            v-for="category in categories"
            :key="category.id"
            class="category-item"
            :class="{ active: selectedCategory === category.id }"
            @click="selectCategory(category.id)"
        >
          <div class="category-icon">
            <span class="icon-content" v-if="category.icon">{{ category.icon }}</span>
            <span v-else class="all-icon">전체</span>
          </div>
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- 인기 BEST 섹션 -->
    <div class="best-section">
      <div class="best-header">
        <h2 class="best-title">
          인기 BEST
          <span class="info-icon">ⓘ</span>
        </h2>
        <div class="filter-tabs">
          <button
              v-for="tab in filterTabs"
              :key="tab.id"
              class="filter-tab"
              :class="{ active: selectedFilter === tab.id }"
              @click="selectFilter(tab.id)"
          >
            {{ tab.name }}
          </button>
        </div>
      </div>

      <!-- 상품 가로 슬라이더 -->
      <div class="products-slider-container">
        <div
            class="products-horizontal-slider"
            ref="slider"
            @mousedown="startDrag"
            @mousemove="drag"
            @mouseup="endDrag"
            @mouseleave="endDrag"
            @wheel="handleWheel"
        >
          <div
              v-for="(product, index) in filteredProducts"
              :key="product.id"
              class="product-card"
          >
            <!-- 순위 배지 -->
            <div class="rank-badge">{{ index + 1 }}</div>

            <!-- 라이브 배지 (라이브 상품인 경우) -->
            <div v-if="product.isLive" class="live-badge">LIVE</div>

            <!-- 시청자 수 (라이브 상품인 경우) -->
            <div v-if="product.viewers" class="viewers-count">{{ product.viewers }}명 시청</div>

            <!-- 상품 이미지 -->
            <div class="product-image">
              <img :src="product.image" :alt="product.title" />
            </div>

            <!-- 상품 정보 -->
            <div class="product-info">
              <h3 class="product-title">{{ product.title }}</h3>
              <div class="product-pricing">
                <span v-if="product.discount" class="discount-rate">{{ product.discount }}%</span>
                <span class="price">{{ formatPrice(product.price) }}원</span>
                <span v-if="product.originalPrice" class="original-price">{{ formatPrice(product.originalPrice) }}원</span>
              </div>
              <div class="shipping-info">무료배송</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const selectedCategory = ref('ALL')
const selectedFilter = ref('live')
const slider = ref(null)
const isDragging = ref(false)
const startX = ref(0)
const scrollLeft = ref(0)

const categories = ref([
  { id: 'ALL', name: '전체', icon: null },
  { id: 'FRESH', name: '신선식품', icon: '🥬' },       // 채소, 과일, 정육 등
  { id: 'PROCESSED', name: '가공식품', icon: '🥫' },   // 통조림, 장류 등
  { id: 'MEAL_KIT', name: '간편식/밀키트', icon: '🍱' },
  { id: 'BAKERY', name: '베이커리', icon: '🍞' },
  { id: 'DAIRY', name: '유제품/음료', icon: '🥛' },
  { id: 'HEALTH', name: '건강식품', icon: '💊' },
  { id: 'KITCHEN', name: '주방용품', icon: '🍳' },
  { id: 'LIVING', name: '생활용품', icon: '🧻' },
  { id: 'BABY', name: '유아동', icon: '🍼' }
])

const filterTabs = ref([
  { id: 'live', name: '라이브' },
  { id: 'popular', name: '수출팸' },
  { id: 'sale', name: '상품' }
])

const products = ref([
  {
    id: 1,
    title: '양심이면 품질 써야하는 오븐랙 리뷰티컵랩 LIVE',
    price: 627200,
    originalPrice: null,
    discount: 2,
    image: '/api/placeholder/300/200',
    isLive: true,
    viewers: '26만',
    category: 'LIFE'
  },
  {
    id: 2,
    title: '신제품 LIVE💝 으뜨는 업(UP)의 기본 좋은 면판볼을 만나보세요!',
    price: 114900,
    originalPrice: null,
    discount: 22,
    image: '/api/placeholder/300/200',
    isLive: true,
    viewers: '50만',
    category: 'FASHION'
  },
  {
    id: 3,
    title: '[핫이슈] 쿠구 용기리 & 밥솔 등 베스트 직대 50% 혜택!',
    price: 449000,
    originalPrice: null,
    discount: 30,
    image: '/api/placeholder/300/200',
    isLive: false,
    viewers: '30만',
    category: 'LIFE'
  },
  {
    id: 4,
    title: '인테리어 서상철 데이터드 놓지🏠 직대 할인+토드다꾸 식별까지',
    price: 1330000,
    originalPrice: null,
    discount: 15,
    image: '/api/placeholder/300/200',
    isLive: false,
    viewers: '36만',
    category: 'LIFE'
  },
  {
    id: 5,
    title: '오늘의라이브 SALE!! [슈퍼쿠콘특성 맛질 걸맞일예약고 메여싼 오 메스트 가전 모음',
    price: 1699000,
    originalPrice: null,
    discount: 35,
    image: '/api/placeholder/300/200',
    isLive: false,
    viewers: '2만',
    category: 'TECH'
  },
  {
    id: 6,
    title: '[슈퍼세일] 겨울맞이 난방용품 베스트 모음전',
    price: 299000,
    originalPrice: 450000,
    discount: 40,
    image: '/api/placeholder/300/200',
    isLive: false,
    viewers: null,
    category: 'LIFE'
  },
  {
    id: 7,
    title: '스마트폰 액세서리 대전 📱 케이스부터 충전기까지',
    price: 89000,
    originalPrice: 150000,
    discount: 41,
    image: '/api/placeholder/300/200',
    isLive: true,
    viewers: '15만',
    category: 'TECH'
  },
  {
    id: 8,
    title: '건강식품 특가 🌿 면역력 UP 비타민 모음전',
    price: 199000,
    originalPrice: 280000,
    discount: 29,
    image: '/api/placeholder/300/200',
    isLive: false,
    viewers: '8만',
    category: 'FOOD'
  },
  {
    id: 9,
    title: '겨울 패딩&코트 🧥 따뜻하고 스타일리시하게',
    price: 399000,
    originalPrice: 600000,
    discount: 33,
    image: '/api/placeholder/300/200',
    isLive: true,
    viewers: '22만',
    category: 'FASHION'
  },
  {
    id: 10,
    title: '홈카페 용품 ☕ 원두부터 머신까지 한번에',
    price: 259000,
    originalPrice: 350000,
    discount: 26,
    image: '/api/placeholder/300/200',
    isLive: false,
    viewers: '12만',
    category: 'LIFE'
  }
])

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
  console.log('선택된 카테고리:', categoryId)
}

const selectFilter = (filterId) => {
  selectedFilter.value = filterId
  console.log('선택된 필터:', filterId)
}

const filteredProducts = computed(() => {
  let filtered = products.value

  // 카테고리 필터
  if (selectedCategory.value !== 'ALL') {
    filtered = filtered.filter(product => product.category === selectedCategory.value)
  }

  // 탭 필터
  if (selectedFilter.value === 'live') {
    filtered = filtered.filter(product => product.isLive)
  }

  return filtered
})

const formatPrice = (price) => {
  return price.toLocaleString()
}

// 슬라이더 드래그 기능
const startDrag = (e) => {
  e.preventDefault()
  isDragging.value = true
  startX.value = e.pageX
  scrollLeft.value = slider.value.scrollLeft
  slider.value.style.cursor = 'grabbing'
  slider.value.style.userSelect = 'none'
}

const drag = (e) => {
  if (!isDragging.value) return
  e.preventDefault()

  const x = e.pageX
  const walk = (x - startX.value) * 1.5 // 드래그 감도 조절
  slider.value.scrollLeft = scrollLeft.value - walk
}

const endDrag = () => {
  if (!isDragging.value) return
  isDragging.value = false
  slider.value.style.cursor = 'grab'
  slider.value.style.userSelect = 'auto'
}

// 마우스 휠 스크롤 (수평 스크롤)
const handleWheel = (e) => {
  e.preventDefault()
  const scrollAmount = e.deltaY * 0.5 // 스크롤 속도 조절
  slider.value.scrollLeft += scrollAmount
}

// 터치 이벤트 지원 (모바일)
const startTouch = (e) => {
  isDragging.value = true
  startX.value = e.touches[0].pageX
  scrollLeft.value = slider.value.scrollLeft
}

const touchMove = (e) => {
  if (!isDragging.value) return
  e.preventDefault()

  const x = e.touches[0].pageX
  const walk = (x - startX.value) * 1.5
  slider.value.scrollLeft = scrollLeft.value - walk
}

const endTouch = () => {
  isDragging.value = false
}
</script>


<style scoped src="@/assets/css/category.css"></style>