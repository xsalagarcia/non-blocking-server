package Non.blocking.server.messages;

import Non.blocking.server.ServerException;
import Non.blocking.server.Socket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    @DisplayName("Create a message, getting the message in byte[] (ready to send) create again from byte[]")
    void createMessage(){
        Socket socket = null;
        try {
            socket = giveMeASocket();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


        Message m1 = new Message(MessageType.TEXT, "Origin", "destination", 3433, "Sample body".getBytes(StandardCharsets.UTF_8));


        byte[] messageInBytes = null;
        try {
            messageInBytes = m1.getMessageInBytes(socket);
        } catch (ServerException e) {
            e.printStackTrace();
        }

        byte[] bundle = Arrays.copyOfRange(messageInBytes, 5, messageInBytes.length);

        try {
            bundle = socket.decryptWithSecretKey(bundle);
        } catch (ServerException e) {
            e.printStackTrace();
        }


        final Socket socket2 = socket;


        try {

            //without mac check
            Message m2 = new Message(MessageType.getTypeByByte(messageInBytes[0])
                    , bundle);

            //with mac check
            Message m3 = new Message(MessageType.getTypeByByte(messageInBytes[0]), bundle, socket);

            assertAll (() -> assertEquals(m1.messageId, m2.messageId, m3.messageId),
                    () -> assertEquals(m1.origin, m2.origin),
                    () -> assertEquals(m1.destination, m2.destination),
                    () -> assertEquals(Arrays.compare(m1.body, m2.body), 0),
                    () -> assertEquals(Arrays.compare(m1.getMessageInBytes(socket2), m2.getMessageInBytes(socket2)),0));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Create a welcome message and unpack it")
    void packingUnpackingWelcome(){
        Message welcomeMessage = MessageUtilities.createWelcome(654654);

        ByteBuffer messageInByteBuffer = null;
        try {
            messageInByteBuffer = welcomeMessage.getMessageInByteBuffer(giveMeASocket());
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] messageInBytes = messageInByteBuffer.array();

        Message wm2 = new Message( MessageType.getTypeByByte(messageInBytes[0]), Arrays.copyOfRange(messageInBytes, 5, messageInBytes.length));

        long socketId =  MessageUtilities.byteArrayToLong(Arrays.copyOfRange(wm2.body, 0, 8));

        try {
            PublicKey pk = KeyFactory.getInstance(MessageUtilities.DEFAULT_PAIR_KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(Arrays.copyOfRange(wm2.body, 8, wm2.body.length)));

            assertEquals(pk, MessageUtilities.getPublicKey());

        } catch (Exception e){
            e.printStackTrace();
        }





    }


    Socket giveMeASocket() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        Socket socket = new Socket();
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = new SecureRandom();
        int keyBitSize = 256;
        kg.init(keyBitSize, secureRandom);
        socket.setSecretKeyCiphers(kg.generateKey().getEncoded());
        socket.setMacGenerator(new byte[] {1,2,3});
        return socket;
    }
}
