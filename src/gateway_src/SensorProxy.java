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
	ArrayList<sensorBuff> sensorList;
	byte[] receiveData;
	CommandMessage cmd;
	sensorBuff s;
	DatagramSocket socket;

	public SensorProxy(ArrayList<sensorBuff> sensorList, int serverPort, DatagramSocket socket) throws IOException {

		this.sensorList = sensorList;
		byte[] receiveData = new byte[128];
		DemonPacket = new DatagramPacket(receiveData, receiveData.length);
		s = new sensorBuff();
		this.socket = socket;

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

				LocalDateTime dateSensor = LocalDateTime.now();

				s = new sensorBuff();

				// Setando_campos_do_buffer_de_sensor
				s.setIP(DemonPacket.getAddress());
				s.setPort(DemonPacket.getPort());
				s.setSensor(sensor);
				s.setDate(dateSensor);

				sensorList.add(s);

				System.out.println("Adicionando sensor!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}