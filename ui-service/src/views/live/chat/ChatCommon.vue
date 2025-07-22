<template>
  <div class="chat-container">
    <!-- 상단 툴바 -->
    <div class="chat-topbar">
      <span class="chat-participant-count">참여중: {{ participantCount }}명</span>
      <button class="notice-toggle-btn" @click="toggleNotice">
        {{ isNoticeExpanded ? '공지 숨기기' : '라이브 공지사항 보기' }}
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
                    <span class="admin-nickname">관리자 {{ msg.from }}</span>
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
        <button @click="toggleTools" class="tools-toggle">스티커</button>
      </div>

      <!-- 스티커 도구창 -->
      <div v-if="showTools" class="chat-tools">
        <div class="tools-header">
          <div class="tab-buttons">
            <button :class="{ active: activeTab === 'bear' }" @click="activeTab = 'bear'">곰</button>
            <button :class="{ active: activeTab === 'rabbit' }" @click="activeTab = 'rabbit'">토끼</button>
          </div>
          <button class="close-tools" @click="showTools = false">닫기</button>
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
import { ref, nextTick, onMounted, onUnmounted, computed, defineExpose } from 'vue';
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

const emit = defineEmits(['host-detected']);

const broadcastIdNum = computed(() => {
  const id = typeof props.broadcastId === 'string' ? parseInt(props.broadcastId) : props.broadcastId;
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
const isBanned = ref(false);
const alertRef = ref(null);
const noticeMessage = ref('');
const isNoticeExpanded = ref(false);

const broadcastStatus = ref('');
const isChatEnabled = ref(false);
const isHost = ref(false);
const participantCount = ref(0);
const hasInitialParticipantSet = ref(false);
const showContextMenu = ref(false);
const contextMenuPos = ref({ x: 0, y: 0 });
const selectedMsg = ref(null);

const isConnecting = ref(false);
const connectionRetries = ref(0);
const maxRetries = 5;
const connectionStatus = ref('disconnected');
const uuid = getOrCreateUUID();

const currentUser = computed(() => {
  return userState.currentUser || userState.name || null;
});

const currentUserId = computed(() => {
  return userState.userId || userState.id || null;
});

const normalize = str => String(str || '').trim();

const isMyMessage = msg => {
  return msg.userId && msg.userId === currentUserId.value;
};

const displayNotice = computed(() => {
  return noticeMessage.value.trim() !== '' ? noticeMessage.value : '등록된 공지사항이 없습니다.';
});

let socket = null;
let stompClient = null;
let chatSubscription = null;

// const getWebSocketUrl = () => {
//   const hostname = window.location.hostname;
//   const port = window.location.port;
//
//   if (hostname === 'localhost' || hostname === '127.0.0.1' || port === '5173') {
//     return 'http://192.168.4.134:8080/ws-chat';  // 로컬 개발용
//   } else {
//     return import.meta.env.VITE_PROD_WS_URL;     // GitHub Secrets에서 가져오기
//   }
// };
// ChatCommon.vue의 createWebSocketConnection 함수 수정

const createWebSocketConnection = () => {
  if (connectionStatus.value === 'connecting') {
    return;
  }

  connectionStatus.value = 'connecting';
  isConnecting.value = true;

  if (stompClient) {
    try {
      stompClient.deactivate();
    } catch (error) {
      // 무시
    }
  }

  //  URL 변경
  // const wsUrl = 'http://3.39.101.58:8081/ws-chat';
  const wsUrl = import.meta.env.VITE_PROD_WS_URL;

  try {
    //  socket 변수 제거하고 직접 webSocketFactory에서 생성
    stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),  //  직접 생성
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      connectHeaders: {
        Authorization: (localStorage.getItem('jwt') || sessionStorage.getItem('jwt')) ?
            `Bearer ${localStorage.getItem('jwt') || sessionStorage.getItem('jwt')}` : '',
        uuid,
        broadcastId: props.broadcastId
      },

      onConnect: (frame) => {
        connectionStatus.value = 'connected';
        isConnecting.value = false;
        connectionRetries.value = 0;

        messages.value.push({
          text: '채팅방에 연결되었습니다.',
          systemOnly: true
        });

        //  채팅 메시지 구독
        chatSubscription = stompClient.subscribe('/topic/public', (msg) => {
          try {
            const received = JSON.parse(msg.body);

            if (received.type === 'notice') {
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
            // 무시
          }
        });

        //  방송 상태 변경 구독
        stompClient.subscribe(`/topic/broadcast/${props.broadcastId}/status`, msg => {
          const payload = JSON.parse(msg.body);
          broadcastStatus.value = payload.status;
          isChatEnabled.value = ['live', 'start', 'stop'].includes(broadcastStatus.value.toLowerCase());
        });

        //  참여자 수 구독
        stompClient.subscribe(`/topic/participants/${props.broadcastId}`, msg => {
          const count = parseInt(msg.body, 10);

          if (!hasInitialParticipantSet.value) {
            return;
          }

          participantCount.value = isNaN(count) ? 0 : count;
        });

        //  채팅 금지 STOMP 채널 구독
        if (userState.userId) {
          stompClient.subscribe(`/topic/ban/${userState.userId}`, msg => {
            const data = JSON.parse(msg.body);
            isBanned.value = data.banned;

            if (data.banned) {
              alertRef.value?.open('⚠ 부적절한 채팅창사용으로 5분간 채팅이 금지되었습니다.');
              setTimeout(() => {
                isBanned.value = false;
              }, data.duration * 1000);
            } else {
              alertRef.value?.open('채팅 금지가 해제되었습니다.');
            }
          });
        }
      },

      onStompError: (frame) => {
        connectionStatus.value = 'failed';
        isConnecting.value = false;

        if (connectionRetries.value < maxRetries) {
          connectionRetries.value++;

          messages.value.push({
            text: `채팅 서버 재연결 시도 중... (${connectionRetries.value}/${maxRetries})`,
            systemOnly: true
          });

          setTimeout(() => {
            createWebSocketConnection();
          }, 5000);
        } else {
          connectionStatus.value = 'failed';
          messages.value.push({
            text: '채팅 서버 연결에 실패했습니다. 페이지를 새로고침 해주세요.',
            systemOnly: true
          });
        }
      },

      onWebSocketError: (error) => {
        connectionStatus.value = 'failed';
        isConnecting.value = false;
      },

      onDisconnect: (frame) => {
        connectionStatus.value = 'disconnected';
        isConnecting.value = false;

        messages.value.push({
          text: '채팅 서버 연결이 끊어졌습니다.',
          systemOnly: true
        });
      }
    });

    stompClient.activate();

  } catch (error) {
    connectionStatus.value = 'failed';
    isConnecting.value = false;
  }
};

