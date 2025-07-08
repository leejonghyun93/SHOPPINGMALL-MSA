<template>
  <div class="chat-container">
    <!-- 공지 영역 -->
    <div class="notice-banner" :class="{ expanded: isNoticeExpanded }">
      <div class="notice-text" :class="{ expanded: isNoticeExpanded }">
        📢 {{ displayNotice }}
      </div>
      <button
          v-if="shouldShowMoreBtn"
          class="notice-toggle-btn"
          @click="toggleNotice"
      >
        {{ isNoticeExpanded ? '접기' : '더보기' }}
      </button>
    </div>

    <!-- 메시지 + 입력창 묶음 -->
    <div class="chat-main">
      <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll">
        <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="['chat-message', msg.systemOnly ? 'system-message' : (isMyMessage(msg) ? 'my-message' : 'other-message')]"
        >
          <template v-if="msg.systemOnly">
            <div class="system-box">{{ msg.text }}</div>
          </template>
          <template v-else>
            <div class="chat-line">
              <template v-if="!isMyMessage(msg)">
                <div class="nickname">{{ msg.from }}</div>
              </template>
              <div class="bubble">
                <img v-if="msg.type === 'sticker'" :src="stickerMap[msg.text]" class="chat-sticker" />
                <span v-else class="chat-content">{{ msg.text }}</span>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- 최근 메시지로 이동 -->
      <div v-if="showScrollToBottom" class="scroll-to-bottom" @click="scrollToBottom">
        최근 메시지로 이동
      </div>

      <!-- 입력창 -->
      <div class="chat-input">
        <input
            ref="inputRef"
            v-model="newMessage"
            @focus="handleInputFocus"
            @keyup.enter="sendMessage"
            :placeholder="isLoggedIn.value ? '메시지를 입력하세요' : '로그인 후 사용가능'"
        />
        <button @click="sendMessage">전송</button>
        <button @click="toggleTools" class="tools-toggle">😎</button>
      </div>

      <!-- 도구창 -->
      <div v-if="showTools" class="chat-tools">
        <div class="tools-header">
          <div class="tab-buttons">
            <button :class="{ active: activeTab === 'bear' }" @click="activeTab = 'bear'">🐻</button>
            <button :class="{ active: activeTab === 'rabbit' }" @click="activeTab = 'rabbit'">🐰</button>
          </div>
          <button class="close-tools" @click="showTools = false">✖</button>
        </div>
        <div class="sticker-list">
          <img
              v-for="(src, key) in filteredStickers"
              :key="key"
              :src="src"
              class="sticker-item"
              @click="() => sendSticker(key)"
          />
        </div>
      </div>
    </div>

    <!-- 로그인 안내 -->
    <div v-if="showLoginModal" class="login-popup-overlay">
      <div class="login-popup">
        <p>로그인 후 채팅이 가능합니다.</p>
        <div class="popup-buttons">
          <button @click="goToLogin">로그인 하고 채팅 참여하기</button>
          <button @click="showLoginModal = false">로그인 없이 방송 시청하기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, computed, defineExpose } from 'vue';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { stickerMap } from './EmojiMap';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { userState } from './UserState';

const props = defineProps({
  class: String,
  broadcastId: Number,
  role: { type: String, default: 'user' } // future use
});

const router = useRouter();
const isLoggedIn = ref(false);
const showLoginModal = ref(false);
const messages = ref([]);
const newMessage = ref('');
const messagesContainer = ref(null);
const inputRef = ref(null);
const showTools = ref(false);
const showScrollToBottom = ref(false);
const loading = ref(true);
const activeTab = ref('bear');

const normalize = str => String(str || '').trim();
const isMyMessage = msg => normalize(msg.from) === normalize(userState.currentUser);

const noticeMessage = ref('') // 공지사항 메시지

const filteredStickers = computed(() => {
  return Object.fromEntries(
      Object.entries(stickerMap).filter(([key]) => key.startsWith(activeTab.value))
  );
});

