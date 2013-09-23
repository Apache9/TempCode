import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

/**
 * @author zhangduo
 */
public class SocketSaslServer {

    public static final Map<String, String> SASL_PROPS;

    static {
        Map<String, String> props = new HashMap<String, String>();
        props.put(Sasl.QOP, "auth");
        props.put(Sasl.SERVER_AUTH, "true");
        SASL_PROPS = Collections.unmodifiableMap(props);
    }

    private final ServerSocket ss;

    private final Subject subject;

    private Thread acceptor = new Thread("acceptor") {

        @Override
        public void run() {
            for (;;) {
                try {
                    Socket socket = ss.accept();
                    System.err.println("Accept connection from "
                            + socket.getRemoteSocketAddress());
                    new Verifier(socket).start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    };

    private class Verifier extends Thread {

        private final Socket socket;

        private DataInputStream in;

        private DataOutputStream out;

        public Verifier(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        private byte[] receive() throws IOException {
            int sz = in.readInt();
            byte[] token = new byte[sz];
            in.readFully(token);
            return token;
        }

        private void send(boolean finish, byte[] token) throws IOException {
            out.writeBoolean(finish);
            if (token != null) {
                out.writeInt(token.length);
                out.write(token);
            } else {
                out.writeInt(0);
            }
        }

        @Override
        public void run() {
            try {
                Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {

                    @Override
                    public Object run() throws IOException {
                        SaslServer saslServer = Sasl.createSaslServer("GSSAPI",
                                "ODFS",
                                InetAddress.getLocalHost().getHostName(),
                                SASL_PROPS, new SaslGssCallbackHandler());
                        for (;;) {
                            byte[] token = receive();
                            System.err.println("Receive token: "
                                    + UserLoginHelper.toHex(token));
                            byte[] challenge = saslServer.evaluateResponse(token);
                            System.out.println("Generate challenge: "
                                    + UserLoginHelper.toHex(challenge));
                            if (saslServer.isComplete()) {
                                send(true, challenge);
                                break;
                            } else {
                                send(false, challenge);
                            }
                        }
                        System.err.println("AuthorizedID "
                                + saslServer.getAuthorizationID());
                        return null;
                    }
                });

            } catch (PrivilegedActionException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class SaslGssCallbackHandler implements CallbackHandler {

        @Override
        public void handle(Callback[] callbacks)
                throws UnsupportedCallbackException {
            AuthorizeCallback ac = null;
            for (Callback callback: callbacks) {
                if (callback instanceof AuthorizeCallback) {
                    ac = (AuthorizeCallback) callback;
                } else {
                    throw new UnsupportedCallbackException(callback,
                            "Unrecognized SASL GSSAPI Callback");
                }
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
                    System.err.println("SASL server GSSAPI callback: setting "
                            + "canonicalized client ID: " + authzid);
                    ac.setAuthorizedID(authzid);
                }
            }
        }
    }

    public SocketSaslServer(String keyTab) throws IOException, LoginException {
        ss = new ServerSocket(0);
        System.err.println("Listening on port " + ss.getLocalPort());
        subject = UserLoginHelper.loginFromKeyTab(keyTab, "ODFS/"
                + InetAddress.getLocalHost().getHostName());
    }

    public void start() {
        acceptor.start();
    }

    public static void main(String[] args) throws LoginException, IOException {
        new SocketSaslServer(args[0]).start();
    }

}
