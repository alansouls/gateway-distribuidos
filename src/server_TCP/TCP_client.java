package server_TCP;

import java.net.*;
import java.io.*;

public class TCP_client {
	public static void main(String args[]) {
		int serverPort = 7896;
		String serverIp = "localhost";
		String msg = "teste";
		Socket s = null;
		try {
			s = new Socket(serverIp, serverPort);
			DataInputStream entrada = new DataInputStream(s.getInputStream());
			DataOutputStream saida = new DataOutputStream(s.getOutputStream());
			saida.writeUTF(msg);; // UTF is a string encoding see Sn. 4.4
			System.out.println("Msg enviada: " + msg);
			String data = entrada.readUTF(); // read a line of data from the stream
			System.out.println("Msg recebida: " + data);
		} catch (UnknownHostException e) {
			System.out.println("Socket:" + e.getMessage());
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("readline:" + e.getMessage());
		} finally {
			if (s != null)
				try {
					s.close();
				} catch (IOException e) {
					System.out.println("close:" + e.getMessage());
				}
		}
	}
}
