package org.r2d2.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import lejos.hardware.Button;

public class Calibration {
	
	public void start() throws IOException, ClassNotFoundException {
//		loadCalibration();
//		Controler.SCREEN.drawText("Calibration", "Appuyez sur echap ", "pour skipper");
//		boolean skip = Controler.INPUT.waitOkEscape(Button.ID_ESCAPE);
//		if (skip || calibration()) {
//			if (!skip) {
//				saveCalibration();
//			}
//			Controler.SCREEN.drawText("Lancer", "Appuyez sur OK si la", "ligne noire est à gauche", "Appuyez sur tout autre",
//					"elle est à droite");
//			if (Controler.INPUT.isThisButtonPressed(INPUT.waitAny(), Button.ID_ENTER)) {
//				mainLoop(true);
//			} else {
//				mainLoop(false);
//			}
//		}
//		cleanUp();
	}

	/**
	 * Charge la calibration du fichier de configuration si elle existe
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void loadCalibration() throws FileNotFoundException, IOException, ClassNotFoundException {
		File fichierPince = new File("calibracionpince");
		File fichierCouleur = new File("calibracioncouleur");
		if (fichierPince.exists()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fichierPince));
			Controler.GRABER.setOpenTime((long) ois.readObject());
			ois.close();
		}
		if (fichierCouleur.exists()) {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fichierCouleur));
			Controler.COLOR.setCalibration((float[][]) ois.readObject());
			ois.close();
		}
	}

	/**
	 * Sauvegarde la calibration
	 * 
	 * @throws IOException
	 */
	public void saveCalibration() throws IOException {
		Controler.SCREEN.drawText("Sauvegarde", "Appuyez sur le bouton central ", "pour valider id", "Echap pour ne pas sauver");
		if (Controler.INPUT.waitOkEscape(Button.ID_ENTER)) {
			File fichierPince = new File("calibracionpince");
			File fichierCouleur = new File("calibracioncouleur");
			if (!fichierPince.exists()) {
				fichierPince.createNewFile();
			} else {
				fichierPince.delete();
				fichierPince.createNewFile();
			}

			ObjectOutputStream str = new ObjectOutputStream(new FileOutputStream(fichierPince));
			str.writeObject(Controler.COLOR.getCalibration());
			str.writeObject(Controler.GRABER.getOpenTime());
			str.flush();
			str.close();

			if (!fichierCouleur.exists()) {
				fichierCouleur.createNewFile();

				ObjectOutputStream strCouleur = new ObjectOutputStream(new FileOutputStream(fichierCouleur));
				strCouleur.writeObject(Controler.COLOR.getCalibration());
				strCouleur.flush();
				strCouleur.close();

			}

		}
	}

}