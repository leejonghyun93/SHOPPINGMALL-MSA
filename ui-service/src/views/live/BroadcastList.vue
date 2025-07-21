<template>
  <div class="main-container">
    <!-- 메인 카테고리 섹션 (동그란 아이콘들) -->
    <div class="category-container">
      <div class="category-list">
        <div
            v-for="category in categories"
            :key="category.categoryId"
            class="category-item"
            :class="{ active: String(selectedCategory) === String(category.categoryId) }"
            @click="selectCategory(category.categoryId)"
        >
          <div class="category-icon">
            <!--  아이콘 표시 로직 수정 - 카테고리 페이지와 동일하게 -->
            <img v-if="category.icon" :src="category.icon" :alt="category.name" class="icon-image" />
            <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M9 11H15M9 15H15M17 21H7C5.89543 21 5 20.1046 5 19V5C5 3.89543 5.89543 3 7 3H12.5858C12.851 3 13.1054 3.10536 13.2929 3.29289L19.7071 9.70711C19.8946 9.89464 20 10.149 20 10.4142V19C20 20.1046 19.1046 21 18 21H17Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
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

    <!-- 라이브 방송 목록 섹션 -->
    <div class="live-broadcast-container">
      <div class="section-header">
        <h2 class="section-title">
          {{ selectedCategoryName }} 라이브 방송
        </h2>
        <div class="live-count">
          <span class="count-badge">{{ allBroadcasts.length }}개 방송 진행중</span>
        </div>
      </div>

      <!-- 로딩 상태 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>방송 목록을 불러오는 중...</p>
      </div>

      <!-- 에러 상태 -->
      <div v-else-if="error" class="error-container">
        <div class="error-icon"><i class="fas fa-exclamation-triangle"></i></div>
        <h3>방송 목록을 불러오는데 실패했습니다</h3>
        <p>{{ error }}</p>
        <button @click="retryFetch" class="retry-button">다시 시도</button>
      </div>

      <!-- 라이브 방송 카드 리스트 (가로 스크롤) -->
      <div v-else-if="allBroadcasts.length > 0" class="broadcast-scroll-container">
        <div class="broadcast-list">
          <div
              v-for="broadcast in allBroadcasts"
              :key="broadcast.broadcast_id || broadcast.broadcastId"
              class="broadcast-card"
              @click="goToBroadcast(broadcast)"
          >
            <!-- 방송 썸네일 -->
            <div class="broadcast-thumbnail">
              <img
                  :src="getBroadcastThumbnail(broadcast)"
                  :alt="broadcast.title"
                  class="thumbnail-image"
                  @error="handleImageError"
              />

              <!-- 라이브 배지 -->
              <div class="live-badge">
                <span class="live-dot"></span>
                {{ getBroadcastStatusText(broadcast.broadcast_status) }}
              </div>

              <!-- 시청자 수 -->
              <div class="viewer-count">
                <i class="fas fa-users viewer-icon"></i>
                {{ formatViewerCount(broadcast.current_viewers) }}
              </div>

              <!-- 방송 시간 -->
<!--              <div class="broadcast-time">-->
<!--                {{ getBroadcastDuration(broadcast.actual_start_time) }}-->
<!--              </div>-->
            </div>

            <!-- 방송 정보 -->
            <div class="broadcast-info">
              <h3 class="broadcast-title">{{ broadcast.title }}</h3>
              <p class="broadcast-description">{{ broadcast.description || '방송 설명이 없습니다.' }}</p>

              <!-- 방송자 정보 -->
              <div class="broadcaster-info">
                <span class="broadcaster-name">{{ broadcast.broadcaster_name || '방송자' }}</span>
              </div>

              <!-- 카테고리 태그 -->
              <div class="broadcast-tags">
<!--                <span class="category-tag">{{ broadcast.category_name || '일반' }}</span>-->
<!--                <span v-if="broadcast.tags" class="tags">-->
<!--            {{ formatTags(broadcast.tags) }}-->
<!--          </span>-->
              </div>

              <!-- 좋아요 수 -->
              <div class="broadcast-stats">
          <span class="like-count">
            <i class="fas fa-heart heart-icon"></i>
            {{ broadcast.like_count || 0 }}
          </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 방송이 없을 때 -->
      <div v-else class="no-broadcasts">
        <div class="no-broadcast-icon"><i class="fas fa-tv"></i></div>
        <h3>{{ selectedCategoryName }} 카테고리에 진행 중인 라이브 방송이 없습니다</h3>
        <p>다른 카테고리를 선택하거나 잠시 후 다시 확인해주세요!</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import apiClient from '@/api/axiosInstance.js'
