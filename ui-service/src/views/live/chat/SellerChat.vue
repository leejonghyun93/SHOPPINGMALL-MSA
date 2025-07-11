<template>
  <div class="chat-wrapper">
    <!-- 상단 헤더 (항상 보임) -->
    <div class="chat-header">
      <div class="chat-title">💬 실시간 채팅</div>
      <div class="chat-header-buttons">
        <div class="pin-wrapper">
          <button class="pin-btn" @click="showModal = true">📌 공지사항 등록</button>
        </div>
        <div class="toggle-wrapper">
          <button class="collapse-toggle-btn" @click="isCollapsed = !isCollapsed">
            {{ isCollapsed ? '펼치기 ▼' : '접기 ▲' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 접기/펼치기 되는 채팅 영역 -->
    <transition name="chat-toggle">
      <div v-show="!isCollapsed" class="chat-area-wrapper">
        <ChatCommon
            ref="chatRef"
            :broadcastId="props.broadcastId"
            class="chat-area"
        />
      </div>
    </transition>

    <!-- 고정 메시지 모달 -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal-content">
        <textarea v-model="noticeText" placeholder="고정할 메시지를 입력하세요" />
        <div class="modal-buttons">
          <button @click="sendNotice">고정</button>
          <button @click="clearNotice">해제</button>
          <button class="close-btn" @click="showModal = false">닫기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ChatCommon from './ChatCommon.vue'

const props = defineProps({
  broadcastId: Number
})

const chatRef = ref(null)
const noticeText = ref('')
const isCollapsed = ref(false)
const showModal = ref(false)

const sendNotice = () => {
  if (!noticeText.value.trim()) return
  chatRef.value.sendNotice(noticeText.value)
  showModal.value = false
}

const clearNotice = () => {
  chatRef.value.sendNotice('')
  noticeText.value = ''
  showModal.value = false
}
</script>

<style scoped>
.chat-wrapper {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
  height: auto; /* 유동적 높이 */
}

/* 헤더는 항상 고정 */
.chat-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background-color: #f9fafb;
  border-bottom: 1px solid #eee;
  position: relative;
}

.chat-title {
  font-size: 15px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-header-buttons {
  display: flex;
  gap: 10px;
}

.pin-wrapper {
  position: absolute;
  right: 100px;
  top: 50%;
  transform: translateY(-50%);
}

.pin-btn,
.collapse-toggle-btn {
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  background-color: #e2e8f0;
  color: #1f2937;
}

.collapse-toggle-btn {
  background-color: transparent;
  color: #3498db;
  font-weight: bold;
}

/* 접히는 영역만 감싸기 */
.chat-area-wrapper {
  height: 385px;
  overflow: hidden;
}

/* ChatCommon 안쪽은 100% 채우기 */
.chat-area {
  height: 100%;
}

/* 접기/펼치기 애니메이션 */
.chat-toggle-enter-active,
.chat-toggle-leave-active {
  transition: max-height 0.3s ease, opacity 0.2s ease;
}
.chat-toggle-enter-from,
.chat-toggle-leave-to {
  max-height: 0;
  opacity: 0;
}
.chat-toggle-enter-to,
.chat-toggle-leave-from {
  max-height: 1000px;
  opacity: 1;
}

/* 모달 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-content {
  background: #fff;
  padding: 20px;
  width: 360px;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modal-content textarea {
  resize: none;
  width: 100%;
  height: 100px;
  padding: 10px;
  font-size: 14px;
  border: 1px solid #ccc;
  border-radius: 6px;
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-buttons button {
  padding: 8px 14px;
  font-size: 13px;
  font-weight: bold;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.modal-buttons button:first-of-type {
  background-color: #facc15;
}

.modal-buttons button:nth-of-type(2) {
  background-color: #f87171;
  color: white;
}

.modal-buttons .close-btn {
  background-color: #eee;
  color: #333;
  margin-left: auto;
}
</style>