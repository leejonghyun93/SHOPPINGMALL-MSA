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

        <!-- 서버 상태 표시 (알림 서버만) -->
        <div class="ms-auto">
          <span :class="['badge', serverStatus === 'connected' ? 'bg-success' : 'bg-danger']">
            {{ serverStatus === 'connected' ? '알림 서버 연결됨' : '알림 서버 연결 안됨' }}
          </span>
        </div>
      </div>
    </div>
  </div>

  <!-- 방송 스케줄 목록 -->
  <div class="broadcast-schedule-container">
    <div class="container-fluid py-3">

      <!-- 로딩 중 -->
      <div v-if="isLoadingSchedule" class="text-center py-5">
        <div class="spinner-border text-primary mb-3" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
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
                :class="{
                  'past-broadcast': shouldShowAsPast(broadcast),
                  'live-broadcast': broadcast.status && broadcast.status.trim() === 'live'
                }"
                @click="handleBroadcastClick(broadcast)"
            >
              <!-- 시간 표시 -->
              <div class="time-display d-flex align-items-center justify-content-center">
                <span class="time-text">{{ timeSlot.time }}</span>
                <!-- 🔥 시간 지남 표시 추가 -->
                <div v-if="shouldShowAsPast(broadcast)" class="past-indicator">
                  <i class="fas fa-clock text-muted" style="font-size: 10px;"></i>
                </div>
              </div>

              <!-- 방송 썸네일 -->
              <div class="broadcast-thumbnail">
                <img
                    :src="broadcast.thumbnailUrl || '/default-thumbnail.jpg'"
                    :alt="broadcast.title"
                    @error="handleImageError"
                >
                <!-- 방송 상태 오버레이 -->
                <div v-if="broadcast.status && broadcast.status.trim() === 'live'" class="live-overlay">
                  <span class="text-white fw-bold">LIVE</span>
                </div>
                <div v-else-if="shouldShowAsPast(broadcast)" class="past-overlay">
                  <span class="text-white">{{ getBroadcastStatusText(broadcast) }}</span>
                </div>
              </div>

              <!-- 방송 정보 -->
              <div class="broadcast-info">
                <!-- 방송 제목 -->
                <h6 class="broadcast-title" :class="{
                  'text-muted': shouldShowAsPast(broadcast),
                  'text-danger': broadcast.status && broadcast.status.trim() === 'live'
                }">
                  {{ broadcast.title }}
                  <!-- 라이브 뱃지 -->
                  <span v-if="broadcast.status && broadcast.status.trim() === 'live'" class="badge bg-danger ms-2">LIVE</span>
                </h6>

                <!-- 상품 정보 -->
                <div class="product-section" v-if="broadcast.productName">
                  <div class="product-icon-name">
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

                <!-- 방송 상태별 버튼/상태 표시 -->
                <div class="broadcast-action">
                  <!-- 🔥 예정된 방송이고 시작시간이 안 지난 경우만 알림 받기 버튼 표시 -->
                  <button
                      v-if="shouldShowNotificationButton(broadcast)"
                      :class="[
                        'btn btn-outline-primary btn-sm',
                        'notification-btn-new',
                        broadcast.isNotificationSet ? 'notification-active btn-primary' : ''
                      ]"
                      @click.stop="toggleNotification(broadcast)"
                      :disabled="isNotificationLoading || serverStatus !== 'connected'"
                  >
                    <span v-if="isNotificationLoading">로딩...</span>
                    <span v-else>{{ broadcast.isNotificationSet ? '알림설정됨' : '알림받기' }}</span>
                  </button>

                  <!-- 라이브 중인 방송 (live) - 방송중 상태 표시 -->
                  <div v-else-if="broadcast.status && broadcast.status.trim() === 'live'" class="live-status">
                    <span class="badge bg-danger">
                      <i class="fas fa-circle me-1" style="font-size: 8px; animation: blink 1s infinite;"></i>
                      방송중
                    </span>
                    <button
                        class="btn btn-danger btn-sm ms-2"
                        @click.stop="watchLiveBroadcast(broadcast)"
                    >
                      시청하기
                    </button>
                  </div>

                  <!-- 종료된 방송 (ended) - 방송종료 상태 표시 -->
                  <div v-else-if="broadcast.status && broadcast.status.trim() === 'ended'" class="ended-status">
                    <span class="badge bg-secondary">방송 종료</span>
                    <!-- 다시보기가 가능한 경우만 버튼 표시 -->
                    <button
                        v-if="broadcast.videoUrl"
                        class="btn btn-outline-primary btn-sm ms-2"
                        @click.stop="watchReplay(broadcast)"
                    >
                      다시보기
                    </button>
                  </div>

                  <!-- 일시정지된 방송 (paused) -->
                  <div v-else-if="broadcast.status && broadcast.status.trim() === 'paused'" class="paused-status">
                    <span class="badge bg-warning text-dark">일시정지</span>
                  </div>

                  <!-- 시작중인 방송 (starting) -->
                  <div v-else-if="broadcast.status && broadcast.status.trim() === 'starting'" class="starting-status">
                    <span class="badge bg-info">시작 준비중</span>
                  </div>

                  <!-- 🔥 시간이 지났지만 아직 상태가 scheduled인 경우 -->
                  <div v-else-if="isScheduledButPast(broadcast)" class="past-scheduled-status">
                    <span class="badge bg-warning text-dark">시간 경과</span>
                    <small class="text-muted ms-2">방송이 예정 시간을 지났습니다</small>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 방송이 없는 경우 -->
      <div v-else class="no-broadcasts text-center py-5">
        <div class="display-4 text-muted mb-3">📺</div>
        <h5 class="text-muted">{{ getSelectedDateText() }}에 예정된 방송이 없습니다</h5>
        <p class="text-muted small">다른 날짜를 선택해보세요</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import {
  NOTIFICATION_CONFIG,
  notificationApiCall,
  getCurrentUser,
  subscribeBroadcastStart,
  unsubscribeBroadcast
} from '@/config/notificationConfig'

