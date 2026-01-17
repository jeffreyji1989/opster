import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 0 // No timeout
})

service.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    ElMessage.error(error.message || 'Request failed')
    return Promise.reject(error)
  }
)

export default service
