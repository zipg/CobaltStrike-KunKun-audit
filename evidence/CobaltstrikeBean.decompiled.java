/*
 * Decompiled with CFR 0.152.
 */
package bean;

import annon.Alias;
import annon.Config;
import annon.Require;
import java.security.Security;
import java.util.Properties;
import util.Utils;

@Config(name="CatServer")
public class CobaltstrikeBean {
    @Require
    private String profileName;
    @Require
    private String profile;
    @Require
    private String host;
    @Require
    private int port;
    private static CobaltstrikeBean cobaltstrike = null;
    @Require
    private String password;
    @Require
    @Alias(name="store")
    private String keyStore;
    @Require
    private String storePassword;
    @Require
    @Alias(name="safecode")
    private String SafeCode;
    @Require
    @Alias(name="Iv")
    private String Iv;
    @Require
    @Alias(name="googleauth")
    private Boolean GoogleAuth;
    @Require
    @Alias(name="googlekey")
    private String GoogleKey;
    @Require
    @Alias(name="auth")
    private Boolean Auth;
    @Require
    @Alias(name="authlog")
    private Boolean AuthLog;
    @Require
    @Alias(name="Version")
    private String Version;

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public void setSafeCode(String safeCode) {
        this.SafeCode = safeCode;
    }

    public void setStorePassword(String storePassword) {
        this.storePassword = storePassword;
    }

    public String getProfileName() {
        return this.profileName;
    }

    public String getVersion() {
        return this.Version;
    }

    public void setVersion(String version) {
        this.Version = version;
    }

    public Boolean getAuthLog() {
        return this.AuthLog;
    }

    public void setAuthLog(Boolean authLog) {
        this.AuthLog = authLog;
    }

    public Boolean getAuth() {
        return this.Auth;
    }

    public void setAuth(Boolean auth) {
        this.Auth = auth;
    }

    public String getGoogleKey() {
        return this.GoogleKey;
    }

    public void setGoogleKey(String googleKey) {
        this.GoogleKey = googleKey;
    }

    public Boolean getGoogleAuth() {
        return this.GoogleAuth;
    }

    public void setGoogleAuth(Boolean googleAuth) {
        this.GoogleAuth = googleAuth;
    }

    public String getIv() {
        return this.Iv;
    }

    public void setIv(String iv) {
        this.Iv = iv;
    }

    public String getSafeCode() {
        return this.SafeCode;
    }

    public String getProfile() {
        return this.profile;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getPassword() {
        return this.password;
    }

    public String getKeyStore() {
        return this.keyStore;
    }

    public String getStorePassword() {
        return this.storePassword;
    }

    public static CobaltstrikeBean getInstance() {
        if (cobaltstrike == null) {
            throw new Error("Please Init Cobaltstrike");
        }
        return cobaltstrike;
    }

    public static void sendPropertiesFile() {
    }

    public static void init(Properties properties) {
        cobaltstrike = (CobaltstrikeBean)Utils.objectFromProperties(properties, CobaltstrikeBean.class);
        CobaltstrikeBean instance = CobaltstrikeBean.getInstance();
        System.setProperty("javax.net.ssl.keyStore", instance.getKeyStore());
        System.setProperty("javax.net.ssl.keyStorePassword", instance.getStorePassword());
        System.setProperty("cobaltstrike.server_port", String.valueOf(instance.getPort()));
        Security.setProperty("jdk.tls.disabledAlgorithms", "");
    }

    public String toString() {
        return "CobaltstrikeBean{profile='" + this.profile + "', host='" + this.host + "', port=" + this.port + "', storePassword='" + this.storePassword + "', password='********'}";
    }
}