// 상태 관리
const isNotificationLoading = ref(false)
const isLoadingSchedule = ref(false)
const serverStatus = ref('disconnected')

// 날짜 관련
const currentDate = ref(new Date())
const selectedDate = ref(new Date())
const days = ['일', '월', '화', '수', '목', '금', '토']

// 방송 스케줄 데이터
const broadcastSchedule = ref([])

// 🔥 새로 추가된 유틸리티 함수들

// 🔥 scheduledStartTime 파싱을 위한 함수 개선
const parseScheduledStartTime = (broadcast) => {
  console.log('방송 데이터 전체:', broadcast) // 디버깅용

  // 여러 가능한 시간 필드를 확인
  const timeFields = [
    'scheduledStartTime',
    'scheduled_start_time',
    'startTime',
    'start_time',
    'broadcastStartTime',
    'broadcast_start_time'
  ]

  for (const field of timeFields) {
    if (broadcast[field]) {
      console.log(`시간 필드 발견: ${field} = ${broadcast[field]}`) // 디버깅용
      return new Date(broadcast[field])
    }
  }

  console.log('시간 필드를 찾을 수 없음') // 디버깅용
  return null
}

/**
 * 🔥 통합된 시작 시간 체크 함수
 */
const isStartTimePassed = (broadcast) => {
  const startTime = parseScheduledStartTime(broadcast)

  if (!startTime || isNaN(startTime.getTime())) {
    console.log('유효하지 않은 시작 시간:', startTime) // 디버깅용
    return false
  }

  const now = new Date()
  const isPast = startTime < now

  console.log(`시간 체크: ${startTime.toLocaleString()} < ${now.toLocaleString()} = ${isPast}`) // 디버깅용

  return isPast
}

/**
 * 방송이 과거 방송으로 표시되어야 하는지 확인
 */
const shouldShowAsPast = (broadcast) => {
  // 상태가 ended면 무조건 과거 방송
  if (broadcast.status && broadcast.status.trim() === 'ended') {
    return true
  }

  // scheduled 상태지만 시작 시간이 지난 경우
  return isScheduledButPast(broadcast)
}

/**
 * scheduled 상태이지만 시작 시간이 지났는지 확인
 */
const isScheduledButPast = (broadcast) => {
  if (broadcast.status && broadcast.status.trim() !== 'scheduled') {
    return false
  }

  return isStartTimePassed(broadcast)
}

/**
 * 알림 받기 버튼을 표시해야 하는지 확인
 */
const shouldShowNotificationButton = (broadcast) => {
  // scheduled 상태이고 시작 시간이 아직 안 지난 경우만 표시
  const isScheduled = broadcast.status && broadcast.status.trim() === 'scheduled'
  const isNotPast = !isStartTimePassed(broadcast)

  console.log(`알림 버튼 표시 체크: 방송 "${broadcast.title}" - scheduled: ${isScheduled}, notPast: ${isNotPast}`) // 디버깅용

  return isScheduled && isNotPast
}

/**
 * 방송 상태 텍스트 반환
 */
