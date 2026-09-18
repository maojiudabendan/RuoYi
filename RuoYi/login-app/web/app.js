/* ============================================================
   app.js —— 登录后空壳页逻辑
   当前页面很简单，只做三件事：
   1) 会话校验：进页面先问服务器"我登录了没"，没登录就踢回登录页
   2) 动态欢迎：根据用户名生成头像、按当前时段显示"早上好/下午好…"
   3) 退出登录：点按钮通知服务器清除登录态，然后跳回登录页
   这页是"受保护"的：没登录会被自动跳走
   ============================================================ */

// 抓出主容器（带 hidden，初始隐藏）
const appView = document.getElementById('app-view');

/* ---------- 会话校验：进页面先确认是否已登录 ---------- */
async function checkSession() {
  try {
    // 请求 /api/me，服务器返回用户名或 401（未登录）
    const r = await apiFetch('/api/me');
    if (r.ok) {
      const d = await r.json(); // 把返回文本转成 JS 对象
      enterApp(d.username);     // 已登录 → 进入欢迎页
    } else {
      location.href = 'login.html'; // 没登录 → 跳回登录页
    }
  } catch (_) {
    location.href = 'login.html'; // 网络异常也跳回登录页
  }
}

/* ---------- 填充动态欢迎信息 ---------- */
function applyIdentity(username) {
  // 取用户名首字母做大写头像
  const initial = (username || 'U').charAt(0).toUpperCase();
  // 根据用户名算一个色相，让每个用户头像颜色不一样
  const hue = (Math.abs(hashStr(username)) % 360);
  const grad = `linear-gradient(135deg, hsl(${hue} 70% 65%), hsl(${(hue + 55) % 360} 70% 58%))`;
  const avatar = document.getElementById('welcome-avatar');
  if (avatar) { avatar.textContent = initial; avatar.style.background = grad; avatar.style.color = '#0e1117'; }
  document.getElementById('welcome-user').textContent = username;

  // 按当前小时选一句问候语
  const h = new Date().getHours();
  const greet = h < 6 ? '夜深了' : h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好';
  document.getElementById('welcome-greet').textContent = greet + '，' + username;
}

// 把字符串转成一个数字（简单哈希），用来生成头像颜色
function hashStr(s) {
  let h = 0;
  for (let i = 0; i < (s || '').length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return h;
}

/* ---------- 显示欢迎页 ---------- */
function enterApp(username) {
  applyIdentity(username);
  appView.classList.remove('hidden'); // 去掉隐藏
  // 强制浏览器重排后再加 show，触发淡入动画
  requestAnimationFrame(() => {
    setTimeout(() => appView.classList.add('show'), 60);
  });
}

/* ---------- 退出登录 ---------- */
document.getElementById('logout-btn').addEventListener('click', async () => {
  await apiFetch('/api/logout', { method: 'POST' }); // 通知服务器清掉登录态
  location.href = 'login.html'; // 跳回登录页
});

/* ---------- 启动：页面加载先校验登录态 ---------- */
checkSession();
