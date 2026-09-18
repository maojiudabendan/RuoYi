/* ============================================================
   register.js —— 注册页逻辑
   流程：填用户名/密码/确认密码 → 点注册 → 发给服务器
        → 成功（服务器已自动登录）→ 跳到主应用 index.html
   注意：密码和确认密码要一致，且至少 6 位
   ============================================================ */

// 抓出页面元素
const registerForm = document.getElementById('register-form');
const registerBtn  = document.getElementById('register-btn');
const registerErr  = document.getElementById('register-error');

// 给注册表单绑定提交事件
registerForm.addEventListener('submit', async (e) => {
  e.preventDefault(); // 阻止默认提交刷新
  const username = document.getElementById('reg-username').value.trim();
  const password = document.getElementById('reg-password').value;
  const confirm  = document.getElementById('reg-confirm').value;
  // 前端先简单校验一遍，省得往服务器发无意义请求
  if (!username || !password) { regError('请填写用户名和密码'); return; }
  if (password !== confirm)   { regError('两次输入的密码不一致'); return; }

  // 提交中：禁用按钮 + 显示"注册中…"
  registerBtn.disabled = true;
  registerBtn.querySelector('span').textContent = '注册中…';
  registerErr.textContent = '';

  try {
    const r = await apiFetch('/api/register', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    const d = await r.json();
    if (d.success) {
      location.href = 'index.html';   // 注册成功且已自动登录 → 进主应用
    } else {
      // 比如用户名已存在、格式不合法等
      regError(d.message || '注册失败');
      registerBtn.disabled = false;
      registerBtn.querySelector('span').textContent = '注 册';
    }
  } catch (err) {
    // 网络异常
    regError(err.message || '网络错误，请确认服务已启动');
    registerBtn.disabled = false;
    registerBtn.querySelector('span').textContent = '注 册';
  }
});

// 显示错误提示并重播抖动动画
function regError(m) {
  registerErr.textContent = m;
  registerErr.style.animation = 'none';
  void registerErr.offsetWidth; // 强制重排以重播动画
  registerErr.style.animation = '';
}
