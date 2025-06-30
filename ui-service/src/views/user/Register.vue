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

      <!-- 인증 -->
      <div class="form-row">
        <label>인증</label>
        <div class="input-inline">
          <button @click="sendAuthCode" :disabled="!form.userPhone">인증번호 발송</button>
          <input type="text" v-model="authCodeInput" placeholder="인증번호 입력" :disabled="!authCodeSent" />
          <button @click="verifyAuthCode" :disabled="!authCodeInput">인증 확인</button>
        </div>
        <div v-if="authMessage" :class="authMessageType" class="auth-message">{{ authMessage }}</div>
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
        <input type="date" v-model="form.birthDate" />
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

      <!-- 이용약관 동의 -->
      <div class="form-row">
        <label>이용약관</label>
        <div class="terms-wrap">
          <div class="checkbox-inline">
            <input type="checkbox" v-model="form.agreeAll" @change="toggleAllAgreements" />
            <span>전체 동의합니다</span>
          </div>

          <div class="checkbox-inline">
            <input type="checkbox" v-model="form.agreeTermsRequired" />
            <span>이용약관 동의 (필수)</span>
            <button type="button" @click="toggleTerms('terms')">보기</button>
          </div>
          <div v-if="show.terms" class="terms-content">여기에 이용약관 내용이 표시됩니다.</div>

          <div class="checkbox-inline">
            <input type="checkbox" v-model="form.agreePrivacy" />
            <span>개인정보 수집 및 이용 동의 (필수)</span>
            <button type="button" @click="toggleTerms('privacy')">보기</button>
          </div>
          <div v-if="show.privacy" class="terms-content">여기에 개인정보 수집 내용이 표시됩니다.</div>

          <div class="checkbox-inline">
            <input type="checkbox" v-model="form.agreeMarketing" />
            <span>마케팅 수신 동의 (선택)</span>
            <button type="button" @click="toggleTerms('marketing')">보기</button>
          </div>
          <div v-if="show.marketing" class="terms-content">여기에 마케팅 수신 내용이 표시됩니다.</div>
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
  gender: 'U',
  agreeTermsRequired: false,
  agreePrivacy: false,
  agreeMarketing: false,
  agreeAll: false,
});

const show = reactive({
  terms: false,
  privacy: false,
  marketing: false,
});

// 상태 관리
const userIdMessage = ref('');
const userIdMessageType = ref('');
const userIdChecked = ref(false);

const authCodeInput = ref('');
const authCodeSent = ref(false);
const authMessage = ref('');
const authMessageType = ref('');
const isPhoneVerified = ref(false);

// 계산된 속성
const passwordMismatch = computed(() => {
  return form.confirmPwd && form.userPwd !== form.confirmPwd;
});

const isFormValid = computed(() => {
  return form.userid &&
      form.userPwd &&
      form.confirmPwd &&
      !passwordMismatch.value &&
      form.userPhone &&
      form.userName &&
      form.emailId &&
      form.emailDomain &&
      userIdChecked.value &&
      isPhoneVerified.value &&
      form.agreeTermsRequired &&
      form.agreePrivacy;
});

// 카카오 주소 API 로드
onMounted(() => {
  loadDaumPostcodeScript();
});

function loadDaumPostcodeScript() {
  if (window.daum && window.daum.Postcode) {
    return;
  }

  const script = document.createElement('script');
  script.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
  script.onload = () => {
    console.log('Daum Postcode API loaded');
  };
  document.head.appendChild(script);
}

// API 기본 URL 설정
const API_BASE_URL = 'http://localhost:8080';

// 아이디 중복 확인
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
    // API Gateway를 통한 호출
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

// 인증번호 발송
function sendAuthCode() {
  if (!form.userPhone) {
    authMessage.value = '전화번호를 입력해주세요.';
    authMessageType.value = 'error';
    return;
  }

  // 실제 API 호출 로직
  authCodeSent.value = true;
  authMessage.value = '인증번호가 발송되었습니다.';
  authMessageType.value = 'success';

  // 5분 타이머 시작 (선택사항)
  setTimeout(() => {
    if (!isPhoneVerified.value) {
      authCodeSent.value = false;
      authMessage.value = '인증번호가 만료되었습니다. 다시 발송해주세요.';
      authMessageType.value = 'error';
    }
  }, 300000); // 5분
}

