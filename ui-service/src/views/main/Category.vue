<template>
  <div class="main-container">
    <!-- 메인 카테고리 섹션 (동그란 아이콘들) -->
    <div class="category-container">
      <div class="category-list">
        <div
            v-for="category in categories"
            :key="category.categoryId"
            class="category-item"
            :class="{ active: selectedCategory === category.categoryId }"
            @click="selectCategory(category.categoryId)"
        >
          <div class="category-icon">
            <span class="icon-content" v-if="category.icon">{{ category.icon }}</span>
            <span v-else class="all-icon">전체</span>
          </div>
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- 하위 카테고리 섹션 -->
    <div v-if="subCategories.length > 0" class="sub-category-container">
      <div class="sub-category-list">
        <!-- 전체 버튼을 맨 앞에 -->
        <div
            class="sub-category-item"
            :class="{ active: selectedSubCategory === '' }"
            @click="selectSubCategory('')"
        >
          <span class="sub-category-name">전체</span>
        </div>

        <!-- 하위 카테고리들 -->
        <div
            v-for="subCategory in subCategories"
            :key="subCategory.categoryId"
            class="sub-category-item"
            :class="{ active: selectedSubCategory === subCategory.categoryId }"
            @click="selectSubCategory(subCategory.categoryId)"
        >
          <span class="sub-category-name">{{ subCategory.name }}</span>
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
            @touchstart.prevent="startTouch"
            @touchmove.prevent="touchMove"
            @touchend="endTouch"
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
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import apiClient from '@/api/axiosInstance.js'

const selectedCategory = ref('ALL')
const selectedSubCategory = ref('')
const selectedFilter = ref('live')
const slider = ref(null)
const isDragging = ref(false)
const startX = ref(0)
const scrollLeft = ref(0)
const router = useRouter()
const route = useRoute()
const subCategories = ref([])

const categories = ref([
  { categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0 }
])

const filterTabs = ref([
  { id: 'live', name: '라이브' },
  { id: 'sale', name: '상품' }
])

const products = ref([
  { id: 1, title: '양심이면 품질 써야하는 오븐랙 리뷰티컵랩 LIVE', price: 627200, originalPrice: null, discount: 2, image: 'https://picsum.photos/300/200?random=1', isLive: true, viewers: '26만', categoryId: '7' },
  { id: 2, title: '신제품 LIVE💝 으뜨는 업(UP)의 기본 좋은 면판볼을 만나보세요!', price: 114900, originalPrice: null, discount: 22, image: 'https://picsum.photos/300/200?random=2', isLive: true, viewers: '50만', categoryId: '6' },
  { id: 3, title: '[핫이슈] 쿠구 용기리 & 밥솔 등 베스트 직대 50% 혜택!', price: 449000, originalPrice: null, discount: 30, image: 'https://picsum.photos/300/200?random=3', isLive: false, viewers: '30만', categoryId: '7' },
  { id: 4, title: '인테리어 서상철 데이터드 놓지🏠 직대 할인+토드다꾸 식별까지', price: 1330000, originalPrice: null, discount: 15, image: 'https://picsum.photos/300/200?random=4', isLive: false, viewers: '36만', categoryId: '8' },
  { id: 5, title: '오늘의라이브 SALE!! [슈퍼쿠콘특성 맛질 걸맞일예약고 메여싼 오 메스트 가전 모음', price: 1699000, originalPrice: null, discount: 35, image: 'https://picsum.photos/300/200?random=5', isLive: false, viewers: '2만', categoryId: '7' },
  { id: 6, title: '[슈퍼세일] 겨울맞이 난방용품 베스트 모음전', price: 299000, originalPrice: 450000, discount: 40, image: 'https://picsum.photos/300/200?random=6', isLive: false, viewers: null, categoryId: '8' },
  { id: 7, title: '스마트폰 액세서리 대전 📱 케이스부터 충전기까지', price: 89000, originalPrice: 150000, discount: 41, image: 'https://picsum.photos/300/200?random=7', isLive: true, viewers: '15만', categoryId: '7' },
  { id: 8, title: '건강식품 특가 🌿 면역력 UP 비타민 모음전', price: 199000, originalPrice: 280000, discount: 29, image: 'https://picsum.photos/300/200?random=8', isLive: false, viewers: '8만', categoryId: '6' },
  { id: 9, title: '겨울 패딩&코트 🧥 따뜻하고 스타일리시하게', price: 399000, originalPrice: 600000, discount: 33, image: 'https://picsum.photos/300/200?random=9', isLive: true, viewers: '22만', categoryId: '9' },
  { id: 10, title: '홈카페 용품 ☕ 원두부터 머신까지 한번에', price: 259000, originalPrice: 350000, discount: 26, image: 'https://picsum.photos/300/200?random=10', isLive: false, viewers: '12만', categoryId: '5' }
])

