<template>
  <div class="container mt-5">
    <div class="row justify-content-center">
      <div class="col-md-6 col-lg-5">
        <h2 class="text-center mb-4">회원가입</h2>
        <form @submit.prevent="onSubmit" id="registerForm" novalidate>
          <div class="form-group">
            <label for="userid">아이디</label>
            <div class="input-group">
              <input
                  type="text"
                  id="userid"
                  v-model.trim="form.userid"
                  class="form-control"
                  placeholder="아이디를 입력하세요"
                  required
              />
              <div class="input-group-append">
                <button
                    type="button"
                    class="btn btn-outline-secondary"
                    @click="checkUserIdAvailability"
                >
                  중복 확인
                </button>
              </div>
            </div>
            <small v-if="errorMessage" class="text-danger mt-2" style="font-size: 0.9em;">
              {{ errorMessage }}
            </small>
          </div>

          <div class="form-group">
            <label for="userPwd">비밀번호</label>
            <input
                type="password"
                id="userPwd"
                v-model="form.userPwd"
                class="form-control"
                placeholder="비밀번호를 입력하세요"
                required
            />
          </div>

          <div class="form-group">
            <label for="confirmPwd">비밀번호 확인</label>
            <input
                type="password"
                id="confirmPwd"
                v-model="form.confirmPwd"
                class="form-control"
                placeholder="비밀번호 확인을 입력하세요"
                required
            />
          </div>

          <div class="form-group">
            <label for="userName">이름</label>
            <input
                type="text"
                id="userName"
                v-model.trim="form.userName"
                class="form-control"
                placeholder="이름을 입력하세요"
                required
            />
          </div>

          <div class="form-group">
            <label for="age">나이</label>
            <input
                type="number"
                id="age"
                v-model.number="form.age"
                class="form-control"
                placeholder="나이를 입력하세요"
                required
                min="1"
            />
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
                <button
                    type="button"
                    class="btn btn-outline-secondary"
                    @click="execDaumPostcode"
                >
                  주소 찾기
                </button>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label for="detailAddress">나머지 주소</label>
            <input
                type="text"
                id="detailAddress"
                v-model.trim="form.detailAddress"
                class="form-control"
                placeholder="나머지 주소를 입력하세요"
                required
            />
          </div>

          <input type="hidden" id="fullAddress" :value="fullAddress" />

          <div class="form-group">
            <label for="userPhone">전화번호</label>
            <input
                type="text"
                id="userPhone"
                v-model.trim="form.userPhone"
                class="form-control"
                placeholder="전화번호를 입력하세요"
                required
            />
          </div>

          <div class="form-group">
            <label for="userEmail">이메일</label>
            <input
                type="email"
                id="userEmail"
                v-model.trim="form.userEmail"
                class="form-control"
                placeholder="이메일을 입력하세요"
                required
            />
          </div>

          <button type="submit" class="btn btn-primary btn-block">
            회원가입
          </button>
        </form>

        <div class="login-link text-center mt-3">
          <p>
            이미 계정이 있으신가요?
            <router-link to="/login">로그인</router-link>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useRouter } from "vue-router";
import { ref, computed } from "vue";

const router = useRouter();

const form = ref({
  userid: "",
  userPwd: "",
  confirmPwd: "",
  userName: "",
  age: null,
  userAddress: "",
  detailAddress: "",
  userPhone: "",
  userEmail: "",
});

const errorMessage = ref("");
const isUseridChecked = ref(false);

const idPattern = /^.{8,}$/;
const passwordPattern = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*()_+{}\[\]:;<>,.?~\\/-]).{8,}$/;
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const phoneRegex = /^\d{3}-\d{3,4}-\d{4}$/;

const fullAddress = computed(() => `${form.value.userAddress} ${form.value.detailAddress}`.trim());

