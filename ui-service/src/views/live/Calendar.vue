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
                <!-- 시간 지남 표시 추가 -->
                <div v-if="shouldShowAsPast(broadcast)" class="past-indicator">
                  시간 지남
                </div>
              </div>

              <!-- 방송 썸네일 -->
              <div class="broadcast-thumbnail">
                <img
                    :src="getBroadcastThumbnail(broadcast)"
                    :alt="broadcast.title"
                    @error="handleImageError"
                    @load="handleImageLoad"
                    loading="lazy"
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
                  <span v-if="broadcast.status && broadcast.status.trim() === 'live'"
                        class="badge bg-danger ms-2">LIVE</span>
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
                <!--                <div class="broadcaster-name">-->
                <!--                  {{ broadcast.broadcasterName }}-->
                <!--                </div>-->

                <!-- 방송 상태별 버튼/상태 표시 -->
                <div class="broadcast-action">
                  <!-- 예정된 방송이고 시작시간이 안 지난 경우만 알림 받기 버튼 표시 -->
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

                  <!-- 시간이 지났지만 아직 상태가 scheduled인 경우 -->
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
        <div class="display-4 text-muted mb-3"></div>
        <h5 class="text-muted">{{ getSelectedDateText() }}에 예정된 방송이 없습니다</h5>
        <p class="text-muted small">다른 날짜를 선택해보세요</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSmartImages } from '@/composables/useSmartImages'
import {
  NOTIFICATION_CONFIG,
  notificationApiCall,
  getCurrentUser,
  subscribeBroadcastStart,
  unsubscribeBroadcast
} from '@/config/notificationConfig'

const { getProductImage, handleImageError, handleImageLoad } = useSmartImages()
const router = useRouter()

// 방송 썸네일 이미지 처리 함수 (useSmartImages 활용)
const getBroadcastThumbnail = (broadcast) => {
  // 1. 방송 썸네일 URL 추출
  const thumbnailUrl = broadcast.thumbnailUrl || broadcast.thumbnail_url

  if (thumbnailUrl && thumbnailUrl.trim() !== '') {
    // useSmartImages의 getProductImage를 활용하여 경로 변환
    const thumbnailObject = {
      mainImage: thumbnailUrl,
      image: thumbnailUrl,
      imageUrl: thumbnailUrl,
      name: broadcast.title || '방송',
      title: broadcast.title || '방송'
    }

    // useSmartImages로 경로 변환 처리
    const convertedImage = getProductImage(thumbnailObject)
    return convertedImage
  }

  // 2. 기본 이미지가 없으면 Picsum 사용
  return `https://picsum.photos/seed/${broadcast.id}/300/200`
}

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

// scheduledStartTime 파싱을 위한 함수 개선
const parseScheduledStartTime = (broadcast) => {
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
      return new Date(broadcast[field])
    }
  }

  return null
}

// 통합된 시작 시간 체크 함수
const isStartTimePassed = (broadcast) => {
  const startTime = parseScheduledStartTime(broadcast)

  if (!startTime || isNaN(startTime.getTime())) {
    return false
  }

  const now = new Date()
  const isPast = startTime < now

  return isPast
}

// 방송이 과거 방송으로 표시되어야 하는지 확인
const shouldShowAsPast = (broadcast) => {
  // 상태가 ended면 무조건 과거 방송
  if (broadcast.status && broadcast.status.trim() === 'ended') {
    return true
  }

  // scheduled 상태지만 시작 시간이 지난 경우
  return isScheduledButPast(broadcast)
}

// scheduled 상태이지만 시작 시간이 지났는지 확인
const isScheduledButPast = (broadcast) => {
  if (broadcast.status && broadcast.status.trim() !== 'scheduled') {
    return false
  }

  return isStartTimePassed(broadcast)
}

// 알림 받기 버튼을 표시해야 하는지 확인
const shouldShowNotificationButton = (broadcast) => {
  // scheduled 상태이고 시작 시간이 아직 안 지난 경우만 표시
  const isScheduled = broadcast.status && broadcast.status.trim() === 'scheduled'
  const isNotPast = !isStartTimePassed(broadcast)

  return isScheduled && isNotPast
}