const socket = new SockJS('http://localhost:8080/ws-chat');
const stompClient = new Client({
  webSocketFactory: () => socket,
  reconnectDelay: 5000,
  onConnect: () => {
    messages.value.push({ text: '채팅방에 입장하셨습니다.', systemOnly: true });

    stompClient.subscribe('/topic/public', msg => {
      const received = JSON.parse(msg.body);

      if (received.type === 'notice') {
        noticeMessage.value = received.text.trim() || '';
        return;
      }

      messages.value.push(received);

      nextTick(() => {
        isScrolledToBottom() ? scrollToBottom() : (showScrollToBottom.value = true);
      });
    });
  }
});

onMounted(async () => {
  stompClient.activate();

  try {
    const res = await axios.get(`/api/chat/history/${props.broadcastId}`);
    const history = res.data || [];

    // ✅ 1. 일반 메시지만 messages 배열에 추가
    messages.value.push(...history.filter(msg => msg.type !== 'notice'));

    // ✅ 2. 마지막 공지 메시지 추출해서 noticeMessage에 반영
    const lastNotice = [...history].reverse().find(msg => msg.type === 'notice');
    if (lastNotice && lastNotice.text.trim()) {
      noticeMessage.value = lastNotice.text.trim();
    }

  } catch (err) {
    console.error('❌ 채팅 기록 조회 실패:', err);
  }

  // ✅ 로그인 유저 정보 확인
  const token = localStorage.getItem('jwt') || sessionStorage.getItem('jwt');
  if (token) {
    try {
      const res = await axios.get('/api/members/me', {
        headers: { Authorization: `Bearer ${token}` },
      });
      userState.currentUser = res.data.nickname;
      userState.userId = res.data.userId;
      isLoggedIn.value = true;
    } catch (err) {
      console.warn('❌ 사용자 정보 조회 실패 (토큰 만료 등):', err);
      localStorage.removeItem('jwt');
      sessionStorage.removeItem('jwt');
    }
  }

  loading.value = false;
  scrollToBottom();
});

const sendMessage = () => {
  if (!isLoggedIn.value || newMessage.value.trim() === '' || !stompClient.connected) return;
  const payload = {
    from: userState.currentUser,
    text: newMessage.value,
    type: 'text',
    broadcastId: props.broadcastId,
    userId: userState.userId
  };
  stompClient.publish({ destination: '/app/sendMessage', body: JSON.stringify(payload) });
  newMessage.value = '';
  focusInput();
  scrollToBottom();
};

const sendSticker = key => {
  if (!isLoggedIn.value || !stompClient.connected) return;
  const payload = {
    from: userState.currentUser,
    type: 'sticker',
    text: key,
    broadcastId: props.broadcastId,
    userId: userState.userId
  };
  stompClient.publish({ destination: '/app/sendMessage', body: JSON.stringify(payload) });
  focusInput();
  scrollToBottom();
};

const sendNotice = (text) => {
  if (!stompClient.connected) return;
  const payload = {
    from: userState.currentUser,
    type: 'notice',
    text: text || '',
    broadcastId: props.broadcastId,
    userId: userState.userId,
  };
  stompClient.publish({
    destination: '/app/sendMessage',
    body: JSON.stringify(payload),
  });
};

const focusInput = () => nextTick(() => inputRef.value?.focus());
const scrollToBottom = () => {
  nextTick(() => {
    const el = messagesContainer.value;
    if (el) {
      el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
      showScrollToBottom.value = false;
    }
  });
};
const isScrolledToBottom = (threshold = 200) => {
  const el = messagesContainer.value;
  return !el || el.scrollHeight - el.scrollTop - el.clientHeight < threshold;
};
const handleScroll = () => { showScrollToBottom.value = !isScrolledToBottom(200); };
const toggleTools = () => { showTools.value = !showTools.value; focusInput();
  if (showTools.value) {
    scrollToBottom();}
};
const goToLogin = () => router.push('/login');
const handleInputFocus = e => {
  if (!isLoggedIn.value) {
    e.target.blur();
    showLoginModal.value = true;
  }
};

