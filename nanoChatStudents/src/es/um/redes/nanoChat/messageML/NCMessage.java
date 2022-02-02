package es.um.redes.nanoChat.messageML;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;


public abstract class NCMessage {
	protected byte opcode;

	// TODO IMPLEMENTAR TODAS LAS CONSTANTES RELACIONADAS CON LOS CODIGOS DE OPERACION
	public static final byte OP_INVALID_CODE = 0;
	public static final byte OP_NICK = 1;
	public static final byte OP_NICK_OK = 2;
	public static final byte OP_NICK_ERROR = 3;
	public static final byte OP_GET_ROOMLIST = 	4;
	public static final byte OP_ROOMLIST = 	5;	
	public static final byte OP_ENTER = 6;
	public static final byte OP_ENTER_OK = 7;
	public static final byte OP_ENTER_ERROR = 8;
	public static final byte OP_GET_INFO = 10;
	public static final byte OP_INFO = 11;
	public static final byte OP_SEND_MESSAGE = 12;
	public static final byte OP_SEND_MESSAGEB = 13;
	public static final byte OP_EXIT = 14;
	public static final byte OP_RENAME = 16;
	public static final byte OP_RENAME_OK = 17;
	public static final byte OP_RENAME_ERROR = 18;
	public static final byte OP_SEND_PRIVMESSAGE = 19;
	public static final byte OP_RCV_PRIVMESSAGE=20;
	public static final byte OP_NEW_ROOM=21;
	public static final byte OP_NEW_ROOM_OK=22;

	public static final char DELIMITER = ':';    //Define el delimitador
	public static final char END_LINE = '\n';    //Define el carácter de fin de línea
	
	public static final String OPERATION_MARK = "operation";
	public static final String MESSAGE_MARK = "message";

	/**
	 * Códigos de los opcodes válidos  El orden
	 * es importante para relacionarlos con la cadena
	 * que aparece en los mensajes
	 */
	private static final Byte[] _valid_opcodes = { 
		OP_NICK,
		OP_NICK_OK,
		OP_NICK_ERROR,
		OP_GET_ROOMLIST,
		OP_ROOMLIST,
		OP_ENTER,
		OP_ENTER_OK,
		OP_ENTER_ERROR,
		OP_GET_INFO,
		OP_INFO,
		OP_SEND_MESSAGE,
		OP_SEND_MESSAGEB,
		OP_EXIT,
		OP_RENAME,
		OP_RENAME_OK,
		OP_RENAME_ERROR,
		OP_SEND_PRIVMESSAGE,
		OP_RCV_PRIVMESSAGE,
		OP_NEW_ROOM,
		OP_NEW_ROOM_OK
		};

	/**
	 * cadena exacta de cada orden
	 */
	private static final String[] _valid_operations_str = {
		"Nick",
		"Nick_ok",
		"Nick_error",
		"Get_roomlist",
		"Roomlist",
		"Enter",
		"Enter_ok",
		"Enter_error",
		"Get_info",
		"Info",
		"Send_message",
		"Send_messageb",
		"Exit",
		"Rename",
		"Rename_ok",
		"Rename_error",
		"Send_privmessage",
		"Rcv_privmessage",
		"New_room",
		"New_room_ok"
	};

	

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;
	
	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0 ; i < _valid_operations_str.length; ++i)
		{
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}
	
	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte stringToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OP_INVALID_CODE);
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	protected static String opcodeToString(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}
	
	//Devuelve el opcode del mensaje
	public byte getOpcode() {
		return opcode;
	}

	//Método que debe ser implementado por cada subclase de NCMessage
	protected abstract String toEncodedString();

	//Analiza la operación de cada mensaje y usa el método readFromString() de cada subclase para parsear
	public static NCMessage readMessageFromSocket(DataInputStream dis) throws IOException {
		String message = dis.readUTF();
		String regexpr = "<"+MESSAGE_MARK+">(.*?)</"+MESSAGE_MARK+">";
		Pattern pat = Pattern.compile(regexpr,Pattern.DOTALL);
		Matcher mat = pat.matcher(message);
		if (!mat.find()) {
			System.out.println("Mensaje mal formado:\n"+message);
			return null;
			// Message not found
		} 
		String inner_msg = mat.group(1);  // extraemos el mensaje

		String regexpr1 = "<"+OPERATION_MARK+">(.*?)</"+OPERATION_MARK+">";
		Pattern pat1 = Pattern.compile(regexpr1);
		Matcher mat1 = pat1.matcher(inner_msg);
		if (!mat1.find()) {
			System.out.println("Mensaje mal formado:\n" +message);
			return null;
			// Operation not found
		} 
		String operation = mat1.group(1);  // extraemos la operación
		
		byte code = stringToOpcode(operation);
		if (code == OP_INVALID_CODE) return null;
		
		switch (code) {
		//TODO Parsear el resto de mensajes 
		case OP_NICK:
		case OP_ENTER:
		case OP_GET_INFO:
		case OP_EXIT:
		case OP_RENAME:
		case OP_SEND_MESSAGE:
		case OP_RENAME_OK:
		case OP_RENAME_ERROR:
		{
			return NCRoomMessage.readFromString(code, message);
		}
		case OP_ROOMLIST:
		case OP_INFO:
		{
			return NCRoomListMessage.readFromString(code, message);
		}
		case OP_NICK_OK:
		case OP_NICK_ERROR:
		case OP_GET_ROOMLIST:
		case OP_ENTER_OK:
		case OP_ENTER_ERROR:
		case OP_NEW_ROOM:
		case OP_NEW_ROOM_OK:
		case OP_RCV_PRIVMESSAGE:
		{
			return NCSimpleMessage.readFromString(code);
		}
		case OP_SEND_MESSAGEB:
		case OP_SEND_PRIVMESSAGE:
		{
			return NCChatMessage.readFromString(code, message);
		}
		default:
			System.err.println("Unknown message type received:" + code);
			return null;
		}

	}

	//TODO Programar el resto de métodos para crear otros tipos de mensajes
	
	public static NCMessage makeRoomMessage(byte code, String room) {
		return new NCRoomMessage(code, room);
	}
	
	public static NCMessage makeSimpleMessage(byte code) {
		return new NCSimpleMessage(code);
	}
	
	public static NCMessage makeChatMessage(byte code, String user, String message) {
		return new NCChatMessage(code, user, message);
	}
	
	public static NCMessage makeRoomListMessage(byte code, List<NCRoomDescription> roomlist) {
		return new NCRoomListMessage(code, roomlist);
	}
}
