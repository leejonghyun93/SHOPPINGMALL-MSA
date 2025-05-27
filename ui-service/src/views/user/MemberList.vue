<template>
  <div class="container mt-5">
    <h2 class="mb-4 text-center">👤 회원관리</h2>

    <!-- 검색 & 정렬 -->
    <div class="row mb-3 d-flex justify-content-between">
      <div class="col-md-8 d-flex">
        <input
            v-model="searchKeyword"
            type="text"
            class="form-control me-2"
            placeholder="이름 또는 아이디"
            @keyup.enter="onSearchChange"
        />
        <button class="btn btn-secondary btn-search" @click="onSearchChange">검색</button>
      </div>
      <div class="col-md-4 d-flex justify-content-end">
        <select v-model="sortOption" class="form-select w-auto" @change="onSortChange">
          <option value="name">이름순</option>
          <option value="userid">아이디순</option>
          <option value="loginTime">최근 로그인순</option>
        </select>
      </div>
    </div>

    <!-- 총 회원 수 -->
    <div class="mb-2 text-end text-muted">
      총 {{ totalItems }}명
    </div>

    <!-- 회원 테이블 -->
    <table class="table table-striped table-hover text-center">
      <thead class="table-dark">
      <tr>
        <th>
          <input
              type="checkbox"
              v-model="allSelected"
              @change="toggleSelectAll"
              aria-label="전체 선택"
          />
        </th>
        <th>번호</th>
        <th>아이디</th>
        <th>이름</th>
        <th>이메일</th>
        <th>주소</th>
        <th>최근 로그인</th>
      </tr>
      </thead>
      <tbody>
      <tr v-if="memberList.length === 0">
        <td colspan="7">회원 목록이 존재하지 않습니다.</td>
      </tr>
      <tr v-for="(user, index) in memberList" :key="user.userid">
        <td>
          <input
              type="checkbox"
              :value="user.userid"
              v-model="selectedUsers"
              :aria-label="`${user.name} 선택`"
          />
        </td>
        <td>{{ totalItems - ((currentPage - 1) * pageSize) - index }}</td>
        <td>{{ user.userid }}</td>
        <td>
          <a href="javascript:void(0)" @click="loadUserDetail(user.userid)">
            {{ user.name }}
          </a>
        </td>
        <td>{{ user.email }}</td>
        <td>{{ user.fullAddress || '-' }}</td>
        <td>{{ formatDateTime(user.loginTime) }}</td>
      </tr>
      </tbody>
    </table>

    <!-- 페이지네이션 -->
    <Pagination
        :currentPage="currentPage"
        :totalItems="totalItems"
        :pageSize="pageSize"
        @change-page="changePage"
    />

    <!-- 로그인 잠금 해제 버튼 -->
    <div class="text-end mt-3">
      <button
          class="btn btn-outline-danger"
          :disabled="selectedUsers.length === 0"
          @click="onUnlockLogin"
      >
        선택 회원 로그인 잠금 해제
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import axios from 'axios'
import Pagination from '@/components/common/Pagination.vue'

const memberList = ref([])
const searchKeyword = ref('')
const sortOption = ref('name')
const currentPage = ref(1)
const totalItems = ref(0)
const pageSize = ref(10)

const selectedUsers = ref([])
const allSelected = ref(false)

// 회원 목록 불러오기
const fetchMemberList = async () => {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/users/list', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        searchValue: searchKeyword.value,
        sortBy: sortOption.value,
      },
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })
    memberList.value = response.data.content || []
    totalItems.value = response.data.totalElements || 0

    // 목록 새로 받아오면 선택 초기화
    selectedUsers.value = []
    allSelected.value = false
  } catch (e) {
    console.error('회원 목록 불러오기 실패', e)
    memberList.value = []
    totalItems.value = 0
  }
}

// 전체 선택 토글
const toggleSelectAll = () => {
  if (allSelected.value) {
    selectedUsers.value = memberList.value.map(user => user.userid)
  } else {
    selectedUsers.value = []
  }
}

// 개별 선택에 따라 전체 선택 체크박스 상태 조절
watch(selectedUsers, (newVal) => {
  allSelected.value = newVal.length === memberList.value.length && memberList.value.length > 0
})

// 페이지 변경
const changePage = (page) => {
  currentPage.value = page
  fetchMemberList()
}

// 검색
const onSearchChange = () => {
  currentPage.value = 1
  fetchMemberList()
}

// 정렬 변경
const onSortChange = () => {
  currentPage.value = 1
  fetchMemberList()
}

// 로그인 잠금 해제 API 호출 - 선택된 회원들만
const onUnlockLogin = async () => {
  if (selectedUsers.value.length === 0) {
    alert('잠금 해제할 회원을 선택해주세요.')
    return
  }

  try {
    const token = localStorage.getItem('token')
    await axios.post('/api/loginCheckOut', { userIds: selectedUsers.value }, {
      headers: {
        Authorization: `Bearer ${token}`,
      }
    })
    alert('선택된 회원들의 로그인 잠금이 해제되었습니다.')
    fetchMemberList()
  } catch (error) {
    alert('로그인 잠금 해제에 실패했습니다.')
    console.error(error)
  }
}

// 유저 상세 보기
const loadUserDetail = (userid) => {
  alert(`유저 상세정보: ${userid}`)
}

// 날짜 포맷
const formatDateTime = (dateTimeStr) => {
  const date = new Date(dateTimeStr)
  return isNaN(date.getTime()) ? '-' : date.toLocaleString()
}

// 초기 데이터 호출
onMounted(() => {
  fetchMemberList()
})
</script>

<style scoped>
.container {
  max-width: 960px;
  min-height: 100vh;
}

/* 높이 조정 */
input.form-control,
button.btn,
.form-select {
  padding: 0.25rem 0.5rem;
  height: 38px;
  font-size: 0.9rem;
}

/* 정렬 드롭다운 너비 */
.form-select {
  width: 40%;
}

/* 검색 버튼 너비 */
.btn-search {
  width: 12%;
}
</style>
