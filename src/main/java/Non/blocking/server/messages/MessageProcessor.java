package Non.blocking.server.messages;

import Non.blocking.server.Database;
import Non.blocking.server.ServerException;
import Non.blocking.server.Socket;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;


/**
 * Only static methods. Given a message, processes it.
 */
public abstract class MessageProcessor {

    /**
     * Entry function to process a message.
     * @param message
     * @param socket
     * @throws ServerException
     */
    public static void processIncomingMessage(Message message, Socket socket) throws ServerException {

        switch (message.messageType){
            case SECRET:
                processSecretKey(message, socket);
                break;

            case LOG_IN:
                if (socket.hasSecretKey() == false) throw new ServerException (ServerException.Type.SECRET_KEY_NEEDED);
                processLogIn(message, socket);
                break;
            case NEW_USER:
                if (socket.hasSecretKey() == false) throw new ServerException (ServerException.Type.SECRET_KEY_NEEDED);
                processNewUser(message, socket);
                break;
            case TEXT:
                if (socket.userName == null) throw new ServerException (ServerException.Type.LOG_IN_NEEDED, message.messageId);
                processText(message, socket);
                break;
            case ALL_USERS:
                if (socket.userName == null) throw new ServerException (ServerException.Type.LOG_IN_NEEDED, message.messageId);
                processAllUsers(message, socket);
                break;
            case ACK:
                break;
            case NACK:
                break;
            default:

        }
    }

    private static void processLogIn (Message message, Socket socket) throws ServerException {

        byte[][] userAndPassword= getUserNameAndPasswordFromBodyMessage(message.body);

        String userNameStr = new String(userAndPassword[0], StandardCharsets.UTF_8);
        //Replace, consult in a database.
        if (!Database.canILogIn(userNameStr, userAndPassword[1])) throw new ServerException(ServerException.Type.LOG_IN_PROBLEM, message.messageId);

        socket.logMe(userNameStr, message.messageId);

    }

    /**
     * Contains clientLong and secretKey.
     * Gets secretKey from message and sets it into the socket.
     * Creates mac object with socketId and long from client.
     * Sends a message with mac of publicKey + secretKey + key for mac generation. All encrypted with secretkey.
     * @param message
     * @param socket
     */
    private static void processSecretKey (Message message, Socket socket) throws ServerException {
        ByteBuffer bb = ByteBuffer.allocate(message.body.length);
        bb.put(message.body);
        bb.flip();

        int clientRandomInt = bb.getInt();
        byte[] secretKeyInBytes = new byte[message.body.length-4];
        bb.get(secretKeyInBytes);
        try {
            socket.setSecretKeyCiphers(secretKeyInBytes);
        } catch ( NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e){
            throw new ServerException(ServerException.Type.SECRET_KEY_PROBLEM);
        }
        byte[] keyForMac = MessageUtilities.byteArraysToByteArray(new byte[][]{
                MessageUtilities.longToByteArray(socket.socketId),
                MessageUtilities.intToByteArray(MessageUtilities.SHARED_SECRET_KEY_COMPLEMENT),
                MessageUtilities.intToByteArray(clientRandomInt)});
        socket.setMacGenerator(keyForMac);

        byte[] messageBody = socket.generateMac(new byte[][]
                {MessageUtilities.getPublicKey().getEncoded(), secretKeyInBytes, keyForMac});

        Message responseMessage = new Message (MessageType.CHECK_WELCOME, MessageUtilities.SERVER_NAME,message.origin,
                MessageUtilities.newMessageId(), messageBody);

        socket.messageWriter.enqueueMessage(responseMessage.getMessageInByteBuffer(socket));

    }

    private static void processText(Message message, Socket socket) throws ServerException {

        Socket destinationSocket = socket.loggedSockets.get(message.destination);

        if (destinationSocket == null) new ServerException(ServerException.Type.WRONG_DESTINATION, message.messageId);

        destinationSocket.messageWriter.enqueueMessage(message.getMessageInByteBuffer(socket));
    }

    private static void processAllUsers (Message message, Socket socket) throws ServerException {

        byte[] body = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeInt(socket.loggedSockets.size());
            Iterator iterator = socket.loggedSockets.keySet().iterator();
            while(iterator.hasNext()){
                oos.writeObject(iterator.next());
            }
            body = baos.toByteArray();
        }catch (Exception e) {
            throw new ServerException(ServerException.Type.PROBLEM_SERIALIZING_USERS);
        }


        Message messageAllUsers = new Message(MessageType.ALL_USERS, MessageUtilities.SERVER_NAME, socket.userName,
                MessageUtilities.newMessageId(), body);

        socket.messageWriter.enqueueMessage(messageAllUsers.getMessageInByteBuffer(socket)); //exception here.

    }

    private static void processNewUser (Message message, Socket socket) throws ServerException {
        byte[][] userAndPassword = getUserNameAndPasswordFromBodyMessage(message.body);
        String userNameStr = new String(userAndPassword[0], StandardCharsets.UTF_8);

        try {
            Database.newUser(userNameStr, userAndPassword[1]);
        } catch (ServerException e) {
            e.messageId = message.messageId;
            throw e;
        }

        socket.logMe(userNameStr, message.messageId);

    }

    /**
     * array[0] is byte[] user, array[1] is byte[] password.
     * @param body message.body
     * @return a byte[][] with two byte[] elements. The first one, represents the user's name, the second represents the password.
     */
    private static byte[][] getUserNameAndPasswordFromBodyMessage(byte[] body) {

        int userNameLength = MessageUtilities.byteArrayToint(Arrays.copyOfRange(body, 0, 4));
        return new byte[][] {Arrays.copyOfRange(body, 4, 4+userNameLength),Arrays.copyOfRange(body, 4+userNameLength, body.length) };
    }






}
