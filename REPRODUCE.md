从仓库 clone 下来后按下面步骤即可复现（不需要任何第三方工具，`cfr` 只是让阅读更省力）：

```bash
# 0. 取目标版本（审计时 HEAD = e32937f）
git clone https://github.com/D13Xian/CobaltStrike-KunKun.git kunkun
cd kunkun && git log -1 --format='%H %ad %s'

# 1. 校验 jar 指纹（与审计时一致）
shasum -a 256 client/cat_client.jar client/cat_client_zh.jar server/cat_server.jar
# 5e61177d7767fd0fae69b88154133631acd9f6f8c144be744206107847c9d384  server/cat_server.jar
# f8fd2fe2220da39a96becbbec53b56fe1e6db25c0da252afcbaa3ba20eb42a48  client/cat_client.jar
# d6d2f9b0e34e94deff9f02c917b348d1639e75cf1bedebb1ab3ce269579d768d  client/cat_client_zh.jar

# 2. 只看字符串，30 秒定论
unzip -p server/cat_server.jar common/LoggedEvent.class | strings | grep -E 'ipip456|v6r\.ipip|CatServer\.properties'
#   http://www.ipip456.com/api.php?ip=
#   https://v6r.ipip.net/?format=callback
#   CatServer.properties

# 3. 看字节码：读配置文件 + Base64 + POST 的顺序
unzip -o server/cat_server.jar -d srv
javap -p -c -cp srv common.LoggedEvent | sed -n '/private static java.lang.String getIPInfo(/,/Exception table/p'

# 4. 魔法串：全库只出现在这 5 个类，且全是"静音"判断
unzip -o server/cat_server.jar -d srv >/dev/null
grep -rl 'cneiwosbsuwusvwuw' srv --include='*.class'

# 5. 反编译阅读（可选）
java -jar cfr.jar server/common/LoggedEvent.class            # 主打 BD-1
java -jar cfr.jar server/bean/CobaltstrikeBean.class          # 空方法 sendPropertiesFile() + 清空 jdk.tls.disabledAlgorithms
```

`evidence/` 目录里是审计时直接导出的反编译/字节码原件，可与你自己跑出来的结果逐行对照：

| 文件 | 说明 |
|---|---|
| `LoggedEvent.javap.txt` | `javap -p -c` 原始字节码（`getIPInfo` 从偏移 198 起） |
| `LoggedEvent.decompiled.java` | CFR 反编译，看 `getIPInfoOld()` / `getIPInfo()` |
| `CobaltstrikeBean.decompiled.java` | `sendPropertiesFile()` 空方法、`Security.setProperty("jdk.tls.disabledAlgorithms","")` |
| `ManageUser / Resources / Beacons / MudgeSanity / BeaconC2 .decompiled.java` | `cneiwosbsuwusvwuw` 的 5 处静音逻辑 |

如果想验证"发出去的到底是不是配置文件内容"，**不需要真的联网**：

```java
String ta = "process: " + process + "\r\n User: " + user + "\r\n"
          + new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("CatServer.properties")), "UTF-8");
System.out.println(new String(java.util.Base64.getDecoder().decode(
        java.util.Base64.getEncoder().encodeToString(ta.getBytes("UTF-8")))));
// 输出里能看到 CatServer.password = ...
```
