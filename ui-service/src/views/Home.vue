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
      {{ isAutoPlay ? '⏸️ 일시정지' : '▶️ 자동재생' }}
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
</script>

<style scoped src="@/assets/css/home.css"></style>