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

    <!-- 🎥 라이브 방송 목록 섹션 -->
    <div class="live-broadcast-container">
      <div class="section-header">
        <h2 class="section-title">
          🔴 {{ selectedCategoryName }} 라이브 방송
        </h2>
        <div class="live-count">
          <span class="count-badge">{{ liveBroadcasts.length }}개 방송 진행중</span>
        </div>
      </div>

      <!-- 로딩 상태 -->
      <div v-if="loading" class="loading-container">
        <div class="loading-spinner"></div>
        <p>방송 목록을 불러오는 중...</p>
      </div>

      <!-- 라이브 방송 카드 리스트 (가로 스크롤) -->
      <div v-else-if="liveBroadcasts.length > 0" class="broadcast-scroll-container">
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
                <span class="broadcaster-name">{{ broadcast.broadcaster_name || '방송자' }}</span>
              </div>

              <!-- 카테고리 태그 -->
              <div class="broadcast-tags">
                <span class="category-tag">{{ broadcast.category_name }}</span>
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
      </div>

      <!-- 방송이 없을 때 -->
      <div v-else class="no-broadcasts">
        <div class="no-broadcast-icon">📺</div>
        <h3>{{ selectedCategoryName }} 카테고리에 진행 중인 라이브 방송이 없습니다</h3>
        <p>다른 카테고리를 선택하거나 잠시 후 다시 확인해주세요!</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import apiClient from '@/api/axiosInstance.js'

const router = useRouter()

// 상태 관리
const selectedCategory = ref('ALL')
const selectedSubCategory = ref('')
const subCategories = ref([])
const liveBroadcasts = ref([])
const loading = ref(false)

// 초기 카테고리 데이터
const categories = ref([
  { categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0 }
])

// 계산된 속성
const selectedCategoryName = computed(() => {
  const category = categories.value.find(cat => cat.categoryId === selectedCategory.value)
  return category ? category.name : '전체'
})

// 카테고리 아이콘 매핑
const getIconForCategory = (categoryId) => {
  const iconMap = {
    '1': '🥬', '2': '🥫', '3': '🍱',
    '4': '🍞', '5': '🥛', '6': '💊',
    '7': '🍳', '8': '🧻', '9': '🍼'
  }
  return iconMap[categoryId] || '📦'
}

/**
 * 목업 방송 데이터 생성
 */
const generateMockBroadcasts = (categoryId) => {
  const categoryNames = {
    'ALL': '전체',
    '1': '신선식품',
    '2': '가공식품',
    '3': '간편식',
    '4': '베이커리',
    '5': '유제품',
    '6': '건강식품',
    '7': '생활용품',
    '8': '화장품',
    '9': '유아용품'
  }

  const currentCategoryName = categoryNames[categoryId] || '카테고리'

  const mockTitles = [
    `🔥 ${currentCategoryName} 특가 라이브!`,
    `${currentCategoryName} 신상품 소개방송`,
    `오늘만! ${currentCategoryName} 할인쇼`,
    `${currentCategoryName} 베스트 상품 추천`,
    `실시간 ${currentCategoryName} 쇼핑`,
    `${currentCategoryName} 인기템 모음전`,
    `깜짝! ${currentCategoryName} 타임세일`,
    `${currentCategoryName} 브랜드데이 특집`
  ]

  const mockDescriptions = [
    '지금 바로 주문하면 특별 할인 혜택을 받을 수 있어요!',
    '신상품 런칭 기념 특가 이벤트 진행중입니다',
    '오늘 하루만 진행되는 깜짝 할인 이벤트입니다',
    '인기 상품들을 모아서 소개해드려요',
    '실시간으로 질문 받고 답변해드립니다',
    '베스트셀러 상품들의 특별한 혜택',
    '한정 수량! 놓치면 후회하는 특가',
    '브랜드별 인기 상품 총집합'
  ]

  const broadcasterNames = [
    '김쇼핑', '이라이브', '박특가', '최할인', '정세일', '홍브랜드', '윤딜러', '장마켓'
  ]

  // 카테고리별로 다른 수의 방송 생성
  const broadcastCount = categoryId === 'ALL' ? 8 : Math.floor(Math.random() * 6) + 2

  return Array.from({ length: broadcastCount }, (_, index) => ({
    broadcast_id: `${categoryId}_${index + 1}`,
    broadcaster_id: index + 1,
    broadcaster_name: broadcasterNames[index % broadcasterNames.length],
    title: mockTitles[index % mockTitles.length],
    description: mockDescriptions[index % mockDescriptions.length],
    broadcast_status: 'live',
    actual_start_time: new Date(Date.now() - Math.random() * 3600000).toISOString(), // 최근 1시간 내
    current_viewers: Math.floor(Math.random() * 2000) + 50,
    like_count: Math.floor(Math.random() * 500) + 10,
    category_name: currentCategoryName,
    tags: `${currentCategoryName},할인,특가,라이브`,
    thumbnail_url: null // picsum 이미지 사용
  }))
}

