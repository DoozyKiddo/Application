import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Author: Ryan Yan
 */

// This class is a server that is run as a console
public class ChatServer {
	private static int uniqueId;
	// ArrayList that keeps track of all the clients
	private ArrayList<ClientThread> clientList;
	private SimpleDateFormat sdf;
	private int port;
	// checks if server is currently running
	private boolean continueRunning;
	
	//constructor
	public ChatServer(int aPort) {
		port = aPort;
		// to display hh:mm:ss
		sdf = new SimpleDateFormat("HH:mm:ss");
		// an ArrayList to keep the list of the Client
		clientList = new ArrayList<ClientThread>();
	}
	
	public static void main(String[] args) {
		int portNumber = 2000;
		// create a server object and start it
		ChatServer server = new ChatServer(portNumber);
		server.start();
	}
	
	public void start() {
		continueRunning = true;
		//create socket server and wait for connection requests 
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			// infinite loop to wait for connections
			while(continueRunning) {
				display("Server waiting for Clients on port " + port + ".");
				// accept connection if requested from client
				Socket socket = serverSocket.accept();
				// break if server stopped
				if(!continueRunning)
					break;
				// if client is connected, create its thread
				ClientThread clientThread = new ClientThread(socket);
				//add this client to ArrayList
				clientList.add(clientThread);
				clientThread.start();
			}
			// try to stop the server
			try {
				serverSocket.close();
				for(int i = 0; i < clientList.size(); i++) {
					ClientThread clientThread = clientList.get(i);
					try {
					// close all data streams and socket
					clientThread.socketInput.close();
					clientThread.socketOutput.close();
					clientThread.socket.close();
					}
					catch(IOException e) {
					}
				}
			}
			catch(Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		}
		catch (IOException e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}
	
	// to stop the server
	@SuppressWarnings("resource")
	protected void stop() {
		continueRunning = false;
		try {
			new Socket("localhost", port);
		}
		catch(Exception e) {
		}
	}
	
	// Display an event to the console
	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);
	}
	
	// to broadcast a message to all Clients
	private synchronized boolean broadcast(String message) {
		// add timestamp to the message
		String time = sdf.format(new Date());
		
		// to check if message is private i.e. client to client message
		boolean isPrivate = false;
		int index1 = message.indexOf("<private | ");
		int index2 = -1;
		if(index1 != -1) {
			index2 = message.indexOf("> ", index1);
			if (index2 != -1 ) {
				isPrivate = true;
			}
		}
		
		// if private message, send message to mentioned username only
		if(isPrivate == true) {
			int index0 = message.indexOf(':');
			String user = message.substring(0, index0);
			String checkUser = message.substring(index1 + 11, index2);
			message = user + ": " + message.substring(index2 + 2);
			String messageLf = time + " " + message;
			boolean found = false;
			// we loop in reverse order to find the mentioned username
			for(int y = clientList.size(); --y>=0;) {
				ClientThread ct1=clientList.get(y);
				String check=ct1.getUsername();
				if(check.equals(checkUser)) {
					// try to write to the Client if it fails remove it from the list
					if(!ct1.writeMessage(messageLf)) {
						clientList.remove(y);
						display("Disconnected client " + ct1.username + " removed from list.");
					}
					// username found and delivered the message
					found = true;
					break;
				}
			}
			// mentioned user not found, return false
			if(found != true) {
				return false; 
			}
			// if message is a broadcast message
		} else {
			String messageLf = time + " " + message;
			// display message
			System.out.print(messageLf);
			
			// we loop in reverse order in case we would have to remove a Client
			// because it has disconnected
			for(int i = clientList.size(); --i >= 0;) {
				ClientThread clientThread = clientList.get(i);
				// try to write to the Client if it fails remove it from the list
				if(!clientThread.writeMessage(messageLf)) {
					clientList.remove(i);
					display("Disconnected user " + clientThread.username + " has been removed from the username list.");
				}
			}
		}
		return true;
	}
	
	public boolean isUsernameTaken(String username) {
		for (int i = 0; i < clientList.size(); i++) {
			ClientThread clientThread = clientList.get(i);
			if (clientThread.getUsername().equals("username")) {
				return true;
			}
		}
		return false;
	}
	
	// if client sent <bye> message to exit
	public synchronized void remove(int id) {
		
		String disconnectedClient = "";
		// scan the array list until we found the Id
		for(int i = 0; i < clientList.size(); i++) {
			ClientThread clientThread = clientList.get(i);
			// if found remove it
			if(clientThread.id == id) {
				disconnectedClient = clientThread.getUsername();
				clientList.remove(i);
				break;
			}
		}
		broadcast(disconnectedClient + " has disconnected from the chat room.");
	}

	// A thread instance that is used for each client
	public class ClientThread extends Thread {
		Socket socket;
		ObjectInputStream socketInput;
		ObjectOutputStream socketOutput;
		int id;
		String username;
		ChatMessage chatMessage;

		// Constructor
		ClientThread(Socket aSocket) {
			// a unique id
			id = ++uniqueId;
			socket = aSocket;
			System.out.println("Thread trying to create Object Input/Output Streams");
			try {
				socketOutput = new ObjectOutputStream(socket.getOutputStream());
				socketInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String)socketInput.readObject();
				broadcast(username + " has connected to the chat room.\n");
			}
			catch (IOException e) {
				display("Exception creating new input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException e) {
			}
		}
		
		public String getUsername() {
			return username;
		}

		public void setUsername(String aUsername) {
			username = aUsername;
		}

		// infinite loop to read and forward message
		public void run() {
			boolean continueRunning = true;
			while(continueRunning) {
				// read a String (which is an object)
				try {
					chatMessage = (ChatMessage) socketInput.readObject();
				}
				catch (IOException e) {
					display(username + " has disconnected without using the <bye> command.");
					break;				
				}
				catch(ClassNotFoundException e) {
					break;
				}
				// get the message from the ChatMessage object received
				String message = chatMessage.getMessage();

				// different actions based on type message
				switch(chatMessage.getType()) {
					case ChatMessage.BYE:
						display(username + " has disconnected using the <bye> command.");
						continueRunning = false;
						break;
					case ChatMessage.USERS:
						writeMessage("\n" + sdf.format(new Date()) + " List of the users connected");
						// send list of active clients
						for(int i = 0; i < clientList.size(); ++i) {
							ClientThread clientThread = clientList.get(i);
							writeMessage((i+1) + ") " + clientThread.username);
						}
						writeMessage("End of user list\n");
						break;
					case ChatMessage.MESSAGE:
						boolean confirmation =  broadcast(username + ": " + message + "\n");
						if(confirmation == false) {
							writeMessage("Unknown user.");
						}
						break;
				}
			}
			// if out of the loop then disconnected and remove from client list
			remove(id);
			close();
		}
		
		// close everything
		private void close() {
			try {
				if(socketOutput != null) {
					socketOutput.close();
				}
			}
			catch(Exception e) {
			}
			try {
				if(socketInput != null) {
					socketInput.close();
				}
			}
			catch(Exception e) {	
			}
			try {
				if(socket != null) {
					socket.close();
				}
			}
			catch (Exception e) {
			}
		}

		// write a String to the Client output stream
		private boolean writeMessage(String msg) {
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				socketOutput.writeObject(msg);
			}
			catch(IOException e) {
				display("Error sending message to " + username + ".");
				display(e.toString());
			}
			return true;
		}
	}
}
