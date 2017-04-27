package org.r2d2.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.r2d2.eye.InputHandler;
import org.r2d2.eye.Screen;
import org.r2d2.motor.Graber;
import org.r2d2.motor.Propulsion;
import org.r2d2.sensor.ColorSensor;
import org.r2d2.sensor.PressionSensor;
import org.r2d2.sensor.VisionSensor;
import org.r2d2.util.CameraClient;
import org.r2d2.util.ObjectPosition;
import org.r2d2.util.R2D2Constants;
import org.r2d2.util.State;

import lejos.hardware.Button;
import lejos.robotics.Color;
import lejos.robotics.objectdetection.Feature;
import lejos.robotics.objectdetection.FeatureDetector;
import lejos.robotics.objectdetection.FeatureListener;
import lejos.robotics.objectdetection.RangeFeatureDetector;
import lejos.utility.Delay;

public final class Controler implements FeatureListener {

	static ColorSensor COLOR = null;
	static Propulsion PROPULSION = null;
	static Graber GRABER = null;
	static PressionSensor PRESSION = null;
	static VisionSensor VISION = null;
	static Screen SCREEN = null;
	static InputHandler INPUT = null;

	// private FeatureDetector fd;

	private CameraClient camera;
	// True = we have a game against a robot. False sino
	private boolean match;

	// True if we are blue side
	private boolean blueSide;

	State state = State.firstMove;

	public Controler() {
		PROPULSION = new Propulsion();
		GRABER = new Graber();
		COLOR = new ColorSensor();
		PRESSION = new PressionSensor();
		VISION = new VisionSensor();
		SCREEN = new Screen();
		INPUT = new InputHandler(SCREEN);
		// motors.add(PROPULSION);
		// motors.add(GRABER);

		camera = new CameraClient(8888);

		FeatureDetector fd = new RangeFeatureDetector(VISION.getDis(), R2D2Constants.DISTANCE_MAX_WALL, 500);
		fd.addListener(this);

	}

	public void start() throws IOException, ClassNotFoundException {
		// Controler.SCREEN.showCucho();
		Delay.msDelay(1000);
		Calibration calibration = new Calibration();
		calibration.loadCalibration();
		Controler.SCREEN.drawText("Calibration", "Appuyez sur echap ", "pour skipper");
		boolean skip = Controler.INPUT.waitOkEscape(Button.ID_ESCAPE);
		if (skip || calibration.calibration()) {
			if (!skip) {
				calibration.saveCalibration();
			}

			Controler.SCREEN.drawText("Lancer", "Appuyez sur OK si on jeu", "contre quelqu'un",
					"Appuyez sur tout autre", "sinon");
			if (Controler.INPUT.isThisButtonPressed(INPUT.waitAny(), Button.ID_ENTER)) {
				match = true;
			} else {
				match = false;
			}

			Controler.SCREEN.drawText("Lancer", "Appuyez sur OK si on est", "dans le cote blue",
					"Appuyez sur tout autre", "sinon");
			if (Controler.INPUT.isThisButtonPressed(INPUT.waitAny(), Button.ID_ENTER)) {
				blueSide = true;
			} else {
				blueSide = false;
			}

			Controler.SCREEN.drawText("Lancer", "Appuyez sur OK si la", "ligne noire est à gauche",
					"Appuyez sur tout autre", "elle est à droite");
			if (Controler.INPUT.isThisButtonPressed(INPUT.waitAny(), Button.ID_ENTER)) {
				mainLoopTest(true);
			} else {
				mainLoopTest(false);
			}

		}
		cleanUp();
	}

	/**
	 * Effectue l'ensemble des actions nécessaires à l'extinction du programme
	 */
	private void cleanUp() {
		if (!GRABER.isOpen()) {
			GRABER.open();
			while (GRABER.isRunning()) {
				GRABER.checkState();
			}
		}
		PROPULSION.runFor(500, true);
		while (PROPULSION.isRunning()) {
			PROPULSION.checkState();
		}
		COLOR.lightOff();
	}

	/**
	 * Lance les tests du robot, peut être desactivé pour la persy cup
	 */
	// private void runTests() {
	// SystemTest.grabberTest(this);
	// }

	/**
	 * Lance la boucle de jeu principale
	 * 
	 * Toutes les opérations dans la boucle principale doivent être le plus
	 * atomique possible. Cette boucle doit s'executer très rapidement.
	 */

