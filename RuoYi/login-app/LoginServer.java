// ============================================================
//  LoginServer.java —— 登录/注册「后端」程序（服务器）
//  作用：接收网页发来的登录、注册、退出等请求，并做出回应。
//  特点：只用 JDK 自带的 HttpServer，不需要安装任何第三方库。
//  运行方式：命令行执行  java -cp . LoginServer
//            然后浏览器打开  http://localhost:8080
// ============================================================

// ===== 导入需要用到的 Java 自带类库 =====
import com.sun.net.httpserver.*;  // JDK 自带的轻量级 HTTP 服务器
import java.io.*;                  // 输入输出流（读取请求内容、写出服务器回应）
import java.net.*;                 // 网络相关（绑定端口、获取访问地址）
import java.nio.charset.StandardCharsets; // 字符编码（统一用 UTF-8，才能正确处理中文）
import java.nio.file.*;            // 文件读写（把用户、会话数据保存到硬盘）
import java.util.*;                // 集合类（Map 键值对、Random 随机数等）
import java.util.concurrent.*;      // 并发工具（线程池，让服务器能同时处理多个请求）

/**
 * 这是一个「类」（class），可以理解为一个小程序的蓝图。
 * 下面所有代码都装在这个 LoginServer 类里。
 *
 * 接口说明（接口 = 网页和服务器之间约定的"通话方式"，比如网址 + 动作）：
 *   POST /api/register 注册：入参 {"username":"alice","password":"abc123"}
 *                           → 校验后写入用户库（存到 users.dat 文件）并自动登录
 *   POST /api/login    登录：入参 {"username":"admin","password":"admin123","remember":true}
 *                           → 设置 Cookie(登录凭证)；remember=true 时 7 天内免登录
 *   GET  /api/me       校验登录：检查你有没有登录，返回 {"username","remember"} 或 401
 *   POST /api/logout   退出：清除登录状态
 *   其余路径（如 / 、/login.html）：直接把 web/ 目录下的网页文件发给浏览器
 *
 * 注意：这里是教学演示，密码用明文保存。真实项目一定要改用数据库 + 加盐哈希（如 bcrypt）。
 */
public class LoginServer {

    // 服务器监听的端口号，浏览器访问 http://localhost:8080 就是在连它
    static final int PORT = 8080;
    // 前端网页文件所在的文件夹名字
    static final String WEB_DIR = "web";
    // 1 小时的毫秒数（3600 秒 × 1000），用作临时会话的过期时长
    static final long HOUR = 3600_000L;
    // 1 周 = 7 天，单位毫秒，用作"记住我"长期会话的过期时长
    static final long WEEK = 7L * 24 * HOUR;
    // 会话数据保存到硬盘的文件路径
    static final Path SESSION_FILE = Paths.get("sessions.dat");
    // 用户数据保存到硬盘的文件路径
    static final Path USER_FILE = Paths.get("users.dat");

    /** 用户库：用户名 -> 密码（演示明文）。里面放了内置账号，注册的新账号也会加进来。 */
    static final Map<String, String> USER_DB = new ConcurrentHashMap<>();
    // 程序一启动就先放两个内置测试账号进去（Map.put 就是"存一个键值对"）
    static { USER_DB.put("admin", "admin123"); USER_DB.put("demo", "demo123"); }

    /** 会话表：登录后生成的 token(凭证) -> 对应的 Session(会话) 对象 */
    static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();
    // 随机数生成器，用来生成一段别人猜不到的登录凭证 token
    static final Random RAND = new Random();

    /** 会话对象：记录这是哪个用户、什么时候过期、是否勾选了"记住我" */
    static class Session {
        final String user;      // 用户名
        long expires;           // 过期时间（毫秒时间戳，一个数字）
        final boolean remember; // 是否勾选了"记住我"
        Session(String user, long expires, boolean remember) {
            // 构造方法：创建会话时把上面三个值存好
            this.user = user; this.expires = expires; this.remember = remember;
        }
    }

    // ===== 程序入口（main 方法）：双击/命令启动服务器时会执行这里 =====
    public static void main(String[] args) throws Exception {
        loadUsers();      // 从硬盘读回之前注册过的账号
        loadSessions();    // 从硬盘读回之前还没过期的登录会话
        // 创建一个 HTTP 服务器，监听本机的 8080 端口
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        // 给不同的网址路径分配不同的"处理器"（Handler）
        server.createContext("/", new StaticHandler());        // 其余路径 → 返回网页文件
        server.createContext("/api/register", new RegisterHandler()); // 注册接口
        server.createContext("/api/login", new LoginHandler());    // 登录接口
        server.createContext("/api/logout", new LogoutHandler());  // 退出接口
        server.createContext("/api/me", new MeHandler());          // 校验是否登录接口
        server.setExecutor(Executors.newCachedThreadPool()); // 用线程池，可同时处理多个请求
        server.start(); // 正式启动服务器，开始监听端口
        // 在控制台打印启动信息，方便你确认服务确实起来了
        System.out.println("========================================");
        System.out.println("  登录服务已启动: http://localhost:" + PORT);
        System.out.println("  内置账号: admin / admin123    demo / demo123");
        System.out.println("  也可在登录页自行注册新账号");
        System.out.println("========================================");
    }

