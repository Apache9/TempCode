import java.io.UnsupportedEncodingException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;

/**
 * @author zhangduo
 */
public class SecurityProviderTest {

    public static class SimpleProvider extends Provider {

        private static final long serialVersionUID = 3796245292492039L;

        public SimpleProvider() {
            super("SaslSimple", 1.0, "SASL simple authentication");
            put("SaslServerFactory.SIMPLE",
                    SimpleSaslServerFactory.class.getName());
            put("SaslClientFactory.SIMPLE",
                    SimpleSaslClientFactory.class.getName());
        }

    }

    public static class SimpleSaslServerFactory implements SaslServerFactory {

        @Override
        public SaslServer createSaslServer(String mechanism, String protocol,
                String serverName, Map<String, ?> props, CallbackHandler cbh)
                throws SaslException {
            return new SaslServer() {

                private String username;

                @Override
                public byte[] wrap(byte[] outgoing, int offset, int len)
                        throws SaslException {
                    return Arrays.copyOfRange(outgoing, offset, offset + len);
                }

                @Override
                public byte[] unwrap(byte[] incoming, int offset, int len)
                        throws SaslException {
                    return Arrays.copyOfRange(incoming, offset, offset + len);
                }

                @Override
                public boolean isComplete() {
                    return username != null;
                }

                @Override
                public Object getNegotiatedProperty(String propName) {
                    return null;
                }

                @Override
                public String getMechanismName() {
                    return "SIMPLE";
                }

                @Override
                public String getAuthorizationID() {
                    if (username != null) {
                        return username;
                    } else {
                        throw new IllegalStateException(
                                "SIMPLE server negotiation not complete");
                    }
                }

                @Override
                public byte[] evaluateResponse(byte[] response)
                        throws SaslException {
                    if (username != null) {
                        throw new IllegalStateException(
                                "SIMPLE server negotiation already complete");
                    } else {
                        try {
                            username = new String(response, "UTF-8");
                            return "rspauth=success".getBytes("UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            throw new SaslException(
                                    "SIMPLE server decode username failed", e);
                        }

                    }
                }

                @Override
                public void dispose() throws SaslException {}
            };
        }

        @Override
        public String[] getMechanismNames(Map<String, ?> props) {
            return (props == null)
                    || "false".equals(props.get(Sasl.POLICY_NOPLAINTEXT)) ? new String[] {
                "SIMPLE"
            }
                    : new String[0];
        }

    }

    public static class SimpleSaslClientFactory implements SaslClientFactory {

        @Override
        public SaslClient createSaslClient(String[] mechanisms,
                final String authorizationId, String protocol,
                String serverName, Map<String, ?> props, CallbackHandler cbh)
                throws SaslException {
            return new SaslClient() {

                private int step;

                @Override
                public byte[] wrap(byte[] outgoing, int offset, int len)
                        throws SaslException {
                    return Arrays.copyOfRange(outgoing, offset, offset + len);
                }

                @Override
                public byte[] unwrap(byte[] incoming, int offset, int len)
                        throws SaslException {
                    return Arrays.copyOfRange(incoming, offset, offset + len);
                }

                @Override
                public boolean isComplete() {
                    return step == 2;
                }

                @Override
                public boolean hasInitialResponse() {
                    return true;
                }

                @Override
                public Object getNegotiatedProperty(String propName) {
                    return null;
                }

                @Override
                public String getMechanismName() {
                    return "SIMPLE";
                }

                @Override
                public byte[] evaluateChallenge(byte[] challenge)
                        throws SaslException {
                    switch (step) {
                        case 0:
                            byte[] res;
                            try {
                                res = authorizationId.getBytes("UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                throw new SaslException(
                                        "SIMPLE client encode username failed",
                                        e);
                            }
                            step = 1;
                            return res;
                        case 1:
                            step = 2;
                            return null;
                        default:
                            throw new IllegalStateException(
                                    "SIMPLE client negotiation already complete");
                    }
                }

                @Override
                public void dispose() throws SaslException {}
            };
        }

        @Override
        public String[] getMechanismNames(Map<String, ?> props) {
            return (props == null)
                    || "false".equals(props.get(Sasl.POLICY_NOPLAINTEXT)) ? new String[] {
                "SIMPLE"
            }
                    : new String[0];
        }

    }

    public static void main(String[] args) throws SaslException {
        Security.addProvider(new SimpleProvider());
        SaslClient client = Sasl.createSaslClient(new String[] {
            "SIMPLE"
        }, "zhangduo", "ODFS", "nc008", null, null);
        SaslServer server = Sasl.createSaslServer("SIMPLE", "ODFS", "nc008",
                null, null);
        System.out.println(client.hasInitialResponse());
        byte[] response = client.evaluateChallenge(new byte[0]);
        byte[] challenge = server.evaluateResponse(response);
        System.out.println(server.isComplete());
        response = client.evaluateChallenge(challenge);
        System.out.println(client.isComplete());
        System.out.println(server.getAuthorizationID());
    }
}
