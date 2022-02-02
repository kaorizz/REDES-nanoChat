package es.um.redes.nanoChat.directory.connector;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	//Tamaño máximo del paquete UDP (los mensajes intercambiados son muy cortos)
	private static final int PACKET_MAX_SIZE = 128;
	
	//Constantes para los opcode
	private static final byte OP_REGISTRAR = 1;
	private static final byte OP_QUERY = 2;
	private static final byte OP_SENDOK = 3;
	private static final byte OP_SENDSERVERINFO = 5;
	
	//Puerto en el que atienden los servidores de directorio
	private static final int DEFAULT_PORT = 6868;
	//Valor del TIMEOUT
	private static final int TIMEOUT = 1000;

	private DatagramSocket socket; // socket UDP
	private InetSocketAddress directoryAddress; // dirección del servidor de directorio

	public DirectoryConnector(String agentAddress) throws IOException {
		//TODO A partir de la dirección y del puerto generar la dirección de conexión para el Socket
		directoryAddress = new InetSocketAddress(InetAddress.getByName(agentAddress), DEFAULT_PORT);
		//TODO Crear el socket UDP
		socket = new DatagramSocket();
	}

	public String convertToUpper(String strToConvert) throws IOException {
		byte [] bufSend = strToConvert.getBytes();
		DatagramPacket dpSend = new DatagramPacket(bufSend, bufSend.length, directoryAddress);
		byte[] bufRec = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(bufRec, bufRec.length);
		socket.send(dpSend);
		socket.receive(dpRec);
		String strConverted = new String(dpRec.getData());
		return strConverted;
	}
	
	
	/**
	 * Envía una solicitud para obtener el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public InetSocketAddress getServerForProtocol(int protocol) throws IOException {

		//TODO Generar el mensaje de consulta llamando a buildQuery()
		byte[] mconsulta = buildQuery(protocol);
		//TODO Construir el datagrama con la consulta
		DatagramPacket datagram1 = new DatagramPacket(mconsulta, mconsulta.length, directoryAddress);
		socket.send(datagram1);
		byte[] mrespuesta = new byte[PACKET_MAX_SIZE];
		DatagramPacket datagram2 = new DatagramPacket(mrespuesta, mrespuesta.length);
		//TODO Enviar datagrama por el socket
		//TODO preparar el buffer para la respuesta
		//TODO Establecer el temporizador para el caso en que no haya respuesta
		//TODO Recibir la respuesta
		socket.setSoTimeout(TIMEOUT);
		try {
			socket.receive(datagram2);
		} catch(SocketTimeoutException e) {
			System.err.println("Error: paquete no enviado");
		}
		return getAddressFromResponse(datagram2);

	}


	//Método para generar el mensaje de consulta (para obtener el servidor asociado a un protocolo)
	private byte[] buildQuery(int protocol) {
		ByteBuffer bb = ByteBuffer.allocate(5);
		bb.put(OP_QUERY);
		bb.putInt(protocol);
		byte[] men = bb.array();
		//TODO Devolvemos el mensaje-- codificado en binario según el formato acordado
		return men;
	}

	//Método para obtener la dirección de internet a partir del mensaje UDP de respuesta
	private InetSocketAddress getAddressFromResponse(DatagramPacket packet) throws UnknownHostException {
		ByteBuffer ret = ByteBuffer.wrap(packet.getData());
		byte opCode = ret.get();
		if (opCode == OP_SENDSERVERINFO) {
			byte[] dd = new byte[] { ret.get(), ret.get(), ret.get(), ret.get() };
			int port = ret.getInt();
			InetAddress dir = InetAddress.getByAddress(dd);
			return new InetSocketAddress(dir, port);
		}
		return null;
		//TODO Analizar si la respuesta no contiene dirección (devolver null)
		
		//TODO Si la respuesta no está vacía, devolver la dirección (extraerla del mensaje)
	}
	
	/**
	 * Envía una solicitud para registrar el servidor de chat asociado a un determinado protocolo
	 * 
	 */
	public boolean registerServerForProtocol(int protocol, int port) throws IOException {
		boolean resp = false;
		//TODO Construir solicitud de registro (buildRegistration)
		byte[] men = buildRegistration(protocol, port);
		DatagramPacket dpSend = new DatagramPacket(men, men.length, directoryAddress);
		socket.send(dpSend);
		byte[] res = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(res, res.length);
		socket.receive(dpRec);
		/*
		//TODO Enviar solicitud
		int i = 0;
		boolean recibido = false;
		while ((!recibido) && (i<10)) {
			socket.send(dpSend);
			socket.setSoTimeout(TIMEOUT);
			try {
			socket.receive(dpRec);
			recibido = true;
			} catch(SocketTimeoutException e) {
				System.err.println("Error: paquete no enviado");
			}
			i++;
		}*/
		//TODO Procesamos la respuesta para ver si se ha podido registrar correctamente
		ByteBuffer ret = ByteBuffer.wrap(dpRec.getData());
		byte opCode = ret.get();
		if (opCode == OP_SENDOK) {
			resp = true;
		}
		return resp;
	}


	//Método para construir una solicitud de registro de servidor
	//OJO: No hace falta proporcionar la dirección porque se toma la misma desde la que se envió el mensaje
	private byte[] buildRegistration(int protocol, int port) {
		//TODO Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer bb = ByteBuffer.allocate(6);
		byte protocolId = ((Integer)protocol).byteValue();
		bb.put(OP_REGISTRAR);
		bb.put(protocolId);
		bb.putInt(port);
		byte[] men = bb.array();
		return men;
	}

	public void close() {
		socket.close();
	}
}
