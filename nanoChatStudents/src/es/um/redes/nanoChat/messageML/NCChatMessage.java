package es.um.redes.nanoChat.messageML;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * CHAT
----

<message>
<operation>operation</operation>
<user>user</user>
<msg>msg</msg>
</message>

Operaciones válidas:

Nick
*/

public class NCChatMessage extends NCMessage {
	
	private String name;
	private String message;
	
	static private final String USER_MARK = "user";
	static private final String MSG_MARK = "msg";
	
	private static final String RE_USER = "<user>(.*?)</user>";
	private static final String RE_MSG = "<msg>(.*?)</msg>";
	
	public NCChatMessage(byte tipo, String user, String message) {
		this.name=user;
		this.message=message;
		super.opcode=tipo;
	}

	public String getName() {
		return name;
	}
	
	public String getMessage() {
		return message;
	}
	
	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE); // Campo operation
		sb.append("<"+USER_MARK+">"+name+"</"+USER_MARK+">"+END_LINE); // Campo user
		sb.append("<"+MSG_MARK+">"+message+"</"+MSG_MARK+">"+END_LINE); // Campo message
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);
		return sb.toString(); // Devolvemos el mensaje
	}
	
	public static NCChatMessage readFromString(byte tipo, String message) {
		String found_user = null;
		String found_msg = null;

		// Tienen que estar los campos porque el mensaje es de tipo RoomMessage
		Pattern pat_user = Pattern.compile(RE_USER);
		Pattern pat_msg = Pattern.compile(RE_MSG);
		Matcher mat_user = pat_user.matcher(message);
		Matcher mat_msg = pat_msg.matcher(message);
		if ((mat_user.find()) && (mat_msg.find())) {
			// Name found
			found_user = mat_user.group(1);
			found_msg = mat_msg.group(1);
		} else {
			System.out.println("Error en ChatMessage: parámetros no encontrados.");
			return null;
		}
		
		return new NCChatMessage(tipo, found_user, found_msg);
	}
}
