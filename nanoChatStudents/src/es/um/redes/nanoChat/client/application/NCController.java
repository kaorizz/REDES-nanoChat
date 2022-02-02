package es.um.redes.nanoChat.client.application;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import es.um.redes.nanoChat.client.comm.NCConnector;
import es.um.redes.nanoChat.client.shell.NCCommands;
import es.um.redes.nanoChat.client.shell.NCShell;
import es.um.redes.nanoChat.directory.connector.DirectoryConnector;
import es.um.redes.nanoChat.messageML.NCChatMessage;
import es.um.redes.nanoChat.messageML.NCMessage;
import es.um.redes.nanoChat.messageML.NCRoomMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

public class NCController {
	//Diferentes estados del cliente de acuerdo con el autómata
	private static final byte PRE_CONNECTION = 1;
	private static final byte PRE_REGISTRATION = 2;
	private static final byte OFF_ROOM = 3;
	//Código de protocolo implementado por este cliente
	//TODO Cambiar para cada grupo
	private static final int PROTOCOL = 0;
	//Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;
	//Conector para enviar y recibir mensajes con el servidor de NanoChat
	private NCConnector ncConnector;
	//Shell para leer comandos de usuario de la entrada estándar
	private NCShell shell;
	//Último comando proporcionado por el usuario
	private byte currentCommand;
	//Nick del usuario
	private String nickname;
	//Sala de chat en la que se encuentra el usuario (si está en alguna)
	private String room;
	//Nuevo nombre para la sala en la que se encuentra el usuario
	private String newName;
	//Mensaje enviado o por enviar al chat
	private String chatMessage;
	//Dirección de internet del servidor de NanoChat
	private InetSocketAddress serverAddress;
	//Estado actual del cliente, de acuerdo con el autómata
	private byte clientStatus = PRE_CONNECTION;

	//Constructor
	public NCController() {
		shell = new NCShell();
	}

	//Devuelve el comando actual introducido por el usuario
	public byte getCurrentCommand() {		
		return this.currentCommand;
	}

	//Establece el comando actual
	public void setCurrentCommand(byte command) {
		currentCommand = command;
	}

