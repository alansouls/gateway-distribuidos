package gateway_src;

import java.net.*;
import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.CommandMessage.CommandType;
import protoClass.SensorOuterClass.Sensor;
import protoClass.SensorOuterClass.Sensor.SensorType;
import protoClass.SensorOuterClass.time;

import java.io.*;

public class TCP_client {
	public static void main(String args[]) {
		int serverPort = 7777;
		String serverIp = "localhost";
		Socket s = null;
		try {
			CommandMessage.Builder cmd = CommandMessage.newBuilder();
			Sensor.Builder sensor = Sensor.newBuilder();
			time.Builder t = time.newBuilder();
			sensor.clear();
			sensor.setId(1);
			sensor.setState(0);
			//
			t.setYear(2019);
			t.setMonth(10);
			t.setDay(11);
			t.setHours(19);
			t.setMinutes(1);
			t.setSeconds(0);
			sensor.setData(t);
			//
			sensor.setType(SensorType.TEMPERATURE);
			cmd.setParameter(sensor);
			cmd.setCommand(CommandType.GET_STATE);
			s = new Socket(serverIp, serverPort);
			DataInputStream entrada = new DataInputStream(s.getInputStream());
			DataOutputStream saida = new DataOutputStream(s.getOutputStream());
			cmd.build().writeTo(saida);
			System.out.println("Msg enviada");
			entrada.readByte(); // read a line of data from the stream
			cmd.build().getParserForType().parseFrom(entrada);
			if(cmd.getParameter().hasState()) {
				System.out.println("Msg recebida!");
			}
			
		} catch (UnknownHostException e) {
			System.out.println("Socket:" + e.getMessage());
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
		} catch (IOException e) {
			System.out.println("readline:" + e.getMessage());
		} finally {
			if (s != null)
				try {
					s.close();
				} catch (IOException e) {
					System.out.println("close:" + e.getMessage());
				}
		}
	}
}
