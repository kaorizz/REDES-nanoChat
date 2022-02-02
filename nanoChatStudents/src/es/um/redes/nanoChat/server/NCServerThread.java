package es.um.redes.nanoChat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import es.um.redes.nanoChat.messageML.NCChatMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomListMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCSimpleMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoom;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * A new thread runs for each connected client
 */
public class NCServerThread extends Thread {
	
	private static final boolean VERBOSE_MODE = true;
	private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSSS");
	
	private Socket socket = null;
	//Manager global compartido entre los Threads
	private NCServerManager serverManager = null;
	//Input and Output Streams
	private DataInputStream dis;
	private DataOutputStream dos;
	//Usuario actual al que atiende este Thread
	String user;
	//RoomManager actual (dependerá de la sala a la que entre el usuario)
	NCRoomManager roomManager;
	//Sala actual
	String currentRoom;

	//Inicialización de la sala
	public NCServerThread(NCServerManager manager, Socket socket) throws IOException {
		super("NCServerThread");
		this.socket = socket;
		this.serverManager = manager;
	}

	//Main loop
	public void run() {
		try {
			//Se obtienen los streams a partir del Socket
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			//En primer lugar hay que recibir y verificar el nick
			receiveAndVerifyNickname();
			//INFO
			System.out.println("* User "+user+" connected");
			//Mientras que la conexión esté activa entonces...
			while (true) {
				//TODO Obtenemos el mensaje que llega y analizamos su código de operación
				NCMessage message = NCMessage.readMessageFromSocket(dis);
				switch (message.getOpcode()) {
					//TODO 1) si se nos pide la lista de salas se envía llamando a sendRoomList();
					case NCMessage.OP_ROOMLIST:
					{
						sendRoomList();
						break;
					}
					//TODO 2) Si se nos pide entrar en la sala entonces obtenemos el RoomManager de la sala,
					case NCMessage.OP_ENTER:
					{
						enterRoom(message);
						break;
					}
					//TODO 2) notificamos al usuario que ha sido aceptado y procesamos mensajes con processRoomMessages()
					//TODO 2) Si el usuario no es aceptado en la sala entonces se le notifica al cliente
					// Si se nos pide crear una sala, la creamos
					case NCMessage.OP_NEW_ROOM:
					{
						newRoom();
						break;
					}
					default:
					{
						System.err.println("* Unknown message type received");
						
					}
				}
			}
		} catch (Exception e) {
			//If an error occurs with the communications the user is removed from all the managers and the connection is closed
			System.out.println("* User "+ user + " disconnected.");
			serverManager.leaveRoom(user, currentRoom);
			serverManager.removeUser(user);
		}
		finally {
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	private void newRoom() throws IOException {
		NCRoom sala = new NCRoom();
		serverManager.registerRoomManager(sala);
		NCSimpleMessage mensaje = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_NEW_ROOM_OK);
		String rawMessage = mensaje.toEncodedString();
		dos.writeUTF(rawMessage);
	}

	// Método con el que podemos entrar a una sala
	private void enterRoom(NCMessage message) throws IOException {
		NCRoomMessage mensaje = (NCRoomMessage) message;
		String sala = mensaje.getName();
		roomManager = serverManager.enterRoom(user, sala, socket);
		if (roomManager == null) {
			NCSimpleMessage error = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_ENTER_ERROR);
			String rawMessage = error.toEncodedString();
			dos.writeUTF(rawMessage);
		}
		else {
			currentRoom = sala;
			NCSimpleMessage enter_ok = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_ENTER_OK);
			String rawMessage = enter_ok.toEncodedString();
			dos.writeUTF(rawMessage);
			NCRoomMessage broad = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ENTER, user);
			roomManager.broadcastMessage(user, broad.toEncodedString());
			processRoomMessages();
		}
	}

	//Obtenemos el nick y solicitamos al ServerManager que verifique si está duplicado
	private void receiveAndVerifyNickname() throws IOException {
		//La lógica de nuestro programa nos obliga a que haya un nick registrado antes de proseguir
		//TODO Entramos en un bucle hasta comprobar que alguno de los nicks proporcionados no está duplicado
		boolean nickOk = false;
		while (!nickOk) {
			NCRoomMessage message = (NCRoomMessage)		NCMessage.readMessageFromSocket(dis);
			if (message != null) {
				byte opCode = message.getOpcode();
				if (opCode == NCMessage.OP_NICK) {
					String nick = message.getName();
					nickOk = serverManager.addUser(nick);
					NCSimpleMessage messageResp;
					if (nickOk) {
						user = nick;
						messageResp = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_NICK_OK);
					}
					else {
						messageResp = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_NICK_ERROR);
					}
					String rawMessageResp = messageResp.toEncodedString();
					showMessageInConsole(rawMessageResp);
					dos.writeUTF(rawMessageResp);
				}
			}
		}
		//TODO Validar el nick utilizando el ServerManager - addUser()
		//TODO Contestar al cliente con el resultado (éxito o duplicado)
	}

	//Mandamos al cliente la lista de salas existentes
	private void sendRoomList()  {
		//TODO La lista de salas debe obtenerse a partir del RoomManager y después enviarse mediante su mensaje correspondiente
		List<NCRoomDescription> roomList = serverManager.getRoomList(null);
		NCRoomListMessage messageResp = (NCRoomListMessage) NCMessage.makeRoomListMessage(NCMessage.OP_ROOMLIST, roomList);
		String rawMessageResp = messageResp.toEncodedString();
		showMessageInConsole(rawMessageResp);
		try {
			dos.writeUTF(rawMessageResp);
		} catch (IOException e) {
			System.err.println("Error: roomlist no enviado");
			e.printStackTrace();
		}
	}

	private void processRoomMessages() throws IOException  {
		//TODO Comprobamos los mensajes que llegan hasta que el usuario decida salir de la sala
		boolean exit = false;
		while (!exit) {
			//TODO Se recibe el mensaje enviado por el usuario
			NCMessage mensaje = (NCMessage) NCMessage.readMessageFromSocket(dis);
			//TODO Se analiza el código de operación del mensaje y se trata en consecuencia
			byte codigo = mensaje.getOpcode();
			if (codigo == NCMessage.OP_SEND_MESSAGE) sendMessage(mensaje);
			else if (codigo == NCMessage.OP_SEND_PRIVMESSAGE) sendPrivMessage(mensaje);
			else if (codigo == NCMessage.OP_RENAME) sendRename(mensaje);
			else if (codigo == NCMessage.OP_GET_INFO) sendRoomInfo(mensaje);
			else {
				exit = true;
				sendExitB(mensaje);
			}
		}
	}
	
	private void sendMessage(NCMessage mensaje) throws IOException {
		NCRoom roomM = (NCRoom) roomManager;
		roomM.setLastMessage((new Date()).getTime());
		NCRoomMessage mensajechat = (NCRoomMessage) mensaje;
		NCChatMessage mensajechatb = (NCChatMessage) NCMessage.makeChatMessage(NCMessage.OP_SEND_MESSAGEB, user, mensajechat.getName());
		roomManager.broadcastMessage(user, mensajechatb.toEncodedString());
	}
	
	private void sendPrivMessage(NCMessage mensaje) throws IOException {
		NCChatMessage privmensaje = (NCChatMessage) mensaje;
		NCChatMessage envmensaje = (NCChatMessage) NCMessage.makeChatMessage(NCMessage.OP_SEND_PRIVMESSAGE, user, privmensaje.getMessage());
		roomManager.privateMessage(user, privmensaje.getName(), envmensaje.toEncodedString());
	}
	
	private void sendRename(NCMessage mensaje) throws IOException {
		NCRoomMessage rename = (NCRoomMessage) mensaje;
		boolean mod = serverManager.renameRoom(currentRoom, rename.getName());
		if (!mod) {
			NCSimpleMessage error = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCMessage.OP_RENAME_ERROR);
			String errorrename = error.toEncodedString();
			dos.writeUTF(errorrename);
		}
		else {
			currentRoom = rename.getName();
			NCRoomMessage rename_ok = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_RENAME_OK, rename.getName());
			roomManager.broadcastMessage(user, rename_ok.toEncodedString());
		}
		
	}
	
	private void sendRoomInfo(NCMessage mensaje) throws IOException {
		NCRoomMessage roominfo = (NCRoomMessage) mensaje;
		List<NCRoomDescription> salas = serverManager.getRoomList(roominfo.getName());
		NCRoomListMessage info = (NCRoomListMessage) NCMessage.makeRoomListMessage(NCMessage.OP_GET_INFO, salas);
		String mensajeinfo = info.toEncodedString();
		dos.writeUTF(mensajeinfo);
	}
	
	private void sendExitB(NCMessage mensaje) throws IOException {
		serverManager.leaveRoom(user, currentRoom);
		NCRoomMessage message_b_exit = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_EXIT, user);
		roomManager.broadcastMessage(user, message_b_exit.toEncodedString());
	}
	
	
	private void showMessageInConsole(String message) {
		if (VERBOSE_MODE) {
			Date currentDateTime = new Date(System.currentTimeMillis());
			String currentDateTimeText = formatter.format(currentDateTime);
			System.out.println("\nMESSAGE (" + currentDateTimeText + ") .....");
			System.out.println(message);
			System.out.println("........(END OF MESSAGE)\n");
		}
	}
	
}