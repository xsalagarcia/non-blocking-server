package Non.blocking.server;

/**
 * Custom server exceptions.
 */
public class ServerException extends Exception {

    public Type type;
    public long messageId = 0;

    public String clientName;

    public ServerException (Type type) {
        super (type.message);
        this.type = type;
    }

    public ServerException (Type type,  long messageId) {
        super (type.message);
        this.messageId = messageId;
        this.type = type;
    }


    public enum Type {
        MAC_NOT_EQUAL ("[ERROR] MAC doesn't match."),
        UNKNOWN_MESSAGE_TYPE ("[ERROR] Message type not recognized."),
        //when for that message type secret key is needed to be set.
        SECRET_KEY_NEEDED("[ERROR] Has to have secret key."),
        LOG_IN_NEEDED("[ERROR] Has to be logged."),
        LOG_IN_PROBLEM("[ERROR]: UserName doesn't exist, password is wrong or user is logged."),
        NEW_USER_PROBLEM("[ERROR], Username already exists"),
        WRONG_DESTINATION("[ERROR]: Destination doesn't exist."),
        SECRET_KEY_PROBLEM("[ERROR]: Secret key problem."),
        PROBLEM_SERIALIZING_USERS ("[ERROR]: Problem serializing users."),
        CONNECTION_PROBLEM("[ERROR]: Connection problem."),
        PRIVATE_KEY_PROBLEM("[ERROR]: Private key problem.");



        public final String message;

        Type(String message) {
            this.message = message;
        }

    }
}
