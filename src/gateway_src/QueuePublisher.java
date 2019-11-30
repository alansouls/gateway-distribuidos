package gateway_src;

import java.util.ArrayList;

import com.rabbitmq.client.*;

import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.Sensor;

public class QueuePublisher extends Thread {
	private String host;
	private String username;
	private String password;
	private String exchangeName;

	public QueuePublisher(String host, String exchangeName, String username, String password) {
		this.host = host;
		this.username = username;
		this.password = password;
		this.exchangeName = exchangeName;
	}
	
	

	public void publishData(String msg, String routingKey) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setUsername(username);
		factory.setPassword(password);
		try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {		
			channel.basicPublish(exchangeName, routingKey, null, msg.getBytes("UTF-8"));
		}
	}
}
