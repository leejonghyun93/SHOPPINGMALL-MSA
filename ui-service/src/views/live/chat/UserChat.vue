<template>
  <ChatCommon :broadcastId="broadcastId" role="user" />
</template>

<script setup>
import ChatCommon from './ChatCommon.vue';
const props = defineProps({ broadcastId: Number });
</script>