const getBroadcastStatusText = (broadcast) => {
  if (broadcast.status && broadcast.status.trim() === 'ended') {
    return '종료'
  }

  if (isScheduledButPast(broadcast)) {
    return '시간 경과'
  }

  return '종료'
}

// 선택된 날짜 텍스트 반환
const getSelectedDateText = () => {
  if (!selectedDate.value) return '오늘'

  const today = new Date()
  const selected = new Date(selectedDate.value)

  // 날짜만 비교 (시간 제외)
  today.setHours(0, 0, 0, 0)
  selected.setHours(0, 0, 0, 0)

  if (selected.getTime() === today.getTime()) {
    return '오늘'
  } else if (selected.getTime() === today.getTime() + 24 * 60 * 60 * 1000) {
    return '내일'
  } else if (selected.getTime() === today.getTime() - 24 * 60 * 60 * 1000) {
    return '어제'
  } else {
    return selected.toLocaleDateString('ko-KR', {
      month: 'long',
      day: 'numeric',
      weekday: 'short'
    })
  }
}

// 알림 서버 연결 체크
const checkNotificationServer = async () => {
  try {
    const response = await notificationApiCall(NOTIFICATION_CONFIG.ENDPOINTS.HEALTH)
    if (response.ok) {
      serverStatus.value = 'connected'
      return true
    }
  } catch (error) {
    console.error('알림 서버 연결 확인 실패:', error)
  }

  serverStatus.value = 'disconnected'
  return false
}

// 🔥 DB에서 방송 스케줄 조회 시 더 정확한 파싱
const fetchBroadcastsFromDB = async (date) => {
  try {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const dateString = `${year}-${month}-${day}`

    const response = await notificationApiCall(`${NOTIFICATION_CONFIG.ENDPOINTS.BROADCASTS_SCHEDULE}?date=${dateString}`)

    if (response.ok) {
      const data = await response.json()

      console.log('서버에서 받은 원본 데이터:', data) // 디버깅용

      // 🔥 방송 데이터 파싱 및 필터링 개선
      if (data && Array.isArray(data)) {
        data.forEach(timeSlot => {
          if (timeSlot.broadcasts && Array.isArray(timeSlot.broadcasts)) {
            // 🔥 ended 상태 방송 필터링 (필요에 따라 주석 해제)
            timeSlot.broadcasts = timeSlot.broadcasts.filter(broadcast => {
              const status = broadcast.status && broadcast.status.trim()

              // 시간이 지난 scheduled 방송도 필터링
              if (status === 'ended') {
                console.log(`종료된 방송 필터링: ${broadcast.title}`)
                return false
              }

              //  시간이 지난 scheduled 방송도 제거
              if (status === 'scheduled') {
                const startTime = parseScheduledStartTime(broadcast)
                if (startTime && startTime < new Date()) {
                  console.log(`시간 지난 예정 방송 필터링: ${broadcast.title}`)
                  return false
                }
              }

              return true
            })

            timeSlot.broadcasts.forEach(broadcast => {
              console.log('방송 데이터 처리 전:', broadcast) // 디버깅용

              // 🔥 시간 필드가 이미 있는지 확인하고 없으면 timeSlot.time을 이용
              if (!parseScheduledStartTime(broadcast)) {
                // timeSlot.time (예: "14:30")과 선택된 날짜를 조합해서 완전한 시간 생성
                if (timeSlot.time) {
                  const [hours, minutes] = timeSlot.time.split(':')
                  const scheduledDateTime = new Date(date)
                  scheduledDateTime.setHours(parseInt(hours), parseInt(minutes), 0, 0)

                  broadcast.scheduledStartTime = scheduledDateTime.toISOString()
                  console.log(`방송 "${broadcast.title}"의 시작 시간 설정: ${broadcast.scheduledStartTime}`)
                }
              }
            })
          }
        })

        // 🔥 빈 timeSlot 제거
        return data.filter(timeSlot =>
            timeSlot.broadcasts && timeSlot.broadcasts.length > 0
        )
      }

      return data || []
    } else {
      console.error('방송 스케줄 조회 실패:', response.status)
      return []
    }
  } catch (error) {
    console.error('방송 스케줄 조회 중 오류:', error)
    return []
  }
}

