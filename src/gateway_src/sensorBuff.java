package gateway_src;

import java.net.InetAddress;
import java.util.ArrayList;

//import protoClass.SensorOuterClass;
import protoClass.SensorOuterClass.Sensor;

public class sensorBuff {
	private Sensor sensor;
	private InetAddress IP;
	private int port;

	public sensorBuff() {
		
	}

	public InetAddress getIP(){
		return this.IP;
	}

	public int getPort(){
		return this.port;
	}

	public void setIP(InetAddress IPsensor){
		this.IP = IPsensor;
	}

	public void setPort(int portSensor){
		this.port = portSensor;
	}
	
	public void setSensor(Sensor sensorParam){
		this.sensor = sensorParam;
	}

	public Sensor getSensor(){
		return this.sensor;
	}
	
	public boolean containSensorPerID(Sensor sensor, ArrayList<sensorBuff> sensorList) {
		for(int i=0; i<sensorList.size(); i++) {
			if(sensorList.get(i).getSensor().getId()==sensor.getId()) {
				return true;
			}
		}
		return false;
	}
	
	public int sensorListIndex(Sensor sensor, ArrayList<sensorBuff> sensorList) {
		for(int i=0; i<sensorList.size(); i++) {
			if(sensorList.get(i).getSensor().getId()==sensor.getId()) {
				return i;
			}
		}
		return -1;
	}
	
}
