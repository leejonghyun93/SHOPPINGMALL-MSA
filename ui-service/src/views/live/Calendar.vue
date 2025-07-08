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
                :class="{ 'past-broadcast': isBroadcastPast(broadcast, timeSlot.time) }"
                @click="handleBroadcastClick(broadcast)"
            >
              <!-- 시간 표시 -->
              <div class="time-display d-flex align-items-center justify-content-center">
                <span class="time-text">{{ timeSlot.time }}</span>
                <!-- 지난 방송 표시 -->
                <div v-if="isBroadcastPast(broadcast, timeSlot.time)" class="past-indicator">
                  <small class="text-muted">종료</small>
                </div>
              </div>

              <!-- 방송 썸네일 -->
              <div class="broadcast-thumbnail">
                <img
                    :src="broadcast.thumbnailUrl || '/default-thumbnail.jpg'"
                    :alt="broadcast.title"
                    @error="handleImageError"
                >
                <!-- 지난 방송 오버레이 -->
                <div v-if="isBroadcastPast(broadcast, timeSlot.time)" class="past-overlay">
                  <span class="text-white">종료</span>
                </div>
              </div>

              <!-- 방송 정보 -->
              <div class="broadcast-info">
                <!-- 방송 제목 -->
                <h6 class="broadcast-title" :class="{ 'text-muted': isBroadcastPast(broadcast, timeSlot.time) }">
                  {{ broadcast.title }}
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

                <!-- 알림 받기 버튼 (방송 시간이 지나지 않은 경우만 표시) -->
                <button
                    v-if="!isBroadcastPast(broadcast, timeSlot.time)"
                    :class="[
                      'notification-btn-new',
                      broadcast.isNotificationSet ? 'notification-active' : ''
                    ]"
                    @click.stop="toggleNotification(broadcast)"
                    :disabled="isNotificationLoading || serverStatus !== 'connected'"
                >
                  <span v-if="isNotificationLoading">로딩...</span>
                  <span v-else>{{ broadcast.isNotificationSet ? '알림설정됨' : '알림받기' }}</span>
                </button>

                <!-- 지난 방송 상태 표시 -->
                <div v-else class="past-broadcast-status">
                  <span class="badge bg-secondary">
                    방송 종료
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 방송이 없는 경우 -->
      <div v-else class="no-broadcasts text-center py-5">
        <div class="display-4 text-muted mb-3">TV</div>
        <h5 class="text-muted">선택한 날짜에 예정된 방송이 없습니다</h5>
        <p class="text-muted small">방송 서비스가 구현되면 실제 방송 목록이 표시됩니다</p>
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

// 방송 시간이 지났는지 확인하는 함수
const isBroadcastPast = (broadcast, timeSlot) => {
  try {
    const now = new Date()
    const broadcastDate = new Date(selectedDate.value)
    const [hours, minutes] = timeSlot.split(':').map(Number)

    broadcastDate.setHours(hours, minutes, 0, 0)

    const durationHours = broadcast.duration || 1
    const broadcastEndTime = new Date(broadcastDate.getTime() + (durationHours * 60 * 60 * 1000))

    return now > broadcastEndTime
  } catch (error) {
    return false
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
    // 연결 실패 시 상태만 업데이트
  }

  serverStatus.value = 'disconnected'
  return false
}

// DB에서 방송 스케줄 조회
const fetchBroadcastsFromDB = async (date) => {
  try {
    const dateString = date.toISOString().split('T')[0]
    const response = await notificationApiCall(`${NOTIFICATION_CONFIG.ENDPOINTS.BROADCASTS_SCHEDULE}?date=${dateString}`)

    if (response.ok) {
      const data = await response.json()
      return data || []
    } else {
      return []
    }
  } catch (error) {
    return []
  }
}

// 사용자 알림 구독 상태 조회
const loadUserNotificationSettings = async (scheduleData) => {
  if (scheduleData.length === 0) return scheduleData

  try {
    const user = getCurrentUser()

    if (!user.identifier) {
      return scheduleData
    }

    const userParam = user.identifier
    const endpoint = `/subscriptions/users/${userParam}`

    const response = await notificationApiCall(endpoint)

    if (response.ok) {
      const userSubscriptions = await response.json()

      const subscribedBroadcastIds = new Set(
          userSubscriptions.map(sub => sub.broadcastId)
      )

      scheduleData.forEach(timeSlot => {
        timeSlot.broadcasts.forEach(broadcast => {
          const isSubscribed = subscribedBroadcastIds.has(broadcast.id)
          broadcast.isNotificationSet = isSubscribed
        })
      })
    } else {
      scheduleData.forEach(timeSlot => {
        timeSlot.broadcasts.forEach(broadcast => {
          broadcast.isNotificationSet = false
        })
      })
    }
  } catch (error) {
    scheduleData.forEach(timeSlot => {
      timeSlot.broadcasts.forEach(broadcast => {
        broadcast.isNotificationSet = false
      })
    })
  }

  return scheduleData
}

// 날짜 관련
const currentDate = ref(new Date())
const selectedDate = ref(new Date())
const days = ['일', '월', '화', '수', '목', '금', '토']

// 방송 스케줄 데이터
const broadcastSchedule = ref([])

// 방송 스케줄 로드 함수
const loadBroadcastSchedule = async (date = selectedDate.value) => {
  isLoadingSchedule.value = true

  try {
    let scheduleData = await fetchBroadcastsFromDB(date)
    scheduleData = await loadUserNotificationSettings(scheduleData)
    broadcastSchedule.value = scheduleData
  } catch (error) {
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
  // 방송 상세 페이지로 이동
}

const handleImageError = (event) => {
  event.target.src = '/default-thumbnail.jpg'
}

const toggleNotification = async (broadcast) => {
  if (isNotificationLoading.value || serverStatus.value !== 'connected') return

  const user = getCurrentUser()

  if (!user.identifier) {
    alert('알림 설정을 위해 로그인이 필요합니다')
    return
  }

  try {
    isNotificationLoading.value = true

    const userParam = user.identifier

    if (broadcast.isNotificationSet) {
      await unsubscribeBroadcast(userParam, broadcast.id)
      broadcast.isNotificationSet = false
      alert('알림 구독이 취소되었습니다')
    } else {
      await subscribeBroadcastStart(userParam, broadcast.id)
      broadcast.isNotificationSet = true
      alert('방송 시작 알림을 설정했습니다!')
    }

  } catch (error) {
    let errorMessage = '알림 설정 중 오류가 발생했습니다.'

    if (error.response) {
      const errorData = error.response.data

      if (errorData && errorData.error) {
        switch (errorData.error) {
          case 'INVALID_PARAMETER':
            if (errorData.message && errorData.message.includes('이미 구독')) {
              broadcast.isNotificationSet = true
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
