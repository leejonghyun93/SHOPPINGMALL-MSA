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
          <!-- ✅ 올바른 방법 -->
          <span class="icon-content" v-if="category.icon">{{ category.icon }}</span>
          <span v-else class="all-icon">전체</span>
        </div>
        <span class="category-name">{{ category.name }}</span>
      </div>
    </div>
  </div>

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
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

// 아이콘 컴포넌트 (실제 프로젝트에서는 라이브러리 사용)
const ChevronLeftIcon = () => '<'
const ChevronRightIcon = () => '>'

// 상태 선언
const currentIndex = ref(0)
const isAutoPlay = ref(true)
let autoPlayInterval = null
let hasMounted = false

// 이미지 배열
const images = ref([
  { src: "https://picsum.photos/seed/1/800/400", alt: "특별 이벤트", title: "6/13 라이브" },
  { src: "https://picsum.photos/seed/2/800/400", alt: "신제품 출시", title: "다우니 신제품" },
  { src: "https://picsum.photos/seed/3/800/400", alt: "무료배송", title: "무료배송 이벤트" },
  { src: "https://picsum.photos/seed/4/800/400", alt: "회원 혜택", title: "회원 전용 할인" },
  { src: "https://picsum.photos/seed/5/800/400", alt: "할인 상품", title: "타임세일" },
])

// 이전/다음 인덱스 계산 (computed로 변경)
const prevIndex = computed(() => (currentIndex.value - 1 + images.value.length) % images.value.length)
const nextIndex = computed(() => (currentIndex.value + 1) % images.value.length)

// 슬라이드 이동
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
  stopAutoPlay()  // 혹시 남아있는 인터벌 있으면 클리어
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

// 마운트 시 자동재생 시작
onMounted(() => {
  if (isAutoPlay.value) startAutoPlay()
})

onUnmounted(() => {
  stopAutoPlay()
})
// 카테고리 데이터
const selectedCategory = ref('ALL') // 이게 빠져있었어요!

// 카테고리 데이터 - 실제 아이콘 대신 이모지 사용
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

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
  console.log('선택된 카테고리:', categoryId)
}

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

// 시청자 수 포맷팅
const formatViewerCount = (count) => {
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`
  }
  return count.toString()
}

// 방송 진행 시간 계산
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

// 방송 페이지로 이동
const goToBroadcast = (broadcastId) => {
  console.log('방송 페이지로 이동:', broadcastId)
  // router.push(`/live/${broadcastId}`)
}
</script>

<style scoped src="@/assets/css/home.css"></style>