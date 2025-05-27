<!-- src/views/board/BoardList.vue -->
<template>
  <div class="container mt-5" >
    <h2 class="mb-4 text-center">📋 게시판</h2>

    <!-- 검색 & 정렬 -->
    <div class="row mb-3 d-flex justify-content-between">
      <div class="col-md-8 d-flex">
        <input
            v-model="searchKeyword"
            type="text"
            class="form-control me-2"
            placeholder="제목"
            @keyup.enter="onSearchChange"
        />
        <button class="btn btn-secondary btn-search" @click="onSearchChange">검색</button>
      </div>
      <div class="col-md-4 d-flex justify-content-end">
        <select v-model="sortOption" class="form-select w-auto" @change="onSortChange">
          <option value="latest">최신순</option>
          <option value="title">제목순</option>
          <option value="popular">인기순</option>
        </select>
      </div>
    </div>

    <!-- 게시글 수 -->
    <div class="mb-2 text-end text-muted">
      총 {{ totalItems }}건
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
      <tr v-for="(post, index) in boardList" :key="post.id">
        <td>{{ totalItems - ((currentPage - 1) * pageSize) - index }}</td>
        <td class="text-start">
          <router-link :to="`/board/${post.bno}`" class="text-decoration-none">
            {{ post.title }}
          </router-link>
        </td>
        <td>{{ post.writerName }}</td>
        <td>{{ formatDate(post.regDate) }}</td>
        <td>{{ post.viewCount }}</td>
      </tr>
      </tbody>
    </table>

    <Pagination
        :currentPage="currentPage"
        :totalItems="totalItems"
        :pageSize="pageSize"
        @change-page="changePage"
    />

    <!-- 글쓰기 버튼 -->
    <div class="text-end">
      <router-link to="/board/write" class="btn btn-primary">글쓰기</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from "axios"
import { useRouter } from "vue-router"
import Pagination from '@/components/common/Pagination.vue'

const boardList = ref([])
const searchKeyword = ref('')
const sortOption = ref('latest')
const currentPage = ref(1)
const totalItems = ref(0)
const pageSize = ref(10)

const router = useRouter()

// 게시글 목록 불러오기
const fetchBoardList = async () => {
  try {
    const response = await axios.get('/api/board/list', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        searchValue: searchKeyword.value || '',
        sortBy: sortOption.value
      }
    })

    // 서버 응답 구조에 따라 수정 필요
    if (response.data.content) {
      // 페이징 정보가 포함된 경우
      boardList.value = response.data.content
      totalItems.value = response.data.totalElements
    } else if (Array.isArray(response.data)) {
      // 단순 배열인 경우
      boardList.value = response.data
      totalItems.value = response.data.length
    } else {
      // 다른 구조인 경우
      boardList.value = response.data.list || response.data.data || []
      totalItems.value = response.data.total || response.data.totalCount || 0
    }
  } catch (e) {
    console.error('게시글 불러오기 실패', e)
    boardList.value = []
    totalItems.value = 0
  }
}

// 페이지 변경
const changePage = (page) => {
  if (page >= 1 && page <= Math.ceil(totalItems.value / pageSize.value)) {
    currentPage.value = page
    fetchBoardList()
  }
}

// 검색어 변경
const onSearchChange = () => {
  currentPage.value = 1 // 검색 시 첫 페이지로
  fetchBoardList()
}

// 정렬 변경
const onSortChange = () => {
  currentPage.value = 1 // 정렬 변경 시 첫 페이지로
  fetchBoardList()
}

// 날짜 포맷팅
function formatDate(dateStr) {
  if (!dateStr) return '-'

  // 나노초 잘라내기
  const safeDateStr = dateStr.split('.')[0]  // "2025-05-24T18:49:04"
  const date = new Date(safeDateStr)

  if (isNaN(date)) return '-'

  return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ` +
      `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

onMounted(() => {
  fetchBoardList()
})
</script>

<style scoped>
.container {
  max-width: 960px;
  min-height: 100vh;
}

/* 높이 조정 */
input.form-control,
button.btn,
.form-select {
  padding: 0.25rem 0.5rem;
  height: 38px;
  font-size: 0.9rem;
}

/* 정렬 드롭다운 너비 */
.form-select {
  width: 40%;
}
.btn-search {
  width: 12%; /* 원하는 가로 사이즈로 조정 */
}
</style>