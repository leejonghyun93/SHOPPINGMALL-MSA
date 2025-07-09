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
        <button @click="sendMessage" :disabled="!isLoggedIn || !newMessage.trim()">전송</button>
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
import { ref, nextTick, onMounted, onUnmounted, computed, defineExpose } from 'vue';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { stickerMap } from './EmojiMap';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { userState } from '@/stores/userState';  // stores 폴더의 userState
import userStateBridge from '@/stores/userStateBridge';  // 🌉 브리지 import (stores 폴더에 있음)

const props = defineProps({
  class: String,
  broadcastId: {
    type: [Number, String],
    required: true,
    default: 0
  },
  role: {
    type: String,
    default: 'user'
  }
});

const broadcastIdNum = computed(() => {
  const id = typeof props.broadcastId === 'string' ? parseInt(props.broadcastId) : props.broadcastId;
  console.log('📌 broadcastId 변환:', props.broadcastId, '->', id);
  return id;
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
const noticeMessage = ref('');
const isNoticeExpanded = ref(false);

// WebSocket 연결 상태 관리
const isConnecting = ref(false);
const connectionRetries = ref(0);
const maxRetries = 5;
const connectionStatus = ref('disconnected');

// 🔄 브리지된 사용자 정보 사용
const currentUser = computed(() => {
  // 우선순위: currentUser > name
  return userState.currentUser || userState.name || null;
});

const currentUserId = computed(() => {
  // 우선순위: userId > id
  return userState.userId || userState.id || null;
});

const normalize = str => String(str || '').trim();
const isMyMessage = msg => normalize(msg.from) === normalize(currentUser.value);

const filteredStickers = computed(() => {
  return Object.fromEntries(
      Object.entries(stickerMap).filter(([key]) => key.startsWith(activeTab.value))
  );
});

const shouldShowMoreBtn = computed(() => {
  return noticeMessage.value.length > 10;
});

const displayNotice = computed(() => {
  return noticeMessage.value.trim() !== '' ? noticeMessage.value : '등록된 공지사항이 없습니다.';
});

// WebSocket 연결 설정
let socket = null;
let stompClient = null;

const createWebSocketConnection = () => {
  console.log('🔄 WebSocket 연결 시도 중... (시도 횟수:', connectionRetries.value + 1, ')');

  if (connectionStatus.value === 'connecting') {
    console.log('⏳ 이미 연결 중입니다.');
    return;
  }

  connectionStatus.value = 'connecting';
  isConnecting.value = true;

  if (stompClient) {
    try {
      stompClient.deactivate();
    } catch (error) {
      console.warn('⚠️ 기존 연결 정리 중 오류:', error);
    }
  }
  const wsUrl = 'http://***.***.*.***:****/ws-chat';

  console.log('🌐 WebSocket URL:', wsUrl);

  try {
    socket = new SockJS(wsUrl);

    stompClient = new Client({
      webSocketFactory: () => {
        console.log("🛰️ [WebSocketFactory] SockJS 연결 생성");
        return socket;
      },

      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: (frame) => {
        console.log("✅ [STOMP] 연결 성공!", frame);
        connectionStatus.value = 'connected';
        isConnecting.value = false;
        connectionRetries.value = 0;

        messages.value.push({
          text: '✅ 채팅방에 연결되었습니다.',
          systemOnly: true
        });

        stompClient.subscribe('/topic/public', (msg) => {
          console.log("📩 [STOMP] 수신 메시지:", msg.body);

          try {
            const received = JSON.parse(msg.body);

            if (received.type === 'notice') {
              console.log("📢 [공지 메시지 수신]", received.text);
              noticeMessage.value = received.text.trim() || '';
              return;
            }

            messages.value.push(received);

            nextTick(() => {
              isScrolledToBottom()
                  ? scrollToBottom()
                  : (showScrollToBottom.value = true);
            });
          } catch (error) {
            console.error('❌ 메시지 파싱 오류:', error);
          }
        });
      },

      onStompError: (frame) => {
        console.error("❌ [STOMP ERROR]", frame);
        connectionStatus.value = 'failed';
        isConnecting.value = false;

        if (connectionRetries.value < maxRetries) {
          connectionRetries.value++;
          console.log(`🔄 재연결 시도 ${connectionRetries.value}/${maxRetries} (5초 후)`);

          messages.value.push({
            text: `🔄 채팅 서버 재연결 시도 중... (${connectionRetries.value}/${maxRetries})`,
            systemOnly: true
          });

          setTimeout(() => {
            createWebSocketConnection();
          }, 5000);
        } else {
          console.error('❌ 최대 재연결 시도 횟수 초과');
          connectionStatus.value = 'failed';
          messages.value.push({
            text: '❌ 채팅 서버 연결에 실패했습니다. 페이지를 새로고침 해주세요.',
            systemOnly: true
          });
        }
      },

      onWebSocketError: (error) => {
        console.error("❌ [WebSocket ERROR]", error);
        connectionStatus.value = 'failed';
        isConnecting.value = false;
      },

      onDisconnect: (frame) => {
        console.log("🔌 [STOMP] 연결 종료", frame);
        connectionStatus.value = 'disconnected';
        isConnecting.value = false;

        messages.value.push({
          text: '🔌 채팅 서버 연결이 끊어졌습니다.',
          systemOnly: true
        });
      }
    });

    stompClient.activate();

  } catch (error) {
    console.error('❌ WebSocket 연결 생성 실패:', error);
    connectionStatus.value = 'failed';
    isConnecting.value = false;
  }
};

// 🔄 브리지 기반 사용자 정보 로드
const loadUserInfo = async () => {
  console.log('🔍 사용자 정보 로드 시작');

  // 1. 브리지 상태 확인
  userStateBridge.checkSync();

  // 2. 이미 동기화된 상태에서 사용자 정보가 있다면 바로 사용
  if (currentUser.value && currentUserId.value) {
    console.log('✅ 브리지에서 사용자 정보 확인됨:', currentUser.value);
    isLoggedIn.value = true;
    return;
  }

  const token = localStorage.getItem('jwt') || sessionStorage.getItem('jwt');
  console.log('🔍 토큰 존재:', !!token);

  if (token) {
    try {
      console.log('📡 사용자 프로필 API 호출');
      const res = await axios.get('/api/users/profile', {
        headers: { Authorization: `Bearer ${token}` },
      });

      console.log('📡 API 응답:', res.data);

      if (res.data) {
        let userData = res.data;

        // 중첩된 data 구조 처리
        if (res.data.success && res.data.data) {
          userData = res.data.data;
        }

        // 사용자 정보 추출
        const nickname = userData.nickname || userData.name || userData.username || userData.userName;
        const userId = userData.userId || userData.id || userData.user_id;

        console.log('🔍 추출된 정보:');
        console.log('- nickname:', nickname);
        console.log('- userId:', userId);

        if (nickname) {
          // 🌉 브리지를 통해 양쪽 상태 모두 업데이트
          userState.currentUser = nickname;
          userState.userId = userId;
          userState.name = nickname;
          userState.id = userId;
          userState.email = userData.email;
          userState.role = userData.role || 'USER';
          userState.phone = userData.phone;

          isLoggedIn.value = true;
          console.log('✅ 사용자 정보 설정 성공 (브리지 통해):', nickname);

          // 브리지 강제 동기화
          userStateBridge.forceSync();
        } else {
          console.error('❌ 사용자 닉네임을 찾을 수 없음');
          console.log('📋 사용 가능한 필드:', Object.keys(userData));
        }
      } else {
        console.error('❌ 빈 응답 데이터');
      }
    } catch (err) {
      console.error('❌ 사용자 정보 조회 실패:', err);

      if (err.response?.status === 401) {
        console.log('🗑️ 만료된 토큰 제거');
        localStorage.removeItem('jwt');
        sessionStorage.removeItem('jwt');
        isLoggedIn.value = false;
      }
    }
  } else {
    console.log('⚠️ 토큰 없음 - 로그인 필요');
    isLoggedIn.value = false;
  }

  console.log('✅ 사용자 정보 로드 완료:');
  console.log('- currentUser:', currentUser.value);
  console.log('- currentUserId:', currentUserId.value);
  console.log('- isLoggedIn:', isLoggedIn.value);
};

// 채팅 히스토리 로드
const loadChatHistory = async () => {
  try {
    const res = await axios.get(`/api/chat/history/${broadcastIdNum.value}`);
    const history = res.data || [];

    messages.value.push(...history.filter(msg => msg.type !== 'notice'));

    const lastNotice = [...history].reverse().find(msg => msg.type === 'notice');
    if (lastNotice && lastNotice.text.trim()) {
      noticeMessage.value = lastNotice.text.trim();
    }

    console.log('✅ 채팅 히스토리 로드 성공:', history.length, '개 메시지');
  } catch (err) {
    console.error('❌ 채팅 기록 조회 실패:', err);
  }
};

// 메시지 전송 함수
const sendMessage = () => {
  console.log('🔍 sendMessage 호출');
  console.log('- 연결 상태:', connectionStatus.value);
  console.log('- STOMP 연결:', stompClient?.connected);
  console.log('- 사용자 정보:', currentUser.value);
  console.log('- 로그인 상태:', isLoggedIn.value);

  if (!newMessage.value.trim()) {
    console.log('❌ 빈 메시지');
    return;
  }

  if (!isLoggedIn.value) {
    console.log('❌ 로그인 안됨 - 로그인 모달 표시');
    showLoginModal.value = true;
    return;
  }

  // 🌉 브리지 상태 확인 후 사용자 정보 재확인
  if (!currentUser.value) {
    console.log('❌ 사용자 정보 없음 - 브리지 상태 확인');
    userStateBridge.checkSync();

    if (!currentUser.value) {
      console.log('❌ 브리지 후에도 사용자 정보 없음 - 재로드 시도');

      loadUserInfo().then(() => {
        console.log('🔄 사용자 정보 재로드 완료:', currentUser.value);

        if (currentUser.value) {
          console.log('✅ 재로드 성공 - 메시지 전송 재시도');
          sendMessage();
        } else {
          console.log('❌ 재로드 실패 - 로그인 필요');
          showLoginModal.value = true;
        }
      });
      return;
    }
  }

  if (connectionStatus.value !== 'connected' || !stompClient || !stompClient.connected) {
    console.error('❌ WebSocket 연결 안됨 - 상태:', connectionStatus.value);

    if (connectionStatus.value !== 'connecting') {
      console.log('🔄 재연결 시도');
      createWebSocketConnection();
    }

    messages.value.push({
      text: '🔄 채팅 서버에 연결 중입니다. 잠시 후 다시 시도해주세요.',
      systemOnly: true
    });
    return;
  }

  const payload = {
    from: currentUser.value,
    text: newMessage.value.trim(),
    type: 'text',
    broadcastId: broadcastIdNum.value,
    userId: currentUserId.value
  };

  console.log('📤 메시지 전송:', payload);

  try {
    stompClient.publish({
      destination: '/app/sendMessage',
      body: JSON.stringify(payload)
    });

    newMessage.value = '';
    focusInput();

    console.log('✅ 메시지 전송 성공');
  } catch (error) {
    console.error('❌ 메시지 전송 실패:', error);

    messages.value.push({
      text: '❌ 메시지 전송에 실패했습니다. 다시 시도해주세요.',
      systemOnly: true
    });
  }
};

// 공지사항 전송
const sendNotice = (text) => {
  if (connectionStatus.value !== 'connected' || !stompClient || !stompClient.connected) {
    console.error('❌ WebSocket 연결 안됨 - 공지사항 전송 불가');
    return;
  }

  const payload = {
    from: currentUser.value,
    type: 'notice',
    text: text || '',
    broadcastId: broadcastIdNum.value,
    userId: currentUserId.value,
  };

  console.log('📢 공지사항 전송:', payload);

  stompClient.publish({
    destination: '/app/sendMessage',
    body: JSON.stringify(payload),
  });
};

// 스티커 전송
const sendSticker = (stickerKey) => {
  if (!isLoggedIn.value) {
    showLoginModal.value = true;
    return;
  }

  if (connectionStatus.value !== 'connected' || !stompClient || !stompClient.connected) {
    console.error('❌ WebSocket 연결 안됨 - 스티커 전송 불가');
    return;
  }

  const payload = {
    from: currentUser.value,
    type: 'sticker',
    text: stickerKey,
    broadcastId: broadcastIdNum.value,
    userId: currentUserId.value,
  };

  console.log('📤 스티커 전송:', payload);

  stompClient.publish({
    destination: '/app/sendMessage',
    body: JSON.stringify(payload),
  });

  showTools.value = false;
  focusInput();
};

// 나머지 함수들 (기존과 동일)
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
  if (showTools.value) {
    scrollToBottom();
  }
};
const goToLogin = () => router.push('/login');
const handleInputFocus = e => {
  if (!isLoggedIn.value) {
    e.target.blur();
    showLoginModal.value = true;
  }
};
const toggleNotice = () => {
  isNoticeExpanded.value = !isNoticeExpanded.value;
};

// 디버깅 함수
const checkWebSocketConnection = () => {
  console.log('🔍 WebSocket 연결 상태:');
  console.log('- connectionStatus:', connectionStatus.value);
  console.log('- stompClient exists:', !!stompClient);
  console.log('- stompClient.connected:', stompClient?.connected);
  console.log('- isConnecting:', isConnecting.value);
  console.log('- connectionRetries:', connectionRetries.value);
  console.log('- currentUser:', currentUser.value);
  console.log('- currentUserId:', currentUserId.value);
  console.log('- isLoggedIn:', isLoggedIn.value);
  console.log('- broadcastId:', broadcastIdNum.value);

  // 🌉 브리지 상태도 확인
  userStateBridge.checkSync();
};

const reconnect = () => {
  console.log('🔄 수동 재연결 시도');
  connectionRetries.value = 0;
  connectionStatus.value = 'disconnected';
  createWebSocketConnection();
};

// 컴포넌트 마운트
onMounted(async () => {
  console.log('🚀 ChatCommon 마운트 시작 - broadcastId:', broadcastIdNum.value);

  // 🌉 브리지 초기화 확인
  userStateBridge.forceSync();

  // 사용자 정보 로드
  await loadUserInfo();

  // 채팅 히스토리 로드
  await loadChatHistory();

  // WebSocket 연결 (약간 지연)
  setTimeout(() => {
    createWebSocketConnection();
  }, 1000);

  loading.value = false;
  scrollToBottom();

  console.log('✅ 마운트 완료 - 사용자 정보:', currentUser.value);
});

// 컴포넌트 언마운트
onUnmounted(() => {
  console.log('🧹 ChatCommon 언마운트 - 연결 정리');
  connectionStatus.value = 'disconnected';
  if (stompClient) {
    stompClient.deactivate();
  }
});

defineExpose({
  sendNotice,
  checkWebSocketConnection,
  reconnect
});

// 개발자 도구 디버깅
if (typeof window !== 'undefined') {
  window.chatDebug = {
    checkWebSocketConnection,
    reconnect,
    sendMessage,
    stompClient: () => stompClient,
    userState,
    currentUser,
    currentUserId,
    isLoggedIn,
    newMessage,
    connectionStatus,
    isConnecting,
    connectionRetries,
    // 🌉 브리지 디버깅
    bridge: userStateBridge,
    forceSync: () => userStateBridge.forceSync(),
    checkSync: () => userStateBridge.checkSync()
  };
}
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