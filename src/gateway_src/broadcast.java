package gateway_src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

class broadcast extends Thread {
	DatagramSocket broadSocket;
	DatagramPacket broadPacket;
	byte[] recData;
	int sensorPort, serverPort;

	public broadcast(byte[] recData, int sensorPort, DatagramSocket socket) throws UnknownHostException, IOException {
		this.recData = recData;
		this.sensorPort = sensorPort;
		broadPacket = new DatagramPacket(recData, recData.length, InetAddress.getByName("192.168.137.255"), sensorPort);
		broadSocket = socket;
	}

	public void run() {
		try {
			broadSocket.setBroadcast(true);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		try {
			while (true) {
				broadSocket.send(broadPacket);
				Thread.sleep(10000);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
