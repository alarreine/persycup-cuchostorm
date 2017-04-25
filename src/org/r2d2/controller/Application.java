package org.r2d2.controller;

import lejos.utility.Delay;

public class Application {

	public static void main(String[] args) {
		Controler controler = new Controler();
		try {
			controler.start();
		} catch (Throwable e) {
			e.printStackTrace();
			Delay.msDelay(10000);
		}
		System.exit(0);
	}

}
