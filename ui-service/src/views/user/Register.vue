<template>
  <div class="form-wrapper">
    <h2 class="form-title">회원가입</h2>

    <div class="form-table">
      <!-- 아이디 -->
      <div class="form-row">
        <label>아이디 <span class="required">*</span></label>
        <div class="input-wrap">
          <div class="input-inline">
            <input type="text" v-model="form.userid" placeholder="아이디 입력" />
            <button @click="checkUserIdAvailability" :disabled="!form.userid">중복 확인</button>
          </div>
          <small v-if="userIdMessage" :class="userIdMessageType">{{ userIdMessage }}</small>
        </div>
      </div>

      <!-- 비밀번호 -->
      <div class="form-row">
        <label>비밀번호 <span class="required">*</span></label>
        <input type="password" v-model="form.userPwd" />
      </div>

      <!-- 비밀번호 확인 -->
      <div class="form-row">
        <label>비밀번호 확인 <span class="required">*</span></label>
        <div class="input-wrap">
          <input type="password" v-model="form.confirmPwd" />
          <small v-if="passwordMismatch" class="error">비밀번호가 일치하지 않습니다.</small>
        </div>
      </div>

      <!-- 전화번호 -->
      <div class="form-row">
        <label>전화번호 <span class="required">*</span></label>
        <input type="text" v-model="form.userPhone" placeholder="010-1234-5678" />
      </div>

      <!-- 이름 -->
      <div class="form-row">
        <label>이름 <span class="required">*</span></label>
        <input type="text" v-model="form.userName" />
      </div>

      <!-- 닉네임 -->
      <div class="form-row">
        <label>닉네임</label>
        <input type="text" v-model="form.nickname" />
      </div>

      <!-- 이메일 -->
      <div class="form-row">
        <label>이메일 <span class="required">*</span></label>
        <div class="input-inline">
          <input type="text" v-model="form.emailId" placeholder="example" />
          <span>@</span>
          <select v-model="form.emailDomain">
            <option disabled value="">선택</option>
            <option value="gmail.com">gmail.com</option>
            <option value="naver.com">naver.com</option>
            <option value="daum.net">daum.net</option>
            <option value="hotmail.com">hotmail.com</option>
            <option value="custom">직접 입력</option>
          </select>
        </div>
        <div v-if="form.emailDomain === 'custom'" class="mt-1">
          <input type="text" v-model="form.customDomain" placeholder="직접 입력 (예: mydomain.com)" />
        </div>
      </div>

      <!-- 주소 -->
      <div class="form-row">
        <label>주소</label>
        <div class="input-wrap">
          <div class="input-inline">
            <input type="text" v-model="form.zipcode" placeholder="우편번호" readonly />
            <button @click="execDaumPostcode">주소 검색</button>
          </div>
          <input type="text" v-model="form.userAddress" placeholder="주소" readonly class="mt-1" />
        </div>
      </div>

      <!-- 상세 주소 -->
      <div class="form-row">
        <label>상세주소</label>
        <input type="text" v-model="form.detailAddress" placeholder="상세주소를 입력하세요" />
      </div>

      <!-- 생년월일 -->
      <div class="form-row">
        <label>생년월일</label>
        <input
            type="text"
            v-model="form.birthDate"
            placeholder="YYYY-MM-DD (예: 1990-01-01)"
            pattern="\d{4}-\d{2}-\d{2}"
            maxlength="10"
            @input="formatBirthDate"
        />
      </div>

      <!-- 성별 -->
      <div class="form-row">
        <label>성별</label>
        <div class="gender-toggle">
          <label :class="{ active: form.gender === 'M' }">
            <input type="radio" value="M" v-model="form.gender" /> 남자
          </label>
          <label :class="{ active: form.gender === 'F' }">
            <input type="radio" value="F" v-model="form.gender" /> 여자
          </label>
          <label :class="{ active: form.gender === 'U' }">
            <input type="radio" value="U" v-model="form.gender" /> 선택 안 함
          </label>
        </div>
      </div>
    </div>

    <button class="submit-btn" @click="submitForm" :disabled="!isFormValid">회원가입</button>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const form = reactive({
  userid: '',
  userPwd: '',
  confirmPwd: '',
  userPhone: '',
  userName: '',
  nickname: '',
  emailId: '',
  emailDomain: '',
  customDomain: '',
  zipcode: '',
  userAddress: '',
  detailAddress: '',
  birthDate: '',
  gender: 'U'
});

