package gateway_src;
import java.net.*;
//import java.text.SimpleDateFormat;
import java.util.ArrayList;
import com.google.protobuf.UninitializedMessageException;
import java.io.*;
import java.time.*;
//import java.time.format.DateTimeFormatter;
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
	DatagramSocket socketUDP;
	DatagramPacket packet;
	DatagramPacket packetRec;
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
	
	public void sendSensorByID(CommandMessage msg) throws Exception {
		cmd.setParameter(getSensorById(msg.getParameter().getId(), msg.getParameter().getType()));
		cmd.setCommand(CommandType.SET_STATE);
		byte[] request = cmd.build().toByteArray();
		byte[] response = new byte[128];
		socketUDP = new DatagramSocket();
		sensorBuff s = new sensorBuff();
		int i = s.sensorListIndex(cmd.getParameter(), sensorList);
		packet = new DatagramPacket(request, request.length, sensorList.get(i).getIP(), sensorList.get(i).getPort());
		packetRec = new DatagramPacket(response, response.length);
		socketUDP.send(packet);
		socketUDP.receive(packetRec);
		byte[] sensorByte = new byte[packetRec.getData().length];
		response = packet.getData();
		int size = response[0];
		for(int j=1; j<size; j++) {
			sensorByte[j] = response[j+1];
		}
<<<<<<< HEAD
		System.out.println("Sensor atualizado recebido!");
		CommandMessage cmd = CommandMessage.parseFrom(response);
=======
		CommandMessage cmd = CommandMessage.parseFrom(sensorByte);
>>>>>>> parent of 671e535... Add ports and BuildMessages
		updateSensorById(cmd.getParameter().getId(), cmd.getParameter());
		cmd.writeTo(out);
	}
	
	public void handleMessage(CommandMessage msg) throws IOException{
		if (msg.getCommand() == CommandType.GET_STATE) {
			cmd.setParameter(getSensorById(msg.getParameter().getId(), msg.getParameter().getType()));
			byte[] sendData = cmd.build().toByteArray();
			out.write(sendData);
		}
		else if (msg.getCommand() == CommandType.SET_STATE) {
			try {
				sendSensorByID(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void run() {
		try {
			//repete_o_processo_até_dar_um_erro,_ai_fecha_o_socket.
			while (true) {
				byte[] request = new byte[1024];
				System.out.println("Preparing to read...");
				int size = in.read(request);
				byte[] b = new byte[size];
				for (int i = 0; i < size; i++) {
					b[i] = request[i];
				}
				System.out.println("Message read");
				CommandMessage cmdMessage = CommandMessage.parseFrom(b);
				//handleMessage(cmdMessage);
				out.write(cmdMessage.toByteArray());
				System.out.println("Message send ok!");
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
				byte[] sensorByte = new byte[DemonPacket.getData().length];
				byte[] response = DemonPacket.getData();
				int size = response[0];
				for(int j=1; j<size+1; j++) {
					sensorByte[j-1] = response[j];
				}
				CommandMessage cmd = CommandMessage.parseFrom(sensorByte);
				Sensor sensor = cmd.getParameter();
				LocalDateTime dateSensor = LocalDateTime.now();
				s = new sensorBuff();
				//Setando_campos_do_buffer_de_sensor
				s.setIP(DemonPacket.getAddress());
				s.setPort(DemonPacket.getPort());
				s.setSensor(sensor);
				s.setDate(dateSensor);
				
				if(s.containSensorPerID(sensor, sensorList)==false) {
					sensorList.add(s);
				} else if(s.containSensorPerID(sensor, sensorList)==true){
					int i = s.sensorListIndex(sensor, sensorList);
					LocalDateTime dateInList = sensorList.get(i).getDate();
					if(dateSensor.isAfter(dateInList)){
						sensorList.remove(s.sensorListIndex(sensor, sensorList));
						sensorList.add(s);
					}
				}
				DemonPacket.setData(null);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			DemonSocket.close();
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
		broadSocket = new DatagramSocket(7777);
	}
	
	public void run(){
		try {
			broadSocket.setBroadcast(true);
		} catch (SocketException e) {
			e.printStackTrace();
		}
			try {
				while(true) {
					broadSocket.send(broadPacket);
					Thread.sleep(10000);
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				broadSocket.close();
			}
	}
}
