package es.um.redes.nanoChat.server.roomManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class NCRoom extends NCRoomManager {

	private Map<String, Socket> usuarios;
	private long lastMessage;
	
	public NCRoom() {
		usuarios = new HashMap<String, Socket>();
		lastMessage = 0;
	}
	
	@Override
	public boolean registerUser(String user, Socket soc) {
		if (!usuarios.containsKey(user)) {
			usuarios.put(user, soc);
			return true;
		}
		return false;
	}
	
	@Override
	public void privateMessage(String user, String des, String msg) throws IOException {
		for (String usu:usuarios.keySet()) {
			if (des.equals(usu)) {
				Socket s = usuarios.get(des);
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				dos.writeUTF(msg);
			}
		}
	}
	
	@Override
	public void broadcastMessage(String user, String msg) throws IOException {
		for (String usu:usuarios.keySet()) {
			if (!usu.equals(user)) {
				Socket s = usuarios.get(usu);
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				dos.writeUTF(msg);
			}
		}
	}
	
	
	@Override
	public NCRoomDescription getDescription() {
		List<String> users = new ArrayList<String>();
		for (String usu:usuarios.keySet()) {
			users.add(usu);
		}
		NCRoomDescription desc = new NCRoomDescription(roomName, users, lastMessage);
		return desc;
	}
	
	@Override
	public int usersInRoom() {
		return 0;
	}
	
	public void setRoomName(String name) {
		roomName=name;
	}
	
	public void setLastMessage(long lastmsg) {
		lastMessage=lastmsg;
	}
	
	public void removeUser(String user) {
		usuarios.remove(user);
	}
	
	
}
