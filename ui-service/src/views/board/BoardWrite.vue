<template>
  <div class="container mt-5">
    <h2 class="mb-4 text-center">✏️ 글쓰기</h2>

    <form @submit.prevent="submitPost">
      <!-- 제목 -->
      <div class="mb-3">
        <label for="title" class="form-label">제목</label>
        <input
            v-model="title"
            type="text"
            id="title"
            class="form-control"
            required
        />
      </div>

      <!-- 내용 -->
      <div class="mb-3">
        <label for="content" class="form-label">내용</label>
        <textarea
            v-model="content"
            id="content"
            class="form-control"
            rows="8"
            required
        ></textarea>
      </div>

      <!-- 비밀번호 -->
      <div class="mb-3">
        <label for="passwd" class="form-label">비밀번호</label>
        <input
            v-model="passwd"
            type="password"
            id="passwd"
            class="form-control"
            required
        />
      </div>

      <!-- 버튼 -->
      <div class="text-end">
        <button type="submit" class="btn btn-primary me-2">등록</button>
        <router-link to="/board" class="btn btn-secondary">취소</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const title = ref('')
const content = ref('')
const passwd = ref('')

const router = useRouter()

const submitPost = async () => {
  try {
    const token = localStorage.getItem('jwtToken') || sessionStorage.getItem('jwtToken')
    console.log('token:', token)

    const postData = {
      title: title.value,
      content: content.value,
      passwd: passwd.value // 비회원 비밀번호도 함께 전송
    }

    if (!token) {
      alert('로그인이 필요합니다.')
      return
    }

    await axios.post('/api/board/write', postData, {
      headers: {
        Authorization: `Bearer ${token}`
      }
    })

    alert('게시글이 등록되었습니다.')
    router.push('/boardList')
  } catch (e) {
    console.error('글 등록 실패', e)
    alert('글 등록에 실패했습니다.')
  }
}
</script>

<style scoped>
.container {
  max-width: 960px;
  min-height: 100vh;
}
</style>
