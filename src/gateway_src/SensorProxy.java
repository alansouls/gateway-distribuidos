package gateway_src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDateTime;
import java.util.ArrayList;

import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.Sensor;

public class SensorProxy extends Thread {

	DatagramPacket DemonPacket;
	byte[] receiveData;
	CommandMessage cmd;
	DatagramSocket socket;
	QueuePublisher pub;

	public SensorProxy(QueuePublisher pub, int serverPort, DatagramSocket socket) throws IOException {

		byte[] receiveData = new byte[128];
		this.DemonPacket = new DatagramPacket(receiveData, receiveData.length);
		this.socket = socket;
		this.pub = pub;

	}

	public void run() {
		try {
			while (true) {
				System.out.println("Pronto para receber respostas:");

				socket.receive(DemonPacket);

				byte[] data = new byte[DemonPacket.getLength()];

				for (int i = 0; i < data.length; i++) {

					data[i] = DemonPacket.getData()[i];

				}

				CommandMessage cmd = CommandMessage.parseFrom(data);

				System.out.println(cmd.toString());

				Sensor sensor = cmd.getParameter();

				float sensorState = sensor.getState();
				
				String sensorType = sensor.getType().toString();
							
				String dataSend = Float.toString(sensorState);
		
				try {
					this.pub.publishData(dataSend, sensorType);
				} catch (Exception e) {
					System.out.println("Erro na conexão com RabbitMQ.");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}