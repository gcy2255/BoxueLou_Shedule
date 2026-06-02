import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/**
 * 获取所有教室及当前状态
 */
export function getAllRooms() {
  return api.get('/rooms')
}

/**
 * 搜索教室
 */
export function searchRooms(keyword) {
  return api.get('/rooms/search', { params: { keyword } })
}

/**
 * 获取教室详情及课表
 */
export function getRoomDetail(roomId) {
  return api.get(`/rooms/${roomId}`)
}

/**
 * 查询教室指定时间状态
 */
export function getRoomStatus(roomId, week, day, period) {
  return api.get(`/rooms/${roomId}/status`, { params: { week, day, period } })
}

/**
 * 获取当前时间信息
 */
export function getCurrentTime() {
  return api.get('/time')
}
