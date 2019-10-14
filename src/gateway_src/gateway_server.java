package gateway_src;
import java.net.*;
import java.util.ArrayList;

import com.google.protobuf.UninitializedMessageException;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

//import protoClass.SensorOuterClass;
import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.Sensor;
import protoClass.SensorOuterClass.Sensor.SensorType;
//import protoClass.SensorOuterClass.Sensor.SensorType;
import protoClass.SensorOuterClass.CommandMessage.CommandType;

public class gateway_server {
	public static void main(String args[]) throws IOException {
		ServerSocket listenSocket = null;
		
		ArrayList<sensorBuff> sensorList = new ArrayList<sensorBuff>();
		
		int serverPort = 6543; // the server port
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
				System.out.println("Ouvindo...");
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
			System.out.println("Received attempt to connect!");
			clientSocket = aClientSocket;
			this.sensorList = sensorList;
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			cmd = CommandMessage.newBuilder();
			sb = new sensorBuff();
			
		} catch (IOException e) {
			e.printStackTrace();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Sensor getSensorById(int id, SensorType type) {
		if (id == -1) {
			for (int i = 0; i < sensorList.size() -1; i++) {
				if (sensorList.get(i).getSensor().getType() == type) {
					return sensorList.get(i).getSensor();
				}
			}
		}
		for (int i = 0; i < sensorList.size() -1; i++) {
			if (sensorList.get(i).getSensor().getId() == id) {
				return sensorList.get(i).getSensor();
			}
		}
		return null;
	}
	
	public void removeSensorById(int id) {
		for (int i = 0; i < sensorList.size() -1; i++) {
			if (sensorList.get(i).getSensor().getId() == id) {
				sensorList.remove(sensorList.get(i));
				break;
			}
		}
	}
	
	public void updateSensorById(int id, Sensor sensor) {
		for (int i = 0; i < sensorList.size() -1; i++) {
			if (sensorList.get(i).getSensor().getId() == id) {
				sensorList.get(i).setSensor(sensor);
			}
		}
	}
	
	public void handleMessage(CommandMessage msg) throws IOException{
		if (msg.getCommand() == CommandType.GET_STATE) {
			cmd.setParameter(getSensorById(msg.getParameter().getId(), msg.getParameter().getType()));
			byte[] sendData = cmd.build().toByteArray();
			out.write(sendData);
		}
		else if (msg.getCommand() == CommandType.SET_STATE) {
			Sensor sensor = getSensorById(msg.getParameter().getId(), msg.getParameter().getType());
			Sensor.Builder sensorBuilder = sensor.toBuilder();
			sensorBuilder.setState(msg.getParameter().getState());
			updateSensorById(sensor.getId(), sensorBuilder.build());
			cmd.setParameter(sensorBuilder.build());
			byte[] sendData = cmd.build().toByteArray();
			out.write(sendData);
		}
		// não entendi o que é o SENSOR_STATE, mas se tiver que ser visto aqui, adiciona
	}

	public void run() {
		try {
			//repete o processo até dar um erro, ai fecha o socket.
			while (true) {
				byte[] request = new byte[1024];
				System.out.println("Preparing to read...");
				in.read(request);
				System.out.println("Message read");
				CommandMessage cmdMessage = CommandMessage.parseFrom(request);
				handleMessage(cmdMessage);
			}
			
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UninitializedMessageException e) {
		}
		finally {
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
	CommandMessage cmd;
	sensorBuff s;
	
	public SensorProxy(ArrayList<sensorBuff> sensorList) throws IOException {
		
		this.sensorList = sensorList;
		byte[] receiveData = new byte[5];
		DemonSocket = new DatagramSocket();
		DemonPacket = new DatagramPacket(receiveData, receiveData.length);
		s = new sensorBuff();
		
	}
	
	public void run() {
		try {
			while(true) {
				DemonSocket.receive(DemonPacket);
				CommandMessage cmd = CommandMessage.parseFrom((DemonPacket.getData()));
				Sensor sensor = cmd.getParameter();
				s = new sensorBuff();
				//Setando_campos_do_buffer_de_sensor
				s.setIP(DemonPacket.getAddress());
				s.setPort(DemonPacket.getPort());
				s.setSensor(sensor);
				
				if(s.containSensorPerID(sensor, sensorList)==false) {
					sensorList.add(s);
				} else if(s.containSensorPerID(sensor, sensorList)==true){
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
					LocalDateTime date = LocalDateTime.parse(cmd.getParameter().getDate(),formatter);
					int i = s.sensorListIndex(sensor, sensorList);
					LocalDateTime dateInList = LocalDateTime.parse(sensorList.get(i).getSensor().getDate(),formatter);
					if(date.isAfter(dateInList)) {
						sensorList.remove(s.sensorListIndex(sensor, sensorList));
						sensorList.add(s);
					}
				}
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
