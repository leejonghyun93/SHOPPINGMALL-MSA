<template>
  <div class="main-container">
    <div v-if="searchKeyword" class="search-results-header">
      <div class="search-info">
        <h2 class="search-title">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M21 21L16.514 16.506L21 21ZM19 10.5C19 15.194 15.194 19 10.5 19C5.806 19 2 15.194 2 10.5C2 5.806 5.806 2 10.5 2C15.194 2 19 5.806 19 10.5Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          "{{ searchKeyword }}" 검색 결과
        </h2>
        <div class="search-stats">
          <span class="result-count">{{ filteredProducts.length }}개 상품</span>
          <span v-if="allProducts.length > 0" class="total-count">
            (전체 {{ allProducts.length }}개 중)
          </span>
        </div>
      </div>
      <div class="search-actions">
        <button @click="clearSearch" class="btn btn-outline-secondary btn-sm">
          검색 해제
        </button>
      </div>
    </div>

    <!-- 메인 카테고리 섹션 (검색 중이 아닐 때만 표시) -->
    <div v-if="!searchKeyword" class="category-container">
      <div class="category-list">
        <div
            v-for="category in categories"
            :key="category.categoryId"
            class="category-item"
            :class="{ active: String(selectedCategory) === String(category.categoryId) }"
            @click="selectCategory(category.categoryId)"
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

    <!-- 하위 카테고리 섹션 (검색 중이 아닐 때만 표시) -->
    <div v-if="!searchKeyword && subCategories.length > 0" class="sub-category-container">
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
          {{ searchKeyword ? '검색 상품' : '인기 BEST' }}
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
            <path d="m9 12 2 2 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
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

      <!-- 로딩 상태 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>{{ searchKeyword ? '검색 중...' : '상품을 불러오는 중...' }}</p>
        <p class="loading-details">
          {{ searchKeyword ?
            `검색어: ${searchKeyword}` :
            `카테고리: ${selectedCategory} / 서브카테고리: ${selectedSubCategory || '없음'}`
          }}
        </p>
      </div>

      <!-- 상품이 없을 때 -->
      <div v-else-if="!displayProducts || displayProducts.length === 0" class="no-products">
        <div class="no-products-icon">
          <svg v-if="searchKeyword" width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M21 21L16.514 16.506L21 21ZM19 10.5C19 15.194 15.194 19 10.5 19C5.806 19 2 15.194 2 10.5C2 5.806 5.806 2 10.5 2C15.194 2 19 5.806 19 10.5Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <svg v-else width="64" height="64" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20.5 16V10C20.5 9.61929 20.3069 9.27312 20 9.1L12 5.5L4 9.1C3.69308 9.27312 3.5 9.61929 3.5 10V16C3.5 16.3807 3.69308 16.7269 4 16.9L12 20.5L20 16.9C20.3069 16.7269 20.5 16.3807 20.5 16Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h3>{{ searchKeyword ? '검색 결과가 없습니다' : '선택한 카테고리에 상품이 없습니다' }}</h3>
      </div>

      <!-- 상품이 있을 때 -->
      <div v-else>
        <!-- 인기 BEST 가로 슬라이더 -->
        <div class="products-slider-container">
          <div class="products-info">
            <span class="products-count">
              {{ searchKeyword ?
                `검색 결과 TOP ${Math.min(displayProducts.length, 10)}` :
                `인기 상품 TOP ${Math.min(displayProducts.length, 10)}`
              }}
            </span>
          </div>

          <div
              class="products-horizontal-slider"
              ref="slider"
              @mousedown="startDrag"
              @mousemove="drag"
              @mouseup="endDrag"
              @mouseleave="endDrag"
              @wheel.passive="handleWheelPassive"
              @touchstart.passive="startTouchPassive"
              @touchmove.passive="touchMovePassive"
              @touchend="endTouch"
          >
            <div
                v-for="(product, index) in displayProducts.slice(0, 10)"
                :key="product.id || index"
                class="product-card horizontal"
                @click="goToProductDetail(product)"
            >
              <!-- 순위 배지 -->
              <div class="rank-badge">{{ index + 1 }}</div>

              <!-- 라이브 배지 (라이브 상품인 경우) -->
              <div v-if="product.isLive" class="live-badge">
                <span class="live-dot"></span>
                LIVE
              </div>

              <!-- 시청자 수 (라이브 상품인 경우) -->
              <div v-if="product.viewers" class="viewers-count">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21M16 7C16 9.20914 14.2091 11 12 11C9.79086 11 8 9.20914 8 7C8 4.79086 9.79086 3 12 3C14.2091 3 16 4.79086 16 7Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                {{ product.viewers }}
              </div>

              <!-- 상품 이미지 -->
              <div class="product-image">
                <img
                    :src="getProductImage(product)"
                    :alt="product.name || '상품명 없음'"
                    @error="handleImageError"
                    @load="handleImageLoad"
                    loading="lazy"
                />
                <!-- 이미지 로딩 실패 시 오버레이 -->
                <div v-if="product.imageError" class="image-error-overlay">
                  <span>이미지 로드 실패</span>
                </div>
              </div>

              <!-- 상품 정보 -->
              <div class="product-info">
                <h3 class="product-title" v-html="highlightSearchKeyword(product.name)"></h3>
                <div class="product-pricing">
                  <span class="price">{{ formatPrice(product.price) }}원</span>
                  <span v-if="product.originalPrice && product.originalPrice !== product.price" class="original-price">
                    {{ formatPrice(product.originalPrice) }}원
                  </span>
                </div>
                <div class="product-meta">
                  <span class="shipping-info">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M16 3H5C3.89543 3 3 3.89543 3 5V19C3 20.1046 3.89543 21 5 21H16M16 3L21 8V21C21 20.1046 20.1046 19 19 19H16M16 3V8H21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    무료배송
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 전체 상품 그리드 섹션 -->
    <div v-if="displayProducts && displayProducts.length > 0" class="products-grid-section">
      <div class="grid-header">
        <h2 class="grid-title">{{ searchKeyword ? '검색 결과' : '전체 상품' }}</h2>
        <div class="grid-controls">
          <div class="sort-controls">
            <label for="sortSelect">정렬:</label>
            <select id="sortSelect" v-model="selectedSort" @change="handleSortChange" class="sort-select">
              <option value="default">기본순</option>
              <option value="price-low">가격 낮은순</option>
              <option value="price-high">가격 높은순</option>
              <option value="discount">할인율순</option>
              <option value="name">상품명순</option>
              <option v-if="searchKeyword" value="relevance">관련도순</option>
            </select>
          </div>
          <div class="products-total">
            총 {{ sortedProducts.length }}개 상품
          </div>
        </div>
      </div>

      <!-- 상품 4열 그리드 -->
      <div class="products-grid">
        <div
            v-for="(product, index) in paginatedProducts"
            :key="product.id || index"
            class="product-card grid"
            @click="goToProductDetail(product)"
        >
          <!-- 라이브 배지 (라이브 상품인 경우) -->
          <div v-if="product.isLive" class="live-badge">
            <span class="live-dot"></span>
            LIVE
          </div>

          <!-- 상품 이미지 -->
          <div class="product-image">
            <img
                :src="getProductImage(product)"
                :alt="product.name || '상품명 없음'"
                @error="handleImageError"
                @load="handleImageLoad"
                loading="lazy"
            />
            <!-- 이미지 로딩 실패 시 오버레이 -->
            <div v-if="product.imageError" class="image-error-overlay">
              <span>이미지 로드 실패</span>
            </div>
          </div>

          <!-- 상품 정보 -->
          <div class="product-info">
            <h3 class="product-title" v-html="highlightSearchKeyword(product.name)"></h3>
            <div class="product-pricing">
              <span class="price">{{ formatPrice(product.price) }}원</span>
              <span v-if="product.originalPrice && product.originalPrice !== product.price" class="original-price">
                {{ formatPrice(product.originalPrice) }}원
              </span>
            </div>
            <div class="product-meta">
              <span class="shipping-info">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M16 3H5C3.89543 3 3 3.89543 3 5V19C3 20.1046 3.89543 21 5 21H16M16 3L21 8V21C21 20.1046 20.1046 19 19 19H16M16 3V8H21" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                무료배송
              </span>
              <span v-if="product.categoryId" class="category-tag">{{ product.categoryId }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 더보기 버튼 -->
      <div v-if="sortedProducts.length > currentPage * itemsPerPage" class="load-more-container">
        <button @click="loadMore" class="load-more-btn">
          더보기 ({{ sortedProducts.length - currentPage * itemsPerPage }}개 상품 더 있음)
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, watch, onMounted, computed} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import apiClient from '@/api/axiosInstance.js'
import { useSmartImages } from '@/composables/useSmartImages'

