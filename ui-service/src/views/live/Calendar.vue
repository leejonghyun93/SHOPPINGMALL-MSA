<template>
  <!-- 헤더 바로 아래에 붙는 달력 -->
  <div class="calendar-container bg-light border-bottom">
    <div class="container-fluid">
      <div class="d-flex align-items-center justify-content-start py-2">
        <!-- 이전 버튼 -->
        <button
            class="btn btn-sm btn-outline-secondary me-2"
            @click="movePrevious"
        >
          ‹
        </button>

        <!-- 날짜들 -->
        <div class="d-flex gap-1 mx-2" style="overflow-x: auto; white-space: nowrap;">
          <div
              v-for="(date, index) in dates"
              :key="index"
              @click="handleDateClick(date)"
              :class="[
              'd-flex flex-column align-items-center justify-content-center',
              'rounded text-center position-relative',
              'calendar-date-item',
              isSelected(date) ? 'selected-date' :
              isToday(date) ? 'today-date' : 'normal-date',
              isOtherMonth(date) ? 'other-month' : ''
            ]"
              style="min-width: 50px; height: 65px; cursor: pointer; transition: all 0.2s;"
          >
            <!-- 요일 -->
            <small class="fw-normal" style="font-size: 10px; line-height: 1;">
              {{ days[date.getDay()] }}
            </small>
            <!-- 날짜 -->
            <span class="fw-bold" style="font-size: 16px; line-height: 1;">
              {{ date.getDate() }}
            </span>
          </div>
        </div>

        <!-- 다음 버튼 -->
        <button
            class="btn btn-sm btn-outline-secondary ms-2"
            @click="moveNext"
        >
          ›
        </button>
      </div>
    </div>
  </div>

  <!-- 방송 스케줄 목록 -->
  <div class="broadcast-schedule-container">
    <div class="container-fluid py-3">

      <!-- 로딩 중 -->
      <div v-if="isLoadingSchedule" class="text-center py-5">
        <i class="fas fa-spinner fa-spin fa-2x text-primary mb-3"></i>
        <h5 class="text-muted">방송 스케줄을 불러오는 중...</h5>
      </div>

      <!-- 날짜별 방송 목록 -->
      <div v-else-if="broadcastSchedule.length > 0">
        <div v-for="timeSlot in broadcastSchedule" :key="timeSlot.time" class="time-slot-section mb-4">
          <!-- 해당 시간의 방송들 -->
          <div class="broadcast-list">
            <div
                v-for="broadcast in timeSlot.broadcasts"
                :key="broadcast.id"
                class="broadcast-item d-flex mb-3 bg-white"
                @click="handleBroadcastClick(broadcast)"
            >
              <!-- 시간 표시 -->
              <div class="time-display d-flex align-items-center justify-content-center">
                <span class="time-text">{{ timeSlot.time }}</span>
              </div>

              <!-- 방송 썸네일 -->
              <div class="broadcast-thumbnail">
                <img
                    :src="broadcast.thumbnail || '/default-thumbnail.jpg'"
                    :alt="broadcast.title"
                    @error="handleImageError"
                >
              </div>

              <!-- 방송 정보 -->
              <div class="broadcast-info">
                <!-- 방송 제목 -->
                <h6 class="broadcast-title">
                  {{ broadcast.title }}
                </h6>

                <!-- 상품 정보 -->
                <div class="product-section" v-if="broadcast.productName">
                  <div class="product-icon-name">
                    <i class="fas fa-gift"></i>
                    <span class="product-name">{{ broadcast.productName }}</span>
                  </div>
                  <div class="price-section" v-if="broadcast.salePrice">
                    <span class="sale-price">{{ formatPrice(broadcast.salePrice) }}원</span>
                  </div>
                </div>

                <!-- 방송자 정보 -->
                <div class="broadcaster-name">
                  {{ broadcast.broadcasterName }}
                </div>

                <!-- 알림 받기 버튼 -->
                <button
                    :class="[
                      'notification-btn-new',
                      broadcast.isNotificationSet ? 'notification-active' : ''
                    ]"
                    @click.stop="toggleNotification(broadcast)"
                    :disabled="isNotificationLoading"
                >
                  <i v-if="isNotificationLoading" class="fas fa-spinner fa-spin"></i>
                  <i v-else class="fas fa-bell"></i>
                  {{ broadcast.isNotificationSet ? '알림설정됨' : '알림받기' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 방송이 없는 경우 -->
      <div v-else class="no-broadcasts text-center py-5">
        <i class="fas fa-tv fa-3x text-muted mb-3"></i>
        <h5 class="text-muted">선택한 날짜에 예정된 방송이 없습니다</h5>
        <p class="text-muted small">다른 날짜를 선택해보세요</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'

// 서버 설정
const NOTIFICATION_SERVICE_URL = 'http://localhost:8096'
const BROADCAST_SERVICE_URL = 'http://localhost:8080'  // 방송 서비스 URL
const API_BASE_URL = `${NOTIFICATION_SERVICE_URL}/api/notifications`

// 상태 관리
const isNotificationLoading = ref(false)
const isLoadingSchedule = ref(false)
const currentApiUrl = ref(API_BASE_URL)

// API 호출 함수
const apiCall = async (url, options = {}) => {
  const token = localStorage.getItem('jwtToken') || localStorage.getItem('token')

  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }

  if (token) {
    defaultHeaders['Authorization'] = `Bearer ${token}`
  }

  const requestOptions = {
    mode: 'cors',
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers
    }
  }

  return fetch(url, requestOptions)
}

