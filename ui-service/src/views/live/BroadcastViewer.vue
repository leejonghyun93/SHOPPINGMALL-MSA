<template>
  <div class="live-broadcast-viewer">
    <!-- 상단 헤더 -->
    <div class="broadcast-header">
      <div class="channel-info">
        <div class="channel-logo">
          <img src="https://picsum.photos/seed/channel/40/40" alt="채널 로고" />
        </div>
        <div class="channel-details">
          <h1 class="broadcast-title">[디지털어워즈][LG전자 가전상담회] 김(지냉방고)박사</h1>
          <div class="channel-stats">
            <span class="live-badge">LIVE</span>
            <span class="viewer-count">744 시청</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button class="action-btn">상품목록</button>
        <button class="action-btn">라이브 소개</button>
        <button class="action-btn">혜택</button>
        <button class="action-btn">공지</button>
      </div>
    </div>

    <div class="main-content">
      <!-- 왼쪽: 방송 영상 -->
      <div class="video-section">
        <!-- 비디오 플레이어 -->
        <div class="video-player">
          <div class="video-overlay">
            <div class="control-overlay">
              <!-- 중앙 재생 버튼 -->
              <button class="play-btn-center" @click="togglePlay">
                <span v-if="!isPlaying" class="play-icon">▶</span>
                <span v-else class="pause-icon">⏹</span>
              </button>

              <!-- 상품 소개 팝업 -->
              <div class="product-popup">
                <div class="popup-header">
                  <span class="popup-icon">🔍</span>
                  <span class="popup-text">상품소개 더보기</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 비디오 하단 컨트롤 -->
          <div class="video-controls">
            <div class="controls-left">
              <button class="control-btn" @click="togglePlay">
                <span v-if="!isPlaying" class="play-icon">▶</span>
                <span v-else class="pause-icon">⏸</span>
              </button>
              <span class="time-display">00:00 / 45:32</span>
            </div>
            <div class="controls-center">
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progress + '%' }"></div>
              </div>
            </div>
            <div class="controls-right">
              <button class="control-btn" @click="toggleMute">
                <span v-if="!isMuted">🔊</span>
                <span v-else>🔇</span>
              </button>
              <button class="control-btn">⚙️</button>
              <button class="control-btn" @click="toggleFullscreen">⛶</button>
            </div>
          </div>
        </div>

        <!-- 상품 정보 섹션 -->
        <div class="product-info-section">
          <div class="product-card">
            <img src="https://picsum.photos/seed/product1/200/150" alt="상품 이미지" class="product-image" />
            <div class="product-details">
              <h3 class="product-title">LG 디오스 오브제컬렉션 김치톡톡 Z300PSFT 327L 1등급 (...</h3>
              <div class="product-pricing">
                <div class="discount-info">
                  <span class="original-price">기존 적립 1%</span>
                  <span class="discount-price">0원</span>
                </div>
                <div class="special-offer">
                  <span class="live-special">라이브 특별 적립 2%</span>
                  <span class="special-price">0원</span>
                </div>
                <div class="final-price">
                  <span class="final-label">최대혜택가</span>
                  <span class="final-amount">0원</span>
                </div>
                <div class="sub-info">
                  <span class="payment-info">무이자카드 네이버페이 할인 + 적립금 포함 시</span>
                </div>
              </div>
            </div>
            <div class="product-actions">
              <button class="heart-btn" @click="toggleLike">
                <span class="heart-icon">🤍</span>
                <span class="heart-count">248</span>
              </button>
              <button class="share-btn">📤</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 오른쪽: 상품 목록 & 채팅 -->
      <div class="sidebar">
        <!-- 상품 목록 -->
        <div class="product-list">
          <div class="product-item" v-for="product in products" :key="product.id">
            <div class="product-badge">{{ product.discount }}</div>
            <img :src="product.image" :alt="product.name" class="product-thumb" />
            <div class="product-info">
              <h4 class="product-name">{{ product.name }}</h4>
              <div class="product-price">
                <span class="discount-rate">{{ product.discountRate }}</span>
                <span class="price">{{ product.price }}</span>
              </div>
              <div class="shipping-info">{{ product.shipping }}</div>
            </div>
          </div>

          <button class="view-more-btn">상품 6개 전체 보기</button>
        </div>

        <!-- 실시간 채팅 -->
        <div class="chat-section">
          <div class="chat-header">
            <h3>실시간 채팅</h3>
            <button class="chat-toggle" @click="toggleChat">
              <span v-if="chatExpanded">🔽</span>
              <span v-else>🔼</span>
            </button>
          </div>

          <div v-if="chatExpanded" class="chat-container">
            <!-- 채팅 메시지 목록 -->
            <div class="chat-messages" ref="chatMessages">
              <div
                v-for="message in chatMessages"
                :key="message.id"
                class="chat-message"
                :class="{ 'my-message': message.isMine }"
              >
                <div class="message-content">
                  <span class="username">{{ message.username }}</span>
                  <span class="message-text">{{ message.text }}</span>
                </div>
                <span class="message-time">{{ message.time }}</span>
              </div>
            </div>

            <!-- 채팅 입력 -->
            <div class="chat-input-container">
              <input
                type="text"
                v-model="newMessage"
                @keypress.enter="sendMessage"
                placeholder="채팅을 입력하세요..."
                class="chat-input"
              />
              <button @click="sendMessage" class="send-btn">전송</button>
            </div>
          </div>

          <!-- 자주 묻는 질문 -->
          <div class="faq-section">
            <div class="faq-header">
              <h3>자주 묻는 질문</h3>
              <button class="faq-toggle" @click="toggleFaq">
                <span v-if="faqExpanded"></span>
                <span v-else class="arrow-down">⬇</span>
                <span v-else class="arrow-down"></span>
              </button>
            </div>

            <div v-if="faqExpanded" class="faq-list">
              <div class="faq-item" v-for="faq in faqs" :key="faq.id">
                <div class="faq-question" @click="toggleFaqItem(faq.id)">
                  <span>{{ faq.question }}</span>
                  <span class="faq-arrow">{{ faq.expanded ? '▼' : '▶' }}</span>
                </div>
                <div v-if="faq.expanded" class="faq-answer">
                  {{ faq.answer }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 비디오 컨트롤 상태
const isPlaying = ref(false)
const isMuted = ref(false)
const progress = ref(35) // 진행률 35%

// 채팅 상태
const chatExpanded = ref(true)
const faqExpanded = ref(false)
const newMessage = ref('')
const chatMessages = ref([
  { id: 1, username: '***', text: '안녕하세요^^', time: '12:30', isMine: false },
  { id: 2, username: '***', text: '안녕하세요!!', time: '12:31', isMine: false },
  { id: 3, username: '***', text: '안녕하세요', time: '12:31', isMine: false },
  { id: 4, username: '구매자', text: '안녕하세요 ~~~~~~', time: '12:32', isMine: false },
  { id: 5, username: '***', text: '생명 뭐 하시네요', time: '12:32', isMine: false }
])

// 상품 목록 (하드코딩)
const products = ref([
  {
    id: 1,
    name: 'LG 디오스 오브제컬렉션 XYZ321 (X321AA3+Y321AA3+Z321...',
    discountRate: '23%',
    price: '2,830,000원',
    shipping: '무료배송',
    image: 'https://picsum.photos/seed/prod1/60/60'
  },
  {
    id: 2,
    name: 'LG 디오스 오브제컬렉션 김치톡톡 Z400MEEF23 스테드뭉 강...',
    discountRate: '23%',
    price: '1,910,000원',
    shipping: '무료배송',
    image: 'https://picsum.photos/seed/prod2/60/60'
  }
])

// FAQ 데이터
const faqs = ref([
  {
    id: 1,
    question: '배송은 언제 가능한가요?',
    answer: '주문 완료 후 영업일 기준 2-3일 내 배송됩니다.',
    expanded: false
  },
  {
    id: 2,
    question: '설치 서비스가 포함되나요?',
    answer: '대형 가전 제품의 경우 무료 설치 서비스가 포함됩니다.',
    expanded: false
  },
  {
    id: 3,
    question: '환불이나 교환이 가능한가요?',
    answer: '구매 후 7일 이내 미사용 제품에 한해 교환/환불이 가능합니다.',
    expanded: false
  }
])

// 채팅 메시지 ID 카운터
let messageIdCounter = 6

// 가상 WebSocket 연결 (하드코딩)
let chatInterval = null

// 메서드들
const togglePlay = () => {
  isPlaying.value = !isPlaying.value
}

const toggleMute = () => {
  isMuted.value = !isMuted.value
}

const toggleFullscreen = () => {
  alert('전체화면 기능 (실제 구현 시 Fullscreen API 사용)')
}

const toggleLike = () => {
  alert('좋아요! 하트 +1')
}

const toggleChat = () => {
  chatExpanded.value = !chatExpanded.value
}

const toggleFaq = () => {
  faqExpanded.value = !faqExpanded.value
}

const toggleFaqItem = (id) => {
  const faq = faqs.value.find(f => f.id === id)
  if (faq) {
    faq.expanded = !faq.expanded
  }
}

const sendMessage = () => {
  if (newMessage.value.trim()) {
    const now = new Date()
    const timeStr = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`

    chatMessages.value.push({
      id: messageIdCounter++,
      username: '나',
      text: newMessage.value,
      time: timeStr,
      isMine: true
    })

    newMessage.value = ''

    // 채팅 스크롤을 맨 아래로
    nextTick(() => {
      scrollToBottom()
    })
  }
}

const scrollToBottom = () => {
  const chatContainer = document.querySelector('.chat-messages')
  if (chatContainer) {
    chatContainer.scrollTop = chatContainer.scrollHeight
  }
}

// 가상 실시간 채팅 (다른 사용자 메시지 시뮬레이션)
const startMockChat = () => {
  const mockMessages = [
    '와 이 냉장고 진짜 예쁘네요!',
    '가격이 어떻게 되나요?',
    '설치비는 따로인가요?',
    '색상 다른 것도 있나요?',
    '에너지 효율은 1등급인가요?',
    '용량이 큰 것도 있나요?',
    '할인 혜택 더 있나요?',
    '언제까지 특가인가요?'
  ]

  const usernames = ['구매고민중', '냉장고맘', '가전왕', '절약이', '리뷰어']

  chatInterval = setInterval(() => {
    if (Math.random() < 0.3) { // 30% 확률로 메시지 생성
      const now = new Date()
      const timeStr = `${now.getHours()}:${now.getMinutes().toString().padStart(2, '0')}`

      chatMessages.value.push({
        id: messageIdCounter++,
        username: usernames[Math.floor(Math.random() * usernames.length)],
        text: mockMessages[Math.floor(Math.random() * mockMessages.length)],
        time: timeStr,
        isMine: false
      })

      // 메시지가 많아지면 오래된 것 제거
      if (chatMessages.value.length > 50) {
        chatMessages.value.shift()
      }

      nextTick(() => {
        scrollToBottom()
      })
    }
  }, 3000) // 3초마다 체크
}

// 라이프사이클
onMounted(() => {
  console.log('라이브 방송 시청 페이지 로드됨:', route.params.broadcastId)
  startMockChat()

  // 진행률 애니메이션 (가상)
  setInterval(() => {
    if (isPlaying.value) {
      progress.value = Math.min(progress.value + 0.1, 100)
    }
  }, 1000)
})

onUnmounted(() => {
  if (chatInterval) {
    clearInterval(chatInterval)
  }
})
</script>
<style scoped src="@/assets/css/broadcastViewer.css"></style>


