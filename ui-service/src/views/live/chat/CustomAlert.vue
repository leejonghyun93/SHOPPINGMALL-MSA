<template>
  <teleport to="body">
    <div v-if="visible" class="alert-overlay" role="alertdialog" aria-modal="true">
      <div class="alert-box">
        <p class="alert-message">{{ message }}</p>
        <button class="alert-button" @click="close">확인</button>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref } from 'vue';

const visible = ref(false);
const message = ref('');

const open = (msg) => {
  message.value = msg;
  visible.value = true;
  setTimeout(() => {
    visible.value = false;
  }, 5000); // 5초 후 자동 닫힘
};

const close = () => {
  visible.value = false;
};

defineExpose({ open });
</script>

<style scoped>
.alert-overlay {
  z-index: 99999;
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-box {
  background-color: #fff;
  padding: 24px 32px;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
  max-width: 90%;
  text-align: center;
}

.alert-message {
  font-size: 15px;
  color: #333;
  margin-bottom: 18px;
}

.alert-button {
  padding: 8px 20px;
  font-size: 14px;
  border: none;
  border-radius: 6px;
  background-color: #3b82f6;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.alert-button:hover {
  background-color: #2563eb;
}
</style>