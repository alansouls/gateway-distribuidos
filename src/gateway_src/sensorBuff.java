package gateway_src;

import java.net.InetAddress;
import java.util.ArrayList;
import java.time.*;

//import protoClass.SensorOuterClass;
import protoClass.SensorOuterClass.Sensor;

public class sensorBuff {
	private Sensor sensor;
	private InetAddress IP;
	private int port;
	private LocalDateTime date;

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
	
	public LocalDateTime getDate(){
		return this.date;
	}
	
	public void setDate(LocalDateTime date) {
		this.date = date;
	}
	
	public boolean containSensorPerID(Sensor sensor, ArrayList<sensorBuff> sensorList) {
		for(int i=0; i<sensorList.size(); i++) {
			if(sensorList.get(i).getSensor().getId()==sensor.getId()) {
				return true;
			}
		}
		return false;
	}
	
	public int sensorListIndex(Sensor sensor, ArrayList<sensorBuff> sensorList) throws Exception {
		for(int i=0; i<sensorList.size(); i++) {
			if(sensorList.get(i).getSensor().getId()==sensor.getId()) {
				return i;
			}
		}
		throw new Exception();
	}
	
}
