package org.r2d2.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import lejos.hardware.Button;
import lejos.robotics.Color;

public class Calibration {
	

	/**
	 * Load file if exists
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void loadCalibration() throws FileNotFoundException, IOException, ClassNotFoundException {
		File fichierPince = new File("calibracionpince");
		File fichierCouleur = new File("calibracioncouleur");
		if (fichierCouleur.exists()) {
			ObjectInputStream oisColor = new ObjectInputStream(new FileInputStream(fichierCouleur));
			Controler.COLOR.setCalibration((float[][]) oisColor.readObject());
			oisColor.close();
		}

		if (fichierPince.exists()) {
			ObjectInputStream oisPince = new ObjectInputStream(new FileInputStream(fichierPince));
			Controler.GRABER.setOpenTime((long) oisPince.readObject());
			oisPince.close();
		}
	}

	/**
	 * Save calibration
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

			ObjectOutputStream strPince = new ObjectOutputStream(new FileOutputStream(fichierPince));
			strPince.writeObject(Controler.GRABER.getOpenTime());
			strPince.flush();
			strPince.close();

			if (!fichierCouleur.exists()) {
				fichierCouleur.createNewFile();

				ObjectOutputStream strCouleur = new ObjectOutputStream(new FileOutputStream(fichierCouleur));
				strCouleur.writeObject(Controler.COLOR.getCalibration());
				strCouleur.flush();
				strCouleur.close();

			}

		}
	}
	
	/**
	 * S'occupe d'effectuer l'ensemble des calibrations nécessaires au bon
	 * fonctionnement du robot.
	 * 
	 * @return vrai si tout c'est bien passé.
	 */
	public boolean calibration() {

		return calibrationGrabber() && calibrationCouleur();
	}

	private boolean calibrationGrabber() {
		Controler.SCREEN.drawText("Calibration", "Calibration de la fermeture de la pince", "Appuyez sur le bouton central ",
				"pour continuer");
		if (Controler.INPUT.waitOkEscape(Button.ID_ENTER)) {
			Controler.SCREEN.drawText("Calibration", "Appuyez sur ok", "pour lancer et arrêter");
			Controler.INPUT.waitAny();
			Controler.GRABER.startCalibrate(false);
			Controler.INPUT.waitAny();
			Controler.GRABER.stopCalibrate(false);

			Controler.SCREEN.drawText("Calibration", "Appuyer sur Entree", "pour commencer la", "calibration de l'ouverture");
			Controler.INPUT.waitAny();

			Controler.SCREEN.drawText("Calibration", "Appuyer sur Entree", "Quand la pince est ouverte");
			Controler.GRABER.startCalibrate(true);
			Controler.INPUT.waitAny();
			Controler.GRABER.stopCalibrate(true);

		} else {
			return false;
		}
		return true;
	}

	/**
	 * Effectue la calibration de la couleur
	 * 
	 * @return renvoie vrai si tout c'est bien passé
	 */
	private boolean calibrationCouleur() {
		Controler.SCREEN.drawText("Calibration", "Préparez le robot à la ", "calibration des couleurs",
				"Appuyez sur le bouton central ", "pour continuer");
		if (Controler.INPUT.waitOkEscape(Button.ID_ENTER)) {
			Controler.COLOR.lightOn();

			// calibration gris
			Controler.SCREEN.drawText("Gris", "Placer le robot sur ", "la couleur grise");
			Controler.INPUT.waitAny();
			Controler.COLOR.calibrateColor(Color.GRAY);

			// calibration rouge
			Controler.SCREEN.drawText("Rouge", "Placer le robot ", "sur la couleur rouge");
			Controler.INPUT.waitAny();
			Controler.COLOR.calibrateColor(Color.RED);

			// calibration noir
			Controler.SCREEN.drawText("Noir", "Placer le robot ", "sur la couleur noir");
			Controler.INPUT.waitAny();
			Controler.COLOR.calibrateColor(Color.BLACK);

			// calibration jaune
			Controler.SCREEN.drawText("Jaune", "Placer le robot sur ", "la couleur jaune");
			Controler.INPUT.waitAny();
			Controler.COLOR.calibrateColor(Color.YELLOW);

			// calibration bleue
			Controler.SCREEN.drawText("BLeue", "Placer le robot sur ", "la couleur bleue");
			Controler.INPUT.waitAny();
			Controler.COLOR.calibrateColor(Color.BLUE);

			// calibration vert
			Controler.SCREEN.drawText("Vert", "Placer le robot ", "sur la couleur vert");
			Controler.INPUT.waitAny();
			Controler.COLOR.calibrateColor(Color.GREEN);

			// calibration blanc
			Controler.SCREEN.drawText("Blanc", "Placer le robot ", "sur la couleur blanc");
			Controler.INPUT.waitAny();
			Controler.COLOR.calibrateColor(Color.WHITE);

			Controler.COLOR.lightOff();
			return true;
		}
		return true;
	}

}

