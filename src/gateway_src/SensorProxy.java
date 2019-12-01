package gateway_src;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.time.LocalDateTime;
import java.util.ArrayList;

import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.Sensor;
import protoClass.SensorOuterClass.CommandMessage.CommandType;


public class SensorProxy extends Thread {

	DatagramPacket DemonPacket;
	byte[] receiveData;
	CommandMessage cmd;
	DatagramSocket socket;
	QueuePublisher pub;
	sensorBuff s;
	ArrayList<sensorBuff> sensorList;

	public SensorProxy(ArrayList<sensorBuff> sensorList, QueuePublisher pub, int serverPort, DatagramSocket socket) throws IOException {

		byte[] receiveData = new byte[128];
		this.DemonPacket = new DatagramPacket(receiveData, receiveData.length);
		this.socket = socket;
		this.pub = pub;
		s = new sensorBuff();
		this.sensorList = sensorList;

	}
	
	private String getByteString(byte[] bytes){
        String str = "/";
        for (int i = 0; i < bytes.length; i++){
            str += Integer.toString(bytes[i]);
            str += "/";
        }

        return str;
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

				CommandMessage msg = CommandMessage.parseFrom(data);
				
				System.out.println(msg.toString());		
				
				Sensor sensor = msg.getParameter();	
				String sensorType = sensor.getType().toString();
				LocalDateTime dateSensor = LocalDateTime.now();
								
				s = new sensorBuff();
				
				// Setando campoos do buffer de sensor
				s.setIP(DemonPacket.getAddress());
				s.setPort(DemonPacket.getPort());
				s.setSensor(sensor);
				s.setDate(dateSensor);
				
				CommandMessage.Builder cmd = CommandMessage.newBuilder();
				cmd.setParameter(sensor);
				cmd.setCommand(CommandType.SET_STATE);
							
				byte[] msg_byte = cmd.build().toByteArray();
				
				String msg_string = getByteString(msg_byte);
		
				try {	
					this.pub.publishData(msg_string, sensorType);
				} catch (Exception e) {
					System.out.println("Erro na conexão com RabbitMQ.");
				}
				
								
				if(s.containSensorPerID(sensor, sensorList)==false) {
					sensorList.add(s);
					System.out.println("Adicionando sensor!");
				} else if(s.containSensorPerID(sensor, sensorList)==true){
					int i = s.sensorListIndex(sensor, sensorList);
					LocalDateTime dateInList = sensorList.get(i).getDate();
					if(dateSensor.isAfter(dateInList)){
						sensorList.remove(i);
						sensorList.add(s);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}