import { useSmartImages } from '@/composables/useSmartImages'

const router = useRouter()
const route = useRoute()
const { getProductImage, handleImageError, handleImageLoad } = useSmartImages()

// 상태 관리
const selectedCategory = ref('ALL')
const selectedSubCategory = ref('')
const subCategories = ref([])
const allBroadcasts = ref([])
const loading = ref(false)
const error = ref(null)
const refreshInterval = ref(null)

// 초기 카테고리 데이터
const categories = ref([
  { categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0 }
])

// 계산된 속성
const selectedCategoryName = computed(() => {
  if (selectedSubCategory.value) {
    const subCategory = subCategories.value.find(cat => cat.categoryId === selectedSubCategory.value)
    if (subCategory) return subCategory.name
  }

  const category = categories.value.find(cat => String(cat.categoryId) === String(selectedCategory.value))
  return category ? category.name : '전체'
})

//  아이콘 처리 함수 추가 (카테고리 페이지와 동일)
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

/**
 * 메인 카테고리 조회
 */
const fetchMainCategories = async () => {
  try {
    const res = await apiClient.get('/api/categories/main', { withAuth: false })

    if (res.data && res.data.length > 0) {
      const allCategory = {
        categoryId: 'ALL',
        name: '전체',
        icon: null, //  아이콘 필드 추가
        categoryDisplayOrder: 0
      }

      const serverCategories = res.data
          .filter(cat => cat.categoryUseYn === 'Y' && cat.categoryLevel === 1)
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .map(cat => ({
            categoryId: String(cat.categoryId),
            name: cat.name,
            icon: getIconForCategory(cat), //  아이콘 처리 함수 사용
            categoryDisplayOrder: cat.categoryDisplayOrder,
            categoryIcon: cat.categoryIcon,
            iconUrl: cat.iconUrl
          }))

      categories.value = [allCategory, ...serverCategories]
    }
  } catch (error) {
    categories.value = [{
      categoryId: 'ALL',
      name: '전체',
      icon: null,
      categoryDisplayOrder: 0
    }]
  }
}

/**
 * 방송-상품 카테고리 연동으로 라이브 방송 조회
 */