const loadUserInfo = async () => {
  userStateBridge.checkSync();

  if (currentUser.value && currentUserId.value) {
    isLoggedIn.value = true;
    return;
  }

  const token = localStorage.getItem('jwt') || sessionStorage.getItem('jwt');

  if (token) {
    try {
      const res = await axios.get('/api/users/profile', {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.data) {
        let userData = res.data;

        if (res.data.success && res.data.data) {
          userData = res.data.data;
        }

        const nickname = userData.nickname || userData.name || userData.username || userData.userName;
        const userId = userData.userId || userData.id || userData.user_id;

        if (nickname) {
          userState.currentUser = nickname;
          userState.userId = userId;
          userState.name = nickname;
          userState.id = userId;
          userState.email = userData.email;
          userState.role = userData.role || 'USER';
          userState.phone = userData.phone;

          isLoggedIn.value = true;
          userStateBridge.forceSync();
        }
      }

      try {
        const hostRes = await axios.get(`/api/members/me/${props.broadcastId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });

        isHost.value = hostRes.data.host === true;

        if (isHost.value) {
          userState.currentUser = '관리자';
        }

        emit('host-detected', isHost.value);

      } catch (hostErr) {
        // 무시
      }

    } catch (err) {
      if (err.response?.status === 401) {
        localStorage.removeItem('jwt');
        sessionStorage.removeItem('jwt');
        isLoggedIn.value = false;
      }
    }
  } else {
    isLoggedIn.value = false;
  }
};

const loadBroadcastStatus = async () => {
  try {
    const res = await axios.get(`/api/broadcasts/${props.broadcastId}/status`);
    broadcastStatus.value = res.data.status;
    isChatEnabled.value = ['live', 'start', 'stop'].includes(broadcastStatus.value.toLowerCase());
  } catch (err) {
    isChatEnabled.value = true;
  }
};

const loadInitialParticipantCount = async () => {
  try {
    const res = await axios.get(`/api/chat/participants/${props.broadcastId}`);
    participantCount.value = res.data.count;
    hasInitialParticipantSet.value = true;
  } catch (e) {
    participantCount.value = 0;
    hasInitialParticipantSet.value = true;
  }
};

const loadChatHistory = async () => {
  try {
    const res = await axios.get(`/api/chat/history/${broadcastIdNum.value}`);
    const history = res.data || [];

    messages.value.push(...history.filter(msg => msg.type !== 'notice'));

    const lastNotice = [...history].reverse().find(msg => msg.type === 'notice');
    if (lastNotice && lastNotice.text.trim()) {
      noticeMessage.value = lastNotice.text.trim();
    }

  } catch (err) {
    // 무시
  }
};

const sendMessage = () => {
  if (!newMessage.value.trim()) {
    return;
  }

  if (!isLoggedIn.value) {
    showLoginModal.value = true;
    return;
  }

  if (!isChatEnabled.value) {
    messages.value.push({
      text: '현재 채팅이 비활성화되어 있습니다.',
      systemOnly: true
    });
    return;
  }
  if (!isLoggedIn.value || isBanned.value || newMessage.value.trim() === '' || !stompClient.connected) {
    if (isBanned.value) {
      alertRef.value?.open('⚠️ 채팅이 금지된 상태입니다. 잠시 후 다시 시도해주세요.');
    }
    return;
  }
  if (!currentUser.value) {
    userStateBridge.checkSync();

    if (!currentUser.value) {
      loadUserInfo().then(() => {
        if (currentUser.value) {
          sendMessage();
        } else {
          showLoginModal.value = true;
        }
      });
      return;
    }
  }

  if (connectionStatus.value !== 'connected' || !stompClient || !stompClient.connected) {
    if (connectionStatus.value !== 'connecting') {
      createWebSocketConnection();
    }

    messages.value.push({
      text: '채팅 서버에 연결 중입니다. 잠시 후 다시 시도해주세요.',
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

  try {
    stompClient.publish({
      destination: '/app/sendMessage',
      body: JSON.stringify(payload)
    });

    newMessage.value = '';
    focusInput();

  } catch (error) {
    messages.value.push({
      text: '메시지 전송에 실패했습니다. 다시 시도해주세요.',
      systemOnly: true
    });
  }
};

const sendNotice = (text) => {
  if (connectionStatus.value !== 'connected' || !stompClient || !stompClient.connected) {
    return;
  }

  const payload = {
    from: currentUser.value,
    type: 'notice',
    text: text || '',
    broadcastId: broadcastIdNum.value,
    userId: currentUserId.value,
  };

  stompClient.publish({
    destination: '/app/sendMessage',
    body: JSON.stringify(payload),
  });
};

const sendSticker = (stickerKey) => {
  if (!isLoggedIn.value) {
    showLoginModal.value = true;
    return;
  }

  if (!isChatEnabled.value) {
    messages.value.push({
      text: '현재 채팅이 비활성화되어 있습니다.',
      systemOnly: true
    });
    return;
  }

  if (connectionStatus.value !== 'connected' || !stompClient || !stompClient.connected) {
    return;
  }

  const payload = {
    from: currentUser.value,
    type: 'sticker',
    text: stickerKey,
    broadcastId: broadcastIdNum.value,
    userId: currentUserId.value,
  };

  stompClient.publish({
    destination: '/app/sendMessage',
    body: JSON.stringify(payload),
  });

  showTools.value = false;
  focusInput();
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
const checkWebSocketConnection = () => {
  userStateBridge.checkSync();
};

const reconnect = () => {
  connectionRetries.value = 0;
  connectionStatus.value = 'disconnected';
  createWebSocketConnection();
};

onMounted(async () => {
  userStateBridge.forceSync();

  await loadUserInfo();
  await loadBroadcastStatus();
  await loadChatHistory();
  await loadInitialParticipantCount();

  setTimeout(() => {
    createWebSocketConnection();
  }, 1000);
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

onUnmounted(() => {
  connectionStatus.value = 'disconnected';

  if (chatSubscription) {
    chatSubscription.unsubscribe();
  }

  if (stompClient) {
    stompClient.deactivate();
  }

  const disconnectId = isLoggedIn.value ? currentUserId.value : uuid;
  if (disconnectId) {
    navigator.sendBeacon(`/api/chat/disconnect/${props.broadcastId}?id=${disconnectId}`);
    document.removeEventListener('click', () => showContextMenu.value = false);
  }
});

defineExpose({
  sendNotice,
  checkWebSocketConnection,
  reconnect
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