	//Registra en atributos internos los posibles parámetros del comando tecleado por el usuario
	public void setCurrentCommandArguments(String[] args) {
		//Comprobaremos también si el comando es válido para el estado actual del autómata
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				nickname = args[0];
			break;
		case NCCommands.COM_ENTER:
			room = args[0];
			break;
		case NCCommands.COM_SEND:
			chatMessage = args[0];
			break;
		case NCCommands.COM_PRIVMESSAGE:
			nickname = args[0];
			chatMessage = args[1];
			break;
		case NCCommands.COM_RENAME:
			newName = args[0];
			break;
		default:
		}
	}

	//Procesa los comandos introducidos por un usuario que aún no está dentro de una sala
	public void processCommand() throws IOException {
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				registerNickName();
			else
				System.out.println("* You have already registered a nickname ("+nickname+")");
			break;
		case NCCommands.COM_ROOMLIST:
			//TODO LLamar a getAndShowRooms() si el estado actual del autómata lo permite
			//TODO Si no está permitido informar al usuario
			if (clientStatus == OFF_ROOM)
				getAndShowRooms();
			else
					System.out.println("* First of all, you must register with 'nick <name>'");
			break;
		case NCCommands.COM_ENTER:
			//TODO LLamar a enterChat() si el estado actual del autómata lo permite
			//TODO Si no está permitido informar al usuario
			if (clientStatus == OFF_ROOM) enterChat();
			else System.out.println("* First of all, you must register with 'nick <name>'");
			break;
		case NCCommands.COM_NEWROOM:
			if (clientStatus == OFF_ROOM) newRoom();
			else System.out.println("* First of all, you must register with 'nick <name>'");
			break;
		case NCCommands.COM_QUIT:
			//Cuando salimos tenemos que cerrar todas las conexiones y sockets abiertos
			ncConnector.disconnect();			
			directoryConnector.close();
			break;
		default:
		}
	}
	
	//Método para registrar el nick del usuario en el servidor de NanoChat
	private void registerNickName() {
		try {
			//Pedimos que se registre el nick (se comprobará si está duplicado)
			boolean registered = ncConnector.registerNickname(nickname);
			//TODO: Cambiar la llamada anterior a registerNickname() al usar mensajes formateados 
			if (registered) {
				//TODO Si el registro fue exitoso pasamos al siguiente estado del autómata
				clientStatus = OFF_ROOM;
				System.out.println("* Your nickname is now "+nickname);
			}
			else
				//En este caso el nick ya existía
				System.out.println("* The nickname is already registered. Try a different one.");			
		} catch (IOException e) {
			System.out.println("* There was an error registering the nickname");
		}
	}

	// Método para crear nuevas salas
	private void newRoom() throws IOException {
		// Llamamos al método newRoom de la clase NCConnector
		boolean nueva = ncConnector.newRoom();
		if (!nueva) {
			// Si no se ha podido crear, se lo notificamos al usuario
			System.out.println("* There was an error creating the new room");
		}
		else {
			// Si se ha podido crear, lo notificamos al usuario y cambiamos el valor de clientStatus a "OFF_ROOM"
			System.out.println("* The room was succesfully created");
			clientStatus = OFF_ROOM;
		}
	}
	
	//Método que solicita al servidor de NanoChat la lista de salas e imprime el resultado obtenido
	private void getAndShowRooms() {
		try {
			//TODO Lista que contendrá las descripciones de las salas existentes
			List<NCRoomDescription> roomList;
			//TODO Le pedimos al conector que obtenga la lista de salas ncConnector.getRooms()
			roomList = ncConnector.getRooms();
			//TODO Una vez recibidas iteramos sobre la lista para imprimir información de cada sala
			if (roomList != null) {
				for (NCRoomDescription roomDescription:roomList) {
					System.out.println(roomDescription.toPrintableString());
				}
			}
			else System.out.println("* There is not any room available");
		} catch (IOException e) {
			System.out.println("* There was an error getting the room list");
		}
	}

	//Método para tramitar la solicitud de acceso del usuario a una sala concreta
	private void enterChat() throws IOException {
		//TODO Se solicita al servidor la entrada en la sala correspondiente ncConnector.enterRoom()
		//TODO Si la respuesta es un rechazo entonces informamos al usuario y salimos
		//TODO En caso contrario informamos que estamos dentro y seguimos
		//TODO Cambiamos el estado del autómata para aceptar nuevos comandos
		boolean entrar = false;
		try {
			entrar = ncConnector.enterRoom(room);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (!entrar) {
			System.out.println("* Error: cannot enter the room");
		}
		else {
			System.out.println("* You entered the room");
			do {
				//Pasamos a aceptar sólo los comandos que son válidos dentro de una sala
				readRoomCommandFromShell();
				processRoomCommand();
			} while (currentCommand != NCCommands.COM_EXIT);
			System.out.println("* You are out of the room");
			//TODO Llegados a este punto el usuario ha querido salir de la sala, cambiamos el estado del autómata
			clientStatus = OFF_ROOM;
		}
		
		
	}

	//Método para procesar los comandos específicos de una sala
	private void processRoomCommand() throws IOException {
		switch (currentCommand) {
		case NCCommands.COM_ROOMINFO:
			//El usuario ha solicitado información sobre la sala y llamamos al método que la obtendrá
			try {
				getAndShowInfo();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case NCCommands.COM_SEND:
			//El usuario quiere enviar un mensaje al chat de la sala
			try {
				sendChatMessage();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case NCCommands.COM_SOCKET_IN:
			//En este caso lo que ha sucedido es que hemos recibido un mensaje desde la sala y hay que procesarlo
			processIncommingMessage();
			break;
		case NCCommands.COM_PRIVMESSAGE:
			// En este caso el usuario envía un mensaje privado a otro
			sendPrivMessage();
			break;
		case NCCommands.COM_RENAME:
			// En este caso el usuario se dispone a renombrar la sala en la que se encuentra
			renameRoom();
			break;
		case NCCommands.COM_EXIT:
			//El usuario quiere salir de la sala
			exitTheRoom();
			break;
		}		
	}

	//Método para solicitar al servidor la información sobre una sala y para mostrarla por pantalla
	private void getAndShowInfo() throws IOException{
		//TODO Pedimos al servidor información sobre la sala en concreto
		if (room!=null) {
			NCRoomDescription desc = ncConnector.getRoomInfo(room);
			//TODO Mostramos por pantalla la información
			System.out.println(desc.toPrintableString());
		}
	}

	// Método para renombrar la sala en la que nos encontramos
	private void renameRoom() throws IOException {
		ncConnector.renameRoom(newName);
		room = newName;
	}
	
	//Método para notificar al servidor que salimos de la sala
	private void exitTheRoom() throws IOException {
		//TODO Mandamos al servidor el mensaje de salida
		try {
			ncConnector.leaveRoom(room);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//TODO Cambiamos el estado del autómata para indicar que estamos fuera de la sala
		clientStatus = OFF_ROOM;
	}
	
	//Método para enviar un mensaje privado a un usuario de la sala
	private void sendPrivMessage() throws IOException {
		ncConnector.sendPrivmessage(nickname, chatMessage);
	}

	//Método para enviar un mensaje al chat de la sala
	private void sendChatMessage() throws IOException{
		//TODO Mandamos al servidor un mensaje de chat
		ncConnector.sendMessage(nickname, chatMessage);
	}

	//Método para procesar los mensajes recibidos del servidor mientras que el shell estaba esperando un comando de usuario
	private void processIncommingMessage() throws IOException {		
		//TODO Recibir el mensaje
		NCMessage mensaje = ncConnector.receiveMessage();
		//TODO En función del tipo de mensaje, actuar en consecuencia
		//TODO (Ejemplo) En el caso de que fuera un mensaje de chat de broadcast mostramos la información de quién envía el mensaje y el mensaje en sí
		byte op = mensaje.getOpcode();
		switch(op) {
		case NCMessage.OP_ENTER:
			NCRoomMessage roomMessage = (NCRoomMessage) mensaje;
			System.out.println("* The user "+roomMessage.getName()+" has joined the room.");
			break;
		case NCMessage.OP_SEND_MESSAGEB:
			NCChatMessage chatMessage = (NCChatMessage) mensaje;
			if (!nickname.equals(chatMessage.getName()))
				System.out.println(chatMessage.getName()+": "+chatMessage.getMessage());
			break;
		case NCMessage.OP_SEND_PRIVMESSAGE:
			NCChatMessage chatMessage2 = (NCChatMessage) mensaje;
			if (!nickname.equals(chatMessage2.getName()))
				System.out.println("(*Private*) "+chatMessage2.getName()+": "+chatMessage2.getMessage());
			break;
		case NCMessage.OP_RENAME_OK:
			NCRoomMessage roomMessage2 = (NCRoomMessage) mensaje;
			room = roomMessage2.getName();
			System.out.println("Nombre de la sala cambiado a: "+room);
			break;
		case NCMessage.OP_RENAME_ERROR:
			System.out.println("Error al intentar cambiar el nombre de la sala");
			break;
		case NCMessage.OP_EXIT:
			NCRoomMessage roomMessage3 = (NCRoomMessage) mensaje;
			System.out.println("* The user "+roomMessage3.getName()+" has left the room.");
			break;
		}
		
	}

	//MNétodo para leer un comando de la sala 
	public void readRoomCommandFromShell() {
		//Pedimos un nuevo comando de sala al shell (pasando el conector por si nos llega un mensaje entrante)
		shell.readChatCommand(ncConnector);
		//Establecemos el comando tecleado (o el mensaje recibido) como comando actual
		setCurrentCommand(shell.getCommand());
		//Procesamos los posibles parámetros (si los hubiera)
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	//Método para leer un comando general (fuera de una sala)
	public void readGeneralCommandFromShell() {
		//Pedimos el comando al shell
		shell.readGeneralCommand();
		//Establecemos que el comando actual es el que ha obtenido el shell
		setCurrentCommand(shell.getCommand());
		//Analizamos los posibles parámetros asociados al comando
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	//Método para obtener el servidor de NanoChat que nos proporcione el directorio
	public boolean getServerFromDirectory(String directoryHostname) {
		//Inicializamos el conector con el directorio y el shell
		System.out.println("* Connecting to the directory...");
		//Intentamos obtener la dirección del servidor de NanoChat que trabaja con nuestro protocolo
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
			serverAddress = directoryConnector.getServerForProtocol(PROTOCOL);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			serverAddress = null;
		}
		//Si no hemos recibido la dirección entonces nos quedan menos intentos
		if (serverAddress == null) {
			System.out.println("* Check your connection, the directory is not available.");		
			return false;
		}
		else return true;
	}
	
	//Método para establecer la conexión con el servidor de Chat (a través del NCConnector)
	public boolean connectToChatServer() {
			try {
				//Inicializamos el conector para intercambiar mensajes con el servidor de NanoChat (lo hace la clase NCConnector)
				ncConnector = new NCConnector(serverAddress);
			} catch (IOException e) {
				System.out.println("* Check your connection, the game server is not available.");
				serverAddress = null;
			}
			//Si la conexión se ha establecido con éxito informamos al usuario y cambiamos el estado del autómata
			if (serverAddress != null) {
				System.out.println("* Connected to "+serverAddress);
				clientStatus = PRE_REGISTRATION;
				return true;
			}
			else return false;
	}

	//Método que comprueba si el usuario ha introducido el comando para salir de la aplicación
	public boolean shouldQuit() {
		return currentCommand == NCCommands.COM_QUIT;
	}

}
