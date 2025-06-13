
<!-- App.vue -->
<template>
  <div id="app">
    <div class="app-container">
      <Header />
      <main class="main-content">
        <router-view />
      </main>
      <Footer />
    </div>
  </div>
</template>

<script>
import Header from './components/layout/Header.vue'
import Footer from './components/layout/Footer.vue'

export default {
  components: {
    Header,
    Footer
  }
}
</script>

<style>
#app {
  min-height: 100vh;
}

.app-container {
  max-width: clamp(320px, 100vw, 1400px);
  margin: 0 auto;
  padding: 0 15px;
  box-sizing: border-box;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  width: 100%;
}

@media (max-width: 768px) {
  .app-container {
    padding: 0 10px;
  }
}
</style>