function checkUserIdAvailability() {
  // 아이디가 입력되지 않은 경우
  if (!form.value.userid) {
    alert("아이디를 입력해주세요.");
    return;
  }

  // 아이디 패턴 검사
  if (!idPattern.test(form.value.userid)) {
    alert("아이디는 최소 8자 이상이어야 합니다.");
    return;
  }

  // 아이디 중복 확인 API 호출
  fetch('/api/users/checkUserid?userid=' + encodeURIComponent(form.value.userid))

      .then(response => {
        if (!response.ok) {
          throw new Error('서버 응답 오류: ' + response.status);
        }
        return response.json();
      })
      .then(data => {
        if (data.available === true) {
          alert("사용 가능한 아이디입니다.");
          isUseridChecked.value = true;
          errorMessage.value = "";
        } else {
          alert("이미 존재하는 아이디입니다.");
          isUseridChecked.value = false;
          errorMessage.value = "이미 존재하는 아이디입니다.";
        }
      })
      .catch(error => {
        console.error("중복 확인 중 오류 발생:", error);
        alert("중복 확인 중 오류가 발생했습니다.");
        isUseridChecked.value = false;
      });
}

function execDaumPostcode() {
  new window.daum.Postcode({
    oncomplete: function (data) {
      form.value.userAddress = data.roadAddress || data.jibunAddress;
    },
  }).open();
}

function onSubmit() {
  if (!form.value.userid) {
    alert("아이디를 입력해주세요.");
    return;
  }
  if (!idPattern.test(form.value.userid)) {
    alert("아이디는 최소 8자 이상이어야 합니다.");
    return;
  }
  if (!isUseridChecked.value) {
    alert("아이디 중복 확인을 해주세요.");
    return;
  }
  if (!form.value.userPwd) {
    alert("비밀번호를 입력해주세요.");
    return;
  }
  if (!passwordPattern.test(form.value.userPwd)) {
    alert("비밀번호는 최소 8자 이상, 영문/숫자/특수문자를 포함해야 합니다.");
    return;
  }
  if (!form.value.confirmPwd) {
    alert("비밀번호 확인을 입력해주세요.");
    return;
  }
  if (form.value.userPwd !== form.value.confirmPwd) {
    alert("비밀번호가 일치하지 않습니다.");
    return;
  }
  if (!form.value.userName) {
    alert("이름을 입력해주세요.");
    return;
  }
  if (!form.value.age || form.value.age <= 0) {
    alert("나이를 올바르게 입력해주세요.");
    return;
  }
  if (!form.value.userAddress) {
    alert("주소를 입력해주세요.");
    return;
  }
  if (!form.value.detailAddress) {
    alert("상세주소를 입력해주세요.");
    return;
  }
  if (!form.value.userPhone) {
    alert("전화번호를 입력해주세요.");
    return;
  }
  if (!phoneRegex.test(form.value.userPhone)) {
    alert("전화번호는 000-0000-0000 형식으로 입력해야 합니다.");
    return;
  }
  if (!form.value.userEmail) {
    alert("이메일을 입력해주세요.");
    return;
  }
  if (!emailRegex.test(form.value.userEmail)) {
    alert("이메일 주소가 유효하지 않습니다.");
    return;
  }

  const userData = {
    userid: form.value.userid,
    passwd: form.value.userPwd,
    name: form.value.userName,
    age: form.value.age,
    address: form.value.userAddress,
    detailAddress: form.value.detailAddress,
    fullAddress: fullAddress.value,
    phone: form.value.userPhone,
    email: form.value.userEmail,
  };

  fetch("/api/users", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(userData),
  })
      .then((res) => {
        if (!res.ok) {
          throw new Error('사용자 등록 실패: ' + res.status);
        }
        return res.json();
      })
      .then((data) => {
        alert("회원가입이 완료되었습니다.");
        router.push('/login');
      })
      .catch((err) => {
        alert("회원가입 중 오류가 발생했습니다. 다시 시도해 주세요.");
        console.error("회원가입 오류:", err);
      });
}

onMounted(() => {
  const script = document.createElement("script");
  script.src = "https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
  script.async = true;
  document.head.appendChild(script);
});
</script>
