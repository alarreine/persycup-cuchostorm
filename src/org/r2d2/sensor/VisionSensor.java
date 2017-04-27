package org.r2d2.sensor;

import org.r2d2.util.R2D2Constants;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.RangeFinderAdapter;
import lejos.robotics.SampleProvider;
import lejos.robotics.filter.MeanFilter;
import lejos.robotics.filter.MedianFilter;

public class VisionSensor {

	private EV3UltrasonicSensor sonar = null;
	private Port port = null;

	public VisionSensor() {
		port = LocalEV3.get().getPort(R2D2Constants.IR_SENSOR);
		sonar = new EV3UltrasonicSensor(port);
	}

	/**
	 * 
	 * @return la distance lue par le capteur ultrason
	 */
	public float[] getRaw() {
		float[] sample = new float[1];
		sonar.fetchSample(sample, 0);
		return sample;
	}

	public double getDistance() {
		sonar.getDistanceMode();
		SampleProvider distance= sonar.getDistanceMode();
		float[] sample = new float[1];
		SampleProvider average = new MeanFilter(distance,5);
		average.fetchSample(sample, 0);
		return sample[0];
	}
	
	public RangeFinderAdapter getDis(){
		sonar.getDistanceMode();
		SampleProvider distance= sonar.getMode("Distance");
		SampleProvider average = new MedianFilter(distance,5);
		return new RangeFinderAdapter(average);
	}
	
	public EV3UltrasonicSensor getSensor(){
		return sonar;
	};

}