const router = useRouter()
const route = useRoute()
const { getProductImage, handleImageError, handleImageLoad } = useSmartImages()

// 반응형 상태 변수들
const selectedCategory = ref('ALL')
const selectedSubCategory = ref('')
const selectedFilter = ref('sale')
const selectedSort = ref('default')
const currentPage = ref(1)
const itemsPerPage = ref(12)
const slider = ref(null)
const isDragging = ref(false)
const startX = ref(0)
const scrollLeft = ref(0)
const subCategories = ref([])
const loading = ref(false)

// 검색 관련 변수
const searchKeyword = ref('')
const allProducts = ref([])

// 초기 데이터
const categories = ref([
  {categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0}
])

const filterTabs = ref([
  {id: 'sale', name: '상품'}
])

const products = ref([])

// 아이콘 처리 로직
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
    return `/icons/${iconFile}`;
  }

  return null;
};

// 검색 관련 계산된 속성들
const filteredProducts = computed(() => {
  if (!searchKeyword.value) {
    return products.value
  }

  const keyword = searchKeyword.value.toLowerCase().trim()
  return allProducts.value.filter(product => {
    const name = product.name?.toLowerCase() || ''
    return name.includes(keyword)
  })
})

const displayProducts = computed(() => {
  return searchKeyword.value ? filteredProducts.value : products.value
})

