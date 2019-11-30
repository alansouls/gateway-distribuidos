package gateway_src;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;

import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.CommandMessage.CommandType;

public class gateway_server {

	public static void main(String args[]) throws IOException {
		
		String rabbitHost = "ec2-3-89-88-24.compute-1.amazonaws.com";
		String exchangeName = "DIST";
		String user = "dist";
		String pass = "dist";
		
		QueuePublisher pub = new QueuePublisher(rabbitHost, exchangeName, user, pass);

		int sensorReceivePort = 7777;
		int sensorPort = 8888; // the sensor port
		DatagramSocket socket = new DatagramSocket(sensorReceivePort);

		try {
			// Código_de_mensagem_de_descoberta
			CommandMessage.Builder cmd = CommandMessage.newBuilder();

			cmd.clear();

			cmd.setCommand(CommandType.GET_STATE);

			byte[] sendData = cmd.build().toByteArray();

			broadcast b = new broadcast(sendData, sensorPort, socket);
			b.start();

			SensorProxy s = new SensorProxy(pub, sensorReceivePort, socket);
			s.start();
			
		} catch (IOException e) {

			e.printStackTrace();

		}
	}
}
