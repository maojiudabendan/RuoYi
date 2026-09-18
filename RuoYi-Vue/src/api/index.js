// ============================================================
//  api/index.js —— 统一的后端请求封装（对应原 common.js 的 apiFetch）
//  约定：
//   - 后端 LoginServer 跑在 8080 端口，开发时由 Vite 代理把 /api 转发过去
//   - credentials: 'include' 让浏览器自动带上登录 Cookie
//   - 网络连不上时，抛出可读的"先启动服务"错误
// ============================================================

export async function apiFetch(path, options = {}) {
  const opts = Object.assign({ credentials: 'include' }, options)
  // 传了 body 但没指定 Content-Type，就自动补上 JSON 类型
  if (opts.body && !(opts.headers && opts.headers['Content-Type'])) {
    opts.headers = Object.assign({}, opts.headers, { 'Content-Type': 'application/json' })
  }
  try {
    return await fetch(path, opts)
  } catch (e) {
    const err = new Error(
      '无法连接后端服务。请确认已启动服务（运行 start.bat 或 java LoginServer），' +
      '并通过 http://localhost:8080 访问本页面。'
    )
    err.network = true
    throw err
  }
}

// 登录：成功返回 { success:true, username, remember }
export async function login(username, password, remember) {
  const r = await apiFetch('/api/login', {
    method: 'POST',
    body: JSON.stringify({ username, password, remember })
  })
  return r.json()
}

// 注册：成功返回 { success:true, username } 并自动登录
export async function register(username, password) {
  const r = await apiFetch('/api/register', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  })
  return r.json()
}

// 校验登录态：返回 { ok:true, data } 或 { ok:false, status }
export async function me() {
  const r = await apiFetch('/api/me')
  if (r.ok) return { ok: true, data: await r.json() }
  return { ok: false, status: r.status }
}

// 退出登录
export async function logout() {
  await apiFetch('/api/logout', { method: 'POST' })
}
