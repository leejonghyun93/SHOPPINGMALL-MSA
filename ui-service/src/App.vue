<template>
  <Header />
  <router-view />
  <Footer />
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