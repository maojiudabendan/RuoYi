/* ============================================================
   login.js —— 登录页逻辑
   流程：用户填账号密码 → 点登录 → 把数据发给服务器
        → 成功就跳到主应用 index.html，失败就提示错误
   ============================================================ */

// 先抓出页面上要用到的元素
const loginForm = document.getElementById('login-form'); // 表单
const loginBtn  = document.getElementById('login-btn');  // 登录按钮
const loginErr  = document.getElementById('login-error');// 错误提示区

// 给表单绑定"提交"事件（点按钮或回车都会触发）
loginForm.addEventListener('submit', async (e) => {
  e.preventDefault(); // 阻止浏览器默认的"刷新页面"提交行为，改由我们自己的代码处理
  // 读取输入框的值；trim() 去掉首尾空格，避免多打空格
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;
  const remember = document.getElementById('remember').checked; // 是否勾选"记住我"
  if (!username || !password) { showError('请输入用户名和密码'); return; }

  // 提交中：禁用按钮 + 显示"登录中…"，防止重复点击
  loginBtn.disabled = true;
  loginBtn.querySelector('span').textContent = '登录中…';
  loginErr.textContent = '';

  try {
    // 用公共 apiFetch 向服务器发登录请求
    const r = await apiFetch('/api/login', {
      method: 'POST', // 用 POST 方式提交
      body: JSON.stringify({ username, password, remember }) // 把数据转成 JSON 字符串
    });
    const d = await r.json(); // 服务器返回的内容（如 {success:true,...}）
    if (d.success) {
      location.href = 'index.html';   // 登录成功 → 跳到主应用
    } else {
      // success 为 false，比如用户名密码错
      showError(d.message || '登录失败');
      loginBtn.disabled = false; // 恢复按钮
      loginBtn.querySelector('span').textContent = '登 录';
    }
  } catch (err) {
    // 网络异常（服务没起等），显示友好提示
    showError(err.message || '网络错误，请确认服务已启动');
    loginBtn.disabled = false;
    loginBtn.querySelector('span').textContent = '登 录';
  }
});

// 把错误信息写到页面上，并重新触发抖动动画引起注意
function showError(msg) {
  loginErr.textContent = msg;
  loginErr.style.animation = 'none';
  void loginErr.offsetWidth; // 强制重排，这样动画能重新播放
  loginErr.style.animation = '';
}
