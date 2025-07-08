<template>
  <div class="chat-test-page">
    <h2>🔧 채팅 테스트 페이지 (broadcastId: {{ broadcastId }} / role: {{ role }})</h2>

    <!-- 역할에 따라 해당 컴포넌트만 렌더링 -->
    <SellerChat v-if="role === 'seller'" :broadcastId="broadcastId" />
    <UserChat v-else :broadcastId="broadcastId" />
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router';
import SellerChat from './SellerChat.vue';
import UserChat from './UserChat.vue';

const route = useRoute();
const broadcastId = Number(route.params.broadcastId);
const role = route.params.role || 'user'; // 기본값 'user'
</script>

<style scoped>
.chat-test-page {
  padding: 20px;
}
</style>