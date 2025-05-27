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
      axios.get(`/api/board/${bno}`)
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



<style scoped>
.board-detail {
  display: flex;
  justify-content: center;
  padding: 30px 15px;
  background-color: #f4f4f4;
  min-height: 100vh;
}

.detail-card {
  background: white;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 800px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 600px;
}

.title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #333;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  font-size: 14px;
  color: #777;
  gap: 10px;
  margin-bottom: 25px;
}

.content {
  font-size: 16px;
  height: 55vh;
  color: #444;
  line-height: 1.7;
  white-space: pre-wrap;
  border-top: 1px solid #eee;
  border-bottom: 1px solid #eee;
  padding: 20px 0;

}

.actions-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  margin-bottom: 10px;
}

.actions {
  display: flex;
  gap: 10px;
}

/* 공통 버튼 스타일 */
.action-btn {
  display: inline-block;
  padding: 8px 16px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  border: 1px solid #ccc;
  background-color: #f8f9fa;
  color: #333;
  text-decoration: none;
  text-align: center;
  cursor: pointer;
  min-width: 90px;
  height: 38px;
  line-height: 20px;
  transition: background-color 0.2s, box-shadow 0.2s;
}

/* 목록 버튼 */
.back-btn {
  border: 1px solid #1976d2;
  background-color: #ffffff;
  color: #1976d2;
}

.back-btn:hover {
  background-color: #e3f2fd;
}

/* 수정 버튼 */
.edit-btn {
  background-color: #4caf50;
  color: white;
  box-shadow: 0 2px 6px rgba(76, 175, 80, 0.6);
}

.edit-btn:hover {
  background-color: #388e3c;
  box-shadow: 0 4px 10px rgba(56, 142, 60, 0.8);
}

/* 삭제 버튼 */
.delete-btn {
  background-color: #f44336;
  color: white;
  box-shadow: 0 2px 6px rgba(244, 67, 54, 0.6);
}

.delete-btn:hover {
  background-color: #d32f2f;
  box-shadow: 0 4px 10px rgba(211, 47, 47, 0.8);
}


.back-link {
  align-self: flex-start;
  margin-top: auto;
  text-decoration: none;
  color: #1976d2;
  font-weight: bold;
  font-size: 14px;
}

.back-link:hover {
  text-decoration: underline;
}
</style>
