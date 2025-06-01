<template>
  <div class="board-detail">
    <div class="detail-card">
      <h2 class="title">{{ board.title }}</h2>
      <div class="meta">
        <span>✏️ 작성자: {{ board.writerName }}</span>
        <span>📅 작성일: {{ formatDate(board.createdAt) }}</span>
        <span>👁️ 조회수: {{ board.viewCount }}</span>
      </div>
      <div class="content">
        <p>{{ board.content }}</p>
      </div>

      <div class="actions-row">
        <router-link class="action-btn back-btn" to="/boardList">목록</router-link>

        <div v-if="isLogin && isOwner" class="actions">
          <button class="action-btn edit-btn" @click="goToEdit">수정</button>
          <button class="action-btn delete-btn" @click="deleteBoard">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { jwtDecode } from 'jwt-decode';
import '@/assets/css/boardDetail.css';

export default {
  name: 'BoardDetail',
  data() {
    return {
      board: {},
      isOwner: false,
      isLogin: false, // ✅ 로그인 상태 변수 추가
    };
  },
  mounted() {
    this.fetchBoardDetail();
  },
  methods: {
    fetchBoardDetail() {
      const bno = this.$route.params.bno;
      const token = localStorage.getItem('token');
      axios.get(`/api/board/${bno}`, {
        headers: token ? {Authorization: `Bearer ${token}`} : {}
      })
          .then(response => {
            this.board = response.data;

            const token = localStorage.getItem('token');
            if (token) {
              try {
                const decoded = jwtDecode(token);
                const currentUserId = decoded.userId || decoded.sub || decoded.id;
                this.isLogin = true; // ✅ 로그인 상태 true
                this.isOwner = String(currentUserId) === String(this.board.writer);
              } catch (error) {
                console.error('JWT 디코딩 실패:', error);
                this.isLogin = false;
              }
            } else {
              this.isLogin = false;
            }
          })
          .catch(error => {
            console.error('게시글 불러오기 실패:', error);
            alert('게시글을 불러오지 못했습니다.');
          });
    },
    goToEdit() {
      this.$router.push(`/board/edit/${this.board.bno}`);
    },
    deleteBoard() {
      const password = prompt("게시글 작성 시 입력한 비밀번호를 입력해주세요.");
      if (!password) return;

      const token = localStorage.getItem('token');  // 토큰 키를 'token' 으로 통일
      if (!token) {
        alert('로그인이 필요합니다.');
        return;
      }

      axios.delete(`/api/board/${this.board.bno}`, {
        headers: {
          Authorization: `Bearer ${token}`
        },
        params: {
          passwd: password
        }
      })
          .then(() => {
            alert('삭제되었습니다.');
            this.$router.push('/boardList');
          })
          .catch(error => {
            console.error('삭제 실패:', error);
            if (error.response && error.response.data) {
              alert(`삭제 실패: ${error.response.data}`);
            } else {
              alert('삭제에 실패했습니다.');
            }
          });
    },
    formatDate(dateStr) {
      if (!dateStr) return '';
      return new Date(dateStr).toLocaleDateString();
    }
  }
};
</script>

