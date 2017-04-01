package test;

import org.r2d2.sensor.VisionSensor;
import org.r2d2.util.R2D2Constants;

import ev3code.demo.DifferentialDrive;
import lejos.hardware.ev3.LocalEV3;
import lejos.utility.Delay;

public class Main {

	public static void main(String[] args) {

		VisionSensor vision = new VisionSensor();
		DifferentialDrive drive = new DifferentialDrive(LocalEV3.get().getPort(R2D2Constants.LEFT_WHEEL),
				LocalEV3.get().getPort(R2D2Constants.RIGHT_WHEEL));
		double a;
		while (true) {

			a = vision.getDistance();
			System.out.println(a);

			if (a > 0.15) {
				drive.forward();
			} else {
				drive.Backward();
			}

			Delay.msDelay(1000);
		}

	}

}
