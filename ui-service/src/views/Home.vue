<template>

  <div class="home">

    <section class="hero">
      <h2>Welcome to Our Store!</h2>
      <p>Find the best products at the best prices.</p>
      <button @click="goToProducts">Shop Now</button>
    </section>

    <section class="featured-products">
      <h3>Featured Products</h3>
      <div class="products-grid">
        <div class="product-card" v-for="product in featuredProducts" :key="product.id">
          <img :src="product.image" :alt="product.name" />
          <h4>{{ product.name }}</h4>
          <p>{{ formatCurrency(product.price) }}</p>
          <button @click="addToCart(product)">Add to Cart</button>
        </div>
      </div>
    </section>


  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";

import '@/assets/css/home.css'

const router = useRouter();

const featuredProducts = ref([
  // { id: 1, name: "Red T-Shirt", price: 19900, image: "https://via.placeholder.com/150?text=Red+T-Shirt" },
  // { id: 2, name: "Blue Jeans", price: 49900, image: "https://via.placeholder.com/150?text=Blue+Jeans" },
  // { id: 3, name: "Sneakers", price: 79900, image: "https://via.placeholder.com/150?text=Sneakers" },
]);

function goToProducts() {
  router.push("/products");
}

function addToCart(product) {
  alert(`${product.name} added to cart!`);
}

function formatCurrency(value) {
  if (typeof value !== "number") return value;
  return "₩ " + value.toLocaleString();
}
</script>

