package gateway_src;

import java.io.*;
import java.net.*;
import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.CommandMessage.CommandType;
import protoClass.SensorOuterClass.Sensor;
import protoClass.SensorOuterClass.Sensor.SensorType;

class sensorFAKE {
	public static void main(String args[]) {
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(8888);
			LED l1 = new LED(0, 10);
			//System.out.println("Servidor em execução!");
			int id = 0;
			while (true) {
				id++;
				System.out.println("Esperando Requisição " + id + " ...");
				byte[] receiveData = new byte[1024];
				DatagramPacket request = new DatagramPacket(receiveData, receiveData.length, 8888);
				serverSocket.receive(request);
				System.out.println("Requisição recebida");
				CommandMessage cmd = CommandMessage.parseFrom(request.getData());
				if(cmd.getCommand()==CommandType.GET_STATE) {
					cmd.toBuilder().clear();
					Sensor.Builder sensor = Sensor.newBuilder();
					sensor.setState(l1.getState());
					sensor.setId(l1.getID());
					sensor.setType(l1.type);
					cmd.toBuilder().setParameter(sensor);
					cmd.toBuilder().setCommand(CommandType.SENSOR_STATE);
				} else if(cmd.getCommand()==CommandType.SET_STATE) {
					l1.setState(cmd.getParameter().getState());
					cmd.toBuilder().clear();
					Sensor.Builder sensor = Sensor.newBuilder();
					sensor.setState(l1.getState());
					sensor.setId(l1.getID());
					sensor.setType(l1.type);
					cmd.toBuilder().setParameter(sensor);
					cmd.toBuilder().setCommand(CommandType.SENSOR_STATE);
				}
				request = new DatagramPacket(cmd.toByteArray(), cmd.toByteArray().length, 6543);
				serverSocket.send(request);
				System.out.println("Sensor enviado!");
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (serverSocket != null) serverSocket.close();
		}
	}
}

class LED {
	private float state;
	private int id;
	public SensorType type;
	
	public LED(float state, int id){
		this.id = id;
		this.state = state;
		this.type = SensorType.LIGHT;
	}
	
	public SensorType getType() {
		return this.type;
	}
	
	public float getState() {
		return this.state;
	}
	
	public int getID() {
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setState(float state) {
		this.state = state;
	}
}

