package Non.blocking.server.messages;

import Non.blocking.server.ServerException;
import Non.blocking.server.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Represents a message.
 */
public class Message {

    /**Message type*/
    public MessageType messageType;

    /**Message sender*/
    public String origin;

    /**Message destination, another user or {@code MessageUtilities.SERVER_NAME}*/
    public String destination;

    /**An id number for each message, usually got from {@code MessageUtilities.newMessageId()}*/
    public long messageId;

    /**The message body*/
    public byte[] body;


    /**
     * Use this to create a new message.
     * @param messageType
     * @param origin
     * @param destination
     * @param messageId
     * @param body
     */
    public Message(MessageType messageType, String origin, String destination, long messageId, byte[] body){
        this.messageType = messageType;
        this.origin = origin;
        this.destination = destination;
        this.messageId = messageId;
        this.body = body;
    }

    /**
     * Use this to create a new message with several byte[] to join at body message.
     * @param messageType
     * @param origin
     * @param destination
     * @param messageId
     * @param body
     */
    public Message(MessageType messageType, String origin, String destination, long messageId, byte[][] body){
        this(messageType, origin, destination, messageId, MessageUtilities.byteArraysToByteArray(body));
    }

    /**
     * Use this to recreate a received message (Except messageType == SECRET).
     * @param messageTypeAndLength
     * @param bundle
     * @throws Exception if source MAC and calculated MAC isn't the same.
     */
    /*public Message(byte[] messageTypeAndLength, byte[] bundle) {
        this(MessageType.getTypeByByte(messageTypeAndLength[0]), bundle);
    }*/

    /**
     * Use this to recreate a received message without MAC check (messageType == SECRET)
     * Constructor from {@code MessageType} and [@code byte[]} for bundle, without MAC check.
     * @param messageType
     * @param bundle
     */
    public static Message rebuildMessage (MessageType messageType, byte[] bundle )  {

        ByteBuffer bb = ByteBuffer.allocate(bundle.length);
        bb.put(bundle);
        bb.flip();

        Long messageId = bb.getLong();

        byte[] originInBytes = new byte[bb.getInt()];
        bb.get(originInBytes);

        byte[] destinationInBytes = new byte[bb.getInt()];
        bb.get(destinationInBytes);

        byte[] body = new byte[bb.getInt()];
        bb.get(body);
        return new Message(messageType,
                new String (originInBytes, StandardCharsets.UTF_8),
                new String (destinationInBytes, StandardCharsets.UTF_8),
                messageId,
                body);

    }

    /**
     * Constructor from {@code MessageType} and [@code byte[]} for bundle, with MAC check.
     * @param messageType
     * @param bundle
     * @param socket
     * @throws Exception
     */
    public static Message rebuildAndCheckMessage (MessageType messageType, byte[] bundle, Socket socket ) throws ServerException {

        ByteBuffer bb = ByteBuffer.allocate(bundle.length);
        bb.put(bundle);
        bb.flip();

        long messageId = bb.getLong();

        byte[] originInBytes = new byte[bb.getInt()];
        bb.get(originInBytes);

        byte[] destinationInBytes = new byte[bb.getInt()];
        bb.get(destinationInBytes);

        byte[] body = new byte[bb.getInt()];
        bb.get(body);

        byte[] srcMac = new byte[bb.getInt()];
        bb.get(srcMac);

        byte[] calculatedMac = socket.generateMac(new byte[][]{new byte[]{messageType.byteValue}, originInBytes, destinationInBytes, body});
        if (Arrays.compare(srcMac, calculatedMac) != 0)
            throw new ServerException (ServerException.Type.MAC_NOT_EQUAL);

        return new Message (messageType,
                new String (originInBytes, StandardCharsets.UTF_8),
                new String (destinationInBytes, StandardCharsets.UTF_8),
                messageId,
                body);

    }






    /**
     * Gets a {@code byte[]} ready to be sent (generates and includes MAC).
     * If messageType == WELCOME, it won't be encrypted. Otherwise, it will be encrypted with SecretKey.
     * @return
     */
    public byte[] getMessageInBytes(Socket socket) throws ServerException {
        return getMessageInByteBuffer(socket).array();
    }


    /**
     * Gets a {@code ByteBuffer} ready to be sent (generates and includes MAC).
     * If messageType == WELCOME, it won't be encrypted neither MAC be included.
     * Otherwise, it will be encrypted with SecretKey and generate a MAC.
     * @param socket It's necessary because each socket have its own mac and private key.
     * @return
     */
    public ByteBuffer getMessageInByteBuffer (Socket socket) throws ServerException {
        byte[] originInBytes = origin.getBytes(StandardCharsets.UTF_8);

        byte[] destinationInBytes = destination.getBytes(StandardCharsets.UTF_8);

        byte[] mac = null;
        if (messageType != MessageType.WELCOME) {
            mac = socket.generateMac(new byte[][]{new byte[]{messageType.byteValue}, originInBytes, destinationInBytes, body});
        }

        ByteBuffer bb = ByteBuffer.allocate( 8 + 4 + originInBytes.length +
                4 + destinationInBytes.length + 4 + body.length + 4 + (mac==null? 0 : mac.length));

        bb.putLong(messageId);
        bb.putInt(originInBytes.length);
        bb.put(originInBytes);
        bb.putInt(destinationInBytes.length);
        bb.put(destinationInBytes);
        bb.putInt(body.length);
        bb.put(body);

        if (mac != null) {
            bb.putInt(mac.length);
            bb.put(mac);

        }

        byte[] bundle;
        if (messageType != MessageType.WELCOME) {
            bundle = socket.encryptWithSecretKey(bb.array());
        } else {
            bundle = bb.array();
        }

        bb = ByteBuffer.allocate (5+bundle.length);
        bb.put(messageType.byteValue);
        bb.putInt(bundle.length);
        bb.put(bundle);

        return bb;
    }






}
