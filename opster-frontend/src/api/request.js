import axios from 'axios'
import { ElMessage } from 'element-plus'

// 从环境变量获取 API 地址
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

const service = axios.create({
  baseURL: apiBaseUrl,
  timeout: 0, // No timeout
  headers: {
    'Content-Type': 'application/json'
  }
})

service.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    console.error('Response:', error.response)

    // 显示详细的错误信息
    let message = error.message || 'Request failed'
    if (error.response && error.response.data) {
      if (typeof error.response.data === 'string') {
        message = error.response.data
      } else if (error.response.data.message) {
        message = error.response.data.message
      }
    } else if (error.response && error.response.status) {
      message = `HTTP ${error.response.status}: ${error.response.statusText}`
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service
