import { ElMessage } from 'element-plus'

export async function request(url, options = {}) {
  const isFormData = options.body instanceof FormData
  const response = await fetch(url, {
    ...options,
    headers: {
      ...(options.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {})
    }
  })
  const payload = await response.json().catch(() => null)
  if (!response.ok || !payload?.success) {
    const message = payload?.message || `请求失败（${response.status}）`
    ElMessage.error(message)
    throw new Error(message)
  }
  return payload.data
}

export const api = {
  tags: () => request('/api/tags'),
  createTag: data => request('/api/tags', { method: 'POST', body: JSON.stringify(data) }),
  updateTag: (id, data) => request(`/api/tags/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  setTagStatus: (id, enabled) => request(`/api/tags/${id}/status`, { method: 'PUT', body: JSON.stringify({ enabled }) }),
  rules: tagId => request(`/api/tags/${tagId}/rules`),
  createRule: (tagId, keyword) => request(`/api/tags/${tagId}/rules`, { method: 'POST', body: JSON.stringify({ keyword }) }),
  setRuleStatus: (id, enabled) => request(`/api/tags/rules/${id}/status`, { method: 'PUT', body: JSON.stringify({ enabled }) }),
  searchPersons: data => request('/api/persons/search', { method: 'POST', body: JSON.stringify(data) }),
  savePerson: data => request('/api/persons', { method: 'POST', body: JSON.stringify(data) }),
  personTags: id => request(`/api/persons/${id}/tags`),
  bindTag: (personId, data) => request(`/api/persons/${personId}/tags`, { method: 'POST', body: JSON.stringify(data) }),
  bindTagBatch: data => request('/api/persons/tags/batch', { method: 'POST', body: JSON.stringify(data) }),
  unbindTag: (personId, tagId) => request(`/api/persons/${personId}/tags/${tagId}`, { method: 'DELETE' }),
  reviews: params => request(`/api/persons/tag-bindings/reviews?${new URLSearchParams(params)}`),
  review: (id, data) => request(`/api/persons/tag-bindings/${id}/review`, { method: 'PUT', body: JSON.stringify(data) }),
  deleteRuleResult: id => request(`/api/persons/tag-bindings/${id}`, { method: 'DELETE' }),
  syncBatches: params => request(`/api/sync/batches?${new URLSearchParams(params)}`),
  runSync: (data, token) => request('/internal/sync/persons/incremental', {
    method: 'POST',
    headers: { 'X-Scheduler-Token': token },
    body: JSON.stringify(data)
  }),
  indicators: (enabledOnly = false) => request(`/api/indicators?enabledOnly=${enabledOnly}`),
  createIndicator: data => request('/api/indicators', { method: 'POST', body: JSON.stringify(data) }),
  setIndicatorStatus: (id, enabled) => request(`/api/indicators/${id}/status`, { method: 'PUT', body: JSON.stringify({ enabled }) }),
  addIndicatorOption: (id, data) => request(`/api/indicators/${id}/options`, { method: 'POST', body: JSON.stringify(data) }),
  ruleSets: () => request('/api/rule-sets'),
  createRuleSet: data => request('/api/rule-sets', { method: 'POST', body: JSON.stringify(data) }),
  publishRuleSet: id => request(`/api/rule-sets/${id}/publish`, { method: 'POST' }),
  recalculateRuleSet: (id, batchNo) => request(`/api/rule-sets/${id}/recalculate`, { method: 'POST', body: JSON.stringify({ batchNo }) }),
  importBatches: () => request('/api/imports/persons/batches'),
  importPersons: (batchNo, file) => {
    const body = new FormData()
    body.append('batchNo', batchNo)
    body.append('file', file)
    return request('/api/imports/persons', { method: 'POST', body })
  },
  profileStatus: () => request('/api/profiles/status'),
  rebuildProfiles: () => request('/api/profiles/rebuild', { method: 'POST' }),
  searchProfiles: data => request('/api/profiles/search', { method: 'POST', body: JSON.stringify(data) })
}
