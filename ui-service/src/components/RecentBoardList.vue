<template>
  <div>
    <h2>최신 글</h2>
    <div class="table-responsive">
      <table class="table table-striped table-sm">
        <thead>
        <tr>
          <th>번호</th>
          <th>제목</th>
          <th>내용</th>
          <th>작성자</th>
          <th>작성일</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(board, index) in boardList" :key="board.bno">
          <td>{{ index + 1 }}</td>
          <td>{{ board.title }}</td>
          <td>{{ board.content }}</td>
          <td>{{ board.writerName }}</td>
          <td>{{ formatDate(board.regDate) }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
const boardList = ref([])

onMounted(async () => {
  const token = localStorage.getItem('token')
  try {
    const res = await axios.get('/api/board/recentList', {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })
    console.log('받은 데이터:', res.data)
    boardList.value = Array.isArray(res.data) ? res.data : res.data.list || []
  } catch (e) {
    console.error('게시글 불러오기 실패:', e)
    boardList.value = []
  }
})

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString()
}
</script>
