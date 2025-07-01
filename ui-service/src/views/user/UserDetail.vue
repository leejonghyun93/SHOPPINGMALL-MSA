template>
  <div class="container mt-5">
    <div class="member-detail">
      <h2>{{ member.name }} 님 정보</h2>

      <div v-if="loading">로딩 중...</div>
      <div v-else>
        <table class="member-table">
          <tbody>
          <tr>
            <th>아이디</th>
            <td>{{ member.userid }}</td>
          </tr>
          <tr>
            <th>이메일</th>
            <td>{{ member.email }}</td>
          </tr>
          <tr>
            <th>나이</th>
            <td>{{ member.age }}</td>
          </tr>
          <tr>
            <th>닉네임</th>
            <td>{{ member.nickname }}</td>
          </tr>
          <tr>
            <th>전화번호</th>
            <td>{{ member.phone }}</td>
          </tr>
          <tr>
            <th>주소</th>
            <td>{{ member.address }}</td>
          </tr>
          <tr>
            <th>상세 주소</th>
            <td>{{ member.detailAddress }}</td>
          </tr>
          <tr>
            <th>전체 주소</th>
            <td>{{ member.fullAddress }}</td>
          </tr>
          <tr>
            <th>권한</th>
            <td>{{ member.role }}</td>
          </tr>
          <tr>
            <th>계정 잠김 여부</th>
            <td>{{ member.accountLocked ? '잠김' : '정상' }}</td>
          </tr>
          <tr>
            <th>가입일</th>
            <td>{{ this.formatDate(member.regDate) }}</td>
          </tr>
          <tr>
            <th>최근 로그인</th>
            <td>{{ this.formatDate(member.loginTime) }}</td>
          </tr>
          <tr>
            <th>로그인 실패 횟수</th>
            <td>{{ member.loginFailCount }}</td>
          </tr>
          </tbody>
        </table>

        <div v-if="isLogin && isOwner" class="actions">
          <button @click="editProfile" class="btn edit-btn">프로필 수정</button>
          <button @click="deleteUser" class="btn delete-btn">회원 탈퇴</button>
          <button @click="goToBoardList" class="btn list-btn">목록</button>
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
import '@/assets/css/userDetail.css';
export default {
  data() {
    return {
      member: {},
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
            this.member = response.data;

            if (token) {
              try {
                const decoded = jwtDecode(token);
                const currentUserId = decoded.userid || decoded.sub || decoded.id;
                this.isLogin = true;
                this.isOwner = String(currentUserId) === String(this.member.userid);
              } catch (error) {
                this.isLogin = false;
                this.isOwner = false;
              }
            } else {
              this.isLogin = false;
              this.isOwner = false;
            }
          })
          .catch(error => {
            alert('사용자 정보를 불러올 수 없습니다.');
          })
          .finally(() => {
            this.loading = false;
          });
    },
    editProfile() {
      this.$router.push(`/users/edit/${this.member.userid}`);
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
    goToBoardList() {
      this.$router.push('/members');
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

            localStorage.removeItem('token');

            this.$store?.member && (this.$store.member = { id: null, name: null, role: null });
            import("@/stores/userStore").then(({ member }) => {
              member.id = null;
              member.name = null;
              member.role = null;
            });

            this.$router.push('/');
          })
          .catch(error => {
            alert('회원 탈퇴에 실패했습니다.');
          });
    }
  }

}
</script>