// 사용자 알림 구독 상태 조회
const loadUserNotificationSettings = async (scheduleData) => {
  if (scheduleData.length === 0) return scheduleData

  try {
    const user = getCurrentUser()
    if (!user.identifier) {
      console.log('사용자 정보 없음 - 로그인 필요')
      return scheduleData
    }

    const userParam = user.identifier

    // 🔥 서버 API 먼저 시도 (실제 DB 상태 반영)
    const endpoint = `/subscriptions/users/${userParam}`
    console.log('사용자 구독 상태 조회 시도:', userParam)

    try {
      const response = await notificationApiCall(endpoint)
      if (response.ok) {
        const userSubscriptions = await response.json()
        console.log('서버 구독 목록:', userSubscriptions)

        const subscribedBroadcastIds = new Set(
            userSubscriptions.map(sub => sub.broadcastId)
        )

        console.log('서버에서 가져온 구독 방송 ID들:', Array.from(subscribedBroadcastIds))

        // 🔥 서버 데이터를 로컬에 동기화
        syncLocalNotifications(userParam, Array.from(subscribedBroadcastIds))

        scheduleData.forEach(timeSlot => {
          timeSlot.broadcasts.forEach(broadcast => {
            const isSubscribed = subscribedBroadcastIds.has(broadcast.id)
            broadcast.isNotificationSet = isSubscribed

            if (isSubscribed) {
              console.log(`방송 "${broadcast.title}" (ID: ${broadcast.id}) - 알림 설정됨 (서버 확인)`)
            }
          })
        })

        return scheduleData
      } else {
        console.warn('서버 구독 상태 조회 실패:', response.status)
        throw new Error('서버 API 실패')
      }
    } catch (serverError) {
      console.warn('서버 연결 실패, 로컬 데이터 사용:', serverError.message)

      // 🔥 서버 실패시에만 로컬 데이터 사용 (경고 표시)
      const localNotifications = getLocalNotifications(userParam)
      console.log('⚠️ 오프라인 모드: 로컬 저장소 알림 설정 사용:', localNotifications)

      scheduleData.forEach(timeSlot => {
        timeSlot.broadcasts.forEach(broadcast => {
          const isSubscribed = localNotifications.includes(broadcast.id)
          broadcast.isNotificationSet = isSubscribed

          if (isSubscribed) {
            console.log(`방송 "${broadcast.title}" (ID: ${broadcast.id}) - 알림 설정됨 (로컬 캐시)`)
          }
        })
      })
    }
  } catch (error) {
    console.error('사용자 알림 설정 로드 실패:', error)
    scheduleData.forEach(timeSlot => {
      timeSlot.broadcasts.forEach(broadcast => {
        broadcast.isNotificationSet = false
      })
    })
  }

  return scheduleData
}

// 🔥 서버 데이터를 로컬에 동기화
const syncLocalNotifications = (userId, serverBroadcastIds) => {
  const key = `notifications_${userId}`
  localStorage.setItem(key, JSON.stringify(serverBroadcastIds))
  console.log('로컬 저장소 동기화 완료:', serverBroadcastIds)
}

// 로컬 저장소에서 알림 설정 가져오기
const getLocalNotifications = (userId) => {
  const key = `notifications_${userId}`
  return JSON.parse(localStorage.getItem(key) || '[]')
}