const getIconForCategory = (categoryId) => {
  const iconMap = {
    '1': '🥬', // 신선식품
    '2': '🥫', // 가공식품
    '3': '🍱', // 간편식/밀키트
    '4': '🍞', // 베이커리
    '5': '🥛', // 유제품/음료
    '6': '💊', // 건강식품
    '7': '🍳', // 주방용품
    '8': '🧻', // 생활용품
    '9': '🍼'  // 유아동
  }
  return iconMap[categoryId] || '📦'
}

const fetchMainCategories = async () => {
  try {
    const res = await apiClient.get('/api/categories/main', {
      withAuth: false
    })

    if (res.data && res.data.length > 0) {
      const allCategory = { categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0 }

      const serverCategories = res.data
          .filter(cat => cat.categoryUseYn === 'Y' && cat.categoryLevel === 1)
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .map(cat => ({
            categoryId: cat.categoryId,
            name: cat.name,
            icon: getIconForCategory(cat.categoryId),
            categoryDisplayOrder: cat.categoryDisplayOrder
          }))

      categories.value = [allCategory, ...serverCategories]
    }
  } catch (error) {
    console.error('메인 카테고리 조회 실패:', error)
    categories.value = [{ categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0 }]
  }
}

const fetchSubCategories = async (parentCategoryId) => {
  try {
    if (parentCategoryId === 'ALL') {
      subCategories.value = []
      selectedSubCategory.value = ''
      return
    }

    const res = await apiClient.get(`/api/categories/${parentCategoryId}/sub`, {
      withAuth: false
    })

    if (res.data && res.data.length > 0) {
      subCategories.value = res.data
          .filter(cat => cat.categoryUseYn === 'Y')
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .map(cat => ({
            categoryId: cat.categoryId,
            name: cat.name,
            categoryDisplayOrder: cat.categoryDisplayOrder
          }))
    } else {
      subCategories.value = []
    }

    selectedSubCategory.value = ''
  } catch (error) {
    console.error('하위 카테고리 조회 실패:', error)
    subCategories.value = []
    selectedSubCategory.value = ''
  }
}

// 카테고리가 변경될 때마다 하위 카테고리 조회
watch(selectedCategory, (newCategory) => {
  fetchSubCategories(newCategory)
})

onMounted(async () => {
  await fetchMainCategories()

  // URL 파라미터에서 초기값 설정
  if (route.params.categoryId) {
    selectedCategory.value = route.params.categoryId
    await fetchSubCategories(route.params.categoryId)

    if (route.params.subCategoryId) {
      selectedSubCategory.value = route.params.subCategoryId
    }
  }
})

// URL 파라미터 변화 감지
watch(() => route.params, async (newParams, oldParams) => {
  console.log('Route params changed:', newParams, oldParams)

  // 카테고리가 변경된 경우
  if (newParams.categoryId && newParams.categoryId !== selectedCategory.value) {
    selectedCategory.value = newParams.categoryId
    await fetchSubCategories(newParams.categoryId)
  }

  // 하위 카테고리가 변경된 경우
  if (newParams.subCategoryId !== selectedSubCategory.value) {
    selectedSubCategory.value = newParams.subCategoryId || ''
  }
}, { immediate: true })

const selectCategory = async (categoryId) => {
  console.log('카테고리 선택:', categoryId)
  selectedCategory.value = categoryId
  selectedSubCategory.value = ''

  // 하위 카테고리 조회
  await fetchSubCategories(categoryId)

  // URL 변경
  if (categoryId === 'ALL') {
    router.push('/category/')
  } else {
    router.push(`/category/${categoryId}`)
  }
}

const selectSubCategory = (subCategoryId) => {
  console.log('하위 카테고리 선택:', subCategoryId)
  selectedSubCategory.value = subCategoryId

  // URL만 변경하고 페이지는 새로고침하지 않음
  if (subCategoryId === '') {
    router.push(`/category/${selectedCategory.value}`)
  } else {
    router.push(`/category/${selectedCategory.value}/${subCategoryId}`)
  }
}

const selectFilter = (filterId) => {
  selectedFilter.value = filterId
}

const filteredProducts = computed(() => {
  let filtered = products.value

  // 메인 카테고리 필터링
  if (selectedCategory.value !== 'ALL') {
    filtered = filtered.filter(product => product.categoryId === selectedCategory.value)
  }

  // 하위 카테고리 필터링 (실제 구현 시 활성화)
  if (selectedSubCategory.value) {
    // TODO: 실제 상품 데이터에 subCategoryId 필드가 있을 때 활성화
    // filtered = filtered.filter(product => product.subCategoryId === selectedSubCategory.value)
    // 임시: 하위 카테고리 선택 시 상품 수를 줄여서 필터링 효과 시뮬레이션
    filtered = filtered.slice(0, Math.max(1, Math.floor(filtered.length / 2)))
  }

  // 라이브/상품 필터링
  if (selectedFilter.value === 'live') {
    filtered = filtered.filter(product => product.isLive)
  }

  return filtered
})

