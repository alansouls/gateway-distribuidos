package gateway_src;
import java.net.*;
import java.io.*;
import protoClass.SensorOuterClass;
import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.Sensor;

@SuppressWarnings("unused")
public class gateway_server {
	public static void main(String args[]) {
		ServerSocket listenSocket = null;
		DatagramSocket UDPserverSocket = null;
		int serverPort = 7777; // the server port
		try {
			UDPserverSocket = new DatagramSocket(7777);
			//Código de mensagem de descoberta
			/* */
			
			byte[] receiveData = new byte[1024];
			DatagramPacket request = new DatagramPacket(receiveData, receiveData.length);
			UDPserverSocket.receive(request);
			InetAddress IpdoSensor = request.getAddress();
			int portadosensor = request.getPort();
			
			listenSocket = new ServerSocket(serverPort);
			while (true) {
				Socket clientSocket = listenSocket.accept();
				Connection c = new Connection(clientSocket, UDPserverSocket);
				c.start();
			}
		} catch (IOException e) {
			System.out.println("Listen socket:" + e.getMessage());
		}
	}
}

class Connection extends Thread {
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	DatagramSocket UDPserverSocket;

	public Connection(Socket aClientSocket, DatagramSocket aUDPserverSocket) {
		try {
			clientSocket = aClientSocket;
			UDPserverSocket = aUDPserverSocket;
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			System.out.println("Connection:" + e.getMessage());
		}
	}

	public void run() {
		try {
			Sensor.Builder sensor = Sensor.newBuilder();
			CommandMessage.Builder cmd = CommandMessage.newBuilder();
			Sensor INsensor = sensor.build().getParserForType().parseFrom(in);
			CommandMessage CMDsensor = cmd.build().getParserForType().parseFrom(in);
			//Preparando_pacote_UDP
			byte[] sendData = null;
			byte[] receiveData = new byte[1000];
			DatagramPacket requestPacket = null;
			DatagramPacket replyPacket = null;
			DatagramPacket replyPacket2 = null;
			
			if(INsensor.getId()!=1) {
				sensor.setType(INsensor.getType());
				sensor.setId(INsensor.getId());
				sensor.setState(INsensor.getState());
				sendData = sensor.build().toByteArray();
				requestPacket = new DatagramPacket(sendData, sendData.length);
				UDPserverSocket.send(requestPacket);
				
			}
			else if(INsensor.getId()!=1 && CMDsensor.hasCommand() && CMDsensor.hasParameter()) {
				sensor.setType(INsensor.getType());
				sensor.setId(INsensor.getId());
				sensor.setState(INsensor.getState());
				cmd.setCommand(CMDsensor.getCommand());
				cmd.setParameter(CMDsensor.getParameter());
				//Envio_da_mensagem_de_requisição_de_estado_aos_sensores_do_tipo_sensor
				sendData = sensor.build().toByteArray();
				requestPacket = new DatagramPacket(receiveData, receiveData.length);
				UDPserverSocket.send(requestPacket);
				//Preparando_pacote_para_resposta_dos_sensores
				replyPacket = new DatagramPacket(receiveData, receiveData.length);
				UDPserverSocket.receive(replyPacket);
				//Envio_da_mensagem_de_requisição_de_estado_aos_sensores_do_tipo_comando
				sendData = cmd.build().toByteArray();
				requestPacket = new DatagramPacket(sendData, sendData.length);
				UDPserverSocket.send(requestPacket);
				//Preparando_pacote_para_resposta_dos_sensores
				receiveData = new byte[1000];
				replyPacket2 = new DatagramPacket(receiveData, receiveData.length);
				UDPserverSocket.receive(replyPacket2);
				//Enviando_dados_via_outputstream
				out.write(replyPacket.getData());
				out.write(replyPacket2.getData());	
			}
			
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("readline:" + e.getMessage());
		} finally {
			try {
				clientSocket.close();
				UDPserverSocket.close();
			} catch (IOException e) {
				/* close failed */}
		}

	}
}

