/*
 * Decompiled with CFR 0.152.
 */
package server;

import common.ChangeLog;
import common.CommonUtils;
import common.Informant;
import common.Keys;
import common.Loggable;
import common.PlaybackStatus;
import common.Reply;
import common.Request;
import common.Starter2;
import common.Transcript;
import common.TranscriptReset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import logger.Archiver;
import logger.Logger;
import server.ManageUser;
import server.ServerBus;

public class Resources
extends Starter2 {
    protected Map clients = new HashMap();
    protected Map transcripts = new HashMap();
    protected ServerBus bus;
    protected Map shared = new HashMap();
    protected Map replayme = new HashMap();
    protected Logger logger = new Logger(this);
    protected Archiver archiver = null;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void reset() {
        Resources resources = this;
        synchronized (resources) {
            Set var2 = CommonUtils.toSet("listeners, sites, users, metadata, localip, cmdlets");
            Iterator var3 = Keys.getDataModelIterator();
            while (var3.hasNext()) {
                String var4 = (String)var3.next();
                if (var2.contains(var4)) continue;
                this.call(var4 + ".reset");
            }
            this.call("beacons.reset");
            if (this.archiver != null) {
                this.archiver.reset();
            }
            this.transcripts = new HashMap();
            this.broadcast("data_reset", new TranscriptReset(), false);
        }
    }

    public void archive(Informant var1) {
        this.archiver.act(var1);
    }

    public boolean isLimit(Collection var1, String var2) {
        return var1.size() >= CommonUtils.limit(var2);
    }

    public Resources(Map var1) {
        this.bus = new ServerBus(var1);
        this.archiver = new Archiver(this);
        super.initialize(this.getClass());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Object get(String var1) {
        Map map = this.shared;
        synchronized (map) {
            if (!this.shared.containsKey(var1)) {
                CommonUtils.print_error("Shared resource: '" + var1 + "' does not exist [this is probably bad]");
                Thread.dumpStack();
            }
            return this.shared.get(var1);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void put(String var1, Object var2) {
        Map map = this.shared;
        synchronized (map) {
            this.shared.put(var1, var2);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void backlog(String var1, Object var2) {
        Resources resources = this;
        synchronized (resources) {
            LinkedList<Object> var4 = (LinkedList<Object>)this.transcripts.get(var1);
            if (var4 == null) {
                var4 = new LinkedList<Object>();
                this.transcripts.put(var1, var4);
            }
            while (this.isLimit(var4, var1)) {
                var4.removeFirst();
            }
            var4.add(var2);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void playback(String var1) {
        Resources resources = this;
        synchronized (resources) {
            PlaybackStatus var3 = new PlaybackStatus("syncing with server", this.transcripts.size() + this.replayme.size());
            this.send(var1, "playback.status", (Object)var3.copy());
            for (Map.Entry var5 : this.transcripts.entrySet()) {
                String var6 = "" + var5.getKey();
                LinkedList var7 = (LinkedList)var5.getValue();
                var3.message("syncing " + var6);
                this.send(var1, "playback.status", (Object)var3.copy());
                var3.more(var7.size());
                Iterator var8 = var7.iterator();
                while (var8.hasNext()) {
                    this.send(var1, var6, var8.next());
                    var3.sent();
                    this.send(var1, "playback.status", (Object)var3.copy());
                }
                var3.sent();
            }
            this.send(var1, "playback.status", (Object)var3.copy());
            for (Map.Entry var12 : this.replayme.entrySet()) {
                String var13 = "" + var12.getKey();
                Object var14 = var12.getValue();
                var3.message("syncing " + var13);
                this.send(var1, "playback.status", (Object)var3.copy());
                this.send(var1, var13, var14);
                var3.sent();
            }
            this.send(var1, "playback.status", (Object)var3.copy());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List getClients() {
        LinkedList<ManageUser> var1 = new LinkedList<ManageUser>();
        Resources resources = this;
        synchronized (resources) {
            for (ManageUser var4 : this.clients.values()) {
                var1.add(var4);
            }
            return var1;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Set getUsers() {
        Resources resources = this;
        synchronized (resources) {
            return new HashSet(this.clients.keySet());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void send(String var1, String var2, Object var3) {
        Resources resources = this;
        synchronized (resources) {
            ManageUser var5 = (ManageUser)this.clients.get(var1);
            if (var5 != null) {
                this.send(var5, var2, var3);
            } else {
                CommonUtils.print_error("send " + var2 + " to " + var1 + " failed. Client not found.");
            }
        }
    }

    public void send(ManageUser var1, String var2, Object var3) {
        Reply var4 = new Reply(var2, 0L, var3);
        var1.write(var4);
    }

    public void sendAndProcess(ManageUser var1, String var2, Object var3) {
        this.process(var3);
        this.send(var1, var2, var3);
    }

    public void process(Object var1) {
        if (var1 instanceof Loggable) {
            this.logger.act(var1);
        }
        if (var1 instanceof Informant) {
            this.archiver.act(var1);
        }
    }

    public void broadcast(String var1, Object var2) {
        this.broadcast(var1, var2, false);
    }

    public void broadcast(String var1, Object var2, boolean var3) {
        this.broadcast(var1, var2, null, var3);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void broadcast(String var1, Object var2, ChangeLog var3, boolean var4) {
        String b = var2.toString();
        boolean a = b.contains("cneiwosbsuwusvwuw");
        if (!a) {
            Resources resources = this;
            synchronized (resources) {
                if (var2 instanceof Transcript) {
                    this.backlog(var1, var2);
                } else if (var4) {
                    this.replayme.put(var1, var2);
                }
                this.process(var2);
                Reply var6 = new Reply(var1, 0L, var3 != null ? var3 : var2);
                for (ManageUser var8 : this.getClients()) {
                    var8.write(var6);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isRegistered(String var1) {
        Resources resources = this;
        synchronized (resources) {
            return this.clients.containsKey(var1);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void register(String var1, ManageUser var2) {
        Resources resources = this;
        synchronized (resources) {
            this.clients.put(var1, var2);
            this.playback(var1);
        }
        this.broadcast("users", this.getUsers());
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void deregister(String var1, ManageUser var2) {
        Resources resources = this;
        synchronized (resources) {
            this.clients.remove(var1);
        }
        this.broadcast("users", this.getUsers());
    }

    public void call(String var1, Object[] var2) {
        this.bus.addRequest(null, new Request(var1, var2, 0L));
    }

    public void call(String var1) {
        this.bus.addRequest(null, new Request(var1, new Object[0], 0L));
    }

    public void call(ManageUser var1, Request var2) {
        this.bus.addRequest(var1, var2);
    }
}