// 🔥 실제 방송 스케줄 API 호출
const fetchBroadcastSchedule = async (date) => {
  try {
    const dateString = date.toISOString().split('T')[0] // YYYY-MM-DD 형식

    // 방송 서비스에서 스케줄 데이터 가져오기
    const response = await apiCall(`${BROADCAST_SERVICE_URL}/api/broadcasts/schedule?date=${dateString}`)

    if (response.ok) {
      const data = await response.json()
      return data || []
    } else {
      console.error('방송 스케줄 조회 실패:', response.status)
      return []
    }
  } catch (error) {
    console.error('방송 스케줄 API 호출 실패:', error)
    return []
  }
}

// 🔥 사용자 알림 구독 상태 조회
const loadUserNotificationSettings = async (scheduleData) => {
  try {
    const user = getCurrentUser()
    const response = await apiCall(`${currentApiUrl.value}/subscriptions/users/${user.id}`)

    if (response.ok) {
      const userSubscriptions = await response.json()
      const subscribedBroadcastIds = new Set(
          userSubscriptions.map(sub => sub.broadcastId)
      )

      // 스케줄 데이터에 구독 상태 반영
      scheduleData.forEach(timeSlot => {
        timeSlot.broadcasts.forEach(broadcast => {
          broadcast.isNotificationSet = subscribedBroadcastIds.has(broadcast.id)
        })
      })
    }
  } catch (error) {
    console.error('사용자 알림 설정 조회 실패:', error)
  }

  return scheduleData
}

// 알림 구독 함수
const subscribeBroadcastStart = async (userId, broadcastId) => {
  const response = await apiCall(`${currentApiUrl.value}/subscriptions/broadcast-start?userId=${userId}&broadcastId=${broadcastId}`, {
    method: 'POST'
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || `HTTP ${response.status}`)
  }

  return await response.json()
}

// 알림 구독 취소 함수
const unsubscribeBroadcast = async (userId, broadcastId) => {
  const response = await apiCall(`${currentApiUrl.value}/subscriptions?userId=${userId}&broadcastId=${broadcastId}&type=BROADCAST_START`, {
    method: 'DELETE'
  })

  if (!response.ok) {
    const errorText = await response.text()
    throw new Error(errorText || `HTTP ${response.status}`)
  }
}

// 사용자 정보 가져오기
const getCurrentUser = () => {
  const token = localStorage.getItem('jwtToken') || localStorage.getItem('token')

  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]))
      return {
        id: payload.userId || payload.sub || payload.id || 1,
        username: payload.username || payload.name || 'testuser'
      }
    } catch (error) {
      console.error('JWT 토큰 파싱 실패:', error)
      return { id: 1, username: 'testuser' }
    }
  }

  return { id: 1, username: 'testuser' }
}

// 날짜 관련
const currentDate = ref(new Date())
const selectedDate = ref(new Date())
const days = ['일', '월', '화', '수', '목', '금', '토']

// 🔥 방송 스케줄 데이터 (DB에서 가져옴)
const broadcastSchedule = ref([])

// 🔥 방송 스케줄 로드 함수
const loadBroadcastSchedule = async (date = selectedDate.value) => {
  isLoadingSchedule.value = true

  try {
    console.log('방송 스케줄 로드:', date.toLocaleDateString('ko-KR'))

    // 1. 방송 스케줄 데이터 가져오기
    let scheduleData = await fetchBroadcastSchedule(date)

    // 2. 사용자 알림 설정 상태 조회
    scheduleData = await loadUserNotificationSettings(scheduleData)

    // 3. 스케줄 업데이트
    broadcastSchedule.value = scheduleData

    console.log('방송 스케줄 로드 완료:', scheduleData.length, '개 시간대')

  } catch (error) {
    console.error('방송 스케줄 로드 실패:', error)
    broadcastSchedule.value = []
  } finally {
    isLoadingSchedule.value = false
  }
}

// 계산된 속성 - 날짜 배열
const dates = computed(() => {
  const result = []
  for (let i = 3; i > 0; i--) {
    const date = new Date(currentDate.value)
    date.setDate(currentDate.value.getDate() - i)
    result.push(date)
  }
  result.push(new Date(currentDate.value))
  for (let i = 1; i <= 7; i++) {
    const date = new Date(currentDate.value)
    date.setDate(currentDate.value.getDate() + i)
    result.push(date)
  }
  return result
})

// 유틸리티 함수들
const isToday = (date) => {
  const today = new Date()
  return date.toDateString() === today.toDateString()
}

const isOtherMonth = (date) => {
  return date.getMonth() !== currentDate.value.getMonth()
}

