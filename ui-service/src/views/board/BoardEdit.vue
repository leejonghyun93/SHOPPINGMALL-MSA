<template>
  <div class="board-edit">
    <h2>게시글 수정</h2>

    <div class="form-group">
      <label for="title">제목</label>
      <input type="text" id="title" v-model="board.title" />
    </div>

    <div class="form-group">
      <label for="content">내용</label>
      <textarea id="content" rows="10" v-model="board.content"></textarea>
    </div>

    <div class="actions">
      <button @click="updateBoard">수정 완료</button>
      <button @click="goBack">취소</button>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { useRoute, useRouter } from 'vue-router';

export default {
  name: 'BoardEdit',
  data() {
    return {
      board: {
        bno: null,
        title: '',
        content: '',
      },
    };
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    return { route, router };
  },
  mounted() {
    this.fetchBoard();
  },
  methods: {
    fetchBoard() {
      const bno = this.route.params.bno;

      axios.get(`/api/board/${bno}`)
          .then(response => {
            this.board = response.data;
          })
          .catch(error => {
            console.error('게시글 조회 실패:', error);
            alert('게시글을 불러오지 못했습니다.');
          });
    },
    updateBoard() {
      axios.put(`/api/board/${this.board.bno}`, this.board)
          .then(() => {
            alert('게시글이 수정되었습니다.');
            this.router.push(`/board/${this.board.bno}`);
          })
          .catch(error => {
            console.error('게시글 수정 실패:', error);
            alert('수정에 실패했습니다.');
          });
    },
    goBack() {
      this.router.back();
    },
  },
};
</script>

<style scoped>
.board-edit {
  padding: 20px;
}
.form-group {
  margin-bottom: 15px;
}
label {
  font-weight: bold;
}
input,
textarea {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
}
.actions button {
  margin-right: 10px;
}
</style>
