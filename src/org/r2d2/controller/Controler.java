package org.r2d2.controller;

import java.io.IOException;
import java.util.List;

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

/**
 * Classe principal qui gére tous les états trouvés pendant le jeux
 * 
 * @author CuchoTeam
 */
public final class Controler implements FeatureListener {

	static ColorSensor COLOR = null;
	static Propulsion PROPULSION = null;
	static Graber GRABER = null;
	static PressionSensor PRESSION = null;
	static VisionSensor VISION = null;
	static Screen SCREEN = null;
	static InputHandler INPUT = null;

	private FeatureDetector fd;

	private CameraClient camera;
	// True = we have a game against a robot. False sino
	private boolean match;


	private boolean test = true;

	private static State state;

	public Controler() {
		PROPULSION = new Propulsion();
		GRABER = new Graber();
		COLOR = new ColorSensor();
		PRESSION = new PressionSensor();
		VISION = new VisionSensor();
		SCREEN = new Screen();
		INPUT = new InputHandler(SCREEN);

		camera = new CameraClient(8888);

		fd = new RangeFeatureDetector(VISION.getDis(), R2D2Constants.DISTANCE_MAX_WALL, 200);
		fd.addListener(this);
		state = State.firstMove;

	}

	public void start() throws IOException, ClassNotFoundException {

		Delay.msDelay(1000);
		Calibration calibration = new Calibration();
		calibration.loadCalibration();
		Controler.SCREEN.drawText("Calibration", "Appuyez sur echap ", "pour skipper");
		boolean skip = Controler.INPUT.waitOkEscape(Button.ID_ESCAPE);
		if (skip || calibration.calibration()) {
			if (!skip) {
				calibration.saveCalibration();
			}

			Controler.SCREEN.drawText("Jeu seul", "Appuyez sur OK si on jeu", "contre quelqu'un",
					"Appuyez sur tout autre", "sinon");
			if (Controler.INPUT.isThisButtonPressed(INPUT.waitAny(), Button.ID_ENTER)) {
				match = true;
			} else {
				match = false;
			}

			Controler.SCREEN.drawText("Location 2", "Appuyez sur OK si la", "ligne noire est à gauche",
					"Appuyez sur tout autre", "elle est à droite");
			if (Controler.INPUT.isThisButtonPressed(INPUT.waitAny(), Button.ID_ENTER)) {
				main(true);
			} else {
				main(false);
			}

		}

		// We set up the first status of the robot
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
	 * Lance la boucle de jeu principale
	 * 
	 * Toutes les opérations dans la boucle principale doivent être le plus
	 * atomique possible. Cette boucle doit s'executer très rapidement.
	 */

	/**
	 * @param seekLeft
	 */
	private void main(boolean seekLeft) {
		SCREEN.clearDraw();
		boolean run = true;
		float searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
		int nbSeek = getNbObjetSurTerrain();
		boolean attempt2 = false;
		while (run) {
			 System.out.println(getState());
			try {
				PROPULSION.checkState();
				GRABER.checkState();
				switch (getState()) {
				case firstMove:
					PROPULSION.run(true);
					changeState(State.playStart);
					break;

				case playStart:
					/**
					 * Premier palet à chercher, 
					 * rotation pour éviter les palets
					 */
					
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
					// state = State.needToSeek;
					changeState(State.needToSeek);

					break;

				case needToSeek:
					 /**
					  * Effectue un quart de tour de recherche 
					  * On effectue la recherche seulement si dans le terain il
					  * y a des pallets. On obtien le nb de palets grace à la
					  * camera.
					  * Si on est dans un match if faur rester 1 pour le robot
					  * sur le terrain
					  */
					 
					nbSeek = getNbObjetSurTerrain();
					nbSeek = match ? nbSeek - 1 : nbSeek;
					if (nbSeek > 1) {
						// state = State.isSeeking;
						changeState(State.isSeeking);
						searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
						PROPULSION.halfTurn(seekLeft, R2D2Constants.SEARCH_SPEED);
					} else {
						SCREEN.drawText("FINITOOOOOO", "On est content !");
					}
					break;

				case isSeeking:
					/**
					 * Si l'objet percu est entre les bornes max et min de la vision :
					 * le robot s'arrête et effectue une deuxième recherche plus lente
					 * afin de mieux s'orienter vers ce palet.
					 * Le robot essaye de regarder des deux côtés, (cf. attempt2) 
					 * S'il ne trouve rien :
					 * il avance pendant un certain temps (modifiable), et effectue cette 
					 * même recherche, jusqu'à trouver une ligne blanche. 
					 */
					float newDist = VISION.getRaw()[0];
					if (newDist < R2D2Constants.MAX_VISION_RANGE && newDist >= R2D2Constants.MIN_VISION_RANGE) {
						if (searchPik == R2D2Constants.INIT_SEARCH_PIK_VALUE) {
							searchPik = newDist;
							PROPULSION.stopMoving();
							if (!attempt2) {
								PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, seekLeft,
										R2D2Constants.SLOW_SEARCH_SPEED);
							} else {
								PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, !seekLeft,
										R2D2Constants.SLOW_SEARCH_SPEED);
							}
						} else {
							if (newDist <= searchPik) {
								searchPik = newDist;
							} else {
								PROPULSION.stopMoving();
								attempt2 = false;
								changeState(State.needToGrab);
							}
						}
					} else {
						searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
					}

					// S'il a fini son tour de recherche et qu'il n'a pas trouvé de palet
					if (!PROPULSION.isRunning() && getState() != State.needToGrab) {
						if (!attempt2) {
							PROPULSION.halfTurn(!seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
							}

							PROPULSION.halfTurn(!seekLeft, R2D2Constants.SEARCH_SPEED);
							changeState(State.isSeeking);
							attempt2 = true;
						} else {
							PROPULSION.halfTurn(seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
							}
							PROPULSION.runFor(1500, true);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								// S'il atteint la limite du terrain
								if (COLOR.getCurrentColor() == Color.WHITE) {
									PROPULSION.stopMoving();
									changeState(State.isSeekingEnd);
								}
								if (PRESSION.isPressed()) {
									PROPULSION.stopMoving();
									changeState(State.isCatching);
									GRABER.close();
								}
							}
							if (getState() == State.isSeeking) {
								changeState(State.needToSeek);
							}
							attempt2 = false;
						}
					}

					break;

				case isSeekingEnd:
					/**
					 * Quand le robot atteint la fin de la recherche (ligne blanche)
					 * il essaye de se mettre dans l'autre moitié du terrain pour effectuer
					 * une autre recherche, a partir d'un autre point de départ.
					 * S'il ne s'y retrouve pas -> isSeekingLost.
					 */
					boolean turnGoodSide = false;
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

					PROPULSION.runFor(4000, true);

					/**
					 * TODO A changer le temps de parcourir une moitié de
					 * terrain ??
					 */

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
						changeState(State.needToSeek);
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

						// Run jusqu'à une ligne noire (signe de la traversée du terrain)
						PROPULSION.runFor(7000, true);

						/**
						 * TODO A changer le temps max pour parcourir 3/4 de
						 * terrain ??
						 */

						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
							if (COLOR.getCurrentColor() == Color.BLACK) {
								break;
							}
						}

						// Continue Run jusqu'à ligne Rouge ou jaune pour
						// commencer nouvelle recherche
						// S'il trouve une autre couleur de ligne, il est perdu :(
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
							if (COLOR.getCurrentColor() == Color.RED || COLOR.getCurrentColor() == Color.YELLOW) {
								PROPULSION.stopMoving();
								PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, !seekLeft,
										R2D2Constants.MAX_ROTATION_SPEED);
								while (PROPULSION.isRunning()) {
									PROPULSION.checkState();
								}
								changeState(State.needToSeek);
							}
							if (COLOR.getCurrentColor() == Color.BLUE || COLOR.getCurrentColor() == Color.GREEN) {
								PROPULSION.stopMoving();
								changeState(State.isSeekingLost);
							}
							if (INPUT.escapePressed()) {
								return;
							}
						}

					}

					break;

				case isSeekingLost:
					/**
					 * S'il le robot est perdu, on cherche de nouveau la ligne blanche 
					 * du camp adverse pour commencer une nouvelle recherche.
					 */
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
					changeState(State.needToSeek);
					break;

				case needToGrab:
					/**
					 * Le robot va chercher le palet.
					 */
					PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, true);
					changeState(State.isGrabing);
					break;

				case isGrabing:
					/**
					 * Si le robot n'a pas atteint le palet,
					 * alors il recule et refait une recherche.
					 */
					if (PRESSION.isPressed()) {
						PROPULSION.stopMoving();
						GRABER.close();
						while (GRABER.isRunning()) {
							GRABER.checkState();
							if (INPUT.escapePressed())
								return;
						}
						changeState(State.isCatching);
					}

					if (!PROPULSION.isRunning() && getState() != State.isCatching) {

						PROPULSION.runFor(1500, false);
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
						}
						changeState(State.needToSeek);
					}

					break;

				case isCatching:
					/**
					 * Il a le palet et s'oriente vers le camp adverse.
					 */
					PROPULSION.orientateNorth();
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
					}
					changeState(State.needToScoreGoal);
					break;

				case needToScoreGoal:
					/**
					 *  We come back to score a goal
					 */
					PROPULSION.run(true);
					while (PROPULSION.isRunning()&&getState()==State.needToScoreGoal) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							PROPULSION.stopMoving();
						}
						if (COLOR.getCurrentColor() == Color.WHITE) {
							PROPULSION.stopMoving();
							changeState(State.needToReleaseBall);
						}
					}
					break;

				case needToReleaseBall:
					/**
					 *  The robot have detected the white line, so it have to release the ball
					 */
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

					changeState(State.needToSeek);

					break;

				case wallInFront:
					/**
					 * The robot has detected a wall, it rollback until it
					 * detect a color not gray. The robot face north, against
					 * the adversary
					 */
					System.out.println("State HOLAA");
					boolean whiteLine=false;
					PROPULSION.stopMoving();
					PROPULSION.run(false);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
						if (COLOR.getCurrentColor() != Color.GRAY) {
							if(COLOR.getCurrentColor() == Color.WHITE){
								whiteLine=true;
							}
							PROPULSION.stopMoving();
						}
					}
					
					if(whiteLine){
						PROPULSION.orientateSouth(seekLeft);
					}else{
						PROPULSION.orientateNorth();
					}
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
					}
					
					PROPULSION.run(true);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
						if(COLOR.getCurrentColor()==Color.WHITE){
							PROPULSION.stopMoving();
						}
					}
					
					PROPULSION.volteFace(seekLeft);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
					}
					
					if (!attempt2)
						seekLeft = !seekLeft;
					changeState(State.needToSeek);

					break;

				case wallInFrontWithPallet:
					/**
					 * The robot has detected a wall but it has a ball. The
					 * robot face north, against the adversary
					 */
					PROPULSION.stopMoving();
					PROPULSION.runFor(R2D2Constants.HALF_SECOND, false);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
					}
					PROPULSION.orientateNorth();
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed()) {
							return;
						}
					}

					changeState(State.needToScoreGoal);

					break;

				}

				if (INPUT.escapePressed())
					return;

			} catch (Throwable t) {
				t.printStackTrace();
				run = false;
			}
		}
	}

	private int getNbObjetSurTerrain() {
		int size = 4;
		if (!test) {
			List<ObjectPosition> listCamera = camera.getObjectPosition();
			size = listCamera.size();
		}

		return size;
	}

	private void changeState(State st) {
		synchronized (state) {
			state = st;
		}

	}

	private State getState() {
		synchronized (state) {
			return state;
		}
	}

	@Override
	public void featureDetected(Feature feature, FeatureDetector detector) {
		System.out.println("State " + state + " Range" + feature.getRangeReading().getRange());
		if(getState() == State.wallInFrontWithPallet || getState()==State.wallInFront){
			return;
		}
		if (getState() == State.needToScoreGoal) {
			changeState(State.wallInFrontWithPallet);
		} else {
			changeState(State.wallInFront);
		}
	}

}