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
            <!-- 🔥 아이콘 표시 로직 수정 -->
            <img v-if="category.iconUrl" :src="category.iconUrl" :alt="category.name" class="icon-image" />
            <i v-else-if="category.icon" :class="category.icon" class="icon-image"></i>
            <i v-else class="fas fa-th-large all-icon"></i>
          </div>
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- 나머지 템플릿 코드는 동일... -->
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
                  :src="broadcast.thumbnail_url || getDefaultThumbnail(broadcast.broadcast_id || broadcast.broadcastId)"
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
              <div class="broadcast-time">
                {{ getBroadcastDuration(broadcast.actual_start_time) }}
              </div>
            </div>

            <!-- 방송 정보 -->
            <div class="broadcast-info">
              <h3 class="broadcast-title">{{ broadcast.title }}</h3>
              <p class="broadcast-description">{{ broadcast.description || '방송 설명이 없습니다.' }}</p>

              <!-- 방송자 정보 -->
              <div class="broadcaster-info">
                <div class="broadcaster-avatar">
                  <img
                      :src="getBroadcasterAvatar(broadcast.broadcaster_id)"
                      :alt="broadcast.broadcaster_name"
                      class="avatar-image"
                      @error="handleAvatarError"
                  />
                </div>
                <span class="broadcaster-name">{{ broadcast.broadcaster_name || '방송자' }}</span>
              </div>

              <!-- 카테고리 태그 -->
              <div class="broadcast-tags">
                <span class="category-tag">{{ broadcast.category_name || '일반' }}</span>
                <span v-if="broadcast.tags" class="tags">
            {{ formatTags(broadcast.tags) }}
          </span>
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

const router = useRouter()
const route = useRoute()

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
  { categoryId: 'ALL', name: '전체', categoryDisplayOrder: 0 }
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
        categoryDisplayOrder: 0
      }

      const serverCategories = res.data
          .filter(cat => cat.categoryUseYn === 'Y' && cat.categoryLevel === 1)
          .sort((a, b) => a.categoryDisplayOrder - b.categoryDisplayOrder)
          .map(cat => ({
            categoryId: String(cat.categoryId),
            name: cat.name,
            categoryDisplayOrder: cat.categoryDisplayOrder
          }))

      categories.value = [allCategory, ...serverCategories]
    }
  } catch (error) {
    categories.value = [{
      categoryId: 'ALL',
      name: '전체',
      categoryDisplayOrder: 0
    }]
  }
}

/**
 * 상품 카테고리 기준으로 라이브 방송 조회
 */
const fetchLiveBroadcastsByCategory = async (categoryId) => {
  try {
    loading.value = true
    error.value = null

    const params = {
      broadcast_status: 'live',
      is_public: 1,
      limit: 100
    }

    if (categoryId !== 'ALL') {
      params.category_id = selectedSubCategory.value || categoryId
    }

    const response = await apiClient.get('/api/broadcasts/live', {
      params,
      withAuth: false
    })

    if (response.data && Array.isArray(response.data)) {
      allBroadcasts.value = response.data.map(broadcast => ({
        broadcast_id: broadcast.broadcastId,
        broadcaster_id: broadcast.broadcasterId,
        broadcaster_name: broadcast.broadcasterName || '방송자',
        title: broadcast.title || '제목 없음',
        description: broadcast.description,
        broadcast_status: broadcast.broadcastStatus,
        actual_start_time: broadcast.actualStartTime,
        current_viewers: broadcast.currentViewers || 0,
        like_count: broadcast.likeCount || 0,
        category_id: broadcast.categoryId,
        category_name: broadcast.categoryName,
        tags: broadcast.tags,
        thumbnail_url: broadcast.thumbnailUrl,
        stream_url: broadcast.streamUrl,
        scheduled_start_time: broadcast.scheduledStartTime,
        scheduled_end_time: broadcast.scheduledEndTime,
        total_viewers: broadcast.totalViewers || 0,
        peak_viewers: broadcast.peakViewers || 0
      }))
    } else {
      allBroadcasts.value = []
    }

  } catch (err) {
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

  selectedCategory.value = normalizedCategoryId
  selectedSubCategory.value = ''

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

  selectedSubCategory.value = subCategoryId

  // 선택된 서브 카테고리로 방송 목록 다시 조회
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

/**
 * 기본 썸네일 생성
 */
const getDefaultThumbnail = (broadcastId) => {
  return `https://picsum.photos/seed/${broadcastId}/300/200`
}

/**
 * 방송자 아바타 생성
 */
const getBroadcasterAvatar = (broadcasterId) => {
  return `https://picsum.photos/seed/user${broadcasterId}/40/40`
}

/**
 * 이미지 에러 처리
 */
const handleImageError = (event) => {
  // event.target.src = '/default-thumbnail.jpg'
}

/**
 * 아바타 이미지 에러 처리
 */
const handleAvatarError = (event) => {
  event.target.src = '/default-avatar.jpg'
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