// 인증번호 확인
function verifyAuthCode() {
  if (!authCodeInput.value) {
    authMessage.value = '인증번호를 입력해주세요.';
    authMessageType.value = 'error';
    return;
  }

  // 실제 인증 로직 (예시: 123456이 정답)
  if (authCodeInput.value === '123456') {
    isPhoneVerified.value = true;
    authMessage.value = '인증이 완료되었습니다.';
    authMessageType.value = 'success';
  } else {
    authMessage.value = '인증번호가 일치하지 않습니다.';
    authMessageType.value = 'error';
  }
}

// 카카오 주소 검색
function execDaumPostcode() {
  if (!window.daum || !window.daum.Postcode) {
    alert('주소 검색 서비스를 로딩 중입니다. 잠시 후 다시 시도해주세요.');
    return;
  }

  new window.daum.Postcode({
    oncomplete: function(data) {
      // 팝업에서 검색결과 항목을 클릭했을때 실행할 코드를 작성하는 부분.
      let addr = ''; // 주소 변수

      // 사용자가 선택한 주소 타입에 따라 해당 주소 값을 가져온다.
      if (data.userSelectedType === 'R') { // 사용자가 도로명 주소를 선택했을 경우
        addr = data.roadAddress;
      } else { // 사용자가 지번 주소를 선택했을 경우(J)
        addr = data.jibunAddress;
      }

      // 우편번호와 주소 정보를 해당 필드에 넣는다.
      form.zipcode = data.zonecode;
      form.userAddress = addr;

      // 상세주소 입력 칸으로 커서를 이동한다.
      document.querySelector('input[v-model="form.detailAddress"]')?.focus();
    }
  }).open();
}

// 전체 동의 토글
function toggleAllAgreements() {
  const checked = form.agreeAll;
  form.agreeTermsRequired = checked;
  form.agreePrivacy = checked;
  form.agreeMarketing = checked;
}

// 약관 보기 토글
function toggleTerms(type) {
  show[type] = !show[type];
}

// 폼 제출
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
      nickname: form.nickname || null,  // 🔥 닉네임 추가
      email: `${form.emailId}@${form.emailDomain === 'custom' ? form.customDomain : form.emailDomain}`,
      phone: form.userPhone,
      zipcode: form.zipcode,
      address: form.userAddress + ' ' + (form.detailAddress || ''),
      myaddress: form.detailAddress || null,
      birthDate: form.birthDate,
      gender: form.gender,
      marketingAgree: form.agreeMarketing ? 'Y' : 'N'
    };

    console.log('회원가입 요청 데이터:', submitData); // 디버깅용 로그

    // API Gateway를 통한 회원가입 호출
    const response = await fetch(`${API_BASE_URL}/api/users/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(submitData)
    });

    console.log('서버 응답 상태:', response.status); // 디버깅용 로그

    if (response.ok) {
      const result = await response.json();
      console.log('회원가입 성공:', result); //  디버깅용 로그

      // 성공 메시지 표시
      alert('회원가입이 완료되었습니다!\n홈페이지로 이동합니다.');

      // 홈페이지로 리다이렉트
      await router.push('/');

    } else {
      // 서버 에러 응답 처리
      const errorData = await response.text();
      console.error('서버 오류 응답:', errorData);

      if (response.status === 409) {
        alert('이미 가입된 아이디 또는 이메일입니다.');
      } else if (response.status === 400) {
        alert('입력 정보가 올바르지 않습니다. 다시 확인해주세요.');
      } else {
        alert(`회원가입 중 오류가 발생했습니다. (오류 코드: ${response.status})`);
      }
    }
  } catch (error) {
    console.error('회원가입 오류:', error);

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