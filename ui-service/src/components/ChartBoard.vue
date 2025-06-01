<template>
  <div class="custom-container">
    <RecentPostList />
    <div class="charts-container">
      <div class="chartGroup" v-for="(chart, index) in charts" :key="index">
        <canvas :id="chart.id" style="width: 100%; height: 300px;"></canvas>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import Chart from 'chart.js/auto'

const charts = [
  { id: 'chart1', endpoint: '/api/board/chart/recent', type: 'bar' },
  { id: 'chart2', endpoint: '/api/board/chart/popular', type: 'doughnut' },
  // chart3는 예시이므로 필요 시 추가하세요
]

onMounted(() => {
  const token = localStorage.getItem('token')

  charts.forEach(chart => {
    fetch(chart.endpoint, {
      method: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token,
        'Content-Type': 'application/json'
      }
    })
        .then(res => {
          if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`)
          return res.json()
        })
        .then(data => {
          console.log(chart.id, data)
          const canvas = document.getElementById(chart.id)
          console.log('canvas element:', canvas)  // 캔버스 존재 여부 확인
          if (!canvas) {
            console.error(`Canvas element with id '${chart.id}' not found.`)
            return
          }

          let config = {}

          if (chart.id === 'chart1') {
            // 최신글 차트 - 조회수 필드명 viewCount 사용
            config = {
              type: 'bar',
              data: {
                labels: data.map(item => item.title),
                datasets: [{
                  label: '조회수',
                  data: data.map(item => item.viewCount),
                  backgroundColor: 'rgba(54, 162, 235, 0.5)'
                }]
              },
              options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                  y: { beginAtZero: true }
                }
              }
            }
          } else if (chart.id === 'chart2') {
            // 인기글 차트 - 동일하게 viewCount 사용
            config = {
              type: 'doughnut',
              data: {
                labels: data.map(item => item.title),
                datasets: [{
                  data: data.map(item => item.viewCount),  // 여기 수정
                  backgroundColor: [
                    'rgba(255, 99, 132, 0.6)',
                    'rgba(54, 162, 235, 0.6)',
                    'rgba(255, 206, 86, 0.6)',
                    'rgba(75, 192, 192, 0.6)',
                    'rgba(153, 102, 255, 0.6)'
                  ]
                }]
              },
              options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'right' } }
              }
            }
          }

          const ctx = canvas.getContext('2d')
          new Chart(ctx, config)
        })
        .catch(error => {
          console.error(`${chart.id} fetch error:`, error)
        })
  })
})
</script>

<style scoped>
.custom-container {
  max-width: 100%;
  overflow-x: hidden;
  padding: 0 15px;
  margin: 0 auto;
}

.charts-container {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  width: 100%;
  margin: 0;
  padding: 0;
  gap: 20px;
  justify-content: flex-start;
}

.chartGroup {
  border: 1px solid #ccc;
  border-radius: 8px;
  padding: 15px;
  background-color: #fff;
  flex: 0 0 calc(48% - 2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
}

.chartGroup canvas {
  width: 100% !important;
  height: 300px !important;
  max-width: 100%;
}

@media (max-width: 768px) {
  .charts-container {
    flex-direction: column;
  }
  .chartGroup {
    flex: 0 0 100%;
  }
}
</style>