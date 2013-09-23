import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.LoginException;

import com.sun.security.auth.module.Krb5LoginModule;

/**
 * @(#)UserLoginHelper.java, 2013-9-3. 
 * 
 * Copyright 2013 Youdao, Inc. All rights reserved.
 * YODAO PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * @author zhangduo
 */
public class UserLoginHelper {

    public static Subject loginFromTicketCache(String ticketCache)
            throws LoginException {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("doNotPrompt", "true");
        map.put("useTicketCache", "true");
        map.put("ticketCache", ticketCache);
        map.put("renewTGT", "false");
        map.put("debug", "true");
        LoginContext ctx = new LoginContext("kerberos-user", null, null,
                new Configuration() {

                    @Override
                    public AppConfigurationEntry[] getAppConfigurationEntry(
                            String name) {
                        return new AppConfigurationEntry[] {
                            new AppConfigurationEntry(
                                    Krb5LoginModule.class.getName(),
                                    LoginModuleControlFlag.REQUIRED, map)
                        };
                    }
                });
        ctx.login();
        return ctx.getSubject();
    }

    public static Subject loginFromKeyTab(String keyTab, String principal)
            throws LoginException {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("doNotPrompt", "true");
        map.put("useKeyTab", "true");
        map.put("keyTab", keyTab);
        map.put("principal", principal);
        map.put("renewTGT", "false");
        map.put("storeKey", "true");
        map.put("refreshKrb5Config", "true");
        map.put("debug", "true");
        LoginContext ctx = new LoginContext("kerberos-keytab", null, null,
                new Configuration() {

                    @Override
                    public AppConfigurationEntry[] getAppConfigurationEntry(
                            String name) {
                        return new AppConfigurationEntry[] {
                            new AppConfigurationEntry(
                                    Krb5LoginModule.class.getName(),
                                    LoginModuleControlFlag.REQUIRED, map)
                        };
                    }
                });
        ctx.login();
        return ctx.getSubject();
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return "NULL";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b: bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        Subject subject = loginFromKeyTab(args[0], args[1]);
        KerberosTicket ticket = subject.getPrivateCredentials(
                KerberosTicket.class).iterator().next();
        System.out.println(ticket);
    }
}
