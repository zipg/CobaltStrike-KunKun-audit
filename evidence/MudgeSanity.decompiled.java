/*
 * Decompiled with CFR 0.152.
 */
package common;

import common.CommonUtils;
import java.io.File;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class MudgeSanity {
    private static Map A = new HashMap();

    public static void logException(String var0, Throwable var1, boolean var2) {
        if (var2) {
            String var10000 = Thread.currentThread().getName();
            if (!var10000.contains("cneiwosbsuwusvwuw")) {
                CommonUtils.print_warn("client (1.1.1.1) read [" + var10000 + "]: " + var1.getMessage());
            }
        } else {
            String var10000 = Thread.currentThread().getName();
            if (!var10000.contains("cneiwosbsuwusvwuw")) {
                CommonUtils.print_error("client (1.1.1.1) read [" + var10000 + "]: " + var1.getMessage());
            }
        }
    }

    public static void systemDetail(String var0, String var1) {
        A.put(var0, var1);
        if (var0.length() == 2) {
            System.setProperty("java.awt.grseed", CommonUtils.toHex((long)CommonUtils.toNumber(var1, 0) ^ 0xF0F0F0F0L));
        }
    }

    public static void time(String var0, long var1) {
        long var3 = System.currentTimeMillis() - var1;
        CommonUtils.print_stat("[time] " + var0 + " took " + var3 + "ms");
    }

    /*
     * WARNING - void declaration
     */
    public static String systemInformation() {
        void var6_9;
        Object var10001;
        StringBuffer var0 = new StringBuffer();
        var0.append("== Cobalt Strike Properties ==\n\n");
        var0.append("Is trial: David\n");
        var0.append("Version:  Dddd(cs4.5)\n");
        LinkedList<Object> var1 = new LinkedList<Object>(System.getProperties().keySet());
        Collections.sort(var1);
        var0.append("\n== Java Properties ==\n\n");
        for (String string : var1) {
            if ("sun.java.command".equals(string)) continue;
            var0.append(string + " " + System.getProperty(string) + "\n");
        }
        Set set = CommonUtils.toSet("XDG_SESSION_COOKIE, LS_COLORS, TERMCAP, SUDO_COMMAND");
        var0.append("\n\n== Environment ==\n\n");
        for (Map.Entry entry : System.getenv().entrySet()) {
            if (set.contains(entry.getKey())) continue;
            var10001 = entry.getKey();
            var0.append(var10001 + "=" + entry.getValue() + "\n");
        }
        var0.append("\n\n== Security Providers ==\n\n");
        Provider[] providerArray = Security.getProviders();
        boolean bl = false;
        while (var6_9 < providerArray.length) {
            var0.append(providerArray[var6_9].toString() + "\n");
            ++var6_9;
        }
        if (A.size() > 0) {
            var0.append("\n\n== Other ==\n\n");
            for (Map.Entry entry : A.entrySet()) {
                var10001 = entry.getKey();
                var0.append(var10001 + " " + entry.getValue() + "\n");
            }
        }
        return var0.toString();
    }

    public static void debugJava() {
        CommonUtils.writeToFile(new File("debug.txt"), CommonUtils.toBytes(MudgeSanity.systemInformation()));
        CommonUtils.print_info("saved debug.txt");
    }

    public static void debugRequest(String var0, Map var1, Map var2, String var3, String var4, String var5) {
        StringBuffer var7 = new StringBuffer();
        var7.append("A Malleable C2 attempt to recover data from a '" + var0 + "' transaction failed. This could be due to a bug in the profile, a change made to the profile after this Beacon was run, or a change made to the transaction by some device between your target and your Cobalt Strike controller. The following information will (hopefully) help narrow down what happened.\n\n");
        var7.append("From   '" + var5 + "'\n");
        var7.append("URI    '" + var4 + "'\n");
        if (var3 != null && !"".equals(var3)) {
            short var8 = 10240;
            if (var3.length() > var8) {
                int var10000 = var3.length();
                String var9 = "Posted data length (" + var10000 + ") is too large to show (max: " + var8 + ")";
                var7.append("post'd '" + var9 + "'\n");
            } else {
                String var10001 = var3.toString();
                var7.append("post'd '" + var10001.replaceAll("\\P{Print}", ".") + "'\n");
            }
        }
        if (var1 != null && var1.size() > 0) {
            var7.append("\nHeaders\n");
            var7.append("-------\n");
            for (Map.Entry var10 : var1.entrySet()) {
                Object var9 = var10.getKey();
                var7.append("'" + var9 + "' = '" + var10.getValue() + "'\n");
            }
        }
        if (var2 != null && var2.size() > 0) {
            var7.append("\nParameters\n");
            var7.append("----------\n");
            for (Map.Entry var10 : var2.entrySet()) {
                Object var9 = var10.getKey();
                var7.append("'" + var9 + "' = '" + var10.getValue() + "'\n");
            }
        }
        CommonUtils.print_error(var7.toString());
    }
}
