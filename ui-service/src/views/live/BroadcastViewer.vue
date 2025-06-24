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

<style scoped>
.live-broadcast-viewer {
  width: 100%;
  min-height: 100vh;
  background: #f5f5f5;
}

/* 상단 헤더 */
.broadcast-header {
  background: white;
  padding: 15px 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.channel-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.channel-logo img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}

.broadcast-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0;
  color: #333;
}

.channel-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.live-badge {
  background: #ff4757;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.viewer-count {
  color: #666;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 10px;
}
.play-icon, .pause-icon, .arrow-down {
  font-size: inherit;
  line-height: 1;
}

.play-icon {
  margin-left: 2px; /* 재생 아이콘 중앙 정렬 */
}
.action-btn {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f8f9fa;
  border-color: #007bff;
}

/* 메인 컨텐츠 */
.main-content {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 20px;
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 비디오 섹션 */
.video-section {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.video-player {
  position: relative;
  width: 100%;
  height: 500px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-image: url('https://picsum.photos/seed/broadcast/800/500');
  background-size: cover;
  background-position: center;
}

.video-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.control-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  position: relative;
}

.play-btn-center {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255,255,255,0.9);
  border: none;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s;
}

.play-btn-center:hover {
  background: white;
  transform: scale(1.1);
}

.product-popup {
  position: absolute;
  bottom: 20px;
  right: 20px;
  background: rgba(0,0,0,0.7);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.video-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0,0,0,0.7));
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.controls-left,
.controls-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.controls-center {
  flex: 1;
}

.control-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  padding: 5px;
  border-radius: 4px;
  transition: background 0.2s;
}

.control-btn:hover {
  background: rgba(255,255,255,0.2);
}

.time-display {
  color: white;
  font-size: 14px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: rgba(255,255,255,0.3);
  border-radius: 2px;
  cursor: pointer;
}

.progress-fill {
  height: 100%;
  background: #ff4757;
  border-radius: 2px;
  transition: width 0.5s;
}

/* 상품 정보 섹션 */
.product-info-section {
  padding: 20px;
}

.product-card {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.product-image {
  width: 200px;
  height: 150px;
  object-fit: cover;
  border-radius: 8px;
}

.product-details {
  flex: 1;
}

.product-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 15px 0;
  color: #333;
}

.product-pricing {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.discount-info,
.special-offer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.original-price,
.live-special {
  font-size: 14px;
  color: #666;
}

.discount-price,
.special-price {
  font-weight: bold;
  color: #333;
}

.final-price {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #fff3cd;
  border: 2px solid #ffc107;
  border-radius: 8px;
}

.final-label {
  font-size: 16px;
  font-weight: bold;
  color: #856404;
}

.final-amount {
  font-size: 24px;
  font-weight: bold;
  color: #dc3545;
}

.sub-info {
  font-size: 12px;
  color: #666;
  text-align: center;
  margin-top: 5px;
}

.product-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.heart-btn,
.share-btn {
  background: none;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.heart-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.heart-icon {
  font-size: 20px;
}

.heart-count {
  font-size: 12px;
  color: #666;
}

.heart-btn:hover,
.share-btn:hover {
  border-color: #007bff;
  background: #f8f9fa;
}

/* 사이드바 */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 상품 목록 */
.product-list {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.product-item {
  display: flex;
  gap: 12px;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.product-item:last-child {
  border-bottom: none;
}

.product-badge {
  position: absolute;
  top: 10px;
  left: 0;
  background: #ff4757;
  color: white;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: bold;
}

.product-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 13px;
  font-weight: 500;
  margin: 0;
  color: #333;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-price {
  display: flex;
  align-items: center;
  gap: 6px;
}

.discount-rate {
  color: #ff4757;
  font-weight: bold;
  font-size: 14px;
}

.price {
  color: #333;
  font-weight: bold;
  font-size: 14px;
}

.shipping-info {
  color: #666;
  font-size: 11px;
}

.view-more-btn {
  width: 100%;
  padding: 12px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  margin-top: 15px;
  transition: all 0.2s;
}

.view-more-btn:hover {
  background: #e9ecef;
}

/* 채팅 섹션 */
.chat-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  overflow: hidden;
}

.chat-header,
.faq-header {
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
}

.chat-header h3,
.faq-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.chat-toggle,
.faq-toggle {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.chat-container {
  height: 300px;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-message {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-message.my-message {
  align-items: flex-end;
}

.chat-message.my-message .message-content {
  background: #007bff;
  color: white;
}

.message-content {
  background: #f1f3f4;
  padding: 8px 12px;
  border-radius: 12px;
  max-width: 80%;
  word-wrap: break-word;
}

.username {
  font-weight: bold;
  font-size: 12px;
  margin-right: 6px;
}

.message-text {
  font-size: 14px;
}

.message-time {
  font-size: 11px;
  color: #999;
  align-self: flex-start;
}

.chat-message.my-message .message-time {
  align-self: flex-end;
}

.chat-input-container {
  display: flex;
  gap: 8px;
  padding: 15px;
  border-top: 1px solid #f0f0f0;
}

.chat-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
}

.chat-input:focus {
  border-color: #007bff;
}

.send-btn {
  padding: 10px 16px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.send-btn:hover {
  background: #0056b3;
}

/* FAQ 섹션 */
.faq-list {
  padding: 0;
}

.faq-item {
  border-bottom: 1px solid #f0f0f0;
}

.faq-item:last-child {
  border-bottom: none;
}

.faq-question {
  padding: 15px 20px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background 0.2s;
}

.faq-question:hover {
  background: #f8f9fa;
}

.faq-arrow {
  font-size: 12px;
  color: #666;
}

.faq-answer {
  padding: 0 20px 15px 20px;
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  background: #f9f9f9;
}

/* 반응형 */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .sidebar {
    order: -1;
  }
}

@media (max-width: 768px) {
  .broadcast-header {
    flex-direction: column;
    gap: 15px;
    align-items: stretch;
  }

  .header-actions {
    justify-content: center;
  }

  .main-content {
    padding: 10px;
    gap: 10px;
  }

  .video-player {
    height: 250px;
  }

  .product-card {
    flex-direction: column;
  }

  .product-image {
    width: 100%;
    height: 200px;
  }

  .controls-center {
    display: none;
  }

  .video-controls {
    padding: 10px;
  }
}

@media (max-width: 480px) {
  .broadcast-title {
    font-size: 16px;
  }

  .header-actions {
    flex-wrap: wrap;
  }

  .action-btn {
    flex: 1;
    min-width: calc(50% - 5px);
  }

  .chat-container {
    height: 250px;
  }
}
</style>