const userIdMessage = ref('');
const userIdMessageType = ref('');
const userIdChecked = ref(false);

const passwordMismatch = computed(() => {
  return form.confirmPwd && form.userPwd !== form.confirmPwd;
});

// 인증번호 관련 조건 제거된 폼 유효성 검사
const isFormValid = computed(() => {
  return form.userid &&
      form.userPwd &&
      form.confirmPwd &&
      !passwordMismatch.value &&
      form.userPhone &&
      form.userName &&
      form.emailId &&
      form.emailDomain &&
      userIdChecked.value;
});

onMounted(() => {
  loadDaumPostcodeScript();
});

function loadDaumPostcodeScript() {
  if (window.daum && window.daum.Postcode) {
    return;
  }

  const script = document.createElement('script');
  script.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
  document.head.appendChild(script);
}

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

async function checkUserIdAvailability() {
  if (!form.userid) {
    userIdMessage.value = '아이디를 입력해주세요.';
    userIdMessageType.value = 'error';
    return;
  }

  if (form.userid.length < 4) {
    userIdMessage.value = '아이디는 4자 이상 입력해주세요.';
    userIdMessageType.value = 'error';
    userIdChecked.value = false;
    return;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/users/checkUserId?userId=${form.userid}`);

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();

    if (data.available) {
      userIdMessage.value = '사용 가능한 아이디입니다.';
      userIdMessageType.value = 'success';
      userIdChecked.value = true;
    } else {
      userIdMessage.value = '이미 사용 중인 아이디입니다.';
      userIdMessageType.value = 'error';
      userIdChecked.value = false;
    }
  } catch (error) {
    userIdMessage.value = '중복 확인 중 오류가 발생했습니다.';
    userIdMessageType.value = 'error';
    userIdChecked.value = false;
  }
}

function execDaumPostcode() {
  if (!window.daum || !window.daum.Postcode) {
    alert('주소 검색 서비스를 로딩 중입니다. 잠시 후 다시 시도해주세요.');
    return;
  }

  new window.daum.Postcode({
    oncomplete: function(data) {
      let addr = '';

      if (data.userSelectedType === 'R') {
        addr = data.roadAddress;
      } else {
        addr = data.jibunAddress;
      }

      form.zipcode = data.zonecode;
      form.userAddress = addr;

      document.querySelector('input[v-model="form.detailAddress"]')?.focus();
    }
  }).open();
}

function formatBirthDate(event) {
  let value = event.target.value.replace(/\D/g, '');

  if (value.length >= 4) {
    value = value.substring(0, 4) + '-' + value.substring(4);
  }
  if (value.length >= 7) {
    value = value.substring(0, 7) + '-' + value.substring(7);
  }
  if (value.length > 10) {
    value = value.substring(0, 10);
  }

  form.birthDate = value;
}

async function submitForm() {
  if (!isFormValid.value) {
    alert('필수 정보를 모두 입력해주세요.');
    return;
  }

  try {
    const submitData = {
      userId: form.userid,
      password: form.userPwd,
      name: form.userName,
      nickname: form.nickname || null,
      email: `${form.emailId}@${form.emailDomain === 'custom' ? form.customDomain : form.emailDomain}`,
      phone: form.userPhone,
      zipcode: form.zipcode,
      address: form.userAddress + ' ' + (form.detailAddress || ''),
      myAddress: form.detailAddress || null,
      birthDate: form.birthDate,
      gender: form.gender,
      marketingAgree: 'N'
    };

    const response = await fetch(`${API_BASE_URL}/api/users/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(submitData)
    });

    if (response.ok) {
      const result = await response.json();

      alert('회원가입이 완료되었습니다!\n홈페이지로 이동합니다.');

      await router.push('/');

    } else {
      const errorData = await response.text();

      if (response.status === 409) {
        alert('이미 가입된 아이디 또는 이메일입니다.');
      } else if (response.status === 400) {
        alert('입력 정보가 올바르지 않습니다. 다시 확인해주세요.');
      } else {
        alert(`회원가입 중 오류가 발생했습니다. (오류 코드: ${response.status})`);
      }
    }
  } catch (error) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      alert('서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.');
    } else {
      alert('회원가입 중 예상치 못한 오류가 발생했습니다.');
    }
  }
}

function goToLogin() {
  router.push('/login');
}

function goToHome() {
  router.push('/');
}
</script>

<style scoped src="@/assets/css/register.css"></style>