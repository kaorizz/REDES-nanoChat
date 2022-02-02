package es.um.redes.nanoChat.messageML;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

/*
 * ROOM
----

<message>
<operation>operation</operation>
	<rooms>
		<name>sala1</name>
			<users>
				<nick>user1</nick>
				<nick>user2</nick>
				...
			</users>
		<name>sala2</name>
		<name>sala3</name>
		...
	</rooms>
</message>

Operaciones válidas:

Nick
*/

public class NCRoomListMessage extends NCMessage {

	private static List<NCRoomDescription> descripciones;
	
	static private final String ROOMS_MARK = "rooms";
	static private final String USERS_MARK = "users";
	static private final String NAME_MARK = "name";
	static private final String NICK_MARK = "nick";
	
	private static final String RE_GROUP = "<name>(.*?)</name>[\n\t]*<users>([\n\t]*<nick>(.*?)</nick>)*[\n\t]*</users>";
	
	public NCRoomListMessage(byte tipo, List<NCRoomDescription> salas) {
		this.opcode=tipo;
		NCRoomListMessage.descripciones=new ArrayList<NCRoomDescription>(salas);
	}
	
	public List<NCRoomDescription> getDescriptions() {
		return new ArrayList<NCRoomDescription>(descripciones);
	}
	
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("\t<"+ROOMS_MARK+">"+END_LINE);
		for (NCRoomDescription desc : descripciones) {
			sb.append("\t\t<"+NAME_MARK+">"+desc.roomName+"</"+NAME_MARK+">"+END_LINE);
			sb.append("\t\t\t<"+USERS_MARK+">"+END_LINE);
			for (String usu:desc.members) {
				sb.append("\t\t\t\t<"+NICK_MARK+">"+usu+"</"+NICK_MARK+">"+END_LINE);
			}
			sb.append("\t\t\t</"+USERS_MARK+">"+END_LINE);
			sb.append("\t\t</"+NAME_MARK+">"+desc.roomName+"</"+NAME_MARK+">"+END_LINE);
		}
		sb.append("\t</"+ROOMS_MARK+">"+END_LINE);
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);
		return sb.toString();
	}
	
	public static NCRoomListMessage readFromString(byte tipo, String message) {
		List<NCRoomDescription> salas = new ArrayList<NCRoomDescription>();
		
		//String found_name = null;
		//String found_nick = null;


		Pattern pat_group = Pattern.compile(RE_GROUP);
		//Pattern pat_nick = Pattern.compile(RE_NICK);

		
		Matcher mat_group = pat_group.matcher(message);
		if (mat_group.find()) {
			String sala = mat_group.group(1);
			String m = mat_group.group(3);
			List<String> mlist = new ArrayList<String>();
			mlist.add(m);
			NCRoomDescription n = new NCRoomDescription(sala, mlist, 0);
			salas.add(n);
		}
		/*
		Matcher mat_name = pat_name.matcher(message);
		Matcher mat_nick = pat_nick.matcher(message);
		if (mat_name.find()) {
			List<String> membs = new ArrayList<String>();
			if (mat_nick.find()) {
				for (String nombre : mat_nick.group(1)) {
					
					
					
				}
			}
			
			NCRoomDescription ncdesc = new NCRoomDescription(mat_name.group(1));
			Matcher mat_user = pat_name.matcher(message);
			if (mat_user.find());
			
			
		}
		for (NCRoomDescription desc : descripciones) {
			Matcher mat_name2 = pat_name.matcher(message);
			List<String> miembros = new ArrayList<String>();
			if (desc.roomName.equals(mat_name2.group(1))) {
				found_name = mat_name2.group(1);
				for (String usu:desc.members) {
					Matcher mat_nick2 = pat_nick.matcher(message);
					if (usu.equals(mat_nick2.group(1))) {
						found_nick = mat_nick2.group(1);
						miembros.add(found_nick);
					}
				}
			}
			NCRoomDescription nc = new NCRoomDescription(found_name, miembros, 0);
			salas.add(nc);
		}*/
		return new NCRoomListMessage(tipo, salas);
	}
	
}
