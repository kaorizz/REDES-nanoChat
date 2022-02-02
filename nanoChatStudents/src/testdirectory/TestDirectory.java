package testdirectory;

import java.io.IOException;
import es.um.redes.nanoChat.directory.connector.DirectoryConnector;

public class TestDirectory {
	
	public static void main(String[] args) throws IOException {
		String strToConvert = new String("Hola Mundo xd");
		DirectoryConnector dc = new DirectoryConnector("127.0.0.1");
		String resp = dc.convertToUpper(strToConvert);
		System.out.println(resp);
	}
}