const isSelected = (date) => {
  return selectedDate.value && date.toDateString() === selectedDate.value.toDateString()
}

// 이벤트 핸들러들
const handleDateClick = async (date) => {
  selectedDate.value = date
  console.log('선택된 날짜:', date.toLocaleDateString('ko-KR'))

  // 🔥 선택된 날짜의 방송 스케줄 다시 로드
  await loadBroadcastSchedule(date)
}

const movePrevious = () => {
  const newDate = new Date(currentDate.value)
  newDate.setDate(currentDate.value.getDate() - 1)
  currentDate.value = newDate
}

const moveNext = () => {
  const newDate = new Date(currentDate.value)
  newDate.setDate(currentDate.value.getDate() + 1)
  currentDate.value = newDate
}

const formatPrice = (price) => {
  return price ? price.toLocaleString('ko-KR') : '0'
}

const handleBroadcastClick = (broadcast) => {
  console.log('방송 클릭:', broadcast.title)
  // 방송 상세 페이지로 이동
}

const handleImageError = (event) => {
  event.target.src = '/default-thumbnail.jpg'
}

// 알림 토글 함수
const toggleNotification = async (broadcast) => {
  if (isNotificationLoading.value) return

  const user = getCurrentUser()

  try {
    isNotificationLoading.value = true

    if (broadcast.isNotificationSet) {
      // 구독 취소
      await unsubscribeBroadcast(user.id, broadcast.id)
      broadcast.isNotificationSet = false
      alert('✅ 알림 구독이 취소되었습니다')
    } else {
      // 구독 신청
      await subscribeBroadcastStart(user.id, broadcast.id)
      broadcast.isNotificationSet = true
      alert('🔔 방송 시작 알림을 설정했습니다!')
    }

  } catch (error) {
    console.error('❌ 알림 설정 실패:', error)
    alert(`❌ 알림 설정 중 오류가 발생했습니다: ${error.message}`)
  } finally {
    isNotificationLoading.value = false
  }
}

// 🔥 날짜 변경시 자동 스케줄 로드
watch(selectedDate, async (newDate) => {
  await loadBroadcastSchedule(newDate)
})

// 컴포넌트 초기화
onMounted(async () => {
  console.log('🚀 컴포넌트 초기화 시작')

  // 테스트용 토큰 설정
  if (!localStorage.getItem('jwtToken')) {
    const testPayload = { userId: 1, username: 'testuser' }
    const testToken = btoa(JSON.stringify(testPayload))
    localStorage.setItem('jwtToken', `test.${testToken}.signature`)
    console.log('🧪 테스트 토큰 생성됨')
  }

  // 🔥 실제 방송 스케줄 로드
  await loadBroadcastSchedule()

  console.log('✅ 초기화 완료')
})
</script>

<style scoped>
.calendar-container {
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
}

.calendar-date-item {
  border: 1px solid transparent;
  background-color: white;
  margin: 0 1px;
}

.calendar-date-item:hover {
  background-color: #e9ecef;
  border-color: #ced4da;
}

.today-date {
  background-color: white !important;
  color: #dc3545 !important;
  border-color: #dc3545 !important;
}

.selected-date {
  background-color: #dc3545 !important;
  color: white !important;
  border-color: #dc3545 !important;
}

.normal-date {
  color: #495057;
}

.other-month {
  color: #6c757d;
  opacity: 0.6;
}

.broadcast-schedule-container {
  background-color: #f8f9fa;
  min-height: 400px;
}

.broadcast-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid #e9ecef;
}

.broadcast-item:hover {
  background-color: #f8f9fa;
}

.time-display {
  width: 70px;
  height: 120px;
  background-color: white;
  border-right: 1px solid #e9ecef;
}

.time-text {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.broadcast-thumbnail {
  position: relative;
  width: 160px;
  height: 120px;
  flex-shrink: 0;
}

.broadcast-thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.broadcast-info {
  padding: 12px 16px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.broadcast-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin: 0 0 8px 0;
  line-height: 1.3;
}

.notification-btn-new {
  border: 1px solid #ff6b6b;
  background-color: white;
  color: #ff6b6b;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  margin-bottom: 12px;
  align-self: flex-start;
  transition: all 0.2s ease;
}

.notification-btn-new:hover {
  background-color: #ff6b6b;
  color: white;
}

.notification-btn-new.notification-active {
  background-color: #28a745;
  border-color: #28a745;
  color: white;
}

.notification-btn-new:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.product-section {
  margin-bottom: 8px;
}

.product-icon-name {
  display: flex;
  align-items: flex-start;
  margin-bottom: 6px;
}

.product-icon-name i {
  color: #4a90e2;
  margin-right: 6px;
  margin-top: 2px;
  font-size: 12px;
}

.product-name {
  font-size: 13px;
  color: #333;
  line-height: 1.3;
}

.sale-price {
  font-size: 16px;
  font-weight: bold;
  color: #ff6b6b;
}

.broadcaster-name {
  font-size: 12px;
  color: #666;
}
</style>