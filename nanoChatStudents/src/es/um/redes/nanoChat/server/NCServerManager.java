package es.um.redes.nanoChat.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

/**
 * Esta clase contiene el estado general del servidor (sin la lógica relacionada con cada sala particular)
 */
class NCServerManager {

	//Primera habitación del servidor
	final static byte INITIAL_ROOM = 'A';
	final static String ROOM_PREFIX = "Room";
	//Siguiente habitación que se creará
	byte nextRoom;
	//Usuarios registrados en el servidor
	private Set<String> users = new HashSet<String>();
	//Habitaciones actuales asociadas a sus correspondientes RoomManagers
	private Map<String,NCRoomManager> rooms = new HashMap<String,NCRoomManager>();

	NCServerManager() {
		nextRoom = INITIAL_ROOM;
	}

	//Método para registrar un RoomManager
	public void registerRoomManager(NCRoomManager rm) {
		//TODO Dar soporte para que pueda haber más de una sala en el servidor
		String roomName = ROOM_PREFIX + (char) nextRoom;
		rooms.put(roomName, rm);
		rm.setRoomName(roomName);
		nextRoom++;
	}
	
	//Devuelve la descripción de las salas existentes
	public synchronized List<NCRoomDescription> getRoomList(String room) {
		//TODO Pregunta a cada RoomManager cuál es la descripción actual de su sala
		//TODO Añade la información al ArrayList
		List<NCRoomDescription> descripciones = new ArrayList<NCRoomDescription>();
		if (room!=null) {
			if (rooms.containsKey(room)) descripciones.add(rooms.get(room).getDescription());
		}
		else {
			for (String rm: rooms.keySet()) descripciones.add(rooms.get(rm).getDescription());
		}
		return descripciones;
	}


	//Intenta registrar al usuario en el servidor.
	public synchronized boolean addUser(String user) {
		//TODO Devuelve true si no hay otro usuario con su nombre
		//TODO Devuelve false si ya hay un usuario con su nombre
		return users.add(user);
	}

	//Elimina al usuario del servidor
	public synchronized void removeUser(String user) {
		//TODO Elimina al usuario del servidor
		users.remove(user);
	}

	//Un usuario solicita acceso para entrar a una sala y registrar su conexión en ella
	public synchronized NCRoomManager enterRoom(String u, String room, Socket s) {
		//TODO Verificamos si la sala existe
		//TODO Decidimos qué hacer si la sala no existe (devolver error O crear la sala)
		//TODO Si la sala existe y si es aceptado en la sala entonces devolvemos el RoomManager de la sala
		if (!rooms.containsKey(room)) return null;
		else {
			NCRoomManager roomManager = rooms.get(room);
			if (roomManager.registerUser(u, s)) return roomManager;
			else return null;
		}
	}

	//Un usuario deja la sala en la que estaba
	public synchronized void leaveRoom(String u, String room) {
		//TODO Verificamos si la sala existe
		//TODO Si la sala existe sacamos al usuario de la sala
		//TODO Decidir qué hacer si la sala se queda vacía
		NCRoomManager roomManager = rooms.get(room);
		if (roomManager!=null) roomManager.removeUser(u);
	}
	
	// Método para obtener la descripción de una sala existente
	public synchronized NCRoomDescription getRoomDescription(String room) {
		if (rooms.containsKey(room)) return rooms.get(room).getDescription();
		return null;
	}
	
	
	public synchronized boolean renameRoom(String room, String newName) {
		NCRoomManager roomManager = rooms.get(room);
		NCRoomManager nuevoroomManager = rooms.get(newName);
		
		if ((roomManager!=null) && (nuevoroomManager==null)) {
			roomManager.setRoomName(newName);
			rooms.remove(room);
			rooms.put(newName, roomManager);
			return true;
		}
		return false;
	}
}
