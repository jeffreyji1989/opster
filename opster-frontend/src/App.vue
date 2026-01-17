<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const message = ref('Loading...')
const status = ref('')

const fetchData = async () => {
  try {
    // 使用 vite 代理，请求 /api/hello 会被转发到 http://localhost:8080/api/hello
    const response = await axios.get('/api/hello')
    message.value = response.data.message
    status.value = response.data.status
  } catch (error) {
    console.error('Error fetching data:', error)
    message.value = 'Error fetching data'
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="container">
    <h1>Opster System</h1>
    <div class="card">
      <h2>Backend Response:</h2>
      <p>Message: {{ message }}</p>
      <p>Status: {{ status }}</p>
    </div>
    <button @click="fetchData">Refresh Data</button>
  </div>
</template>

<style scoped>
.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
  font-family: Arial, sans-serif;
  text-align: center;
}

.card {
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 2rem;
  margin: 2rem 0;
}

button {
  background-color: #42b883;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

button:hover {
  background-color: #3aa876;
}
</style>
