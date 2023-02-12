package Non.blocking.server;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public abstract class Database {
    private static HashMap<String, byte[]> usersAndPasswords = null;

    /**
     * Returns true if user exist and password matches.
     * @param userName
     * @param password
     * @return
     */
    public static boolean canILogIn (String userName, byte[] password){
        if (usersAndPasswords == null){
            usersAndPasswords = new HashMap<String, byte[]>();
            usersAndPasswords.put("User1","User1p".getBytes(StandardCharsets.UTF_8) );
            usersAndPasswords.put("User2","User2p".getBytes(StandardCharsets.UTF_8) );
            usersAndPasswords.put("User3","User3p".getBytes(StandardCharsets.UTF_8) );
        }

        byte[] passwordDB = usersAndPasswords.get(userName);

        return (passwordDB != null) && (Arrays.compare(passwordDB, password) == 0);
    }

    public static void newUser (String userName, byte[] password) throws ServerException{
        if (usersAndPasswords == null){
            usersAndPasswords = new HashMap<String, byte[]>();
            usersAndPasswords.put("User1","User1p".getBytes(StandardCharsets.UTF_8) );
            usersAndPasswords.put("User2","User2p".getBytes(StandardCharsets.UTF_8) );
            usersAndPasswords.put("User3","User3p".getBytes(StandardCharsets.UTF_8) );
        }

        if (usersAndPasswords.get(userName) == null) {
            usersAndPasswords.put(userName, password);
        } else {
            throw new ServerException(ServerException.Type.NEW_USER_PROBLEM);
        }


    }

    public static Set<String> getAllUsers(){
        return usersAndPasswords.keySet();
    }




}