// 방송 스케줄 로드 함수
const loadBroadcastSchedule = async (date = selectedDate.value) => {
  isLoadingSchedule.value = true

  try {
    let scheduleData = await fetchBroadcastsFromDB(date)
    scheduleData = await loadUserNotificationSettings(scheduleData)
    broadcastSchedule.value = scheduleData
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
  if (broadcast.status && broadcast.status.trim() === 'live') {
    watchLiveBroadcast(broadcast)
  } else if (broadcast.status && broadcast.status.trim() === 'ended' && broadcast.videoUrl) {
    watchReplay(broadcast)
  }
}

// 라이브 방송 시청
const watchLiveBroadcast = (broadcast) => {
  alert(`${broadcast.title} 라이브 방송을 시청합니다.`)
  // router.push(`/live/${broadcast.id}`)
}

// 다시보기 시청
const watchReplay = (broadcast) => {
  alert(`${broadcast.title} 다시보기를 시청합니다.`)
  // router.push(`/replay/${broadcast.id}`)
}

const handleImageError = (event) => {
  event.target.src = '/default-thumbnail.jpg'
}

// 🔥 toggleNotification에서도 같은 함수 사용
const toggleNotification = async (broadcast) => {
  if (isNotificationLoading.value || serverStatus.value !== 'connected') return

  const user = getCurrentUser()
  if (!user.identifier) {
    alert('알림 설정을 위해 로그인이 필요합니다')
    return
  }

  // 🔥 같은 함수 사용으로 통일
  if (isStartTimePassed(broadcast)) {
    alert('이미 시작 시간이 지난 방송입니다.')
    return
  }

  try {
    isNotificationLoading.value = true
    const userParam = user.identifier

    console.log('=== 알림 토글 시작 ===')
    console.log('사용자 ID:', userParam)
    console.log('방송 ID:', broadcast.id)
    console.log('현재 알림 상태:', broadcast.isNotificationSet)

    if (broadcast.isNotificationSet) {
      console.log('알림 구독 취소 요청...')
      const result = await unsubscribeBroadcast(userParam, broadcast.id)
      console.log('구독 취소 결과:', result)

      broadcast.isNotificationSet = false
      removeLocalNotification(userParam, broadcast.id)
      alert('알림 구독이 취소되었습니다')
    } else {
      console.log('알림 구독 설정 요청...')
      const result = await subscribeBroadcastStart(userParam, broadcast.id)
      console.log('구독 설정 결과:', result)

      broadcast.isNotificationSet = true
      saveLocalNotification(userParam, broadcast.id)
      alert('방송 시작 알림을 설정했습니다!')
    }

  } catch (error) {
    console.error('알림 설정 오류:', error)
    broadcast.isNotificationSet = !broadcast.isNotificationSet

    let errorMessage = '알림 설정 중 오류가 발생했습니다.'

    if (error.response) {
      const errorData = error.response.data

      if (errorData && errorData.error) {
        switch (errorData.error) {
          case 'INVALID_PARAMETER':
            if (errorData.message && errorData.message.includes('이미 구독')) {
              broadcast.isNotificationSet = true
              saveLocalNotification(userParam, broadcast.id)
              alert('이미 알림이 설정되어 있습니다!')
              return
            } else {
              errorMessage = '잘못된 요청입니다. 페이지를 새로고침해주세요.'
            }
            break
          case 'INVALID_USER_ID':
            errorMessage = '사용자 정보가 올바르지 않습니다. 다시 로그인해주세요.'
            localStorage.removeItem('jwt')
            break
          case 'INTERNAL_ERROR':
            errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
            break
          default:
            errorMessage = errorData.message || errorMessage
        }
      }
      else if (error.response.status === 401) {
        errorMessage = '인증이 필요합니다. 다시 로그인해주세요.'
        localStorage.removeItem('jwt')
      } else if (error.response.status === 403) {
        errorMessage = '권한이 없습니다.'
      } else if (error.response.status === 409) {
        broadcast.isNotificationSet = true
        saveLocalNotification(userParam, broadcast.id)
        alert('이미 알림이 설정되어 있습니다!')
        return
      } else if (error.response.status >= 500) {
        errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'
      }
    }
    else if (!error.response) {
      errorMessage = '네트워크 연결을 확인해주세요.'
    }

    alert(errorMessage)
  } finally {
    isNotificationLoading.value = false
  }
}

// 로컬 저장소에 알림 설정 저장
const saveLocalNotification = (userId, broadcastId) => {
  const key = `notifications_${userId}`
  const existing = JSON.parse(localStorage.getItem(key) || '[]')
  if (!existing.includes(broadcastId)) {
    existing.push(broadcastId)
    localStorage.setItem(key, JSON.stringify(existing))
  }
}

// 로컬 저장소에서 알림 설정 제거
const removeLocalNotification = (userId, broadcastId) => {
  const key = `notifications_${userId}`
  const existing = JSON.parse(localStorage.getItem(key) || '[]')
  const filtered = existing.filter(id => id !== broadcastId)
  localStorage.setItem(key, JSON.stringify(filtered))
}

// 날짜 변경시 자동 스케줄 로드
watch(selectedDate, async (newDate) => {
  await loadBroadcastSchedule(newDate)
})

// 컴포넌트 초기화
onMounted(async () => {
  await checkNotificationServer()
  await loadBroadcastSchedule()
})
</script>

<style scoped src="@/assets/css/calendar.css"></style>
<style scoped>
/* 라이브 방송 깜빡임 효과 */
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0.3; }
}

.live-broadcast {
  border-left: 4px solid #dc3545 !important;
}

.live-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(220, 53, 69, 0.9) 0%, rgba(255, 107, 107, 0.9) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: live-pulse 2s infinite;
}

@keyframes live-pulse {
  0% { background: rgba(220, 53, 69, 0.9); }
  50% { background: rgba(255, 107, 107, 0.9); }
  100% { background: rgba(220, 53, 69, 0.9); }
}

.past-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}

.broadcast-action {
  margin-top: 8px;
}

.live-status, .ended-status, .paused-status, .starting-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-sm {
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
}

/* 🔥 새로 추가된 스타일 */
.past-scheduled-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.past-indicator {
  position: absolute;
  bottom: 5px;
  left: 50%;
  transform: translateX(-50%);
}
</style>