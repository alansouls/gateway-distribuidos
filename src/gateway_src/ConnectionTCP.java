package gateway_src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.ArrayList;

import com.google.protobuf.UninitializedMessageException;

import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.Sensor;
import protoClass.SensorOuterClass.CommandMessage.CommandType;
import protoClass.SensorOuterClass.Sensor.SensorType;

public class ConnectionTCP extends Thread {
	
	DataInputStream in;
	DataOutputStream out;
	
	DatagramSocket socketUDP;
	DatagramPacket packet;
	DatagramPacket packetRec;
	
	Socket clientSocket;
	
	ArrayList<sensorBuff> sensorList;
	
	CommandMessage.Builder cmd;
	sensorBuff sb;

	public ConnectionTCP(Socket aClientSocket, ArrayList<sensorBuff> sensorList, DatagramSocket socket)
			throws IOException, EOFException {
		
		try {
			System.out.println("Received attempt to connect!");
			
			clientSocket = aClientSocket;
			this.sensorList = sensorList;
			
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			
			cmd = CommandMessage.newBuilder();
			
			sb = new sensorBuff();
			this.socketUDP = socket;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Sensor getSensorById(int id, SensorType type) {
		
		if (id == -1) {
			for (int i = 0; i < sensorList.size(); i++) {
				if (sensorList.get(i).getSensor().getType() == type) {
					return sensorList.get(i).getSensor();
				}
			}
		}
		for (int i = 0; i < sensorList.size(); i++) {
			if (sensorList.get(i).getSensor().getId() == id) {
				return sensorList.get(i).getSensor();
			}
		}

		return null;
	}

	public void removeSensorById(int id) {
		for (int i = 0; i < sensorList.size(); i++) {
			if (sensorList.get(i).getSensor().getId() == id) {
				sensorList.remove(sensorList.get(i));
				break;
			}
		}
	}

	public void updateSensorById(int id, Sensor sensor) {
		for (int i = 0; i < sensorList.size(); i++) {
			if (sensorList.get(i).getSensor().getId() == id) {
				sensorList.get(i).setSensor(sensor);
			}
		}
	}

	public void sendSensorByID(CommandMessage msg) throws Exception {
		
		cmd.setParameter(msg.getParameter());
		cmd.setCommand(CommandType.SET_STATE);
		
		byte[] request = cmd.build().toByteArray();
		byte[] response = new byte[128];
		
		sensorBuff s = new sensorBuff();
		
		int i = s.sensorListIndex(cmd.getParameter(), sensorList);
		
		packet = new DatagramPacket(request, request.length, sensorList.get(i).getIP(), sensorList.get(i).getPort());
		packetRec = new DatagramPacket(response, response.length);
		
		socketUDP.send(packet);
		
		System.out.println("Sensor atualizado recebido!");
		
		CommandMessage cmd = CommandMessage.parseFrom(packet.getData());
		
		System.out.println(cmd.toString());
		
		updateSensorById(cmd.getParameter().getId(), cmd.getParameter());
		
		out.write(cmd.toByteArray());
	}

	public void handleMessage(CommandMessage msg) throws IOException {
		if (msg.getCommand() == CommandType.GET_STATE) {
			cmd.setCommand(CommandType.SET_STATE);

			Sensor sensorTemp = getSensorById(msg.getParameter().getId(), msg.getParameter().getType());

			if (sensorTemp != null) {
				cmd.setParameter(sensorTemp);
				byte[] sendData = cmd.build().toByteArray();
				out.write(sendData);
			} else {
				Sensor.Builder sensorDefault = Sensor.newBuilder();

				sensorDefault.setId(msg.getParameter().getId());
				sensorDefault.setState(0);
				sensorDefault.setType(msg.getParameter().getType());

				cmd.setParameter(sensorDefault);
				byte[] sendData = cmd.build().toByteArray();
				out.write(sendData);
			}
		} else if (msg.getCommand() == CommandType.SET_STATE) {
			try {
				if (getSensorById(msg.getParameter().getId(), msg.getParameter().getType()) != null) {
					sendSensorByID(msg);
				} else {
					Sensor.Builder sensorDefault = Sensor.newBuilder();

					sensorDefault.setId(msg.getParameter().getId());
					sensorDefault.setState(0);
					sensorDefault.setType(msg.getParameter().getType());

					cmd.setParameter(sensorDefault);
					byte[] sendData = cmd.build().toByteArray();
					out.write(sendData);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void run() {
		try {
			// repete_o_processo_até_dar_um_erro,_ai_fecha_o_socket.
			while (true) {
				byte[] request = new byte[1024];
				System.out.println(sensorList.size());
				System.out.println("Preparing to read...");
				int size = in.read(request);
				byte[] b = new byte[size];
				for (int i = 0; i < size; i++) {
					b[i] = request[i];
				}
				// System.out.println(socketUDP.getInetAddress().toString());
				System.out.println("Message read");
				CommandMessage cmdMessage = CommandMessage.parseFrom(b);
				// handleMessage(cmdMessage);
				handleMessage(cmdMessage);
				System.out.println("Message send ok!");
			}
		} catch (EOFException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UninitializedMessageException e) {
		} catch (Exception e) {
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