package es.um.redes.nanoChat.messageML;

/*
 * SIMPLE
----

<message>
<operation>operation</operation>
</message>

Operaciones válidas:

Nick
*/

public class NCSimpleMessage extends NCMessage{
	
	public NCSimpleMessage (byte tipo) {
		this.opcode = tipo;
	}

	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append("<"+MESSAGE_MARK+">"+END_LINE);
		sb.append("<"+OPERATION_MARK+">"+opcodeToString(opcode)+"</"+OPERATION_MARK+">"+END_LINE);	
		sb.append("</"+MESSAGE_MARK+">"+END_LINE);
		return sb.toString(); // Devolvemos el mensaje
	}
	
	public static NCSimpleMessage readFromString(byte tipo) {
		return new NCSimpleMessage(tipo);
	}
}
