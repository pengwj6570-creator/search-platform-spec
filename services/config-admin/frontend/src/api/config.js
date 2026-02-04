import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000
})

// 数据源 API
export const sourceApi = {
  // 获取所有数据源
  getList: () => api.get('/sources'),

  // 获取单个数据源
  get: (sourceId) => api.get(`/sources/${sourceId}`),

  // 创建数据源
  create: (data) => api.post('/sources', data),

  // 更新数据源
  update: (sourceId, data) => api.put(`/sources/${sourceId}`, data),

  // 删除数据源
  delete: (sourceId) => api.delete(`/sources/${sourceId}`)
}

// 搜索对象 API
export const objectApi = {
  // 获取所有搜索对象
  getList: () => api.get('/objects'),

  // 根据appKey获取搜索对象
  getByAppKey: (appKey) => api.get(`/objects?appKey=${appKey}`),

  // 获取单个搜索对象
  get: (objectId) => api.get(`/objects/${objectId}`),

  // 创建搜索对象
  create: (data) => api.post('/objects', data),

  // 更新搜索对象
  update: (objectId, data) => api.put(`/objects/${objectId}`, data),

  // 删除搜索对象
  delete: (objectId) => api.delete(`/objects/${objectId}`)
}

// OpenSearch API
export const searchApi = {
  // 获取集群健康
  getHealth: () => axios.get('http://localhost:9200/_cluster/health'),

  // 获取所有索引
  getIndices: () => axios.get('http://localhost:9200/_cat/indices?v'),

  // 创建索引
  createIndex: (indexName, mapping) => axios.put(`http://localhost:9200/${indexName}`, mapping),

  // 删除索引
  deleteIndex: (indexName) => axios.delete(`http://localhost:9200/${indexName}`),

  // 搜索
  search: (indexName, query) => axios.get(`http://localhost:9200/${indexName}/_search`, { params: { q: query } })
}