    /* ---------------- 用户库持久化（把账号存到硬盘，重启不丢） ---------------- */

    // 启动时调用：从 users.dat 文件把账号读回内存
    static void loadUsers() {
        try {
            // 如果文件不存在，说明还没人注册过，直接返回
            if (!Files.exists(USER_FILE)) return;
            // 一行一行读取文件，每行格式是：用户名|密码
            for (String line : Files.readAllLines(USER_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\\|", -1); // 按竖线 | 切成两半
                if (p.length >= 2 && !p[0].isEmpty()) USER_DB.put(p[0], p[1]); // 存进内存
            }
        } catch (Exception e) { /* 文件损坏就忽略，不影响启动 */ }
    }

    // 注册新账号后调用：把新账号追加写到 users.dat 文件末尾
    static synchronized void appendUser(String user, String pass) {
        try {
            Files.write(USER_FILE, (user + "|" + pass + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) { /* 落盘失败不影响内存里的账号 */ }
    }

    /* ---------------- 会话持久化（把登录状态存到硬盘） ---------------- */

    // 启动时调用：从 sessions.dat 读回还没过期的登录会话
    static void loadSessions() {
        try {
            if (!Files.exists(SESSION_FILE)) return;
            long now = System.currentTimeMillis(); // 当前时间（毫秒）
            for (String line : Files.readAllLines(SESSION_FILE, StandardCharsets.UTF_8)) {
                String[] p = line.split("\\|");
                if (p.length < 4) continue;
                long exp = Long.parseLong(p[2]); // 第 3 段是过期时间
                // 只恢复还没过期的会话
                if (exp > now) SESSIONS.put(p[0], new Session(p[1], exp, "1".equals(p[3])));
            }
        } catch (Exception e) { /* 损坏则忽略 */ }
    }

    // 登录/退出/续期后调用：把所有当前会话重新写入 sessions.dat
    static synchronized void saveSessions() {
        try {
            StringBuilder sb = new StringBuilder(); // 用来拼接要写入的文本
            for (var e : SESSIONS.entrySet()) {     // 遍历每一个会话
                Session s = e.getValue();
                sb.append(e.getKey()).append('|').append(s.user).append('|')
                  .append(s.expires).append('|').append(s.remember ? "1" : "0").append('\n');
            }
            Files.write(SESSION_FILE, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { /* 落盘失败不影响内存会话 */ }
    }

    /* ---------------- 通用小工具方法 ---------------- */

    // 读取请求体（网页发来的那串 JSON 文本，比如 {"username":"admin",...}）
    static String readBody(HttpExchange ex) throws IOException {
        InputStream in = ex.getRequestBody(); // 拿到输入流
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024]; // 每次读 1KB
        int n;
        // 一截一截地把数据读到内存里，直到读完
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toString(StandardCharsets.UTF_8); // 转成字符串返回
    }

    // 统一的 JSON 回应方法：把一段 JSON 文本按 HTTP 协议写回给浏览器
    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, data.length); // code 是状态码，如 200/401
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    // 出错时调用：返回 {"success":false,"message":"..."} 这种格式
    static void sendFail(HttpExchange ex, int code, String msg) throws IOException {
        sendJson(ex, code, "{\"success\":false,\"message\":\"" + msg + "\"}");
    }

    // 从 JSON 文本里取出某个字段的值（用正则表达式匹配 "key":"value"）
    static String parseField(String body, String key) {
        String pat = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pat);
        java.util.regex.Matcher m = p.matcher(body == null ? "" : body);
        return m.find() ? m.group(1) : null;
    }

    // 从 JSON 文本里取出某个布尔字段（true/false）
    static boolean parseBool(String body, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)");
        java.util.regex.Matcher m = p.matcher(body == null ? "" : body);
        return m.find() && "true".equals(m.group(1));
    }

    // 从浏览器发来的 Cookie 里取出 session 凭证的值
    static String cookieToken(HttpExchange ex) {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) return null;
        for (String part : cookie.split(";")) { // Cookie 可能有多个，用分号分隔
            part = part.trim();
            if (part.startsWith("session=")) return part.substring("session=".length());
        }
        return null;
    }

    // 生成一个随机的登录凭证 token（一串十六进制字符）
    static String newToken() {
        return Long.toHexString(System.nanoTime()) + Long.toHexString(RAND.nextLong());
    }

    /* ---------------- 注册接口 ---------------- */
    static class RegisterHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            // 只允许 POST 方式访问，其它方式拒绝
            if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { sendFail(ex, 405, "仅支持 POST"); return; }
            String body = readBody(ex); // 读取网页传来的数据
            String user = parseField(body, "username"); // 取出用户名
            String pass = parseField(body, "password"); // 取出密码
            // 校验：用户名、密码都不能为空
            if (user == null || pass == null) { sendFail(ex, 400, "参数缺失"); return; }
            // 校验：用户名只能包含 3~20 位字母/数字/下划线
            if (!user.matches("[A-Za-z0-9_]{3,20}")) { sendFail(ex, 400, "用户名需 3-20 位字母/数字/下划线"); return; }
            // 校验：密码至少 6 位
            if (pass.length() < 6) { sendFail(ex, 400, "密码至少 6 位"); return; }

