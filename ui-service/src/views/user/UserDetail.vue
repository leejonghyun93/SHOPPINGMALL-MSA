<template>
  <div class="container mt-5">
    <div class="user-detail">
      <h2>{{ user.name }} 님 정보</h2>

      <div v-if="loading">로딩 중...</div>
      <div v-else>
        <table class="user-table">
          <tr>
            <th>아이디</th>
            <td>{{ user.userid }}</td>
          </tr>
          <tr>
            <th>이메일</th>
            <td>{{ user.email }}</td>
          </tr>
          <tr>
            <th>나이</th>
            <td>{{ user.age }}</td>
          </tr>
          <tr>
            <th>닉네임</th>
            <td>{{ user.nickname }}</td>
          </tr>
          <tr>
            <th>전화번호</th>
            <td>{{ user.phone }}</td>
          </tr>
          <tr>
            <th>주소</th>
            <td>{{ user.address }}</td>
          </tr>
          <tr>
            <th>상세 주소</th>
            <td>{{ user.detailAddress }}</td>
          </tr>
          <tr>
            <th>전체 주소</th>
            <td>{{ user.fullAddress }}</td>
          </tr>
          <tr>
            <th>권한</th>
            <td>{{ user.role }}</td>
          </tr>
          <tr>
            <th>계정 잠김 여부</th>
            <td>{{ user.accountLocked ? '잠김' : '정상' }}</td>
          </tr>
          <tr>
            <th>가입일</th>
            <td>{{ this.formatDate(user.regDate) }}</td>
          </tr>
          <tr>
            <th>최근 로그인</th>
            <td>{{ this.formatDate(user.loginTime) }}</td>
          </tr>
          <tr>
            <th>로그인 실패 횟수</th>
            <td>{{ user.loginFailCount }}</td>
          </tr>
        </table>

        <div v-if="isLogin && isOwner" class="actions">
          <button @click="editProfile" class="btn edit-btn">프로필 수정</button>
          <button @click="deleteUser" class="btn delete-btn">회원 탈퇴</button>
        </div>
        <div v-else>
          <p>다른 사용자의 정보입니다.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import {jwtDecode} from 'jwt-decode';

export default {
  data() {
    return {
      user: {},
      isLogin: false,
      isOwner: false,
      loading: true,
    };
  },
  mounted() {
    this.fetchUserDetail();
  },
  methods: {
    fetchUserDetail() {
      const userid = this.$route.params.userid;
      const token = localStorage.getItem('token');
      axios.get(`/api/users/${userid}`, {
        headers: token ? {Authorization: `Bearer ${token}`} : {}
      })
          .then(response => {
            this.user = response.data;

            if (token) {
              try {
                const decoded = jwtDecode(token);
                const currentUserId = decoded.userid || decoded.sub || decoded.id;
                this.isLogin = true;
                this.isOwner = String(currentUserId) === String(this.user.userid);
              } catch (error) {
                console.error('JWT decoding error:', error);
                this.isLogin = false;
                this.isOwner = false;
              }
            } else {
              this.isLogin = false;
              this.isOwner = false;
            }
          })
          .catch(error => {
            console.error('Failed to fetch user:', error);
            alert('사용자 정보를 불러올 수 없습니다.');
          })
          .finally(() => {
            this.loading = false;
          });
    },
    editProfile() {
      this.$router.push(`/users/edit/${this.user.userid}`);
    },
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      });
    },
    deleteUser() {
      if (!confirm('정말 회원 탈퇴를 진행하시겠습니까?')) return;

      const token = localStorage.getItem('token');
      if (!token) {
        alert('로그인이 필요합니다.');
        return;
      }
      const userid = this.$route.params.userid;
      axios.delete(`/api/users/delete/${userid}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
          .then(() => {
            alert('회원 탈퇴가 완료되었습니다.');

            // 🔽 토큰 삭제 + 전역 상태 초기화
            localStorage.removeItem('token');

            // 🔽 전역 상태도 초기화
            this.$store?.user && (this.$store.user = { id: null, name: null, role: null }); // (store 방식에 따라 다름)
            import("@/stores/userStore").then(({ user }) => {
              user.id = null;
              user.name = null;
              user.role = null;
            });

            // 🔽 홈으로 이동
            this.$router.push('/');
          })
          .catch(error => {
            console.error('회원 탈퇴 실패:', error);
            alert('회원 탈퇴에 실패했습니다.');
          });
    }
  }

}
</script>

<style scoped>

.container {
  max-width: 960px;
  min-height: 100vh;
}

.user-detail {
  max-width: 600px;
  margin: 2rem auto;
  padding: 1rem 2rem;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

h2 {
  margin-bottom: 1rem;
}

p {
  margin: 0.4rem 0;
  font-size: 1.1rem;
}

.actions {
  margin-top: 2rem;
  display: flex;
  gap: 1rem;
}

.btn {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  font-size: 1rem;
  transition: background-color 0.3s ease;
}

.edit-btn {
  background-color: #4caf50;
  color: white;
}

.edit-btn:hover {
  background-color: #388e3c;
}

.delete-btn {
  background-color: #f44336;
  color: white;
}

.delete-btn:hover {
  background-color: #d32f2f;
}
</style>
