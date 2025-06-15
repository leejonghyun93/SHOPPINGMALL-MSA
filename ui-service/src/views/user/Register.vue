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
            <button @click="checkUserIdAvailability">중복 확인</button>
          </div>
          <small class="error">{{ errorMessage }}</small>
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
        <input type="password" v-model="form.confirmPwd" />
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
          <button @click="sendAuthCode">인증번호 발송</button>
          <input type="text" v-model="authCodeInput" placeholder="인증번호 입력" />
          <button @click="verifyAuthCode">인증 확인</button>
        </div>
        <div v-if="isPhoneVerified" class="success">✅ 인증 완료</div>
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
        <div class="input-inline">
          <input type="text" v-model="form.userAddress" readonly />
          <button @click="execDaumPostcode">주소 검색</button>
        </div>
      </div>

      <!-- 상세 주소 -->
      <div class="form-row">
        <label>상세주소</label>
        <input type="text" v-model="form.detailAddress" />
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

    <button class="submit-btn" @click="submitForm">회원가입</button>
  </div>
</template>

<script setup>
import { reactive } from 'vue';

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

const errorMessage = ''; // 예시용 에러 메시지
const authCodeInput = ''; // 인증번호 입력용 reactive 변수
const isPhoneVerified = false; // 전화번호 인증 완료 여부

function toggleAllAgreements() {
  const checked = form.agreeAll;
  form.agreeTermsRequired = checked;
  form.agreePrivacy = checked;
  form.agreeMarketing = checked;
}

function toggleTerms(type) {
  show[type] = !show[type];
}

// 가상 함수 예시
function checkUserIdAvailability() {}
function sendAuthCode() {}
function verifyAuthCode() {}
function execDaumPostcode() {}
function submitForm() {}
</script>

<style scoped>
.form-wrapper {
  max-width: 700px;
  margin: 0 auto;
  padding: 30px 15px;
}

.form-title {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 40px;
}

.form-table {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-row {
  display: grid;
  grid-template-columns: 140px 1fr;
  align-items: center;
  padding-top: 12px;
}

label {
  font-weight: 600;
}

.required {
  color: red;
}

input,
select {
  padding: 8px 10px;
  width: 100%;
  border: 1px solid #ccc;
  border-radius: 6px;
  box-sizing: border-box;
}

.input-wrap {
  display: flex;
  flex-direction: column;
}

.input-inline {
  display: flex;
  gap: 8px;
  align-items: center;
}

.input-inline input[type="text"],
.input-inline input[type="password"],
.input-inline select {
  flex: 1;
  min-width: 0;
}

.input-inline button {
  flex-shrink: 0;
  white-space: nowrap;
  padding: 8px 12px;
}

button {
  padding: 8px 10px;
  background-color: #f0f0f0;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

button:hover {
  background-color: #ddd;
}

.submit-btn {
  background-color: #28a745;
  color: white;
  width: 100%;
  margin-top: 40px;
  padding: 12px;
  font-size: 16px;
}

.submit-btn:hover {
  background-color: #218838;
}

/* 성별 */
.gender-toggle {
  display: flex;
  gap: 12px;
}

.gender-toggle label {
  border: 1px solid #ccc;
  border-radius: 20px;
  padding: 8px 16px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
  font-size: 0.95em;
  background-color: #f9f9f9;
}

.gender-toggle input {
  display: none;
}

.gender-toggle label.active {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
}

/* 약관 */
.terms-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.checkbox-inline {
  display: flex;
  align-items: center;
  flex-wrap: nowrap; /* ✅ 줄바꿈 방지 */
  gap: 8px;
}

.checkbox-inline input[type="checkbox"] {
  width: 10%;
  flex-shrink: 0;
  margin: 0;
}

.checkbox-inline span {
  font-size: 0.95em;
  white-space: nowrap; /* ✅ 줄바꿈 방지 */
}

.checkbox-inline button {
  background: none;
  border: none;
  color: #007bff;
  font-size: 0.85em;
  padding: 0;
  cursor: pointer;
  white-space: nowrap; /* ✅ 버튼도 한 줄 유지 */
}

.checkbox-inline button:hover {
  text-decoration: underline;
}

.terms-content {
  margin-left: 26px;
  background-color: #f9f9f9;
  padding: 10px;
  border-left: 3px solid #ccc;
  font-size: 0.9em;
  color: #333;
  line-height: 1.4;
}

.error {
  color: red;
  font-size: 0.85em;
  margin-top: 5px;
}

.success {
  color: green;
  margin-top: 6px;
  font-size: 0.9em;
}
</style>
