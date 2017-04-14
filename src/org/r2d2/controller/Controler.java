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

public final class Controler {

	static ColorSensor COLOR = null;
	static Propulsion PROPULSION = null;
	static Graber GRABER = null;
	static PressionSensor PRESSION = null;
	static VisionSensor VISION = null;
	static Screen SCREEN = null;
	static InputHandler INPUT = null;

	// private ArrayList<TImedMotor> motors = new ArrayList<TImedMotor>();

	private Map<Integer, ObjectPosition> mapObject;
	private CameraClient camera;
	private ObjectPosition me;

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

		mapObject = new HashMap<>();
		camera = new CameraClient(8888);
		me = new ObjectPosition();
	}

	public void start() throws IOException, ClassNotFoundException {
		Calibration calibration = new Calibration();
		calibration.loadCalibration();
		Controler.SCREEN.drawText("Calibration", "Appuyez sur echap ", "pour skipper");
		boolean skip = Controler.INPUT.waitOkEscape(Button.ID_ESCAPE);
		if (skip || calibration()) {
			if (!skip) {
				calibration.saveCalibration();
			}
			Controler.SCREEN.drawText("Lancer", "Appuyez sur OK si la", "ligne noire est à gauche",
					"Appuyez sur tout autre", "elle est à droite");
			if (Controler.INPUT.isThisButtonPressed(INPUT.waitAny(), Button.ID_ENTER)) {
				mainLoop(true);
			} else {
				mainLoop(false);
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
	
	private void mainLoopTest(boolean seekLeft){
		State state = State.firstMove;
		boolean run = true;
		boolean unique = true;
		boolean unique2 = true;
		float searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
		boolean isAtWhiteLine = false;
		int nbSeek = R2D2Constants.INIT_NB_SEEK;
		while (run) {
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
					
					// Bras se ferment et en meme temps, Se decale et avance pour eviter les autres palets
					PROPULSION.rotate(20, !seekLeft, true); 
					/*while(GRABER.isRunning() || PROPULSION.isRunning()){
						GRABER.checkState();
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}*/
					
					
					// Avance pendant deux secondes
					PROPULSION.runFor(2000, true);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}
					
					// Se replace dans la bonne direction
					//PROPULSION.orientateNorth();
					PROPULSION.rotate(20, seekLeft, true); 
					/*while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					} */
					
					// Avance jusqu'a ligne blanche
					PROPULSION.run(true);
					//PROPULSION.runFor(1500, true);
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
					PROPULSION.runFor(R2D2Constants.QUARTER_SECOND, false);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}
					
					// S'oriente sur le côté pour commencer la recherche
					//PROPULSION.rotate(180, seekLeft, false);
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
					state = State.isSeeking;			
					searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
					PROPULSION.halfTurn(seekLeft, R2D2Constants.SEARCH_SPEED);
					break;
				
				
				case isSeeking:
					float newDist = VISION.getRaw()[0];
					// Si la distance de l'objet percu est entre les bornes max et min de la vision : OK
					if (newDist < R2D2Constants.MAX_VISION_RANGE && newDist >= R2D2Constants.MIN_VISION_RANGE){
						if(searchPik == R2D2Constants.INIT_SEARCH_PIK_VALUE){
							searchPik = newDist;
							PROPULSION.stopMoving();
							PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, seekLeft, R2D2Constants.SLOW_SEARCH_SPEED);
							while(PROPULSION.isRunning()){
								PROPULSION.checkState();
								newDist = VISION.getRaw()[0];
								if(searchPik >= newDist){
									searchPik = newDist;
								}else{
									PROPULSION.stopMoving();
									state = State.needToGrab;
								}
							}
						}
					}
					
					// S'il a fini son tour de recherche et qu'il n'a pas trouvé de palet côté seekLeft
					if(!PROPULSION.isRunning() && state != State.needToGrab){
						PROPULSION.halfTurn(!seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
						while (PROPULSION.isRunning()){
							PROPULSION.checkState();
						}
						
						PROPULSION.halfTurn(!seekLeft, R2D2Constants.SEARCH_SPEED);
						state = State.isSeeking2;
					}						
					
					break;
				
				case isSeeking2:
					float newDist2 = VISION.getRaw()[0];
					if (newDist2 < R2D2Constants.MAX_VISION_RANGE && newDist2 >= R2D2Constants.MIN_VISION_RANGE){
						if(searchPik == R2D2Constants.INIT_SEARCH_PIK_VALUE){
							searchPik = newDist2;
							PROPULSION.stopMoving();
							PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, !seekLeft, R2D2Constants.SLOW_SEARCH_SPEED);
							while(PROPULSION.isRunning()){
								PROPULSION.checkState();
								newDist2 = VISION.getRaw()[0];
								if(searchPik >= newDist2){
									searchPik = newDist2;
								}else{
									PROPULSION.stopMoving();
									state = State.needToGrab;
								}
							}
						}
					}
					
					// S'il a fini son tour de recherche et qu'il n'a pas trouvé de palet côté !seekLeft
					if(!PROPULSION.isRunning() && state != State.needToGrab){
						PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, !seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
						}
						PROPULSION.runFor(2000, true);
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
							// S'il atteint la limite du terrain
							if(COLOR.getCurrentColor()==Color.WHITE){
								PROPULSION.stopMoving();
								state = State.isSeekingEnd;
							}
						}
						
						if(state != State.isSeekingEnd){
							state = State.needToSeek;
						}
					}
					
					break;
				
				case isSeekingEnd:
					// TODO verifier d'abord avec liste caméra si il n'y a plus d'objet ?
					// Si ya des objets.. Continuer ! Sinon FIN !
					boolean turnGoodSide=false;
					PROPULSION.volteFace(seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
					while (PROPULSION.isRunning()){
						PROPULSION.checkState();
					}
					
					PROPULSION.run(true);
					while(PROPULSION.isRunning()){
						PROPULSION.checkState();
						if(COLOR.getCurrentColor()==Color.WHITE){
							PROPULSION.stopMoving();
						}
					}
					
					PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
					while(PROPULSION.isRunning()){
						PROPULSION.checkState();
					}
					
					PROPULSION.runFor(4000, true); // TODO A changer le temps de parcourir une moitié de terrain ??
					while(PROPULSION.isRunning()){
						PROPULSION.checkState();
						if(COLOR.getCurrentColor()==Color.BLACK){
							turnGoodSide=true;
						}

					}
					
					// Vrai s'il a traversé le terrain, Faux sinon
					if(turnGoodSide){
						PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
						}
						state = State.needToSeek;
						seekLeft = !seekLeft;
					}else{
						// Recule pour faire demi-tour
						PROPULSION.runFor(R2D2Constants.QUARTER_SECOND, false);
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
						}
						
						// Demi-tour
						PROPULSION.volteFace(seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
						}
						
						// Run jusqu'à une ligne noire (signe de la traversée du terrain)
						PROPULSION.run(true); 
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
							if(COLOR.getCurrentColor()==Color.BLACK){
								break;
							}
						}
						
						// Continue Run jusqu'à ligne Rouge ou jaune pour commencer nouvelle recherche
						// S'il trouve une autre couleur de ligne, il est perdu :(
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
							if(COLOR.getCurrentColor()==Color.RED || COLOR.getCurrentColor()==Color.YELLOW){
								PROPULSION.stopMoving();
							}
							if(COLOR.getCurrentColor()==Color.BLUE || COLOR.getCurrentColor()==Color.GREEN){
								PROPULSION.stopMoving();
								state = State.isSeekingLost;
							}
							if(INPUT.escapePressed()){
								return;
							}
							// TODO Risque de boucle infinie ??
						}
						
						// S'il n'est pas perdu alors 
						if(state != State.isSeekingLost){
							PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, seekLeft, R2D2Constants.MAX_ROTATION_SPEED);
							while(PROPULSION.isRunning()){
								PROPULSION.checkState();
							}
							state = State.needToSeek;
							seekLeft = !seekLeft;
						}
						
					}
					
					break;
				
				case isSeekingLost:
					PROPULSION.stopMoving();
					PROPULSION.orientateNorth();
					while(PROPULSION.isRunning()){
						PROPULSION.checkState();
					}
					PROPULSION.run(true);
					while(PROPULSION.isRunning()){
						PROPULSION.checkState();
						if(INPUT.escapePressed()){
							return;
						}
						if(COLOR.getCurrentColor()==Color.WHITE){
							PROPULSION.stopMoving();
						}
					}
					PROPULSION.halfTurn(seekLeft);
					state = State.needToSeek;
					break;
					
					
				case needToGrab:					
					// Quand il est prêt de l'objet il teste ...
					// MAIS COMMENT ALLER PRET DU PALET ??? mlm
					//PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME,true);	
					PROPULSION.runFor(7000,true);	
					state = State.isGrabing;
					break;
				
				case isGrabing:
					float dist2 = VISION.getRaw()[0];
					// Fonctionne pas trop ~ 
					/*if(VISION.getRaw()[0] >= dist2){
						PROPULSION.stopMoving();
						SCREEN.drawText("on y est presque", " OK ");
						PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME,true);
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
							if(PRESSION.isPressed())
								System.out.println("laaa");
						}
						state = State.isCatching;
					}*/
					if(PRESSION.isPressed()){
						PROPULSION.stopMoving();
						GRABER.close();
						while (GRABER.isRunning()){
							GRABER.checkState();
						}
						state = State.isCatching;			
					}
					
					if(!PROPULSION.isRunning() && state != State.isCatching){
						SCREEN.drawText("Il n'a rien trouve", "il faut continuer de coder mlm!");
					}
					

					break;

					
				case isCatching:
					PROPULSION.orientateNorth();
					while(PROPULSION.isRunning()){
						PROPULSION.checkState();
					}
					SCREEN.drawText("FIN", "FINITOOOO");
					run=false;
					break;
				}
				
				/*
				 * TODO : Case!
				 * - isSeekingEnd -> Si plus d'objet sur le terrain, STOP!
				 * - needToGrab -> Aller pret d'un palet (impossible a tester à la maison) mlm
				 * - isGrabing -> Comment savoir s'il est prêt du palet ?
				 * 		pour pouvoir ajuster l'orientation vers le palet
				 * 		et ne pas continuer pendant le MAXTIMEGRAB'.
				 * 		+ Si OK ^ : 3 tentatives ;) (FAIT PLUS BAS)
				 * - isCatching -> OrienterNord 
				 * - GoBackHome -> run jusqu'à la ligne blanche
				 *      + open graber + backward
				 *      + volte face
				 *      -> needToSeek
				 *      
				 * TODO : GENERAL 
				 * - Aller chercher un palet, en ayant la distance 
				 * 		ou en f° de la vue si c'est possible.
				 * 		(savoir se deplacer sur une distance???)
				 * - Faire la différence entre un robot et un objet
				 * 
				 * note:isGrabing fonctionne que si le robot touche 
				 * le palet du premier coup...
				 * 
				 */
				
				if (INPUT.escapePressed())
					return;

			}catch (Throwable t) {
				t.printStackTrace();
				run = false;
			}
		}
	}
	

	
	private void mainLoop(boolean seekLeft) {
		State state = State.firstMove;
		boolean run = true;
		boolean unique = true;
		boolean unique2 = true;
		float searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
		boolean isAtWhiteLine = false;
		int nbSeek = R2D2Constants.INIT_NB_SEEK;

		while (run) {
			/*
			 * - Quand on part chercher un palet, on mesure le temps de trajet -
			 * Quand on fait le demi tour on parcours ce même temps de trajet -
			 * Si on croise une ligne noire vers la fin du temps de trajet
			 * S'orienter au nord vérifier pendant l'orientation la présence
			 * d'une ligne blanche si on voit une ligne blanche alors le
			 * prochain état sera arrivé à la maison sinon le prochain état sera
			 * aller à la maison.
			 */
			try {

				PROPULSION.checkState();
				GRABER.checkState();

				switch (state) {
				/*
				 * Routine de démarrage du robot : Attraper un palet Emmener le
				 * palet dans le but adverse les roues à cheval sur la ligne
				 * noire. Et passer dans l'état
				 * needToResetInitialSeekOrientation
				 */
				case firstMove:
					//whoAmI();
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
					
					// Bras se ferment et en meme temps, Se decale et avance pour eviter les autres palets
					PROPULSION.rotate(20, !seekLeft, true); 
					while(GRABER.isRunning() || PROPULSION.isRunning()){
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
					//PROPULSION.orientateNorth();
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
					PROPULSION.runFor(R2D2Constants.QUARTER_SECOND, false);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}
					
					// Demi-tour
					PROPULSION.halfTurn(!seekLeft);
					while (PROPULSION.isRunning()) {
						PROPULSION.checkState();
						if (INPUT.escapePressed())
							return;
					}
					
					
					
					/*
					 * propulsion.orientateSouth(seekLeft);
					 * while(propulsion.isRunning()){ propulsion.checkState();
					 * if(input.escapePressed()) return; } state =
					 * State.needToGrab;
					 */
					state = State.needToGrab;
					break;
				/*
				 * Le besoin de chercher un objet nécessite d'avoir le robot
				 * orienté face à l'ouest du terrain. Le nord étant face au camp
				 * adverse Le robot va lancer une rotation de 180° en cherchant
				 * si un pic de distances inférieure à 70cm apparait. Dans ce
				 * cas, il fera une recherche du centre de l'objet et ira
				 * l'attraper
				 *
				 * TODO faire en sorte que le robot n'avance pas pour une durée
				 * indeterminée, mais qu'il avance sur un temps de référence
				 * pour 70 cm de trajet au maximum. Comme ça, si l'objet a été
				 * attrapé pendant ce temps ou à disparu, alors il ne roulera
				 * pas dans le vide pour rien
				 */
				case needToSeek: 
					
					//PROPULSION.orientateWest();
					state = State.isSeeking;			
					searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE; 
					PROPULSION.volteFace(seekLeft, R2D2Constants.SEARCH_SPEED);
					isAtWhiteLine = false;

					break;
				case isSeeking:
					// Prend la distance du palet le plus proche
					float newDist = VISION.getRaw()[0];
					// Si la nouvelle distance est inférieure au rayonMaximum et
					// et supérieure au rayon minimum alors
					// on a trouvé un objet à ramasser.
					if (newDist < R2D2Constants.MAX_VISION_RANGE && newDist >= R2D2Constants.MIN_VISION_RANGE) {

						if (searchPik == R2D2Constants.INIT_SEARCH_PIK_VALUE) {
							if (unique2) {
								unique2 = false;
							} else {
								PROPULSION.stopMoving();
								// TODO, ces 90° peuvent poser problème.
								// Genre, dans le cas où le dernier palet de la
								// recherche
								// a déclenché la recherche du searchPik,
								// du coup on risque de voir le mur.
								// Il serait plus intéressant de faire un rotate
								// west ou east en fonction.
								// Mais bon, on a jamais eu le bug alors ...
								PROPULSION.rotate(R2D2Constants.QUART_CIRCLE, seekLeft,
										R2D2Constants.SLOW_SEARCH_SPEED);
								searchPik = newDist;
							}
						} else {
							if (newDist <= searchPik) {
								searchPik = newDist;
							} else {

								PROPULSION.stopMoving();
								unique2 = true;
								state = State.needToGrab; // trouvé palet 
							}
						}
					} else { // 
						searchPik = R2D2Constants.INIT_SEARCH_PIK_VALUE;
					}
					if (!PROPULSION.isRunning() && state != State.needToGrab) {
						nbSeek += R2D2Constants.STEPS_PER_STAGE;
						if (nbSeek > 10) {
							run = false;
						}
						state = State.needToOrientateNorthToRelease;
						seekLeft = System.currentTimeMillis() % 2 == 0;
					}
					break;
				/*
				 * Le besoin d'attraper un objet correspond au besoin de rouler
				 * sur l'objet pour l'attraper dans les pinces.
				 */
				case needToGrab:
					PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, true);
					state = State.isGrabing;
					seekLeft = !seekLeft;
					break;
				/*
				 * Le robot est dans l'état isGrabing tant qu'il roule pour
				 * attraper l'objet. Première tentative!
				 * TODO Vérifier si le temps de roulage est dépassé
				 */
				case isGrabing:
					
					while(PROPULSION.isRunning()){
						PROPULSION.checkState();
						if(VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE || PRESSION.isPressed()){
							PROPULSION.stopMoving();
							state = State.isCatching;
							GRABER.close();
						}
					}
					
					if(PRESSION.isPressed() || VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE) {
						state = State.isCatching;
					}else{
						PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, false);
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
							if (INPUT.escapePressed())
								return;
						}

						PROPULSION.rotate(15, seekLeft, true);
						while (PROPULSION.isRunning()) {
							PROPULSION.checkState();
							if (INPUT.escapePressed())
								return;
						}							
						
						PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, true);	
						while(PROPULSION.isRunning()){
							PROPULSION.checkState();
							if(VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE || PRESSION.isPressed()){
								PROPULSION.stopMoving();
								state = State.isCatching;
								GRABER.close();
							}
						}
						
						if(PRESSION.isPressed() || VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE) {
							state = State.isCatching;
						}else{
							PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, false);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}
							
							PROPULSION.rotate(30, !seekLeft, true);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}				
					
							PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, true);
							while(PROPULSION.isRunning()){
								PROPULSION.checkState();
								if(VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE || PRESSION.isPressed()){
									PROPULSION.stopMoving();
									state = State.isCatching;
									GRABER.close();
								}
							}
							
							if(PRESSION.isPressed() || VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE) {
								state = State.isCatching;
							}else{
								PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, false); 
								while (PROPULSION.isRunning()) {
									PROPULSION.checkState();
									if (INPUT.escapePressed())
										return;
								}
								
								PROPULSION.rotate(15, seekLeft, true); 
								while (PROPULSION.isRunning()) {
									PROPULSION.checkState();
									if (INPUT.escapePressed())
										return;
								}
								
								PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, false);
								while (PROPULSION.isRunning()) {
									PROPULSION.checkState();
									if (INPUT.escapePressed())
										return;
								}
								state = State.needToGrab;
							}
						}
					}
					break;
					
					/*// Première tentative
					if (VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE || !PROPULSION.isRunning()){
							if(PRESSION.isPressed()) { 
							PROPULSION.stopMoving();
							state = State.isCatching;
							GRABER.close();
						}else{ // Première tentative echouee
							PROPULSION.stopMoving();

							PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, false);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}

							PROPULSION.rotate(15, seekLeft, true);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}							
							
							PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, true);						
							state = State.isGrabing2ndAttempt;														
						}
					 }else{// Collision première tentative 
						state = State.needToSeek; // Chercher un autre si jamais problème avec ce palet
						PROPULSION.runFor(R2D2Constants.HALF_SECOND, false); // Reculer pour eviter la collision	
					}
					break;*/
						
					
					/*// Deuxième tentative
					if(VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE){ 
						if (PRESSION.isPressed()){
							PROPULSION.stopMoving();
							state = State.isCatching;
							GRABER.close();
							
						}else{ // Deuxième tentative échouée		
							PROPULSION.stopMoving(); 
							PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, false);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}
							
							PROPULSION.rotate(30, !seekLeft, true);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}				
					
							PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, true); 					
							state = State.isGrabing3rdAttempt;
							
						}
					}else{ // Collision deuxième tentative
						state = State.needToSeek; 
						PROPULSION.runFor(R2D2Constants.HALF_SECOND, false); 
					}					
					break;*/
					
					/*// Troisième / Dernière tentative
					if(VISION.getRaw()[0] < R2D2Constants.COLLISION_DISTANCE){ 
						if (PRESSION.isPressed()){
							PROPULSION.stopMoving();
							state = State.isCatching;
							GRABER.close();
							
						}else{ // Dernière tentative echouee
							PROPULSION.stopMoving(); // GO TO ANOTHER NEW CASE ? NEW STATE 2 ?
							PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, false); 
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}
							
							PROPULSION.rotate(15, seekLeft, true); // Correction ?? Angle de rotation ?
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}
							
							PROPULSION.runFor(R2D2Constants.MAX_GRABING_TIME, false);
							while (PROPULSION.isRunning()) {
								PROPULSION.checkState();
								if (INPUT.escapePressed())
									return;
							}
							
							state = State.needToSeek; // Rechercher un autre palet
						}
					}else{ // Collision troisième tentative
						state = State.needToSeek; // Chercher un autre si jamais problème avec ce palet
						PROPULSION.runFor(R2D2Constants.HALF_SECOND, false); // Reculer pour eviter la collision
					}				
					
					break;*/
				/*
				 * Is catching correspond à l'état où le robot est en train
				 * d'attraper l'objet. Cet état s'arrête quand les pinces
				 * arrêtent de tourner, temps fonction de la calibration
				 */
				case isCatching:
					if (!GRABER.isRunning()) {
						state = State.needToTurnBackToGoBackHome;
					}
					break;
				/*
				 * Ce état demande au robot de rentrer avec un palet. Dans un
				 * premier temps il effectue un demi tour pour repartir sur la
				 * trajectoire d'où il viens
				 */
				case needToTurnBackToGoBackHome:
					PROPULSION.volteFace(true, R2D2Constants.VOLTE_FACE_ROTATION);
					state = State.isTurningBackToGoBackHome;
					break;
					
				case isTurningBackToGoBackHome:
					if (!PROPULSION.isRunning()) {
						state = State.needToGoBackHome;
					}
					break;
				/*
				 * Dans un second temps, le robot va aller en ligne droite pour
				 * rentrer. Le temps de trajet aller a été mesuré. ? Nous
				 * utilisons cette mesure pour "prédire" à peux prêt quand
				 * est-ce que le robot va arriver à destination. Nous allumerons
				 * les capteurs de couleurs dans les environs pour détecter la
				 * présence d'une ligne blanche ou d'une ligne noire et agir en
				 * conséquence.
				 *
				 * Si une ligne noire est détectée, alors le robot va s'orienter
				 * face au nord et continuer sa route en direction du camp
				 * adverse.
				 *
				 * Celà permet d'assurer que le robot restera au centre du
				 * terrain.
				 *
				 * Si une ligne blanche est détectée, alors le robot sait qu'il
				 * est arrivé et l'état isRunningBackHome sera évacué
				 */
				case needToGoBackHome:
					PROPULSION.run(true);
					state = State.isRunningBackHome;
					break;
				case isRunningBackHome:
					if (!PROPULSION.isRunning()) {
						state = State.needToOrientateNorthToRelease;
					}
					if (PROPULSION.hasRunXPercentOfLastRun(R2D2Constants.ACTIVATE_SENSOR_AT_PERCENT)) {
						if (COLOR.getCurrentColor() == Color.WHITE) {
							PROPULSION.stopMoving();
							isAtWhiteLine = true;
							unique = true;
						}
						if (unique && COLOR.getCurrentColor() == Color.BLACK) {
							PROPULSION.stopMoving();
							unique = false;
							state = State.isAjustingBackHome;
						}
					}
					break;
				/*
				 * Cet état permet de remettre le robot dans la direction du
				 * nord avant de reprendre sa route
				 */
				case isAjustingBackHome:
					if (!PROPULSION.isRunning()) {
						PROPULSION.orientateNorth();
						state = State.isGoingToOrientateN;
					}
					break;
				/*
				 * Cet état correspond à l'orientation du robot face au camp
				 * adverse pour continuer sa route.
				 *
				 * Il y a cependant un cas particulier, dans le cas où quand le
				 * robot tourne, si il voit la couleur blanche, c'est qu'il est
				 * arrivé. Dans ce cas, terminer la rotation dans l'état
				 * isOrientatingNorthToRealease.
				 */
				case isGoingToOrientateN:
					if (COLOR.getCurrentColor() == Color.WHITE) {
						state = State.isOrientatingNorthToRealease;
					}
					if (!PROPULSION.isRunning()) {
						state = State.needToGoBackHome;
					}
					break;
				/*
				 * Correspond à l'état où le robot s'oriente au nord pour
				 * relâcher l'objet
				 */
				case needToOrientateNorthToRelease:
					state = State.isOrientatingNorthToRealease;
					PROPULSION.orientateNorth();
					break;
				case isOrientatingNorthToRealease:
					if (!PROPULSION.isRunning()) {
						if (GRABER.isClose()) {
							state = State.needToRelease;
						} else {
							state = State.needToResetInitialSeekOrientation;
						}
					}
					break;
				/*
				 * Ce état correspond, au moment où le robot a besoin de déposer
				 * le palet dans le cap adverse.
				 */
				case needToRelease:
					GRABER.open();
					state = State.isReleasing;
					break;
				case isReleasing:
					if (!GRABER.isRunning()) {
						state = State.needToResetInitialSeekOrientation;
					}
					break;
				/*
				 * Une fois l'objet rammassé, il faut se remettre en position de
				 * trouver un autre objet. Le robot fait une marcher arrière
				 * d'un certain temps. Puis fera une mise en face de l'ouest
				 */
				case needToResetInitialSeekOrientation:
					state = State.isResetingInitialSeekOrientation;
					if (isAtWhiteLine) {
						PROPULSION.runFor(R2D2Constants.HALF_SECOND * nbSeek, false);
					} else {
						PROPULSION.runFor(R2D2Constants.EMPTY_HANDED_STEP_FORWARD, false);
					}
					break;
				case isResetingInitialSeekOrientation:
					if (!PROPULSION.isRunning()) {
						if (seekLeft) {
							state = State.needToRotateWest;
						} else {
							state = State.needToRotateEast;
						}
						if (COLOR.getCurrentColor() == Color.WHITE)// fin de
																	// partie
							return;
					}
					break;
				/*
				 * Remet le robot face à l'ouest pour recommencer la recherche.
				 * Le robot doit avoir suffisamment reculé pour être dans une
				 * zone où il y aura des palets à ramasser.
				 */
				case needToRotateWest:
					PROPULSION.orientateWest();
					state = State.isRotatingToWest;
					break;
				case isRotatingToWest:
					if (!PROPULSION.isRunning()) {
						state = State.needToSeek;
					}
					break;
				/*
				 * Remet le robot face à l'est pour recommencer la recherche. Le
				 * robot doit avoir suffisamment reculé pour être dans une zone
				 * où il y aura des palets à ramasser.
				 */
				case needToRotateEast:
					PROPULSION.orientateEast();
					state = State.isRotatingToWest; // ? East
					break;
				case isRotatingToEast:
					if (!PROPULSION.isRunning()) {
						state = State.needToSeek;
					}
					break;
				// Évite la boucle infinie
				}
				if (INPUT.escapePressed())
					run = false;
			} catch (Throwable t) {
				t.printStackTrace();
				run = false;
			}
		}
	}

	/**
	 * S'occupe d'effectuer l'ensemble des calibrations nécessaires au bon
	 * fonctionnement du robot.
	 * 
	 * @return vrai si tout c'est bien passé.
	 */
	private boolean calibration() {

		return calibrationGrabber() && calibrationCouleur();
	}

	private boolean calibrationGrabber() {
		SCREEN.drawText("Calibration", "Calibration de la fermeture de la pince", "Appuyez sur le bouton central ",
				"pour continuer");
		if (INPUT.waitOkEscape(Button.ID_ENTER)) {
			SCREEN.drawText("Calibration", "Appuyez sur ok", "pour lancer et arrêter");
			INPUT.waitAny();
			GRABER.startCalibrate(false);
			INPUT.waitAny();
			GRABER.stopCalibrate(false);

			SCREEN.drawText("Calibration", "Appuyer sur Entree", "pour commencer la", "calibration de l'ouverture");
			INPUT.waitAny();

			SCREEN.drawText("Calibration", "Appuyer sur Entree", "Quand la pince est ouverte");
			GRABER.startCalibrate(true);
			INPUT.waitAny();
			GRABER.stopCalibrate(true);

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
		SCREEN.drawText("Calibration", "Préparez le robot à la ", "calibration des couleurs",
				"Appuyez sur le bouton central ", "pour continuer");
		if (INPUT.waitOkEscape(Button.ID_ENTER)) {
			COLOR.lightOn();

			// calibration gris
			SCREEN.drawText("Gris", "Placer le robot sur ", "la couleur grise");
			INPUT.waitAny();
			COLOR.calibrateColor(Color.GRAY);

			// calibration rouge
			SCREEN.drawText("Rouge", "Placer le robot ", "sur la couleur rouge");
			INPUT.waitAny();
			COLOR.calibrateColor(Color.RED);

			// calibration noir
			SCREEN.drawText("Noir", "Placer le robot ", "sur la couleur noir");
			INPUT.waitAny();
			COLOR.calibrateColor(Color.BLACK);

			// calibration jaune
			SCREEN.drawText("Jaune", "Placer le robot sur ", "la couleur jaune");
			INPUT.waitAny();
			COLOR.calibrateColor(Color.YELLOW);

			// calibration bleue
			SCREEN.drawText("BLeue", "Placer le robot sur ", "la couleur bleue");
			INPUT.waitAny();
			COLOR.calibrateColor(Color.BLUE);

			// calibration vert
			SCREEN.drawText("Vert", "Placer le robot ", "sur la couleur vert");
			INPUT.waitAny();
			COLOR.calibrateColor(Color.GREEN);

			// calibration blanc
			SCREEN.drawText("Blanc", "Placer le robot ", "sur la couleur blanc");
			INPUT.waitAny();
			COLOR.calibrateColor(Color.WHITE);

			COLOR.lightOff();
			return true;
		}
		return false;
	}

	// First time
	private void watchObjectFirstTime() {
		List<ObjectPosition> listCamera = camera.getObjectPosition();

		for (int i = 0; i < listCamera.size(); i++) {
			//if (listCamera.get(i).getY() > "pos ligne blanche" 
			//&& listCamera.get(i).getY() < "pos autre ligne blanche") {
			mapObject.put(i, listCamera.get(i));
		}
	}

	// Just for check if the object is not moving
	private void watchObject() {
		List<ObjectPosition> listCamera = camera.getObjectPosition();

		Iterator entries = mapObject.entrySet().iterator();
		int i = 0;
		while (entries.hasNext()) {

			ObjectPosition thisEntry = (ObjectPosition) entries.next();
			if (!thisEntry.equals(listCamera.get(i))) {
				entries.remove();
			}

			i++;
		}

	}

	private void whoAmI() {
		
		/*
		 * Prendre tous les objets compris a l'interieur du terrain
		 * (Sans prendre ceux proches de la ligne blanche). Donc pas la peine?
		 */
		
		List<ObjectPosition> listCamera = camera.getObjectPosition();
		PROPULSION.runFor(20, true); //*
		List<ObjectPosition> listCamera2 = camera.getObjectPosition();
		PROPULSION.runFor(20, false); //*
		
		for (int i = 0; i < listCamera2.size(); i++) {

			for (int j = 0; i < listCamera2.size(); i++) {

				if (listCamera2.get(i).amI(listCamera.get(j))) {
					me.setX(listCamera2.get(i).getX());
					me.setY(listCamera2.get(i).getY());
				}
			}

		}

	}
	
}