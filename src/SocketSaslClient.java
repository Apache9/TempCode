import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;

/**
 * @author zhangduo
 */
public class SocketSaslClient {
    private final Subject subject;

    private final Socket socket;

    private final DataOutputStream out;

    private final DataInputStream in;

    public SocketSaslClient(String ticketCache, String host, int port)
            throws LoginException, IOException {
        this.subject = UserLoginHelper.loginFromTicketCache(ticketCache);
        this.socket = new Socket(host, port);
        System.err.println("connect to " + socket.getRemoteSocketAddress());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
    }

    private void send(byte[] token) throws IOException {
        out.writeInt(token.length);
        out.write(token);
    }

    private static class Challenge {
        boolean finish;

        byte[] challenge;

        public Challenge(boolean finish, byte[] challenge) {
            this.finish = finish;
            this.challenge = challenge;
        }
    }

    private Challenge receive() throws IOException {
        boolean finish = in.readBoolean();
        int sz = in.readInt();
        byte[] challenge = new byte[sz];
        in.readFully(challenge);
        return new Challenge(finish, challenge);
    }

    public void exec() throws PrivilegedActionException, IOException {
        Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {

            @Override
            public Object run() throws Exception {
                SaslClient saslClient = Sasl.createSaslClient(new String[] {
                    "GSSAPI"
                }, null, "ODFS", "nc008", SocketSaslServer.SASL_PROPS, null);
                if (!saslClient.hasInitialResponse()) {
                    throw new IllegalStateException(
                            "sasl client do not have initial response");
                }
                byte[] token = saslClient.evaluateChallenge(new byte[0]);
                System.err.println("Generate initial token: "
                        + UserLoginHelper.toHex(token));
                for (;;) {
                    send(token);
                    Challenge challenge = receive();
                    System.err.println("Receive challenge: "
                            + UserLoginHelper.toHex(challenge.challenge));
                    if (challenge.finish) {
                        System.err.println("auth success!");
                        break;
                    }
                    token = saslClient.evaluateChallenge(challenge.challenge);
                    System.err.println("Generate response: "
                            + UserLoginHelper.toHex(token));
                }
                return null;
            }

        });

    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws LoginException, IOException,
            PrivilegedActionException {
        SocketSaslClient client = new SocketSaslClient(args[0], args[1],
                Integer.parseInt(args[2]));
        try {
            client.exec();
        } finally {
            client.close();
        }
    }
}
