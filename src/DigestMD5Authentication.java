import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/**
 * @author zhangduo
 */
public class DigestMD5Authentication {
    private static final Map<String, String> SASL_PROPS;

    static {
        Map<String, String> props = new HashMap<String, String>();
        props.put(Sasl.QOP, "auth");
        props.put(Sasl.SERVER_AUTH, "true");
        SASL_PROPS = Collections.unmodifiableMap(props);
    }

    public static void main(String[] args) throws SaslException {
        SaslServer server = Sasl.createSaslServer("DIGEST-MD5", "ODFS",
                "nc008", SASL_PROPS, new CallbackHandler() {

                    @Override
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        NameCallback nc = null;
                        PasswordCallback pc = null;
                        AuthorizeCallback ac = null;
                        for (Callback callback: callbacks) {
                            if (callback instanceof AuthorizeCallback) {
                                ac = (AuthorizeCallback) callback;
                            } else if (callback instanceof NameCallback) {
                                nc = (NameCallback) callback;
                            } else if (callback instanceof PasswordCallback) {
                                pc = (PasswordCallback) callback;
                            } else if (callback instanceof RealmCallback) {
                                continue; // realm is ignored
                            } else {
                                throw new UnsupportedCallbackException(
                                        callback,
                                        "Unrecognized SASL DIGEST-MD5 Callback");
                            }
                        }
                        if (pc != null) {
                            System.out.println("SASL server DIGEST-MD5 callback: setting password "
                                    + "for client: " + nc.getDefaultName());
                            pc.setPassword(new char[] {
                                'a'
                            });
                        }
                        if (ac != null) {
                            String authid = ac.getAuthenticationID();
                            String authzid = ac.getAuthorizationID();
                            if (authid.equals(authzid)) {
                                ac.setAuthorized(true);
                            } else {
                                ac.setAuthorized(false);
                            }
                            if (ac.isAuthorized()) {
                                System.out.println("isAuthorized id:" + authzid);
                                ac.setAuthorizedID(authzid);
                            }
                        }
                    }
                });
        SaslClient client = Sasl.createSaslClient(new String[] {
            "DIGEST-MD5"
        }, null, "ODFS", "nc008", SASL_PROPS, new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException,
                    UnsupportedCallbackException {
                NameCallback nc = null;
                PasswordCallback pc = null;
                RealmCallback rc = null;
                for (Callback callback: callbacks) {
                    if (callback instanceof RealmChoiceCallback) {
                        continue;
                    } else if (callback instanceof NameCallback) {
                        nc = (NameCallback) callback;
                    } else if (callback instanceof PasswordCallback) {
                        pc = (PasswordCallback) callback;
                    } else if (callback instanceof RealmCallback) {
                        rc = (RealmCallback) callback;
                    } else {
                        throw new UnsupportedCallbackException(callback,
                                "Unrecognized SASL client callback");
                    }
                }
                if (nc != null) {
                    System.out.println("SASL client callback: setting username: "
                            + "zhangduo");
                    nc.setName("zhangduo");
                }
                if (pc != null) {
                    System.out.println("SASL client callback: setting userPassword");
                    pc.setPassword(new char[] {
                        'a'
                    });
                }
                if (rc != null) {
                    System.out.println("SASL client callback: setting realm: "
                            + rc.getDefaultText());
                    rc.setText(rc.getDefaultText());
                }
            }
        });
        byte[] challenge = server.evaluateResponse(new byte[0]);
        byte[] response = client.evaluateChallenge(challenge);
        challenge = server.evaluateResponse(response);
        System.out.println(server.isComplete());
        System.out.println(new String(challenge));
        response = client.evaluateChallenge(challenge);
        System.out.println(client.isComplete());
    }
}