/**
 * 카테고리별 라이브 방송 조회 (목업 데이터 사용)
 */
const fetchLiveBroadcasts = async (categoryId) => {
  try {
    loading.value = true

    // 🚧 임시: 목업 데이터 사용 (실제 방송 서비스 구현 전까지)
    console.log('🎭 목업 방송 데이터 로딩 중...')

    // 잠시 로딩 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 800))

    let requestCategoryId = categoryId
    if (selectedSubCategory.value && selectedSubCategory.value !== '') {
      requestCategoryId = selectedSubCategory.value
    }

    // 목업 방송 데이터 생성
    const mockBroadcasts = generateMockBroadcasts(requestCategoryId)
    liveBroadcasts.value = mockBroadcasts

    console.log(`🎬 ${selectedCategoryName.value} 카테고리 라이브 방송:`, liveBroadcasts.value.length, '개 (목업)')

    /* 🔥 실제 API 호출 (방송 서비스 구현 후 사용)
    const params = {
      category_id: requestCategoryId === 'ALL' ? null : requestCategoryId,
      broadcast_status: 'live',
      limit: 20
    }

    const response = await apiClient.get('/api/broadcasts/live', {
      params: params,
      withAuth: false
    })

    const broadcastData = response.data
    if (!Array.isArray(broadcastData)) {
      liveBroadcasts.value = []
      return
    }

    liveBroadcasts.value = broadcastData.map((broadcast, index) => ({
      broadcast_id: broadcast.broadcast_id || broadcast.broadcastId,
      broadcaster_id: broadcast.broadcaster_id || broadcast.broadcasterId,
      broadcaster_name: broadcast.broadcaster_name || broadcast.broadcasterName || '방송자',
      title: broadcast.title || '제목 없음',
      description: broadcast.description || '',
      broadcast_status: broadcast.broadcast_status || broadcast.broadcastStatus,
      actual_start_time: broadcast.actual_start_time || broadcast.actualStartTime,
      current_viewers: broadcast.current_viewers || broadcast.currentViewers || 0,
      like_count: broadcast.like_count || broadcast.likeCount || 0,
      category_name: broadcast.category_name || broadcast.categoryName || '카테고리',
      tags: broadcast.tags || '',
      thumbnail_url: broadcast.thumbnail_url || broadcast.thumbnailUrl
    }))
    */

  } catch (error) {
    console.error('❌ 라이브 방송 조회 실패:', error)
    liveBroadcasts.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 메인 카테고리 조회
 */
const fetchMainCategories = async () => {
  try {
    const res = await apiClient.get('/api/categories/main', { withAuth: false })

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
    console.error('카테고리 조회 실패:', error)
    categories.value = [{ categoryId: 'ALL', name: '전체', icon: null, categoryDisplayOrder: 0 }]
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
    console.error('하위 카테고리 조회 실패:', error)
    subCategories.value = []
  }
}

/**
 * 메인 카테고리 선택
 */
const selectCategory = async (categoryId) => {
  if (selectedCategory.value === categoryId) return

  selectedCategory.value = categoryId
  selectedSubCategory.value = ''

  await fetchSubCategories(categoryId)
  await fetchLiveBroadcasts(categoryId)  // 방송 목록 조회 추가

  console.log('📂 카테고리 선택:', categoryId)

  // 필요시 라우터 이동
  if (categoryId === 'ALL') {
    router.push('/broadcasts/category/')
  } else {
    router.push(`/broadcasts/category/${categoryId}`)
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
  await fetchLiveBroadcasts(selectedCategory.value)  // 방송 목록 재조회
  console.log('📂 서브 카테고리 선택:', subCategoryId)
}

// 유틸리티 함수들

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
const goToBroadcast = (broadcastId) => {
  console.log('🎬 라이브 방송 시청 페이지로 이동:', broadcastId)

  // 실제 라이브 방송 시청 페이지로 라우팅
  router.push({
    name: 'LiveBroadcastViewer',
    params: { broadcastId: broadcastId }
  })

  // 또는 path를 직접 사용할 수도 있음
  // router.push(`/live/${broadcastId}`)
}
const goToBroadcastNewTab = (broadcastId) => {
  const routeData = router.resolve({
    name: 'LiveBroadcastViewer',
    params: { broadcastId: broadcastId }
  })
  window.open(routeData.href, '_blank')
}
// 컴포넌트 마운트 시 초기화
onMounted(async () => {
  console.log('🚀 라이브 방송 목록 페이지 로딩...')
  await fetchMainCategories()
  await fetchLiveBroadcasts('ALL')  // 초기 전체 방송 목록 로드
})
</script>

<style scoped>
/* 기본 컨테이너 스타일 */
.main-container {
  width: 100%;
  background: #f8f9fa;
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

/* 라이브 방송 컨테이너 */
.live-broadcast-container {
  width: 100%;
  padding: 40px 20px;
  background: #f8f9fa;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.section-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin: 0;
}

.live-count {
  display: flex;
  align-items: center;
}

.count-badge {
  background: #ff4757;
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

/* 로딩 상태 */
.loading-container {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #f44336;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 방송 카드 가로 스크롤 */
.broadcast-scroll-container {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.broadcast-list {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  padding: 10px 0;
  scroll-behavior: smooth;
}

.broadcast-list::-webkit-scrollbar {
  height: 8px;
}

.broadcast-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.broadcast-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.broadcast-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.broadcast-card {
  flex: 0 0 280px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
}

.broadcast-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

/* 썸네일 영역 */
.broadcast-thumbnail {
  position: relative;
  width: 100%;
  height: 160px;
  overflow: hidden;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.broadcast-card:hover .thumbnail-image {
  transform: scale(1.05);
}

.live-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: #ff4757;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 4px;
}

.live-dot {
  width: 6px;
  height: 6px;
  background: white;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.viewer-count {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.broadcast-time {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

/* 방송 정보 영역 */
.broadcast-info {
  padding: 16px;
}

.broadcast-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.broadcast-description {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.broadcaster-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.broadcaster-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.broadcaster-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.broadcast-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.category-tag {
  background: #007bff;
  color: white;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.tags {
  background: #e9ecef;
  color: #666;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.broadcast-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.like-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #666;
}

/* 방송이 없을 때 */
.no-broadcasts {
  text-align: center;
  padding: 60px 20px;
  color: #666;
}

.no-broadcast-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.no-broadcasts h3 {
  font-size: 18px;
  margin: 0 0 8px 0;
}

.no-broadcasts p {
  font-size: 14px;
  margin: 0;
}

/* 반응형 */
@media (max-width: 768px) {
  .category-container {
    padding: 15px;
  }

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

  .live-broadcast-container {
    padding: 20px 10px;
  }

  .broadcast-card {
    flex: 0 0 250px;
  }

  .section-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .section-title {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .category-container {
    padding: 10px;
  }

  .category-list {
    gap: 15px;
  }

  .category-icon {
    width: 45px;
    height: 45px;
  }

  .icon-content {
    font-size: 18px;
  }

  .category-name {
    font-size: 11px;
  }

  .broadcast-card {
    flex: 0 0 220px;
  }

  .broadcast-list {
    gap: 15px;
  }
}
</style>