const fetchLiveBroadcastsByCategory = async (categoryId) => {
  try {
    loading.value = true
    error.value = null

    console.log(`방송 조회 시작 - categoryId: ${categoryId}, selectedSubCategory: ${selectedSubCategory.value}`)

    // 실제 요청할 카테고리 ID 결정
    const targetCategoryId = selectedSubCategory.value && selectedSubCategory.value !== ''
        ? selectedSubCategory.value
        : categoryId

    const params = {
      broadcast_status: 'live',
      is_public: 1,
      limit: 100
    }

    // 전체가 아닌 경우에만 category_id 파라미터 추가
    if (targetCategoryId !== 'ALL') {
      params.category_id = targetCategoryId
    }

    console.log('API 요청 파라미터:', params)

    const response = await apiClient.get('/api/broadcasts/live', {
      params,
      withAuth: false
    })

    if (response.data && Array.isArray(response.data)) {
      // 백엔드에서 이미 정확한 필터링이 된 데이터를 받으므로
      // 클라이언트에서 추가 필터링 불필요
      allBroadcasts.value = response.data.map(broadcast => ({
        broadcast_id: broadcast.broadcast_id || broadcast.broadcastId,
        broadcaster_id: broadcast.broadcaster_id || broadcast.broadcasterId,
        broadcaster_name: broadcast.broadcaster_name || broadcast.broadcasterName || '방송자',
        title: broadcast.title || '제목 없음',
        description: broadcast.description,
        broadcast_status: broadcast.broadcast_status || broadcast.broadcastStatus,
        actual_start_time: broadcast.actual_start_time || broadcast.actualStartTime,
        current_viewers: broadcast.current_viewers || broadcast.currentViewers || 0,
        like_count: broadcast.like_count || broadcast.likeCount || 0,
        category_id: broadcast.category_id || broadcast.categoryId,
        category_name: broadcast.category_name || broadcast.categoryName,
        tags: broadcast.tags,
        thumbnail_url: broadcast.thumbnail_url || broadcast.thumbnailUrl,
        stream_url: broadcast.stream_url || broadcast.streamUrl,
        scheduled_start_time: broadcast.scheduled_start_time || broadcast.scheduledStartTime,
        scheduled_end_time: broadcast.scheduled_end_time || broadcast.scheduledEndTime,
        total_viewers: broadcast.total_viewers || broadcast.totalViewers || 0,
        peak_viewers: broadcast.peak_viewers || broadcast.peakViewers || 0,
        products: broadcast.products || broadcast.broadcast_products || []
      }))

      console.log(`방송 조회 완료 - categoryId: ${targetCategoryId}, 결과 수: ${allBroadcasts.value.length}`)

      // 디버깅을 위한 로그
      allBroadcasts.value.forEach(broadcast => {
        console.log(`방송: ${broadcast.title}, 카테고리: ${broadcast.category_name}, 상품 수: ${broadcast.products.length}`)
      })

    } else {
      allBroadcasts.value = []
    }

  } catch (err) {
    console.error('방송 목록 조회 실패:', err)
    error.value = err.response?.data?.message || '방송 목록을 불러오는데 실패했습니다'
    allBroadcasts.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 하위 카테고리 조회
 */
const fetchSubCategories = async (parentCategoryId) => {
  try {
    if (parentCategoryId === 'ALL') {
      subCategories.value = []
      return
    }

    const res = await apiClient.get(`/api/categories/${parentCategoryId}/sub`, { withAuth: false })

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

/**
 * 메인 카테고리 선택
 */
const selectCategory = async (categoryId) => {
  const normalizedCategoryId = String(categoryId)

  if (String(selectedCategory.value) === normalizedCategoryId) return

  console.log(`카테고리 선택: ${normalizedCategoryId}`)

  selectedCategory.value = normalizedCategoryId
  selectedSubCategory.value = '' // 서브카테고리 초기화

  // 하위 카테고리 조회
  await fetchSubCategories(normalizedCategoryId)

  // 선택된 카테고리의 방송 목록 조회
  await fetchLiveBroadcastsByCategory(normalizedCategoryId)

  // 라우터 이동
  if (normalizedCategoryId === 'ALL') {
    router.push('/broadcasts/category/')
  } else {
    router.push(`/broadcasts/category/${normalizedCategoryId}`)
  }
}

/**
 * 서브 카테고리 선택
 */
const selectSubCategory = async (subCategoryId) => {
  if (selectedSubCategory.value === subCategoryId) {
    return
  }

  console.log(`서브카테고리 선택: ${subCategoryId}`)

  selectedSubCategory.value = subCategoryId

  // 선택된 서브 카테고리로 방송 목록 다시 조회
  // subCategoryId가 ''(전체)이면 메인 카테고리로, 아니면 서브 카테고리로 조회
  const targetCategoryId = subCategoryId || selectedCategory.value
  await fetchLiveBroadcastsByCategory(targetCategoryId)
}
// 유틸리티 함수들

/**
 * 방송 상태 텍스트 반환
 */
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

//  방송 썸네일 이미지 처리 (useSmartImages 사용)
//  방송 썸네일 이미지 처리 (useSmartImages 활용 + Home.vue 로직 결합)
const getBroadcastThumbnail = (broadcast) => {
  console.log('방송 썸네일 요청:', broadcast.title, broadcast.thumbnail_url || broadcast.thumbnailUrl)

  const thumbnailUrl = broadcast.thumbnail_url || broadcast.thumbnailUrl

  // 1. 썸네일이 있는 경우 - useSmartImages와 동일한 로직
  if (thumbnailUrl && thumbnailUrl.trim() !== '') {
    // DB 경로인 경우
    if (thumbnailUrl.startsWith('/upload/product/main/')) {
      const fileName = thumbnailUrl.split('/').pop()
      const finalUrl = `/images/banners/products/${fileName}`
      console.log('✅ UI Service 폴더 썸네일:', finalUrl)
      return finalUrl
    }

    // 외부 URL인 경우
    if (thumbnailUrl.startsWith('http')) {
      console.log('✅ 외부 썸네일 URL:', thumbnailUrl)
      return thumbnailUrl
    }

    // 파일명만 있는 경우
    if (!thumbnailUrl.includes('/')) {
      const finalUrl = `/images/banners/products/${thumbnailUrl}`
      console.log('✅ 파일명 썸네일:', finalUrl)
      return finalUrl
    }
  }

  // 2. 방송에 연결된 상품 이미지 활용 (BroadcastList의 경우)
  if (broadcast.products && broadcast.products.length > 0) {
    const firstProduct = broadcast.products[0]
    // useSmartImages의 getProductImage 활용
    const productImage = getProductImage(firstProduct)
    console.log('✅ 방송 상품 이미지 사용:', productImage)
    return productImage
  }

  // 3. 최종 기본 이미지
  const defaultImage = '/images/banners/products/default-product.jpg'
  console.log('⚠️ 기본 썸네일 사용:', defaultImage)
  return defaultImage
}

/**
 * 방송자 아바타 생성 (useSmartImages 활용)
 */
const getBroadcasterAvatar = (broadcasterId) => {
  console.log('방송자 아바타 요청:', broadcasterId)

  // useSmartImages를 활용하되, 아바타용으로 수정
  const avatarForImage = {
    id: broadcasterId || 'default',
    name: `방송자${broadcasterId || 'Unknown'}`,
    categoryId: 1, // 기본 카테고리
    mainImage: null,
    image: null
  }

  const smartAvatar = getProductImage(avatarForImage)

  // 기본 이미지인 경우 방송자 전용 picsum 이미지로 대체
  if (smartAvatar === '/images/banners/products/default-product.jpg') {
    const avatarId = broadcasterId || 'default'
    const avatarImage = `https://picsum.photos/seed/avatar-${avatarId}/64/64`
    console.log('방송자 아바타 picsum 생성:', avatarImage)
    return avatarImage
  }

  console.log('방송자 아바타 useSmartImages 결과:', smartAvatar)
  return smartAvatar
}
/**
 * 태그 포맷팅
 */
const formatTags = (tags) => {
  if (!tags) return ''
  return tags.split(',').slice(0, 2).join(', ')
}

/**
 * 시청자 수 포맷팅
 */
const formatViewerCount = (count) => {
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return count?.toString() || '0'
}

/**
 * 방송 진행 시간 계산
 */
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

/**
 * 방송 페이지로 이동
 */
const goToBroadcast = (broadcast) => {
  // 백엔드에서 camelCase로 오는 경우와 snake_case 모두 지원
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

/**
 * 에러 발생시 재시도
 */
const retryFetch = async () => {
  const targetCategoryId = selectedSubCategory.value || selectedCategory.value
  await fetchLiveBroadcastsByCategory(targetCategoryId)
}

/**
 * 자동 새로고침 설정 (30초마다)
 */
const startAutoRefresh = () => {
  refreshInterval.value = setInterval(async () => {
    const targetCategoryId = selectedSubCategory.value || selectedCategory.value
    await fetchLiveBroadcastsByCategory(targetCategoryId)
  }, 30000) // 30초
}

/**
 * 자동 새로고침 중지
 */
const stopAutoRefresh = () => {
  if (refreshInterval.value) {
    clearInterval(refreshInterval.value)
    refreshInterval.value = null
  }
}

/**
 * URL 파라미터 변화 감지 (카테고리)
 */
watch(() => route.params, async (newParams) => {
  if (newParams.categoryId && String(newParams.categoryId) !== String(selectedCategory.value)) {
    selectedCategory.value = String(newParams.categoryId)
    selectedSubCategory.value = ''

    await fetchSubCategories(selectedCategory.value)
    await fetchLiveBroadcastsByCategory(selectedCategory.value)
  }
}, { immediate: false })

// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  // 1. 카테고리 로드
  await fetchMainCategories()

  // 2. URL 파라미터에서 카테고리 설정
  if (route.params.categoryId) {
    selectedCategory.value = String(route.params.categoryId)
    await fetchSubCategories(selectedCategory.value)
  }

  // 3. 선택된 카테고리의 방송 데이터 로드
  await fetchLiveBroadcastsByCategory(selectedCategory.value)

  // 4. 자동 새로고침 시작
  startAutoRefresh()
})

// 컴포넌트 언마운트 시 정리
onUnmounted(() => {
  stopAutoRefresh()
})
</script>
<style scoped src="@/assets/css/boardcastList.css"></style>