const formatPrice = (price) => price.toLocaleString()

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
  const walk = (x - startX.value) * 1.5
  slider.value.scrollLeft = scrollLeft.value - walk
}

const endDrag = () => {
  if (!isDragging.value) return
  isDragging.value = false
  slider.value.style.cursor = 'grab'
  slider.value.style.userSelect = 'auto'
}

const handleWheel = (e) => {
  e.preventDefault()
  const scrollAmount = e.deltaY * 0.5
  slider.value.scrollLeft += scrollAmount
}

const startTouch = (e) => {
  isDragging.value = true
  startX.value = e.touches[0].pageX
  scrollLeft.value = slider.value.scrollLeft
}

const touchMove = (e) => {
  if (!isDragging.value) return
  const x = e.touches[0].pageX
  const walk = (x - startX.value) * 1.5
  slider.value.scrollLeft = scrollLeft.value - walk
}

const endTouch = () => {
  isDragging.value = false
}
</script>

<style scoped>
.main-container {
  width: 100%;
  background: #f8f9fa;
  min-height: 100vh; /* 최소 높이 보장 */
}

/* 카테고리 스타일 */
.category-container {
  width: 100%;
  padding: 20px;
  background: white;
  border-radius: 0px;
  margin: 0;
  border-bottom: solid 1px #cacaca;
}

.category-list {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 30px;
  flex-wrap: wrap;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.category-item:hover {
  transform: scale(1.1);
}

.category-item.active {
  transform: scale(1.15);
}

.category-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.category-item.active .category-icon {
  border: 3px solid #f44336;
  box-shadow: 0 6px 20px rgba(244, 67, 54, 0.3);
}

.icon-content {
  font-size: 24px;
}

.all-icon {
  font-size: 12px;
  font-weight: bold;
  color: #f44336;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  text-align: center;
}

.category-item.active .category-name {
  color: #f44336;
  font-weight: 600;
}

/* 하위 카테고리 스타일 */
.sub-category-container {
  width: 100%;
  padding: 15px 20px;
  background: white;
  border-bottom: solid 1px #e0e0e0;
}

.sub-category-list {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.sub-category-item {
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f5f5f5;
}

.sub-category-item:hover {
  background: #e0e0e0;
}

.sub-category-item.active {
  background: #f44336;
  color: white;
}

.sub-category-name {
  font-size: 13px;
  font-weight: 500;
}

/* 인기 BEST 섹션 스타일 */
.best-section {
  padding: 30px 20px;
  background: white;
  margin: 20px;
  border-radius: 10px;
}

.best-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.best-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  display: flex;
  align-items: center;
  gap: 5px;
}

.info-icon {
  font-size: 16px;
  color: #999;
}

.filter-tabs {
  display: flex;
  gap: 10px;
}

.filter-tab {
  padding: 8px 20px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.filter-tab:hover {
  border-color: #f44336;
  color: #f44336;
}

.filter-tab.active {
  background: #f44336;
  color: white;
  border-color: #f44336;
}

/* 상품 가로 슬라이더 스타일 */
.products-slider-container {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.products-horizontal-slider {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  scroll-behavior: smooth;
  cursor: grab;
  padding: 10px 0;
}

.products-horizontal-slider::-webkit-scrollbar {
  display: none;
}

.products-horizontal-slider {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.product-card {
  background: white;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  position: relative;
  min-width: 280px;
  flex-shrink: 0;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.rank-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  z-index: 2;
}

.live-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #ff4444;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  z-index: 2;
}

.viewers-count {
  position: absolute;
  top: 40px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  z-index: 2;
}

.product-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #f0f0f0;
}

.product-info {
  padding: 15px;
}

.product-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 1.4;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-pricing {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.discount-rate {
  color: #ff4444;
  font-weight: bold;
  font-size: 16px;
}

.price {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.original-price {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.shipping-info {
  font-size: 12px;
  color: #666;
}

/* 반응형 */
@media (max-width: 768px) {
  .category-list {
    gap: 20px;
  }

  .category-icon {
    width: 50px;
    height: 50px;
  }

  .icon-content {
    font-size: 20px;
  }

  .category-name {
    font-size: 12px;
  }

  .sub-category-list {
    gap: 15px;
  }

  .sub-category-item {
    padding: 6px 12px;
  }

  .sub-category-name {
    font-size: 12px;
  }

  .best-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .products-horizontal-slider {
    gap: 15px;
  }

  .product-card {
    min-width: 250px;
  }

  .best-section {
    margin: 10px;
    padding: 20px 15px;
  }
}

@media (max-width: 480px) {
  .product-card {
    min-width: 220px;
  }

  .filter-tabs {
    width: 100%;
    justify-content: center;
  }
}
</style>