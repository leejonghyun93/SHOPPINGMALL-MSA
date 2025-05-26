<template>
  <nav class="d-flex justify-content-center mt-4">
    <ul class="pagination">
      <li class="page-item" :class="{ disabled: currentPage === 1 }">
        <button class="page-link" @click="$emit('change-page', currentPage - 1)">이전</button>
      </li>

      <li
          v-for="page in visiblePages"
          :key="page"
          class="page-item"
          :class="{ active: page === currentPage }"
      >
        <button class="page-link" @click="$emit('change-page', page)">
          {{ page }}
        </button>
      </li>

      <li class="page-item" :class="{ disabled: currentPage === totalPages }">
        <button class="page-link" @click="$emit('change-page', currentPage + 1)">다음</button>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  currentPage: Number,
  totalItems: Number,
  pageSize: {
    type: Number,
    default: 10
  },
  maxVisible: {
    type: Number,
    default: 5
  }
})

const totalPages = computed(() => Math.ceil(props.totalItems / props.pageSize))

const visiblePages = computed(() => {
  const half = Math.floor(props.maxVisible / 2)
  let start = Math.max(props.currentPage - half, 1)
  let end = start + props.maxVisible - 1

  if (end > totalPages.value) {
    end = totalPages.value
    start = Math.max(end - props.maxVisible + 1, 1)
  }

  const pages = []
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})
</script>

<style scoped>
.page-item.active .page-link {
  background-color: #0d6efd;
  border-color: #0d6efd;
  color: white;
}
</style>
