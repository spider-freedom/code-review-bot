import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// TODO: Add auth interceptor when authentication is implemented
// http.interceptors.request.use(config => {
//   const token = localStorage.getItem('token')
//   if (token) config.headers.Authorization = `Bearer ${token}`
//   return config
// })

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  },
)

export default http