// 방송 상태 텍스트 반환
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
    // 에러는 조용히 처리
  }

  serverStatus.value = 'disconnected'
  return false
}

// DB에서 방송 스케줄 조회 시 더 정확한 파싱
const fetchBroadcastsFromDB = async (date) => {
  try {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const dateString = `${year}-${month}-${day}`

    const response = await notificationApiCall(`${NOTIFICATION_CONFIG.ENDPOINTS.BROADCASTS_SCHEDULE}?date=${dateString}`)

    if (response.ok) {
      const data = await response.json()

      // 방송 데이터 파싱 및 필터링 개선
      if (data && Array.isArray(data)) {
        data.forEach(timeSlot => {
          if (timeSlot.broadcasts && Array.isArray(timeSlot.broadcasts)) {
            // ended 상태 방송 필터링 (필요에 따라 주석 해제)
            timeSlot.broadcasts = timeSlot.broadcasts.filter(broadcast => {
              const status = broadcast.status && broadcast.status.trim()

              // 시간이 지난 scheduled 방송도 필터링
              if (status === 'ended') {
                return false
              }

              //  시간이 지난 scheduled 방송도 제거
              if (status === 'scheduled') {
                const startTime = parseScheduledStartTime(broadcast)
                if (startTime && startTime < new Date()) {
                  return false
                }
              }

              return true
            })

            timeSlot.broadcasts.forEach(broadcast => {
              // 시간 필드가 이미 있는지 확인하고 없으면 timeSlot.time을 이용
              if (!parseScheduledStartTime(broadcast)) {
                // timeSlot.time (예: "14:30")과 선택된 날짜를 조합해서 완전한 시간 생성
                if (timeSlot.time) {
                  const [hours, minutes] = timeSlot.time.split(':')
                  const scheduledDateTime = new Date(date)
                  scheduledDateTime.setHours(parseInt(hours), parseInt(minutes), 0, 0)

                  broadcast.scheduledStartTime = scheduledDateTime.toISOString()
                }
              }
            })
          }
        })

        // 빈 timeSlot 제거
        return data.filter(timeSlot =>
            timeSlot.broadcasts && timeSlot.broadcasts.length > 0
        )
      }

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
  console.log('알림 설정 로드 시작')

  if (scheduleData.length === 0) {
    console.log('스케줄 데이터가 비어있음')
    return scheduleData
  }

  try {
    const user = getCurrentUser()
    if (!user.identifier) {
      console.log('사용자 정보 없음')
      // 사용자 정보 없으면 모든 알림을 false로 설정
      scheduleData.forEach(timeSlot => {
        timeSlot.broadcasts.forEach(broadcast => {
          broadcast.isNotificationSet = false
        })
      })
      return scheduleData
    }

    const userParam = user.identifier
    console.log('사용자 ID:', userParam)

    // 3. 현재 스케줄의 모든 방송 ID 수집
    const allBroadcastIds = []
    scheduleData.forEach(timeSlot => {
      timeSlot.broadcasts.forEach(broadcast => {
        const broadcastId = broadcast.id || broadcast.broadcastId || broadcast.broadcast_id
        if (broadcastId) {
          allBroadcastIds.push(String(broadcastId))
        }
      })
    })

    console.log('현재 스케줄의 방송 ID들:', allBroadcastIds)

    // 4. 서버에서 사용자 구독 정보 가져오기
    const endpoint = `/subscriptions/users/${userParam}`

    try {
      const response = await notificationApiCall(endpoint)

      if (response.ok) {
        const userSubscriptions = await response.json()
        console.log('서버에서 가져온 전체 구독 목록:', userSubscriptions)

        // 5. 서버 데이터를 Set으로 변환 (빠른 검색을 위해)
        const subscribedBroadcastIds = new Set(
            userSubscriptions.map(sub => String(sub.broadcastId))
        )

        console.log('구독 중인 방송 ID Set:', Array.from(subscribedBroadcastIds))

        // 6. 현재 스케줄의 각 방송에 대해 구독 상태 설정
        let matchedCount = 0
        scheduleData.forEach(timeSlot => {
          timeSlot.broadcasts.forEach(broadcast => {
            const broadcastId = String(broadcast.id || broadcast.broadcastId || broadcast.broadcast_id || '')
            const isSubscribed = subscribedBroadcastIds.has(broadcastId)

            // 반드시 boolean으로 설정
            broadcast.isNotificationSet = Boolean(isSubscribed)

            if (isSubscribed) {
              matchedCount++
              console.log(`구독 중인 방송 발견: "${broadcast.title}" (ID: ${broadcastId})`)
            } else {
              console.log(`구독 안함: "${broadcast.title}" (ID: ${broadcastId})`)
            }
          })
        })

        console.log(`총 ${matchedCount}개 방송이 구독 상태로 설정됨`)

        // 7. 로컬 스토리지에 백업 (서버와 동기화)
        syncLocalNotifications(userParam, Array.from(subscribedBroadcastIds))

        return scheduleData

      } else {
        console.error('서버 API 호출 실패:', response.status)
        throw new Error(`서버 API 실패: ${response.status}`)
      }

    } catch (serverError) {
      console.error('서버 조회 실패, 로컬 데이터 사용:', serverError)

      // 8. 서버 실패 시 로컬 스토리지에서 복원
      const localNotifications = getLocalNotifications(userParam)
      console.log('로컬 스토리지에서 가져온 알림 목록:', localNotifications)

      scheduleData.forEach(timeSlot => {
        timeSlot.broadcasts.forEach(broadcast => {
          const broadcastId = String(broadcast.id || broadcast.broadcastId || broadcast.broadcast_id || '')
          const isSubscribed = localNotifications.includes(broadcastId)
          broadcast.isNotificationSet = Boolean(isSubscribed)

          if (isSubscribed) {
            console.log(`로컬에서 복원: "${broadcast.title}" (ID: ${broadcastId})`)
          }
        })
      })

      return scheduleData
    }

  } catch (error) {
    console.error('알림 설정 로드 완전 실패:', error)

    // 9. 모든 실패 시 false로 초기화
    scheduleData.forEach(timeSlot => {
      timeSlot.broadcasts.forEach(broadcast => {
        broadcast.isNotificationSet = false
      })
    })

    return scheduleData
  }
}
// 서버 데이터를 로컬에 동기화
const syncLocalNotifications = (userId, serverBroadcastIds) => {
  const key = `notifications_${userId}`
  const stringIds = serverBroadcastIds.map(id => String(id))
  localStorage.setItem(key, JSON.stringify(stringIds))
  console.log('로컬 스토리지 동기화 완료:', stringIds)
}

// 로컬 저장소에서 알림 설정 가져오기
const getLocalNotifications = (userId) => {
  const key = `notifications_${userId}`
  const stored = localStorage.getItem(key)
  const notifications = stored ? JSON.parse(stored) : []
  console.log('로컬 스토리지에서 읽은 알림 목록:', notifications)
  return notifications
}

// 방송 스케줄 로드 함수
const loadBroadcastSchedule = async (date = selectedDate.value) => {
  console.log('방송 스케줄 로드 시작')

  isLoadingSchedule.value = true

  try {
    // 1. 먼저 방송 데이터 가져오기
    let scheduleData = await fetchBroadcastsFromDB(date)
    console.log('DB에서 가져온 스케줄 데이터:', scheduleData)

    // 2. 알림 설정 로드 (가장 중요!)
    scheduleData = await loadUserNotificationSettings(scheduleData)

    // 3. 최종 데이터 설정
    broadcastSchedule.value = scheduleData

    console.log('최종 설정된 스케줄 데이터:', broadcastSchedule.value)

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

// 수정된 방송 클릭 핸들러
const handleBroadcastClick = (broadcast) => {
  // 라이브 방송인 경우 바로 방송 페이지로 이동
  if (broadcast.status && broadcast.status.trim() === 'Live') {
    goToBroadcast(broadcast)
  }
  // 종료된 방송이고 다시보기가 있는 경우
  else if (broadcast.status && broadcast.status.trim() === 'ended' && broadcast.videoUrl) {
    watchReplay(broadcast)
  }
  // 그 외의 경우 (scheduled, starting 등) - 상세 페이지로 이동
  else {
    goToBroadcast(broadcast)
  }
}

// BroadcastList.vue와 동일한 방송 페이지 이동 함수
const goToBroadcast = (broadcast) => {
  // 백엔드에서 camelCase로 오는 경우와 snake_case 모두 지원
  const broadcastId = broadcast.broadcastId || broadcast.broadcast_id || broadcast.id

  if (broadcastId) {
    router.push({
      name: 'LiveBroadcastViewer',
      params: { broadcastId: String(broadcastId) }
    })
  } else {
    alert('방송 정보를 찾을 수 없습니다.')
  }
}

// 수정된 라이브 방송 시청 함수
const watchLiveBroadcast = (broadcast) => {
  // 기존 alert 대신 실제 라우터 이동
  goToBroadcast(broadcast)
}

// 수정된 다시보기 시청 함수
const watchReplay = (broadcast) => {
  // 다시보기 URL이 있으면 새 창에서 열기, 없으면 방송 상세 페이지로 이동
  if (broadcast.videoUrl) {
    window.open(broadcast.videoUrl, '_blank')
  } else {
    goToBroadcast(broadcast)
  }
}

// toggleNotification에서도 같은 함수 사용
const toggleNotification = async (broadcast) => {
  if (isNotificationLoading.value || serverStatus.value !== 'connected') return

  const user = getCurrentUser()
  if (!user.identifier) {
    alert('알림 설정을 위해 로그인이 필요합니다')
    return
  }

  if (isStartTimePassed(broadcast)) {
    alert('이미 시작 시간이 지난 방송입니다.')
    return
  }

  try {
    isNotificationLoading.value = true
    const userParam = user.identifier
    const broadcastId = String(broadcast.id || broadcast.broadcastId || broadcast.broadcast_id || '')

    console.log(`알림 토글 시작 - 방송: "${broadcast.title}", ID: ${broadcastId}, 현재 상태: ${broadcast.isNotificationSet}`)

    if (broadcast.isNotificationSet) {
      // 구독 취소
      await unsubscribeBroadcast(userParam, broadcastId)

      // UI 즉시 업데이트
      broadcast.isNotificationSet = false

      // 로컬 스토리지에서 제거
      removeLocalNotification(userParam, broadcastId)

      console.log('구독 취소 완료')
      alert('알림 구독이 취소되었습니다')

    } else {
      // 구독 설정
      await subscribeBroadcastStart(userParam, broadcastId)

      // UI 즉시 업데이트
      broadcast.isNotificationSet = true

      // 로컬 스토리지에 저장
      saveLocalNotification(userParam, broadcastId)

      console.log('구독 설정 완료')
      alert('방송 시작 알림을 설정했습니다!')
    }

  } catch (error) {
    console.error('알림 설정 에러:', error)

    // 에러 발생 시 원래 상태로 복원
    broadcast.isNotificationSet = !broadcast.isNotificationSet

    // 에러 처리 로직...
    alert('알림 설정 중 오류가 발생했습니다.')

  } finally {
    isNotificationLoading.value = false
  }
}
// 로컬 저장소에 알림 설정 저장
const saveLocalNotification = (userId, broadcastId) => {
  const key = `notifications_${userId}`
  const existing = JSON.parse(localStorage.getItem(key) || '[]')
  const stringId = String(broadcastId)  // 문자열로 변환

  if (!existing.includes(stringId)) {
    existing.push(stringId)
    localStorage.setItem(key, JSON.stringify(existing))
  }
}

const removeLocalNotification = (userId, broadcastId) => {
  const key = `notifications_${userId}`
  const existing = JSON.parse(localStorage.getItem(key) || '[]')
  const stringId = String(broadcastId)  // 문자열로 변환

  const filtered = existing.filter(id => id !== stringId)
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