            synchronized (USER_DB) {
                // 校验：用户名是否已经存在
                if (USER_DB.containsKey(user)) { sendFail(ex, 409, "用户名已存在"); return; }
                USER_DB.put(user, pass); // 写入内存中的用户库
                appendUser(user, pass);  // 同时写入文件，重启不丢
            }
            // 注册成功后自动帮用户登录（先给 1 小时临时会话）
            String token = newToken();
            long now = System.currentTimeMillis();
            SESSIONS.put(token, new Session(user, now + HOUR, false));
            saveSessions();
            // 通过 Set-Cookie 把凭证交给浏览器，之后浏览器访问会自动带上它
            ex.getResponseHeaders().add("Set-Cookie",
                    "session=" + token + "; Path=/; HttpOnly; Max-Age=3600; SameSite=Lax");
            sendJson(ex, 200, "{\"success\":true,\"username\":\"" + user + "\"}");
        }
    }

    /* ---------------- 登录接口 ---------------- */
    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if (!ex.getRequestMethod().equalsIgnoreCase("POST")) { sendFail(ex, 405, "仅支持 POST"); return; }
            String body = readBody(ex);
            String user = parseField(body, "username");
            String pass = parseField(body, "password");
            boolean remember = parseBool(body, "remember"); // 是否勾选"记住我"
            // 校验：用户名存在且密码正确
            if (user != null && pass != null && pass.equals(USER_DB.get(user))) {
                String token = newToken();
                // 勾选"记住我"就给 7 天，否则只给 1 小时
                long ttl = remember ? WEEK : HOUR;
                long now = System.currentTimeMillis();
                SESSIONS.put(token, new Session(user, now + ttl, remember));
                saveSessions();
                ex.getResponseHeaders().add("Set-Cookie",
                        "session=" + token + "; Path=/; HttpOnly; Max-Age=" + (ttl / 1000) + "; SameSite=Lax");
                sendJson(ex, 200, "{\"success\":true,\"username\":\"" + user + "\",\"remember\":" + remember + "}");
            } else {
                // 用户名或密码错误
                sendFail(ex, 401, "用户名或密码错误");
            }
        }
    }

    /* ---------------- 退出接口 ---------------- */
    static class LogoutHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String token = cookieToken(ex);
            // 找到并删除当前会话
            if (token != null) { SESSIONS.remove(token); saveSessions(); }
            // 把浏览器里的 Cookie 清空（Max-Age=0 表示立即过期）
            ex.getResponseHeaders().add("Set-Cookie", "session=; Path=/; Max-Age=0");
            sendJson(ex, 200, "{\"success\":true}");
        }
    }

    /* ---------------- 校验登录状态 + 自动续期 ---------------- */
    static class MeHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String token = cookieToken(ex);
            long now = System.currentTimeMillis();
            Session s = token == null ? null : SESSIONS.get(token);
            if (s == null) { sendJson(ex, 401, "{\"error\":\"unauthorized\"}"); return; } // 没登录
            // 会话已过期，删掉并提示
            if (s.expires <= now) { SESSIONS.remove(token); saveSessions(); sendJson(ex, 401, "{\"error\":\"expired\"}"); return; }
            // 如果勾选了"记住我"，每次访问都自动把过期时间往后推 7 天（续期）
            if (s.remember) { s.expires = now + WEEK; saveSessions(); }
            sendJson(ex, 200, "{\"username\":\"" + s.user + "\",\"remember\":" + s.remember + "}");
        }
    }

    /* ---------------- 静态文件托管（把网页发给浏览器） ---------------- */
    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath(); // 浏览器请求的网址路径，如 /login.html
            // 访问根目录 / 就默认返回 index.html
            String rel = path.equals("/") ? "index.html" : path.replaceFirst("^/", "");
            Path base = Paths.get(WEB_DIR).toAbsolutePath().normalize(); // web 目录的绝对路径
            Path resolved = base.resolve(rel).normalize();
            // 安全检查：防止有人通过 ../ 访问到 web 目录之外（目录穿越攻击）
            if (!resolved.startsWith(base)) { sendJson(ex, 403, "{\"error\":\"forbidden\"}"); return; }
            // 文件不存在或是个文件夹，就退回 index.html（让前端路由自己处理）
            if (!Files.exists(resolved) || Files.isDirectory(resolved)) resolved = base.resolve("index.html").normalize();
            if (!Files.exists(resolved)) { ex.sendResponseHeaders(404, -1); ex.close(); return; } // 真没找到 → 404
            // 读出文件内容，按文件类型设置 Content-Type，然后发给浏览器
            byte[] data = Files.readAllBytes(resolved);
            ex.getResponseHeaders().add("Content-Type", mime(resolved.toString()));
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        }
    }

    // 根据文件后缀名，返回对应的 MIME 类型（告诉浏览器这是 html/css/js/图片等）
    static String mime(String name) {
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".svg"))  return "image/svg+xml";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}
