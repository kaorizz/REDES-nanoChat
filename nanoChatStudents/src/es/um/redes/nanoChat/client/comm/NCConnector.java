package es.um.redes.nanoChat.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import es.um.redes.nanoChat.messageML.NCChatMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomListMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.messageML.NCSimpleMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NCConnector {
	
	private static final boolean VERBOSE_MODE = true;
	private static final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSSS");
	
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;
	
	public NCConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		//TODO Se crea el socket a partir de la dirección proporcionada 
		socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
		//TODO Se extraen los streams de entrada y salida
		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());
	}


	//Método para registrar el nick en el servidor. Nos informa sobre si la inscripción se hizo con éxito o no.
	public boolean registerNickname_UnformattedMessage(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//TODO Enviamos una cadena con el nick por el flujo de salida
		dos.writeUTF(nick);
		//TODO Leemos la cadena recibida como respuesta por el flujo de entrada 
		String rcv = dis.readUTF();
		//TODO Si la cadena recibida es NICK_OK entonces no está duplicado (en función de ello modificar el return)
		boolean resp = false;
		if (rcv.equals("NICK_OK")) {
			resp = true;
		}
		return resp;
	}

	
	//Método para registrar el nick en el servidor. Nos informa sobre si la inscripción se hizo con éxito o no.
	public boolean registerNickname(String nick) throws IOException {
		//Funcionamiento resumido: SEND(nick) and RCV(NICK_OK) or RCV(NICK_DUPLICATED)
		//Creamos un mensaje de tipo RoomMessage con opcode OP_NICK en el que se inserte el nick
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_NICK, nick);
		//Obtenemos el mensaje de texto listo para enviar
		String rawMessage = message.toEncodedString();
		//Escribimos el mensaje en el flujo de salida, es decir, provocamos que se envíe por la conexión TCP
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		//TODO Leemos el mensaje recibido como respuesta por el flujo de entrada
		NCSimpleMessage messageResp = (NCSimpleMessage)		NCMessage.readMessageFromSocket(dis);
		//TODO Analizamos el mensaje para saber si está duplicado el nick (modificar el return en consecuencia)
		byte opCode = messageResp.getOpcode();
		boolean resp = false;
		if (opCode == NCMessage.OP_NICK_OK) resp = true;
		return resp;
	}
	
	//Método para obtener la lista de salas del servidor
	public List<NCRoomDescription> getRooms() throws IOException {
		//Funcionamiento resumido: SND(GET_ROOMS) and RCV(ROOM_LIST)
		//TODO completar el método
		NCSimpleMessage messageSimple = (NCSimpleMessage)	NCMessage.makeSimpleMessage(NCMessage.OP_GET_ROOMLIST);
		String rawMessage = messageSimple.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		NCRoomListMessage resp = (NCRoomListMessage) NCMessage.readMessageFromSocket(dis);
		return resp.getDescriptions();
	}
	
	//Método para solicitar la entrada en una sala
	public boolean enterRoom(String room) throws IOException {
		//Funcionamiento resumido: SND(ENTER_ROOM<room>) and RCV(IN_ROOM) or RCV(REJECT)
		//TODO completar el método
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_ENTER, room);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		boolean resp = false;
		NCSimpleMessage messageResp = (NCSimpleMessage)	NCMessage.readMessageFromSocket(dis);
		byte opCode = messageResp.getOpcode();
		if (opCode == NCMessage.OP_ENTER_OK) resp = true;
		return resp;
	}
	
	//Método para salir de una sala
	public void leaveRoom(String room) throws IOException {
		//Funcionamiento resumido: SND(EXIT_ROOM)
		//TODO completar el método
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_EXIT, room);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
	}
	
	// Método para renombrar la sala actual
	public void renameRoom(String name) throws IOException {
		NCRoomMessage message = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_RENAME, name);
		String rawMessage = message.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
	}
	
	public boolean newRoom() throws IOException {
		NCSimpleMessage mensaje = (NCSimpleMessage) NCMessage.makeSimpleMessage(NCSimpleMessage.OP_NEW_ROOM);
		String rawMessage = mensaje.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		NCMessage resp = NCMessage.readMessageFromSocket(dis);
		if (resp.getOpcode() == NCMessage.OP_NEW_ROOM_OK) return true;
		return false;
	}
	
	//Método que utiliza el Shell para ver si hay datos en el flujo de entrada
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}
	
	//IMPORTANTE!!
	//TODO Es necesario implementar métodos para recibir y enviar mensajes de chat a una sala
	public void sendMessage(String user, String message) throws IOException {
		NCRoomMessage mensaje = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_SEND_MESSAGE, message);
		String rawMessage = mensaje.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
	}
	
	// Método para enviar un mensaje privado a un usuario de la sala
	public void sendPrivmessage(String user, String message) throws IOException {
		NCChatMessage mensaje = (NCChatMessage) NCMessage.makeChatMessage(NCMessage.OP_SEND_PRIVMESSAGE, user, message);
		String rawMessage = mensaje.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
	}
	
	public NCMessage receiveMessage() throws IOException{
		return NCMessage.readMessageFromSocket(dis);
	}
	
	
	//Método para pedir la descripción de una sala
	public NCRoomDescription getRoomInfo(String room) throws IOException {
		//Funcionamiento resumido: SND(GET_ROOMINFO) and RCV(ROOMINFO)
		//TODO Construimos el mensaje de solicitud de información de la sala específica
		NCRoomMessage mensaje = (NCRoomMessage) NCMessage.makeRoomMessage(NCMessage.OP_GET_INFO, room);
		String rawMessage = mensaje.toEncodedString();
		showMessageInConsole(rawMessage);
		dos.writeUTF(rawMessage);
		//TODO Recibimos el mensaje de respuesta
		NCRoomListMessage desc =(NCRoomListMessage) NCMessage.readMessageFromSocket(dis);
		//TODO Devolvemos la descripción contenida en el mensaje
		return desc.getDescriptions().get(0);
	}
	
	//Método para cerrar la comunicación con la sala
	//TODO (Opcional) Enviar un mensaje de salida del servidor de Chat
	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		} finally {
			socket = null;
		}
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
