import java.io.*;

/*
 * Author: Ryan Yan
 */

// This class provides a way to send messages of different types between the server and clients,
// in which each type performs different functions
@SuppressWarnings("serial")
public class ChatMessage implements Serializable {
	// The BYE type disconnects from the Server
	// The USERS type to receive the list of the users connected
	// The MESSAGE type an ordinary text message
	public static final int BYE = 0, USERS = 1, MESSAGE = 2;
	private int type;
	private String message;
	
	// constructor
	public ChatMessage(int aType, String aMessage) {
		type = aType;
		message = aMessage;
	}
	// method to get the type
	public int getType() {
		return type;
	}
	// method to get the message
	public String getMessage() {
		return message;
	}
}