// 검색어 하이라이트 함수
const highlightSearchKeyword = (text) => {
  if (!searchKeyword.value || !text) {
    return text
  }

  const keyword = searchKeyword.value.trim()
  const regex = new RegExp(`(${keyword})`, 'gi')
  return text.replace(regex, '<mark style="background-color: #fff3cd; color: #856404; padding: 2px 4px; border-radius: 3px; font-weight: 600;">$1</mark>')
}

// 검색 해제 함수
const clearSearch = () => {
  searchKeyword.value = ''
  router.push({ path: route.path })
}

// 계산된 속성
const currentCategoryName = computed(() => {
  const category = categories.value.find(cat => cat.categoryId === selectedCategory.value)
  return category ? category.name : selectedCategory.value
})

// 정렬된 상품 목록
const sortedProducts = computed(() => {
  if (!displayProducts.value || displayProducts.value.length === 0) return []

  const sorted = [...displayProducts.value]

  switch (selectedSort.value) {
    case 'price-low':
      return sorted.sort((a, b) => a.price - b.price)
    case 'price-high':
      return sorted.sort((a, b) => b.price - a.price)
    case 'discount':
      return sorted.sort((a, b) => (b.discount || 0) - (a.discount || 0))
    case 'name':
      return sorted.sort((a, b) => a.name.localeCompare(b.name))
    case 'relevance':
      if (searchKeyword.value) {
        return sorted.sort((a, b) => {
          const aName = a.name?.toLowerCase() || ''
          const bName = b.name?.toLowerCase() || ''
          const keyword = searchKeyword.value.toLowerCase()

          const aStartsWith = aName.startsWith(keyword) ? 2 : 0
          const bStartsWith = bName.startsWith(keyword) ? 2 : 0
          const aIncludes = aName.includes(keyword) ? 1 : 0
          const bIncludes = bName.includes(keyword) ? 1 : 0

          return (bStartsWith + bIncludes) - (aStartsWith + aIncludes)
        })
      }
      return sorted
    default:
      return sorted
  }
})

