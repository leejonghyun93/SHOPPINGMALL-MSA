<template>
  <div class="home">
    <div v-if="isAdmin">
      <div class="container-fluid">
        <div class="row">

          <main>
            <ChartBoard />
            <RecentBoardList />
          </main>
        </div>
      </div>
    </div>

    <div v-else>
      <section class="hero">
        <h2>Welcome to Our Board!</h2>
        <p>Find the best products at the best prices.</p>
        <button @click="goToProducts">Shop Now</button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue"
import { useRouter } from "vue-router"


import ChartBoard from '@/components/ChartBoard.vue'
import RecentBoardList from '@/components/RecentBoardList.vue'
import '@/assets/css/home.css'

// 라우터 사용
const router = useRouter()

// 관리자 여부 - 실제 프로젝트에선 로그인 정보 기반으로 설정
const isAdmin = ref(true)  // 테스트 중에는 true/false로 바꿔보세요


</script>
