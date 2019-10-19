package gateway_src;

import protoClass.SensorOuterClass.CommandMessage;
import protoClass.SensorOuterClass.CommandMessage.CommandType;
import protoClass.SensorOuterClass.Sensor;
import protoClass.SensorOuterClass.Sensor.SensorType;

class testes {
	public static void main(String args[]) {

			CommandMessage.Builder cmd = CommandMessage.newBuilder();
			cmd.clear();
			cmd.setCommand(CommandType.GET_STATE);
			Sensor.Builder sensor = Sensor.newBuilder();
			sensor.setState(1);
			sensor.setId(2);
			sensor.setType(SensorType.LIGHT);
			cmd.setParameter(sensor);
			System.out.println(cmd.toString());
	}
}