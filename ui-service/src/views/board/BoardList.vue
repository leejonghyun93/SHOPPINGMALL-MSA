<!-- src/views/board/BoardList.vue -->
<template>
  <div class="container mt-5" >
    <h2 class="mb-4 text-center">📋 게시판</h2>

    <!-- 검색 & 정렬 -->
    <div class="row mb-3 d-flex justify-content-between">
      <div class="col-md-8">
        <input
            v-model="searchKeyword"
            type="text"
            class="form-control"
            placeholder="제목, 작성자로 검색"
        />
      </div>
      <div class="col-md-4 d-flex justify-content-end">
        <select v-model="sortOption" class="form-select w-auto">
          <option value="latest">최신순</option>
          <option value="title">제목순</option>
          <option value="popular">인기순</option>
        </select>
      </div>
    </div>

    <!-- 게시글 수 -->
    <div class="mb-2 text-end text-muted">
      총 {{ filteredList.length }}건
    </div>

    <!-- 테이블 -->
    <table class="table table-striped table-hover text-center">
      <thead class="table-dark">
      <tr>
        <th>번호</th>
        <th>제목</th>
        <th>작성자</th>
        <th>등록일</th>
        <th>조회수</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="(post, index) in sortedList" :key="post.id">
        <td>{{ sortedList.length - index }}</td>
        <td class="text-start">
          <router-link :to="`/board/${post.id}`" class="text-decoration-none">
            {{ post.title }}
          </router-link>
        </td>
        <td>{{ post.writer }}</td>
        <td>{{ formatDate(post.createdAt) }}</td>
        <td>{{ post.viewCount }}</td>
      </tr>
      </tbody>
    </table>

    <!-- 글쓰기 버튼 -->
    <div class="text-end">
      <router-link to="/board/write" class="btn btn-primary">글쓰기</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from "axios"; // 실제 axios 인스턴스
import { useRouter } from "vue-router";

const boardList = ref([])
const searchKeyword = ref('')
const sortOption = ref('latest')

onMounted(async () => {
  try {
    const response = await axios.get('/api/board/list', {
      params: {
        page: 1,
        size: 10,
        searchValue: searchKeyword.value || ''
      }
    })
    boardList.value = response.data
  } catch (e) {
    console.error('게시글 불러오기 실패', e)
  }
})

const filteredList = computed(() => {
  return boardList.value.filter(post =>
      post.title.includes(searchKeyword.value) ||
      post.writer.includes(searchKeyword.value)
  )
})

const sortedList = computed(() => {
  const list = [...filteredList.value]
  switch (sortOption.value) {
    case 'title':
      return list.sort((a, b) => a.title.localeCompare(b.title))
    case 'popular':
      return list.sort((a, b) => b.viewCount - a.viewCount)
    default:
      return list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  }
})

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString()
}
</script>

<style scoped>
.container {
  max-width: 960px;
  min-height: 100vh;
}

.form-select{
  width: 40%;
}
</style>
