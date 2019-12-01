package gateway_src;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.*;

import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.CommandMessage.CommandType;
import protoClass.SensorOuterClass.Sensor;
import protoClass.SensorOuterClass.Sensor.SensorType;

public class RabbitRPC extends Thread {
	private ArrayList<sensorBuff> sensorList;
	private String host;
	private String username;
	private String password;
	private CommandMessage.Builder cmd;
	DatagramSocket socketUDP;
	DatagramPacket packet;
	DatagramPacket packetRec;

	private final String RPC_QUEUE_NAME = "rpc_queue";

	public RabbitRPC(ArrayList<sensorBuff> sensorList, DatagramSocket socket, String host,
			String username, String password) {
		this.sensorList = sensorList;
		this.host = host;
		this.username = username;
		this.password = password;
		this.cmd = CommandMessage.newBuilder();
		this.socketUDP = socket;
	}

	private Sensor getSensorById(SensorType type) {
		for (int i = 0; i < sensorList.size(); i++) {
			if (sensorList.get(i).getSensor().getType() == type) {
				return sensorList.get(i).getSensor();
			}
		}

		return null;
	}

	public void run() {
		ConnectionFactory factory = new ConnectionFactory();

		factory.setHost(host);
		factory.setUsername(username);
		factory.setPassword(password);

		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
			channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
			channel.queuePurge(RPC_QUEUE_NAME);

			channel.basicQos(1);

			System.out.println(" [x] Awaiting RPC requests");

			Object monitor = new Object();

			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
						.correlationId(delivery.getProperties().getCorrelationId()).build();
				
				String response = "";

				try {
					String message = new String(delivery.getBody(), "UTF-8");

					String[] result = message.split("/");

					if (result[0].equals("get_state")) {

						Sensor sensor = null;

						if (result[1] == "GAS") {
							sensor = getSensorById(SensorType.GAS);
						} else if (result[1].equals("LUMINOSITY")) {
							sensor = getSensorById(SensorType.LUMINOSITY);
						} else if (result[1].equals("LIGHT")) {
							sensor = getSensorById(SensorType.LIGHT);
						} else if (result[1].equals("TEMPERATURE")) {
							sensor = getSensorById(SensorType.TEMPERATURE);
						}

						if (sensor != null) {
							response += sensor.getState();
						} else {
							response += "0.0";
						}

					} else if (result[0].equals("set_state")) {

						cmd.setCommand(CommandType.SET_STATE);

						Sensor sensor = null;

						if (result[1] == "GAS") {
							sensor = getSensorById(SensorType.GAS);
						} else if (result[1].equals("LUMINOSITY")) {
							sensor = getSensorById(SensorType.LUMINOSITY);
						} else if (result[1].equals("LIGHT")) {
							sensor = getSensorById(SensorType.LIGHT);
						} else if (result[1].equals("TEMPERATURE")) {
							sensor = getSensorById(SensorType.TEMPERATURE);
						}

						if (sensor != null) {
							Sensor.Builder sensor_send = sensor.toBuilder();
							sensor_send.setState(Float.parseFloat(result[2]));
							cmd.setParameter(sensor_send);

							byte[] sendData = cmd.build().toByteArray();

							sensorBuff s = new sensorBuff();

							try {
								int i = s.sensorListIndex(cmd.getParameter(), sensorList);
								packet = new DatagramPacket(sendData, sendData.length, sensorList.get(i).getIP(),
										sensorList.get(i).getPort());

								socketUDP.send(packet);

								response += "true";
							} catch (Exception e) {
								response += "false";
							}

						} else {
							response += "false";
						}

					}


				} catch (RuntimeException e) {
					System.out.println(" [.] " + e.toString());
				} finally {
					System.out.println(response);
					channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps,
							response.getBytes("UTF-8"));
					channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
					// RabbitMq consumer worker thread notifies the RPC server owner thread
					synchronized (monitor) {
						monitor.notify();
					}
				}
			};

			try {
				channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> {
				}));
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			// Wait and be prepared to consume the message from RPC client.
			while (true) {
				synchronized (monitor) {
					try {
						monitor.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (TimeoutException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
