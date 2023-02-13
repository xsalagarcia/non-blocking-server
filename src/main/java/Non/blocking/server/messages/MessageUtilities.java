package Non.blocking.server.messages;

import Non.blocking.server.ServerException;
import javax.crypto.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

public abstract class MessageUtilities {

    private static final byte[] KEY_FOR_MAC = {1,5,23,1,4,63,78,23,3,56,110,3};

    public static final String DEFAULT_SECRET_KEY_ALGORITHM = "AES";

    public static final String DEFAULT_PAIR_KEY_ALGORITHM = "RSA";

    public static final String DEFAULT_MAC_SK_ALGORITHM = "RawBytes";

    public static final int SHARED_SECRET_KEY_COMPLEMENT = 98123;

    public static final String SERVER_NAME = "server";

    private static KeyPair keyPair = null;

    private static Cipher cipherForDecryptWithPrivate = null;

    private static Cipher cipherForEncryptWithPrivate = null;

    //Use nextMessageId++
    private static long nextMessageId = 0;








    /*
    public static byte[] generateMAC (byte[][] datas){
        if (mac == null){
            try {
                mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(KEY_FOR_MAC, "RawBytes"));
            }catch (NoSuchAlgorithmException | InvalidKeyException e){
                e.printStackTrace();
            }
        }

        for (byte[] data : datas){
            mac.update(data);
        }

        return mac.doFinal();
    }*/



    /**
     * Returns the publicKey (static value). If keyPair doesn't exist, it will create it (only one time).
     * @return publicKey.
     */
    public static PublicKey getPublicKey () {
        if (keyPair == null) {
            KeyPairGenerator kpg = null;
            try {
                kpg = KeyPairGenerator.getInstance("RSA");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            kpg.initialize(2048);
            keyPair = kpg.generateKeyPair();
        }

        return keyPair.getPublic();
    }


    /**
     * Given a byte[] decrypts it with the private key.
     * @param bytes
     * @return byte[] decrypted.
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] decryptWithPrivateKey(byte[] bytes) throws ServerException {

        try {
            if (cipherForDecryptWithPrivate == null) {
                cipherForDecryptWithPrivate = Cipher.getInstance(DEFAULT_PAIR_KEY_ALGORITHM);
                cipherForDecryptWithPrivate.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            }
            return cipherForDecryptWithPrivate.doFinal(bytes);
        } catch (Exception e) {
            throw new ServerException(ServerException.Type.PRIVATE_KEY_PROBLEM);
        }

    }

    /**
     * Given a byte[], decrypts it with the private key. Just for testing.
     * @param bytes
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte [] encryptWithPrivateKey(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException {
        if (cipherForDecryptWithPrivate == null) {
            try {
                cipherForEncryptWithPrivate = Cipher.getInstance(MessageUtilities.DEFAULT_PAIR_KEY_ALGORITHM);
                cipherForEncryptWithPrivate.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return cipherForDecryptWithPrivate.doFinal(bytes);
    }

    /**
     * Creates the welcome message. Contains public socketId + publicKey.
     * @return
     */
    public static Message createWelcome(long socketId){
        return new Message(MessageType.WELCOME, SERVER_NAME, "client", nextMessageId++,
                new byte[][] {longToByteArray(socketId),getPublicKey().getEncoded()});
    }

    public static Message createACK(String destination, long referredMessageId) {
        return new Message(MessageType.ACK, SERVER_NAME, destination, nextMessageId++,
                MessageUtilities.longToByteArray(referredMessageId) );
    }

    public static Message createNACK(String destination, long referredMessageId, String comments) {
        return new Message(MessageType.NACK, SERVER_NAME, destination, nextMessageId++,
                new byte[][]{MessageUtilities.longToByteArray(referredMessageId), comments.getBytes(StandardCharsets.UTF_8)});
    }


    private static short byteArrayToShort(byte[] b) {
        return  (short) (((b[0] & 0xFF) << 8 ) |
                ((b[1] & 0xFF) << 0 ));
    }

    private static byte[] shortToByteArray(short s) {
        return new byte[] {
                (byte)(s >>> 8),
                (byte)s};
    }

    public static byte[] intToByteArray (int i) {
        return new byte[] {
                (byte)(i >>> 24),
                (byte)(i >>> 16),
                (byte)(i >>> 8),
                (byte) i };
    }

    public static byte[] longToByteArray(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public static int byteArrayToint(final byte[] b){
        return ((b[0] & 0xFF) << 24) |
                ((b[1] & 0xFF) << 16) |
                ((b[2] & 0xFF) << 8 ) |
                ((b[3] & 0xFF) << 0 );
    }

    public static long byteArrayToLong(final byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    public static byte[] byteArraysToByteArray(byte[][] arrays){

        int newLength = 0;
        for(byte[] array : arrays) {
            newLength += array.length;
        }

        byte[] finalArray = null;

        int position = 0;

        for (byte[] array: arrays) {
            if (finalArray == null) {
                finalArray = Arrays.copyOf(array, newLength);
                position = array.length;
            } else {
                System.arraycopy(array, 0, finalArray, position, array.length);
                position += array.length;
            }
        }

        return finalArray;
    }

    public static long newMessageId(){
        return nextMessageId++;
    }







}