	private void mainLoopTest(boolean seekLeft) {
		state = State.firstMove;
		boolean run = true;
		float searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
		// int nbSeek = R2D2Constants.INIT_NB_SEEK;
		int nbSeek = getNbObjetSurTerrain();
		boolean attempt2 = false;
		while (run) {
			System.out.println(state);
			try {
				PROPULSION.checkState();
				GRABER.checkState();
				switch (state) {

				case firstMove:
					PROPULSION.run(true);
					state = State.playStart;
					break;

				case playStart:
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (PRESSION.isPressed()) {
							PROPULSION.stopMoving();
							GRABER.close();
						}
					}

					// Bras se ferment et en meme temps, Se decale et avance
					// pour eviter les autres palets
					PROPULSION.rotate(20, !seekLeft, true);
					while (GRABER.isRunning() || PROPULSION.isRunning()) {
						GRABER.checkState();
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}

					// Avance pendant deux secondes

					PROPULSION.runFor(2000, true);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}

					// Se replace dans la bonne direction
					// PROPULSION.orientateNorth();
					PROPULSION.rotate(20, seekLeft, true);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}

					// Avance jusqu'a ligne blanche
					PROPULSION.run(true);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
						if (COLOR.getCurrentColor() == Color.WHITE) {
							PROPULSION.stopMoving();
						}
					}

					// Lache l'objet
					// nbSeek--;
					GRABER.open();
					while (GRABER.isRunning()) {
						GRABER.checkState();
						if (INPUT.escapePressed())
							return;
					}