// 페이지네이션된 상품 목록
const paginatedProducts = computed(() => {
  const startIndex = 0
  const endIndex = currentPage.value * itemsPerPage.value
  return sortedProducts.value.slice(startIndex, endIndex)
})

// 🔥 useSmartImages를 사용한 간단한 fetchProducts 함수
const fetchProducts = async () => {
  try {
    loading.value = true

    let requestCategoryId
    if (selectedSubCategory.value && selectedSubCategory.value !== '') {
      requestCategoryId = selectedSubCategory.value
    } else if (selectedCategory.value === 'ALL') {
      requestCategoryId = 'ALL'
    } else {
      requestCategoryId = selectedCategory.value
    }

    const params = {categoryId: requestCategoryId, limit: 20}

    const response = await apiClient.get('/api/products/filter', {
      params: params,
      withAuth: false
    })

    const productData = response.data

    if (!Array.isArray(productData) || productData.length === 0) {
      products.value = []
      allProducts.value = []
      return
    }

    // 🔥 useSmartImages를 사용한 간단한 이미지 처리
    const convertedProducts = productData.map((product, index) => {
      return {
        id: product.productId || `product_${index}`,
        name: product.name || '상품명 없음',
        price: product.price || product.salePrice || 0,
        originalPrice: product.originalPrice || product.price || 0,
        discount: product.discount || product.discountRate || null,
        mainImage: product.mainImage, // 원본 mainImage 유지
        isLive: product.isLive || false,
        viewers: product.viewers || null,
        categoryId: product.categoryId || 'unknown',
        imageError: false
      }
    })

    products.value = convertedProducts
    allProducts.value = convertedProducts

  } catch (error) {
    products.value = []
    allProducts.value = []
  } finally {
    loading.value = false
  }
}

// 메인 카테고리 조회
const fetchMainCategories = async () => {
  try {
    const res = await apiClient.get('/api/categories/main', {withAuth: false})

    if (res.data && res.data.length > 0) {
      const allCategory = {
        categoryId: 'ALL',
        name: '전체',
        icon: null,
        categoryDisplayOrder: 0
      }

      const serverCategories = res.data
          .filter(cat => cat.categoryUseYn === 'Y' && cat.categoryLevel === 1)
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .map(cat => ({
            categoryId: String(cat.categoryId),
            name: cat.name,
            icon: getIconForCategory(cat),
            categoryDisplayOrder: cat.categoryDisplayOrder,
            categoryIcon: cat.categoryIcon,
            iconUrl: cat.iconUrl
          }))

      categories.value = [allCategory, ...serverCategories]
    }
  } catch (error) {
    categories.value = [{categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0}]
  }
}

// 하위 카테고리 조회
const fetchSubCategories = async (parentCategoryId) => {
  try {
    if (parentCategoryId === 'ALL') {
      subCategories.value = []
      return
    }

    const res = await apiClient.get(`/api/categories/${parentCategoryId}/sub`, {withAuth: false})

    subCategories.value = res.data?.length > 0
        ? res.data
            .filter(cat => cat.categoryUseYn === 'Y')
            .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
            .map(cat => ({
              categoryId: cat.categoryId,
              name: cat.name,
              categoryDisplayOrder: cat.categoryDisplayOrder
            }))
        : []
  } catch (error) {
    subCategories.value = []
  }
}

