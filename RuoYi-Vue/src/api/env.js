// ============================================================
//  api/env.js —— 文件协议环境提示（对应原 common.js 的 showEnvBanner）
//  只有直接在浏览器里双击打开 index.html（file://）时才会提示，
//  通过 http(s) 访问（包括 Vite dev / 构建后由后端托管）不会出现。
// ============================================================

export function mountEnvBanner() {
  if (location.protocol !== 'file:') return
  if (document.getElementById('env-banner')) return

  const b = document.createElement('div')
  b.id = 'env-banner'
  b.style.cssText =
    'position:fixed;top:0;left:0;right:0;z-index:9999;' +
    'background:linear-gradient(135deg,#f87171,#f0a8d0);color:#0e1117;' +
    'padding:12px 16px;font-size:14px;font-weight:600;text-align:center;' +
    'box-shadow:0 4px 20px rgba(0,0,0,.3);line-height:1.5;'
  b.textContent =
    '⚠️ 当前是直接打开的本地文件，登录/注册接口无法访问。' +
    '请先启动服务（运行 start.bat 或 java LoginServer），再通过 http://localhost:8080 打开本页。'
  document.body.appendChild(b)
}
