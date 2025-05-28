<template>
  <div class="container mt-5">
    <h2>회원 정보 수정</h2>

    <div v-if="loading">로딩 중...</div>
    <div v-else>
      <form @submit.prevent="updateUser">
        <div class="form-group">
          <label>아이디</label>
          <input type="text" v-model="user.userid" disabled />
        </div>

        <div class="form-group">
          <label>이메일</label>
          <input type="email" v-model="user.email" required />
        </div>

        <div class="form-group">
          <label>닉네임</label>
          <input type="text" v-model="user.nickname" required />
        </div>

        <div class="form-group">
          <label>전화번호</label>
          <input type="text" v-model="user.phone" />
        </div>

        <div class="form-group">
          <label>주소</label>
          <input type="text" v-model="user.address" />
        </div>

        <div class="form-group">
          <label>상세 주소</label>
          <input type="text" v-model="user.detailAddress" />
        </div>

        <!-- 비밀번호 변경 필드 추가 가능 -->
        <div class="form-group">
          <label>새 비밀번호 (변경 시에만 입력)</label>
          <input type="password" v-model="password" />
        </div>

        <button type="submit" class="btn update-btn">수정 완료</button>
        <button type="button" @click="cancelEdit" class="btn cancel-btn">취소</button>
      </form>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import jwtDecode from 'jwt-decode';

export default {
  data() {
    return {
      user: {},
      password: '',
      loading: true,
      isLogin: false,
      isOwnerOrAdmin: false,
    };
  },
  mounted() {
    this.checkPermissionAndFetch();
  },
  methods: {
    async checkPermissionAndFetch() {
      try {
        const userid = this.$route.params.userid;
        const token = localStorage.getItem('token');

        if (!token) {
          alert('로그인이 필요합니다.');
          this.$router.push('/login');
          return;
        }

        const decoded = jwtDecode(token);
        const currentUserId = decoded.userid || decoded.sub || decoded.id;
        const roles = decoded.role || decoded.roles || [];

        this.isLogin = true;

        // 권한 체크: 본인이거나 관리자(admin)인 경우만 수정 가능
        this.isOwnerOrAdmin =
            String(currentUserId) === String(userid) || roles.includes('ADMIN');

        if (!this.isOwnerOrAdmin) {
          alert('수정 권한이 없습니다.');
          this.$router.push(`/user/${userid}`);
          return;
        }

        // 권한이 있을 때만 사용자 정보 불러오기
        const res = await axios.get(`/api/users/${userid}`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        this.user = res.data;
      } catch (error) {
        console.error(error);
        alert('사용자 정보를 불러올 수 없습니다.');
        this.$router.push('/');
      } finally {
        this.loading = false;
      }
    },

    async updateUser() {
      const token = localStorage.getItem('token');
      if (!token) {
        alert('로그인이 필요합니다.');
        this.$router.push('/login');
        return;
      }

      try {
        // 비밀번호는 필요할 때만 서버에 보냄
        const updateData = { ...this.user };
        if (this.password) updateData.password = this.password;

        await axios.put(`/api/users/${this.user.userid}`, updateData, {
          headers: { Authorization: `Bearer ${token}` },
        });

        alert('회원 정보가 수정되었습니다.');
        this.$router.push(`/user/${this.user.userid}`);
      } catch (error) {
        console.error(error);
        alert('회원 정보 수정에 실패했습니다.');
      }
    },

    cancelEdit() {
      this.$router.push(`/user/${this.user.userid}`);
    },
  },
};
</script>

<style scoped>
.container {
  max-width: 960px;
  min-height: 100vh;
}
.form-group {
  margin-bottom: 1rem;
}

label {
  display: block;
  margin-bottom: 0.4rem;
  font-weight: bold;
}

input[type='text'],
input[type='email'],
input[type='password'] {
  width: 100%;
  padding: 0.5rem;
  font-size: 1rem;
  box-sizing: border-box;
}

.update-btn {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 0.6rem 1.2rem;
  cursor: pointer;
  border-radius: 4px;
  margin-right: 1rem;
}

.update-btn:hover {
  background-color: #388e3c;
}

.cancel-btn {
  background-color: #999;
  color: white;
  border: none;
  padding: 0.6rem 1.2rem;
  cursor: pointer;
  border-radius: 4px;
}

.cancel-btn:hover {
  background-color: #666;
}
</style>
