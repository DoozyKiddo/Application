import java.net.*;
import java.io.*;
import java.util.*;

/*
 * Author: Ryan Yan
 */

// This class is a client that is run as a console
public class ChatClient  {

	// allows reading from the socket
	private ObjectInputStream socketInput;
	// allows writing on the socket
	private ObjectOutputStream socketOutput;
	private Socket socket;
	
	private String server, username;
	private int port;
	
	// constructor
	ChatClient(String aServer, int aPort, String aUsername) {
		server = aServer;
		port = aPort;
		username = aUsername;
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		int portNumber = 2000;
		String serverAddress = "localhost";
		String userName = "Anonymous";
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Enter the username: ");
		userName = scan.nextLine();
		while (userName.equals("") | userName.contains(" ") | userName.contains(":") | userName.contains(">") | userName.contains("<")) {
			System.out.println("Rejected, username must not be null, have spaces, have colons, or have greater than/less than symbols. Please enter another username: ");
			userName = scan.nextLine();
		}
		
		// create the Client object
		ChatClient client = new ChatClient(serverAddress, portNumber, userName);
		
		// try to connect to the server and return if not connected
		if(!client.start())
			return;
		System.out.println("\nWelcome to the chatroom.\n");
		
		// infinite loop to get user input
		while(true) {
			// read message from user
			String msg = scan.nextLine();
			// sends message to disconnect user
			if(msg.equals("<bye>")) {
				client.sendMessage(new ChatMessage(ChatMessage.BYE, ""));
				break;
			}
			// message to check who are present in chatroom
			else if(msg.equals("<users>")) {
				client.sendMessage(new ChatMessage(ChatMessage.USERS, ""));				
			}
			// regular text message
			else {
				client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
			}
		}
		// close scanner and client
		scan.close();
		client.disconnect();	
	}
	
	// method to start chat
	public boolean start() {
		// try to connect to the server
		try {
			socket = new Socket(server, port);
		} 
		// if connection attempt failed
		catch(Exception ec) {
			System.out.println("Error connecting to server:" + ec);
			return false;
		}
		
		System.out.println("Connection accepted.");
	
		// try to create input and output streams
		try {
			socketInput  = new ObjectInputStream(socket.getInputStream());
			socketOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		// if stream creation failed
		catch (IOException eIO) {
			System.out.println("Exception creating new Input/output Streams: " + eIO);
			return false;
		}

		// creates the Thread to listen from the server 
		new ListenFromServer().start();
		// send username to the server
		try
		{
			socketOutput.writeObject(username);
		}
		catch (IOException eIO) {
			System.out.println("Exception doing login : " + eIO);
			disconnect();
			return false;
		}
		// success we inform the caller that it worked
		return true;
	}
	
	// method to send message to server
	public void sendMessage(ChatMessage msg) {
		try {
			socketOutput.writeObject(msg);
		}
		catch(IOException e) {
			System.out.println("Exception writing to server: " + e);
		}
	}

	// try to close input stream, output stream, and 
	private void disconnect() {
		try { 
			if(socketInput != null) {
				socketInput.close();
			}
		}
		catch(Exception e) {
		}
		try {
			if(socketOutput != null) {
				socketOutput.close();
			}
		}
		catch(Exception e) {
		}
        try {
			if(socket != null) {
				socket.close();
			}
		}
		catch(Exception e) {
		}		
	}

	// class that waits for messages from server
	public class ListenFromServer extends Thread {

		public void run() {
			while(true) {
				try {
					// reads message from the input datastream
					String msg = (String)socketInput.readObject();
					System.out.println(msg);
				}
				catch(IOException e) {
					System.out.println("Server has successfully closed the connection.");
					break;
				}
				catch(ClassNotFoundException e) {
				}
			}
		}
	}
}
