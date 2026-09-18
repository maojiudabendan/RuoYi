/* ============================================================
   common.js —— 公共脚本（所有页面都会先加载它）
   它做两件事：
   1) 自检环境：如果你是直接双击打开网页文件（file:// 开头），
      就弹条提示告诉你正确打开方式；
   2) 提供 apiFetch()：统一的"向服务器发请求"方法，失败提示更友好。
   ============================================================ */

/* 判断当前页面是不是"直接打开的本地文件"
   location.protocol 是网址协议：http: 表示经过了服务器，file: 表示直接打开的硬盘文件 */
function isLocalFile() {
  return location.protocol === 'file:';
}

/* 在页面顶部插入一条醒目的横幅（红色渐变），把提示信息显示给用户 */
function showEnvBanner(msg) {
  if (document.getElementById('env-banner')) return; // 已经有横幅就不重复加
  const b = document.createElement('div'); // 新建一个 div 元素
  b.id = 'env-banner';
  // 直接用 JS 写内联样式（背景、位置、字号等）
  b.style.cssText =
    'position:fixed;top:0;left:0;right:0;z-index:9999;' +
    'background:linear-gradient(135deg,#f87171,#f0a8d0);color:#0e1117;' +
    'padding:12px 16px;font-size:14px;font-weight:600;text-align:center;' +
    'box-shadow:0 4px 20px rgba(0,0,0,.3);line-height:1.5;';
  b.textContent = msg;
  document.body.appendChild(b); // 把横幅挂到 body 上显示
}

/* 如果检测到是 file:// 直接打开的，就提示正确用法 */
if (isLocalFile()) {
  showEnvBanner('⚠️ 当前是直接打开的本地文件，登录/注册接口无法访问。' +
    '请先启动服务（运行 start.bat 或 java LoginServer），再通过 http://localhost:8080/login.html 打开本页。');
}

/* ============================================================
   apiFetch —— 封装 fetch，做两件事：
   - credentials:'include'：让浏览器自动带上登录 Cookie（否则服务器认不出你）
   - 网络连不上时，抛出一条"先启动服务"的可读错误，而不是看不懂的报错
   用法：let res = await apiFetch('/api/login', { method:'POST', body: ... })
   ============================================================ */
async function apiFetch(path, options = {}) {
  // Object.assign 把默认配置和调用时传入的配置合并
  const opts = Object.assign({ credentials: 'include' }, options);
  // 如果传了 body 但没指定 Content-Type，就自动补上 JSON 类型
  if (opts.body && !(opts.headers && opts.headers['Content-Type'])) {
    opts.headers = Object.assign({}, opts.headers, { 'Content-Type': 'application/json' });
  }
  try {
    // fetch 是浏览器内置的"发网络请求"方法，返回 Promise（异步结果）
    return await fetch(path, opts);
  } catch (e) {
    // 走到这里说明网络层面失败（比如服务没启动、网址打不开）
    const err = new Error(
      '无法连接后端服务。请确认已启动服务（运行 start.bat 或 java LoginServer），' +
      '并通过 http://localhost:8080 访问本页面。'
    );
    err.network = true; // 打个标记，方便其它脚本判断是不是网络错误
    throw err; // 把错误继续往外抛，让调用处去处理
  }
}
