<template>
  <div class="container mt-5">
    <div class="row justify-content-center">
      <div class="col-md-6 col-lg-5">
        <h2 class="text-center mb-4">회원 정보 수정</h2>

        <div v-if="loading" class="text-center">로딩 중...</div>

        <form v-else @submit.prevent="updateUser" novalidate>
          <div class="form-group">
            <label>아이디</label>
            <input type="text" class="form-control" v-model="user.userid" disabled />
          </div>

          <div class="form-group">
            <label>이름</label>
            <input type="text" class="form-control" v-model="user.name" required />
          </div>

          <div class="form-group">
            <label>나이</label>
            <input type="number" class="form-control" v-model.number="user.age" required min="1" />
          </div>

          <div class="form-group">
            <label>이메일</label>
            <input type="email" class="form-control" v-model.trim="user.email" required />
          </div>

          <div class="form-group">
            <label>닉네임</label>
            <input type="text" class="form-control" v-model.trim="user.nickname" />
          </div>

          <div class="form-group">
            <label>전화번호</label>
            <input type="text" class="form-control" v-model.trim="user.phone" />
          </div>

          <div class="form-group">
            <label for="userAddress">주소</label>
            <div class="input-group">
              <input
                  type="text"
                  id="userAddress"
                  v-model="form.userAddress"
                  class="form-control"
                  placeholder="주소를 입력하세요"
                  readonly
                  required
              />
              <div class="input-group-append">
                <button type="button" class="btn btn-outline-secondary" @click="execDaumPostcode">
                  주소 찾기
                </button>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label>상세 주소</label>
            <input type="text" class="form-control" v-model.trim="user.detailAddress" />
          </div>

          <div class="form-group">
            <label>새 비밀번호 (변경 시에만 입력)</label>
            <input type="password" class="form-control" v-model="userPwd" />
          </div>

          <div class="d-flex justify-content-between mt-4">
            <button type="submit" class="btn btn-success">수정 완료</button>
            <button type="button" @click="cancelEdit" class="btn btn-secondary">취소</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { jwtDecode } from 'jwt-decode';

export default {
  data() {
    return {
      user: {},
      form: {
        userAddress: ''
      },
      password: '',
      loading: true,
      isLogin: false,
      isOwnerOrAdmin: false,
    };
  },
  mounted() {
    this.loadDaumPostcodeScript();
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
        this.isOwnerOrAdmin =
            String(currentUserId) === String(userid) || roles.includes('ADMIN');

        if (!this.isOwnerOrAdmin) {
          alert('수정 권한이 없습니다.');
          this.$router.push(`/user/${userid}`);
          return;
        }

        const res = await axios.get(`/api/users/${userid}`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        this.user = res.data;
        this.form.userAddress = res.data.address || '';
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
        const updateData = {
          ...this.user,
          address: this.form.userAddress,
        };

        // 빈 문자열, 공백이면 passwd 필드 아예 제거
        if (!this.userPwd || this.userPwd.trim() === '') {
          // passwd 필드 제거
          if (updateData.hasOwnProperty('passwd')) {
            delete updateData.passwd;
          }
        } else {
          updateData.passwd = this.userPwd;
        }

        await axios.put(`/api/users/edit/${this.user.userid}`, updateData, {
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

    execDaumPostcode() {
      new window.daum.Postcode({
        oncomplete: (data) => {
          this.form.userAddress = data.roadAddress || data.jibunAddress;
        },
      }).open();
    },

    loadDaumPostcodeScript() {
      const script = document.createElement("script");
      script.src = "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
      script.async = true;
      document.head.appendChild(script);
    },
  },
};
</script>

<style scoped>
.edit-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 3rem 1rem;
  background-color: #f9f9f9;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
}

.loading {
  text-align: center;
  font-size: 1.2rem;
  color: #777;
}

.form-box {
  display: flex;
  flex-direction: column;
}

.form-group {
  margin-bottom: 1.2rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #444;
}

input[type="text"],
input[type="email"],
input[type="number"],
input[type="password"] {
  width: 100%;
  padding: 0.6rem;
  border: 1px solid #ccc;
  border-radius: 6px;
  box-sizing: border-box;
  font-size: 1rem;
}

.button-group {
  display: flex;
  justify-content: space-between;
  margin-top: 1.5rem;
}

.btn {
  padding: 0.6rem 1.4rem;
  font-size: 1rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.update-btn {
  background-color: #4caf50;
  color: white;
}

.update-btn:hover {
  background-color: #388e3c;
}

.cancel-btn {
  background-color: #999;
  color: white;
}

.cancel-btn:hover {
  background-color: #666;
}
</style>
