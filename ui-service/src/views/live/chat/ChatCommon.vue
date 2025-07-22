<template>
  <div class="chat-container">
    <!-- 상단 툴바 -->
    <div class="chat-topbar">
      <span class="chat-participant-count">👥 시청자{{ participantCount }}명</span>
      <button class="notice-toggle-btn" @click="toggleNotice">
        📢 {{ isNoticeExpanded ? '공지 숨기기' : '라이브 공지사항 보기' }}
      </button>
    </div>

    <!-- 공지사항 -->
    <div v-if="isNoticeExpanded" class="notice-banner">
      <div class="notice-text">{{ displayNotice }}</div>
    </div>

    <!-- 채팅 메인 영역 -->
    <div class="chat-main">
      <!-- 우클릭 메뉴 -->
      <div
          v-if="showContextMenu"
          class="context-menu"
          :style="{ top: contextMenuPos.y + 'px', left: contextMenuPos.x + 'px' }"
      >
        <div class="menu-item" @click="handleBanClick">🚫 5분간 채팅금지</div>
      </div>

      <!-- 메시지 리스트 -->
      <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll">
        <div
            v-for="(msg, index) in messages"
            :key="index"
            :class="[
            'chat-message',
            msg.systemOnly ? 'system-message' : isMyMessage(msg) ? 'my-message' : 'other-message'
          ]"
            @contextmenu.prevent="onRightClick($event, msg)"
        >
          <template v-if="msg.systemOnly">
            <div class="system-box">{{ msg.text }}</div>
          </template>
          <template v-else>
            <div class="chat-line">
              <template v-if="!isMyMessage(msg)">
                <div class="nickname">
                  <template v-if="msg.from === '관리자'">
                    <span class="admin-nickname">👑 {{ msg.from }}</span>
                  </template>
                  <template v-else>
                    {{ msg.from }}
                  </template>
                </div>
              </template>
              <div class="bubble" :class="{ 'admin-bubble': msg.from === '관리자' }">
                <img
                    v-if="msg.type === 'sticker'"
                    :src="stickerMap[msg.text]"
                    class="chat-sticker"
                />
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
            :disabled="!isChatEnabled || !isLoggedIn || isBanned"
            :placeholder="
            !isChatEnabled
            ? '채팅이 비활성화되었습니다.'
            : isBanned
            ? '⛔ 채팅이 일시적으로 제한되었습니다.'
            : isLoggedIn
            ? '메시지를 입력하세요'
            : '로그인 후 사용가능'
"
        />
        <button @click="sendMessage" :disabled="!isChatEnabled || !isLoggedIn || isBanned" class="send-button">
          전송
        </button>
        <button @click="toggleTools" class="tools-toggle">😎</button>
      </div>

      <!-- 스티커 도구창 -->
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

    <!-- 로그인 모달 -->
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
  <CustomAlert ref="alertRef" />
</template>


<script setup>
import { ref, nextTick, onMounted, computed, defineExpose, onUnmounted } from 'vue';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { stickerMap } from './EmojiMap';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { userState } from '@/stores/userState.js';
import userStateBridge from '@/stores/userStateBridge';
import { getOrCreateUUID } from '@/stores/uuid.js';
import CustomAlert from '@/views/live/chat/CustomAlert.vue';



const props = defineProps({
  class: String,
  broadcastId: String,
  role: { type: String, default: 'user' }
});

const emit = defineEmits(['host-detected']);

const isMyMessage = (msg) => {
  return msg.userId && msg.userId === userState.userId;
};

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
const isBanned = ref(false);
const alertRef = ref(null);

const broadcastStatus = ref('');
const isChatEnabled = ref(false);
const isHost = ref(false);
const noticeMessage = ref('');
const isNoticeExpanded = ref(false);
const uuid = getOrCreateUUID();
const participantCount = ref(0);
const hasInitialParticipantSet = ref(false);
const showContextMenu = ref(false);
const contextMenuPos = ref({ x: 0, y: 0 });
const selectedMsg = ref(null);


let chatSubscription = null;