					// Recule
					PROPULSION.runFor(R2D2Constants.HALF_SECOND, false);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}

					// Demi-tour pour commencer la recherche
					PROPULSION.rotate(R2D2Constants.HALF_CIRCLE, !seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}
					state = State.needToSeek;

					break;

				case needToSeek:
					// Effectue un quart de tour de recherche
					// On effectue la recherche seulement si dans le terain il
					// y a des pallets. On obtien le nb de palets grace à la
					// camera.
					// Si on est dans un match if faur rester 1 pour le robot
					// sur le terrain
					nbSeek = getNbObjetSurTerrain();
					nbSeek = match ? nbSeek - 1 : nbSeek;
					if (nbSeek > 1) {
						state = State.isSeeking;
						searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
						PROPULSION.halfTurn(seekLeft, R2D2Constants.SEARCH_SPEED);
					} else {
						SCREEN.drawText("FINITOOOOOO", "On est content !");
					}
					break;

				case isSeeking:
					float newDist = VISION.getRaw()[0];
					// Si la distance de l'objet percu est entre les bornes max
					// et min de la vision : OK
					if (newDist < R2D2Constants.MAX_VISION_RANGE && newDist >= R2D2Constants.MIN_VISION_RANGE) {
						if (searchPik == R2D2Constants.INIT_SEARCH_PIK_VALUE) {
							searchPik = newDist;
							PROPULSION.stopMoving();
							if (!attempt2) {
								PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, !seekLeft, R2D2Constants.SLOW_SEARCH_SPEED);
							} else {
								PROPULSION.rotate(10, seekLeft, R2D2Constants.SLOW_SEARCH_SPEED);
							}
						} else {
							if (newDist <= searchPik) {
								searchPik = newDist;
							} else {
								PROPULSION.stopMoving();
								attempt2 = false;
								state = State.needToGrab;
							}
						}
					} else {
						searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
					}
					
					// S'il a fini son tour de recherche et qu'il n'a pas trouvé
					// de palet
					if (!PROPULSION.isRunning() && state != State.needToGrab) {
						if (!attempt2) {
							PROPULSION.halfTurn(!seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
							// PROPULSION.orientateSouth(!seekLeft);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
							}

							PROPULSION.halfTurn(!seekLeft, R2D2Constants.SEARCH_SPEED);
							state = State.isSeeking; // Inutile ??
							attempt2 = true;
						} else {
							PROPULSION.halfTurn(seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
							// PROPULSION.orientateSouth(seekLeft);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
							}
							PROPULSION.runFor(1500, true);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								// S'il atteint la limite du terrain
								if (COLOR.getCurrentColor() == Color.WHITE) {
									PROPULSION.stopMoving();
									state = State.isSeekingEnd;
								}
								if (PRESSION.isPressed()) {
									PROPULSION.stopMoving();
									state = State.isCatching;
									GRABER.close();
								}
							}
							if (state == State.isSeeking) {
								state = State.needToSeek;
							}
							attempt2 = false;
						}
					}

					break;

				case isSeekingEnd:
					// TODO verifier d'abord avec liste caméra si il n'y a plus
					// d'objet ?
					// Si ya des objets.. Continuer ! Sinon FIN !
					boolean turnGoodSide = false;
					// PROPULSION.volteFace(seekLeft,
					// R2D2Constants.MAX_ROTATION_SPEED);
					PROPULSION.orientateNorth();
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
					}

					PROPULSION.run(true);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (COLOR.getCurrentColor() == Color.WHITE)
							PROPULSION.stopMoving();
						if (INPUT.escapePressed())
							return;
					}

					PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
					}

					PROPULSION.runFor(4000, true); // TODO A changer le temps de
													// parcourir une moitié de
													// terrain ??
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (COLOR.getCurrentColor() == Color.BLACK) {
							turnGoodSide = true;
						}

					}

					// Vrai s'il a traversé le terrain, Faux sinon
					if (turnGoodSide) {
						PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
						}
						state = State.needToSeek;
						// seekLeft = !seekLeft;
					} else {
						// Recule pour faire demi-tour
						PROPULSION.runFor(R2D2Constants.QUARTER_SECOND, false);
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
						}

						// Demi-tour
						PROPULSION.volteFace(seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
						}

						// Run jusqu'à une ligne noire (signe de la traversée du
						// terrain)
						PROPULSION.runFor(7000, true); // TODO A changer le
														// temps max pour
														// parcourir 3/4 de
														// terrain ??
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
							if (COLOR.getCurrentColor() == Color.BLACK) {
								break;
							}
						}

						// Continue Run jusqu'à ligne Rouge ou jaune pour
						// commencer nouvelle recherche
						// S'il trouve une autre couleur de ligne, il est perdu
						// :(
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
							if (COLOR.getCurrentColor() == Color.RED || COLOR.getCurrentColor() == Color.YELLOW) {
								PROPULSION.stopMoving();
								PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, !seekLeft,
										R2D2Constants.MAX_ROTATION_SPEED);
								while (PROPULSION.isRunning()) {
									PROPULSION.checkState();
								}
								state = State.needToSeek;
							}
							if (COLOR.getCurrentColor() == Color.BLUE || COLOR.getCurrentColor() == Color.GREEN) {
								PROPULSION.stopMoving();
								state = State.isSeekingLost;
							}
							if (INPUT.escapePressed()) {
								return;
							}
						}

					}

					break;

				case isSeekingLost:
					PROPULSION.stopMoving();
					PROPULSION.orientateNorth();
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
					}
					PROPULSION.run(true);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
						if (COLOR.getCurrentColor() == Color.WHITE) {
							PROPULSION.stopMoving();
						}
					}
					PROPULSION.volteFace(seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
					state = State.needToSeek;
					break;

				case needToGrab:
					PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, true);
					state = State.isGrabing;
					break;

				case isGrabing:
					if (PRESSION.isPressed()) {
						PROPULSION.stopMoving();
						GRABER.close();
						while (GRABER.isRunning()) {
							GRABER.checkState();
							if (INPUT.escapePressed())
								return;
						}
						state = State.isCatching;
					}

					if (!PROPULSION.isRunning() && state != State.isCatching) {

						PROPULSION.runFor(1000, false);
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
						}
						state = State.needToSeek;
					}

					break;

				case isCatching:
					PROPULSION.orientateNorth();
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
					}
					// SCREEN.drawText("FIN", "FINITOOOO");
					state = State.needToGoBackHome;
					break;

				case needToGoBackHome:
					PROPULSION.run(true);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							PROPULSION.stopMoving();
						}
						if (COLOR.getCurrentColor() == Color.WHITE) {
							PROPULSION.stopMoving();
							state = State.needToRelease;
						}
					}
					break;

				case needToRelease:
					nbSeek--;
					GRABER.open();
					while (GRABER.isRunning()) {
						GRABER.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
					}
					PROPULSION.runFor(R2D2Constants.QUARTER_SECOND, false);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}

					PROPULSION.volteFace(seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}

					state = State.needToSeek;

					break;
				// Run Back until line any color.
				case wallInFront:
					PROPULSION.run(false);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
						int color = COLOR.getCurrentColor();
						if ((color == Color.RED && blueSide) || (color == Color.YELLOW && !blueSide)) {
							PROPULSION.stopMoving();

						}

						if ((color == Color.RED && !blueSide) || (color == Color.YELLOW && blueSide)) {
							PROPULSION.stopMoving();
							attempt2 = true;
						}

					}
					PROPULSION.orientateNorth();

					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
					}

					state = State.isSeeking;

					break;

				}

				/*
				 * TODO : Case! - isSeekingEnd -> Si plus d'objet sur le
				 * terrain, STOP! - needToGrab -> Aller pret d'un palet
				 * (impossible a tester à la maison) mlm - isGrabing -> Comment
				 * savoir s'il est prêt du palet ? pour pouvoir ajuster
				 * l'orientation vers le palet et ne pas continuer pendant le
				 * MAXTIMEGRAB'. + Si OK ^ : 3 tentatives ;) (FAIT PLUS BAS) -
				 * isCatching -> OrienterNord - GoBackHome -> run jusqu'à la
				 * ligne blanche + open graber + backward + volte face ->
				 * needToSeek
				 * 
				 * TODO : GENERAL - Aller chercher un palet, en ayant la
				 * distance ou en f° de la vue si c'est possible. (savoir se
				 * deplacer sur une distance???) - Faire la différence entre un
				 * robot et un objet
				 * 
				 * note:isGrabing fonctionne que si le robot touche le palet du
				 * premier coup...
				 * 
				 */

				if (INPUT.escapePressed())
					return;

			} catch (Throwable t) {
				t.printStackTrace();
				run = false;
			}
		}
	}

	private int getNbObjetSurTerrain() {
		List<ObjectPosition> listCamera = camera.getObjectPosition();

		return listCamera != null ? listCamera.size() : 0;
	}

	@Override
	public void featureDetected(Feature feature, FeatureDetector detector) {
		float range = feature.getRangeReading().getRange();
		state = State.wallInFront;
	}

}