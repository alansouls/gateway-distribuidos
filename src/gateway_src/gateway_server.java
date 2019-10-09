package gateway_src;
import java.net.*;
import java.util.ArrayList;
import java.io.*;
import protoClass.SensorOuterClass;
import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.Sensor;
import protoClass.SensorOuterClass.Sensor.SensorType;
import protoClass.SensorOuterClass.CommandMessage.CommandType;

public class gateway_server {
	public static void main(String args[]) throws SocketException {
		ServerSocket listenSocket = null;
		DatagramSocket UDPserverSocket = new DatagramSocket();
		
		ArrayList<InetAddress> listaLED	= new ArrayList<InetAddress>();
		ArrayList<InetAddress> listaLAM	= new ArrayList<InetAddress>();
		ArrayList<InetAddress> listaTEMP	= new ArrayList<InetAddress>();
		
		int serverPort = 7777; // the server port
		try {
			//Código_de_mensagem_de_descoberta
			
			CommandMessage.Builder cmd = CommandMessage.newBuilder();
			cmd.clear();
			cmd.setCommand(CommandType.GET_STATE);
			byte[] sendData = cmd.build().toByteArray();
			DatagramPacket broadPacket = new DatagramPacket(sendData, sendData.length, UDPserverSocket.getLocalAddress(), 8888);
			broadcast(broadPacket, InetAddress.getByName("255.255.255.255"), UDPserverSocket);
			
			SensorList s = new SensorList(listaLED, listaLAM, listaTEMP);
			s.start();
			
			
			listenSocket = new ServerSocket(serverPort);
			while (true) {
				Socket clientSocket = listenSocket.accept();
				ConnectionTCP c = new ConnectionTCP(clientSocket, UDPserverSocket, listaLED, listaLAM, listaTEMP);
				c.start();
			}
		} catch (IOException e) {
			System.out.println("Listen socket:" + e.getMessage());
		}
	}
	
    public static void broadcast(DatagramPacket broadcastPacket, InetAddress address, DatagramSocket socket) throws IOException {
    	      try {
    	    	  socket.setBroadcast(true);
      	          socket.send(broadcastPacket);
    	    } catch (IOException e) {
    	}
	}
}

class ConnectionTCP extends Thread {
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	DatagramSocket UDPserverSocket;
	ArrayList<InetAddress> listaLED;
	ArrayList<InetAddress> listaLAM;
	ArrayList<InetAddress> listaTEMP;

	public ConnectionTCP(Socket aClientSocket, DatagramSocket aUDPserverSocket, ArrayList<InetAddress> listaLED, ArrayList<InetAddress> listaLAM,
	ArrayList<InetAddress> listaTEMP) {
		try {
			clientSocket = aClientSocket;
			UDPserverSocket = aUDPserverSocket;
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			
		}
	}

	public void run() {
		try {
			CommandMessage.Builder cmd = CommandMessage.newBuilder();
			CommandMessage CMDsensor = cmd.build().getParserForType().parseFrom(in);
			//Preparando_pacote_UDP
			byte[] sendData = null;
			byte[] receiveData = new byte[1000];
			DatagramPacket requestPacket = null;
			DatagramPacket replyPacket = null;
			
			if(CMDsensor.hasCommand() && CMDsensor.hasParameter()){
				cmd.setCommand(CMDsensor.getCommand());
				cmd.setParameter(CMDsensor.getParameter());
				sendData = cmd.build().toByteArray();
				
				if(CMDsensor.getParameter().getType()==SensorType.LIGHT) {
					for(int i=0; i<listaLED.size(); i++) {
						requestPacket = new DatagramPacket(sendData, sendData.length, listaLED.get(i), 8888);
						UDPserverSocket.send(requestPacket);
					}
				} else if(CMDsensor.getParameter().getType()==SensorType.LUMINOSITY) {
					for(int i=0; i<listaLAM.size(); i++) {
						requestPacket = new DatagramPacket(sendData, sendData.length, listaLAM.get(i), 8888);
						UDPserverSocket.send(requestPacket);
					}
				} else if(CMDsensor.getParameter().getType()==SensorType.TEMPERATURE) {
					for(int i=0; i<listaTEMP.size(); i++) {
						requestPacket = new DatagramPacket(sendData, sendData.length, listaTEMP.get(i), 8888);
						UDPserverSocket.send(requestPacket);
					}
			}
					
					
			replyPacket = new DatagramPacket(receiveData, receiveData.length);
			UDPserverSocket.receive(replyPacket);
			out.write(replyPacket.getData());	
		}
			
		} catch (EOFException e) {
			
		} catch (IOException e) {
			
		} finally {
				try {
					clientSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				UDPserverSocket.close();
		}

	}
}

class SensorList extends Thread{
	DatagramSocket DemonSocket;
	DatagramPacket DemonPacket;
	ArrayList<InetAddress> listaLED;
	ArrayList<InetAddress> listaLAM;
	ArrayList<InetAddress> listaTEMP;
	byte[] receiveData;
	public SensorList(ArrayList<InetAddress> alistaLED, ArrayList<InetAddress> alistaLAM,
			ArrayList<InetAddress> alistaTEMP) throws IOException {
		
	listaLED = alistaLED;
	listaLAM = alistaLAM;
	listaTEMP = alistaTEMP;
	byte[] receiveData = new byte[5];
	DemonSocket = new DatagramSocket();
	DemonPacket = new DatagramPacket(receiveData, receiveData.length);
		
	}
	
	public void run() {
		try {
			while(true) {
				DemonSocket.receive(DemonPacket);
				CommandMessage.Builder cmd = CommandMessage.newBuilder();
				CommandMessage CMDsensor = cmd.build().getParserForType().parseFrom(DemonPacket.getData());
				if(CMDsensor.getParameter().getType()==SensorType.LIGHT && listaLED.contains(DemonPacket.getAddress())==false) {
					for(int i=0; i<listaLED.size(); i++) {
						listaLED.add(DemonPacket.getAddress());
					}
				} else if(CMDsensor.getParameter().getType()==SensorType.LUMINOSITY && listaLED.contains(DemonPacket.getAddress())==false) {
					for(int i=0; i<listaLAM.size(); i++) {
						listaLAM.add(DemonPacket.getAddress());
					}
				} else if(CMDsensor.getParameter().getType()==SensorType.TEMPERATURE && listaLED.contains(DemonPacket.getAddress())==false) {
					for(int i=0; i<listaTEMP.size(); i++) {
						listaTEMP.add(DemonPacket.getAddress());
					}
			}
			
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}

