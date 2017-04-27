package test;

import org.r2d2.sensor.VisionSensor;
import org.r2d2.util.R2D2Constants;

import ev3code.demo.DifferentialDrive;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.robotics.RangeFinder;
import lejos.robotics.objectdetection.Feature;
import lejos.robotics.objectdetection.FeatureDetector;
import lejos.robotics.objectdetection.FeatureListener;
import lejos.robotics.objectdetection.RangeFeatureDetector;
import lejos.utility.Delay;

public class Test implements FeatureListener {

	static Test test;

	public static void main(String[] args) {
		test = new Test();
		VisionSensor vision = new VisionSensor();
//		DifferentialDrive drive = new DifferentialDrive(LocalEV3.get().getPort(R2D2Constants.LEFT_WHEEL),
//				LocalEV3.get().getPort(R2D2Constants.RIGHT_WHEEL));
		float a = 0.100f;
		FeatureDetector fd = new RangeFeatureDetector(vision.getDis(), a, 500);

		fd.addListener(test);
		while (true) {

			// a = vision.getRaw()[0];
//			a = vision.getDistance();
//			System.out.println(a);
//			System.out.println("+");

			// if (a < 0.09 || a == Double.POSITIVE_INFINITY) {
			// drive.Backward();;
			// } else {
			// drive.forward();;
			// }
			// RangeFinder rf = null;
			
//			Button.ENTER.waitForPressAndRelease();

		}

	}

	@Override
	public void featureDetected(Feature feature, FeatureDetector detector) {
		float range = feature.getRangeReading().getRange();
//		Sound.playTone(1200 - ((int)range * 10), 100);
		System.out.println("Range:" + range);

	}

}
