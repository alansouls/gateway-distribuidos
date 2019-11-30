package gateway_src;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;

import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.CommandMessage.CommandType;

public class gateway_server {

	public static void main(String args[]) throws IOException {

		ArrayList<sensorBuff> sensorList = new ArrayList<sensorBuff>();
		
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

			SensorProxy s = new SensorProxy(sensorList, sensorReceivePort, socket);
			s.start();
			
			// Ler os dados da lista e envia para o RabbitMQ
			while (true) {
								
				if(sensorList.isEmpty() == false) {
					sensorBuff sensor = sensorList.remove(0);
					float sensorState = sensor.getSensor().getState();
					
					String sensorType = sensor.getSensor().getType().toString();
					String data = Float.toString(sensorState);
			
					try {
						pub.publishData(data, sensorType);
					} catch (Exception e) {
						System.out.println("Erro na conexão com RabbitMQ.");
					}
				}
			}

		} catch (IOException e) {

			e.printStackTrace();

		}
	}
}
