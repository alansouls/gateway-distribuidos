package gateway_src;
import java.net.*;
import java.util.ArrayList;
import java.io.*;
//import protoClass.SensorOuterClass;
import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.Sensor;
//import protoClass.SensorOuterClass.Sensor.SensorType;
import protoClass.SensorOuterClass.CommandMessage.CommandType;

public class gateway_server {
	public static void main(String args[]) throws IOException {
		ServerSocket listenSocket = null;
		
		ArrayList<sensorBuff> sensorList = new ArrayList<sensorBuff>();
		
		int serverPort = 7777; // the server port
		int sensorPort = 8888; // the sensor port
		try {
			//Código_de_mensagem_de_descoberta
			
			CommandMessage.Builder cmd = CommandMessage.newBuilder();
			cmd.clear();
			cmd.setCommand(CommandType.GET_STATE);
			byte[] sendData = cmd.build().toByteArray();
			broadcast b = new broadcast(sendData, sensorPort);
			b.start();
			
			SensorProxy s = new SensorProxy(sensorList);
			s.start();
			
			listenSocket = new ServerSocket(serverPort);
			while (true) {
				Socket clientSocket = listenSocket.accept();
				ConnectionTCP c = new ConnectionTCP(clientSocket, sensorList);
				c.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ConnectionTCP extends Thread {
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	ArrayList<sensorBuff> sensorList;
	CommandMessage.Builder cmd;
	sensorBuff sb;

	public ConnectionTCP(Socket aClientSocket, ArrayList<sensorBuff> sensorList) throws IOException, EOFException {
		try {
			clientSocket = aClientSocket;
			this.sensorList = sensorList;
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			cmd = CommandMessage.newBuilder();
			sb = new sensorBuff();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			cmd.build().getParserForType().parseFrom(in);
			byte[] sendData = null;
			
			if(cmd.hasCommand() && cmd.hasParameter()){
				if(sb.containSensorPerID(cmd.getParameter(), sensorList)) {
					cmd.setParameter(sensorList.get(sb.sensorListIndex(cmd.getParameter(), sensorList)).getSensor());
					sendData = cmd.build().toByteArray();
					out.write(sendData);
				}
			}
			
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
				try {
					clientSocket.close();
					in.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}
}

class SensorProxy extends Thread{
	DatagramSocket DemonSocket;
	DatagramPacket DemonPacket;
	ArrayList<sensorBuff> sensorList;
	byte[] receiveData;
	CommandMessage.Builder cmd;
	sensorBuff s;
	
	public SensorProxy(ArrayList<sensorBuff> sensorList) throws IOException {
		
		this.sensorList = sensorList;
		byte[] receiveData = new byte[5];
		DemonSocket = new DatagramSocket();
		DemonPacket = new DatagramPacket(receiveData, receiveData.length);
		cmd = CommandMessage.newBuilder();
		s = new sensorBuff();
		
	}
	
	public void run() {
		try {
			while(true) {
				DemonSocket.receive(DemonPacket);
				cmd.build().getParserForType().parseFrom(DemonPacket.getData());
				Sensor sensor = cmd.getParameter();
				s = new sensorBuff();
				//Setando campos do buffer de sensor
				s.setIP(DemonPacket.getAddress());
				s.setPort(DemonPacket.getPort());
				s.setSensor(sensor);
				
				if(s.containSensorPerID(sensor, sensorList)==false) {
					sensorList.add(s);
				} else {	
					if(sensor.getData().getYear() > sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getYear() || 
					  (sensor.getData().getYear() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getYear() && 
					  sensor.getData().getMonth() > sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getMonth()) ||
					  (sensor.getData().getYear() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getYear() &&
					  sensor.getData().getMonth() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getMonth() &&
					  sensor.getData().getDay() > sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getDay()) ||
					  (sensor.getData().getYear() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getYear() &&
					  sensor.getData().getMonth() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getMonth() &&
					  sensor.getData().getDay() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getDay() &&
					  sensor.getData().getHours() > sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getHours()) ||
					  (sensor.getData().getYear() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getYear() &&
					  sensor.getData().getMonth() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getMonth() &&
					  sensor.getData().getDay() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getDay() &&
					  sensor.getData().getHours() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getHours() &&
					  sensor.getData().getMinutes() > sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getMinutes()) ||
					  (sensor.getData().getYear() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getYear() &&
					  sensor.getData().getMonth() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getMonth() &&
					  sensor.getData().getDay() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getDay() &&
					  sensor.getData().getHours() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getHours() &&
					  sensor.getData().getMinutes() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getMinutes() &&
					  sensor.getData().getSeconds() == sensorList.get(s.sensorListIndex(sensor, sensorList)).getSensor().getData().getSeconds())) {
						
						sensorList.remove(s.sensorListIndex(sensor, sensorList));
						sensorList.add(s);
					}
				}
				cmd.clear();
				DemonPacket.setData(null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

}

class broadcast extends Thread{
	DatagramSocket broadSocket;
	DatagramPacket broadPacket;
	byte[] recData;
	int sensorPort;
	
	public broadcast(byte[] recData, int sensorPort) throws UnknownHostException, IOException {
		this.recData = recData;
		this.sensorPort = sensorPort;
		broadPacket = new DatagramPacket(recData,recData.length,InetAddress.getByName("255.255.255.255"),sensorPort);
		broadSocket = new DatagramSocket();
	}
	
	public void run(){
		try {
			broadSocket.setBroadcast(true);
		} catch (SocketException e2) {
			e2.printStackTrace();
		}
		while(true) {
			try {
				broadSocket.send(broadPacket);
				Thread.sleep(1000);
			} catch (SocketException e1) {
				e1.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