// 정렬 변경 처리
const handleSortChange = () => {
  currentPage.value = 1
}

// 더보기 버튼 처리
const loadMore = () => {
  currentPage.value += 1
}

// 메인 카테고리 선택
const selectCategory = async (categoryId) => {
  const normalizedCategoryId = String(categoryId);

  searchKeyword.value = ''
  selectedCategory.value = normalizedCategoryId
  selectedSubCategory.value = ''
  currentPage.value = 1
  selectedSort.value = 'default'

  try {
    await fetchSubCategories(normalizedCategoryId)
    await fetchProducts()
  } catch (error) {
    console.error('카테고리 선택 실패:', error)
  }

  if (normalizedCategoryId === 'ALL') {
    router.push('/category/')
  } else {
    router.push(`/category/${normalizedCategoryId}`)
  }
}

// 서브 카테고리 선택
const selectSubCategory = async (subCategoryId) => {
  if (selectedSubCategory.value === subCategoryId) {
    return
  }

  selectedSubCategory.value = subCategoryId
  currentPage.value = 1

  await fetchProducts()
}

// 필터 선택
const selectFilter = async (filterId) => {
  selectedFilter.value = filterId
  currentPage.value = 1
  await fetchProducts()
}

// 상품 상세 페이지로 이동
const goToProductDetail = (product) => {
  router.push(`/product/${product.id}`)
}

// 가격 포맷팅
const formatPrice = (price) => {
  return price?.toLocaleString() || '0'
}

// 드래그 스크롤 기능
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

// Passive 이벤트 핸들러들
const handleWheelPassive = (e) => {
  const scrollAmount = e.deltaY * 0.5
  slider.value.scrollLeft += scrollAmount
}

const startTouchPassive = (e) => {
  isDragging.value = true
  startX.value = e.touches[0].pageX
  scrollLeft.value = slider.value.scrollLeft
}

const touchMovePassive = (e) => {
  if (!isDragging.value) return
  const x = e.touches[0].pageX
  const walk = (x - startX.value) * 1.5
  slider.value.scrollLeft = scrollLeft.value - walk
}

const endTouch = () => {
  isDragging.value = false
}

// 라이프사이클 훅

// URL 쿼리 파라미터 변화 감지 (검색어 처리)
watch(() => route.query.search, (newSearch) => {
  if (newSearch && newSearch !== searchKeyword.value) {
    searchKeyword.value = newSearch
    currentPage.value = 1
    selectedSort.value = 'relevance'
  } else if (!newSearch && searchKeyword.value) {
    searchKeyword.value = ''
  }
}, { immediate: true })

// URL 파라미터 변화 감지 (카테고리)
watch(() => route.params, async (newParams, oldParams) => {
  let needsProductRefresh = false
  let needsSubCategoryRefresh = false

  if (newParams.categoryId && newParams.categoryId !== selectedCategory.value) {
    searchKeyword.value = ''

    selectedCategory.value = newParams.categoryId
    selectedSubCategory.value = ''
    needsSubCategoryRefresh = true
    needsProductRefresh = true
  }

  if (needsSubCategoryRefresh) {
    await fetchSubCategories(selectedCategory.value)
  }

  if (needsProductRefresh) {
    await fetchProducts()
  }
}, {immediate: false})

// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  try {
    await fetchMainCategories()

    if (route.query.search) {
      searchKeyword.value = route.query.search
    }

    if (!searchKeyword.value && route.params.categoryId) {
      selectedCategory.value = route.params.categoryId
      await fetchSubCategories(route.params.categoryId)

      if (route.params.subCategoryId) {
        selectedSubCategory.value = route.params.subCategoryId
      }
    }

    await fetchProducts()

  } catch (error) {
    console.error('컴포넌트 초기화 실패:', error)
  }
})
</script>

<style scoped src="@/assets/css/category.css"></style>