const stompClient = new Client({
  webSocketFactory: () => new SockJS(import.meta.env.VITE_PROD_WS_URL),
  reconnectDelay: 5000,
  onConnect: () => {
    // 📌 채팅 메시지 구독
    chatSubscription = stompClient.subscribe('/topic/public', msg => {
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

    // 📌 방송 상태 변경 구독
    stompClient.subscribe(`/topic/broadcast/${props.broadcastId}/status`, msg => {
      const payload = JSON.parse(msg.body);
      broadcastStatus.value = payload.status;
      isChatEnabled.value = ['live', 'start', 'stop'].includes(broadcastStatus.value.toLowerCase());
    });
    // 📌 참여자 수 구독
    stompClient.subscribe(`/topic/participants/${props.broadcastId}`, msg => {
      const count = parseInt(msg.body, 10);
      console.log('👥 참가자 수 수신:', count);

      console.log('🧪 connectHeaders.broadcastId:', props.broadcastId);

      console.log('🧪 uuid:', uuid);

      // if (!hasInitialParticipantSet.value) {
      //   console.log('🧪 초기 API 수신 전이라 STOMP 반영 안 함');
      //    return;
      //  }

      participantCount.value = isNaN(count) ? 0 : count;
    });

    // 📌 채팅 금지 STOMP 채널 구독
    if (userState.userId) {
      stompClient.subscribe(`/topic/ban/${userState.userId}`, msg => {
        const data = JSON.parse(msg.body);
        isBanned.value = data.banned;

        if (data.banned) {
          alertRef.value?.open('⚠️ 부적절한 채팅창사용으로 5분간 채팅이 금지되었습니다.');
          setTimeout(() => {
            isBanned.value = false;
          }, data.duration * 1000);
        } else {
          alertRef.value?.open('✅ 채팅 금지가 해제되었습니다.');
        }
      });
    }


  }
});

onMounted(async () => {
  const token = localStorage.getItem('jwt') || sessionStorage.getItem('jwt');

  stompClient.connectHeaders = {
    Authorization: token ? `Bearer ${token}` : '',
    uuid,
    broadcastId: props.broadcastId
  };
  stompClient.activate();

  if (token) {
    try {
      const res = await axios.get('/api/users/profile', {
        headers: { Authorization: `Bearer ${token}` }
      });
      isLoggedIn.value = true;
      isHost.value = res.data.host === true;
      userState.userId = res.data.userId;
      userState.currentUser = isHost.value ? '관리자' : res.data.nickname;
      emit('host-detected', isHost.value);
    } catch (err) {
      console.warn('❌ 사용자 정보 조회 실패:', err);
      localStorage.removeItem('jwt');
      sessionStorage.removeItem('jwt');
    }
  }

  try {
    const res = await axios.get(`/api/broadcasts/${props.broadcastId}/status`);
    broadcastStatus.value = res.data.status;
    isChatEnabled.value = ['live', 'start', 'stop'].includes(broadcastStatus.value.toLowerCase());
  } catch (err) {
    console.error('❌ 방송 상태 조회 실패:', err);
  }

  try {
    const res = await axios.get(`/api/chat/history/${props.broadcastId}`);
    const history = res.data || [];

    messages.value.push(...history.filter(msg => msg.type !== 'notice'));

    const lastNotice = [...history].reverse().find(msg => msg.type === 'notice');
    if (lastNotice && lastNotice.text.trim()) {
      noticeMessage.value = lastNotice.text.trim();
    }

    messages.value.push({ text: '채팅방에 입장하셨습니다.', systemOnly: true });

  } catch (err) {
    console.error('❌ 채팅 기록 조회 실패:', err);
  }

  try {
    const res = await axios.get(`/api/chat/participants/${props.broadcastId}`);
    console.log('🟢 참가자 수 초기 조회 응답:', res.data);
    participantCount.value = res.data.count;
    hasInitialParticipantSet.value = true;
  } catch (e) {
    console.warn('❌ 참가자 수 초기 조회 실패', e);
  }

  if (userState.userId) {
    try {
      const res = await axios.get(`/api/chat/ban-status/${props.broadcastId}/${userState.userId}`);
      if (res.data.banned) {
        isBanned.value = true;
        console.warn('⚠️ 서버에 의해 금지된 사용자입니다.');
      }
    } catch (err) {
      console.error('❌ 금지 상태 확인 실패:', err);
    }
  }

  document.addEventListener('click', () => showContextMenu.value = false);
  loading.value = false;
  scrollToBottom();
});

const filteredStickers = computed(() => {
  return Object.entries(stickerMap)
      .filter(([key]) => key.startsWith(activeTab.value))
      .reduce((acc, [key, src]) => {
        acc[key] = src;
        return acc;
      }, {});
});

const sendMessage = () => {
  if (!isLoggedIn.value || isBanned.value || newMessage.value.trim() === '' || !stompClient.connected) {
    if (isBanned.value) {
      alertRef.value?.open('⚠️ 채팅이 금지된 상태입니다. 잠시 후 다시 시도해주세요.');
    }
    return;
  }

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
    userId: userState.userId
  };
  stompClient.publish({ destination: '/app/sendMessage', body: JSON.stringify(payload) });
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
const handleScroll = () => {
  showScrollToBottom.value = !isScrolledToBottom(200);
};


const toggleTools = () => {
  showTools.value = !showTools.value;
  focusInput();
  if (showTools.value) scrollToBottom();
};
const goToLogin = () => router.push('/login');
const handleInputFocus = e => {
  if (!isLoggedIn.value) {
    e.target.blur();
    showLoginModal.value = true;
  }
};

const shouldShowMoreBtn = computed(() => noticeMessage.value.length > 10);
const displayNotice = computed(() => noticeMessage.value.trim() || '등록된 공지사항이 없습니다.');
const toggleNotice = () => {
  isNoticeExpanded.value = !isNoticeExpanded.value;
};

const onRightClick = (event, msg) => {
  if (!isHost.value || isMyMessage(msg)) return;

  selectedMsg.value = msg;
  contextMenuPos.value = {
    x: event.pageX,
    y: event.pageY
  };
  showContextMenu.value = true;
}

// 채팅 금지 api호출
const handleBanClick = async () => {
  if (!selectedMsg.value || !selectedMsg.value.userId) return;

  const confirmed = window.confirm(
      `${selectedMsg.value.from} 유저를 5분간 채팅금지하시겠습니까?`
  );

  if (!confirmed) return;

  try {
    await axios.post('/api/chat/ban', null, {
      params: {
        broadcastId: props.broadcastId,
        userId: selectedMsg.value.userId,
        durationSeconds: 300,
      },
    });
    alertRef.value?.open(`${selectedMsg.value.from}님이 5분간 채팅금지되었습니다.`);
  } catch (err) {
    if (err.response?.status === 409) {
      alertRef.value?.open('⚠️ 이미 채팅금지된 사용자입니다.');
    } else {
      alertRef.value?.open('금지 처리 중 오류가 발생했습니다.');
    }
  }

  showContextMenu.value = false;
};

defineExpose({ sendNotice });

onUnmounted(() => {
  if (chatSubscription) chatSubscription.unsubscribe();
  if (stompClient.connected) stompClient.deactivate();
  const disconnectId = isLoggedIn.value ? userState.userId : uuid;
  navigator.sendBeacon(`/api/chat/disconnect/${props.broadcastId}?id=${disconnectId}`);
  document.removeEventListener('click', () => showContextMenu.value = false);
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
  white-space: pre-wrap;
  overflow: auto;
}
.notice-text.expanded {
  -webkit-line-clamp: unset;
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
  display: flex;
  flex-direction: column;
  margin-bottom: 6px;
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
.chat-line {
  display: inline-block;
  max-width: 60%;
}
.bubble {
  background-color: #eeeeee;
  border-radius: 12px;
  padding: 6px 10px;
  max-width: 100%;
  word-break: break-word;
  line-height: 1.4;
}
.my-message .bubble {
  background-color: #d8ecff;
}
.admin-bubble {
  background-color: #fde68a;
  border: 1px solid #f59e0b;
}
.admin-nickname {
  color: #d97706;
  font-weight: bold;
  background-color: #fff7ed;
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 13px;
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
.send-button {
  background-color: #3b82f6;
  color: white;
}
.send-button:disabled {
  background-color: #ccc;
  color: #888;
  cursor: not-allowed;
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
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.login-popup {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
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
.chat-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fef9c3;
  border-bottom: 1px solid #facc15;
  padding: 6px 10px;
  font-size: 13px;
}

.notice-toggle-btn {
  font-size: 13px;
  background: none;
  border: none;
  cursor: pointer;
  color: #d97706;
}

.chat-participant-count {
  font-size: 12px;
  color: #666;
}
.context-menu {
  position: absolute;
  background-color: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 9999;
  min-width: 160px;
}

.menu-item {
  padding: 10px 14px;
  font-size: 14px;
  cursor: pointer;
}

.menu-item:hover {
  background-color: #f3f4f6;
}

.ban-message {
  color: #ef4444;
  font-size: 14px;
  margin: 6px 0 0 10px;
}
</style>