const isNoticeExpanded = ref(false) // 공지사항 확장 상태

const shouldShowMoreBtn = computed(() => {
  return noticeMessage.value.length > 10;
});

const displayNotice = computed(() => {
  return noticeMessage.value.trim() !== '' ? noticeMessage.value : '등록된 공지사항이 없습니다.';
});

const toggleNotice = () => {
  isNoticeExpanded.value = !isNoticeExpanded.value;
};

defineExpose({
  sendNotice
});

</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.notice-banner {
  flex-shrink: 0;
  background: #fef9c3;
  padding: 6px 10px;
  font-size: 13px;
  color: #444;
  border-bottom: 1px solid #facc15;
}
.notice-text {
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  line-clamp: 1;               /* (표준 속성, 의미 없음) */
  -webkit-line-clamp: 1;
  white-space: normal; /* 줄바꿈 가능하도록 */
}
.notice-text.expanded {
  line-clamp: 1;               /* (표준 속성, 의미 없음) */
  -webkit-line-clamp: unset; /* 줄 수 제한 해제 */
}
.notice-toggle-btn {
  align-self: flex-end;
  font-size: 11px;
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  margin-top: 4px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  font-size: 13px;
  background: #f9f9f9;
  min-height: 0;
}

.chat-message {
  margin-bottom: 6px;
  display: flex;
  flex-direction: column;
}
.my-message {
  align-items: flex-end;
}
.other-message {
  align-items: flex-start;
}
.system-message {
  align-items: center;
}
.system-box {
  background: #e0e0e0;
  color: #555;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  margin: 6px 0;
  text-align: center;
}
.nickname {
  font-size: 12px;
  color: #888;
  margin-bottom: 2px;
}
.bubble {
  background-color: #eeeeee;
  border-radius: 12px;
  padding: 6px 10px;
  max-width: 80%;
  word-break: break-word;
  line-height: 1.4;
}
.my-message .bubble {
  background-color: #d8ecff;
}
.chat-sticker {
  width: 42px;
  height: 42px;
  object-fit: contain;
  border-radius: 4px;
  margin-top: 4px;
}

.scroll-to-bottom {
  background: #3b82f6;
  color: white;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  text-align: center;
  margin: 4px auto;
  width: fit-content;
  opacity: 0.8;
}

.chat-input {
  display: flex;
  padding: 6px;
  border-top: 1px solid #ccc;
  background: #fff;
  flex-shrink: 0;
}
.chat-input input {
  flex: 1;
  padding: 6px 8px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.chat-input button {
  margin-left: 6px;
  padding: 6px 10px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  border-radius: 4px;
}
.chat-input button:first-of-type {
  background-color: #3b82f6;
  color: white;
}
.tools-toggle {
  background: #f3f4f6;
  border: 1px solid #ccc;
  color: #333;
}

.chat-tools {
  flex-shrink: 0;
  background: #f8fafc;
  border-top: 1px solid #ddd;
  padding: 8px;
}
.tools-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.tab-buttons button {
  margin-right: 4px;
  font-size: 16px;
  border: none;
  background: transparent;
  cursor: pointer;
}
.tab-buttons .active {
  font-weight: bold;
}
.close-tools {
  background: none;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.sticker-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 100px;
  overflow-y: auto;
}
.sticker-item {
  width: 42px;
  height: 42px;
  object-fit: contain;
  border-radius: 4px;
  cursor: pointer;
}

.login-popup-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.login-popup {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
  text-align: center;
}
.popup-buttons {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  justify-content: center;
}
.popup-buttons button:first-child {
  background-color: #3b82f6;
  color: white;
}
.popup-buttons button:last-child {
  background-color: #eee;
  color: #333;
}
</style>