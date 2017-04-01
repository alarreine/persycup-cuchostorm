// package org.r2d2.controller;
//
// import lejos.hardware.Button;
// import lejos.robotics.Color;
//
// public class SystemTest {
//
// public static void colorTest() {
// Controler.COLOR.lightOn();
// boolean run = true;
// String couleur = "";
// while(run){
// if(couleur.equals("")){
// Controler.SCREEN.drawText("Test du calibrage",
// "Appuyez sur le bouton central ","pour tester une couleur");
// }else{
// Controler.SCREEN.drawText("Test de la calibration",
// "Couleur trouvée : "+couleur,
// "Appuyez sur le bouton central ","pour tester une couleur");
// }
// if(Controler.INPUT.waitOkEscape(Button.ID_ENTER)){
// switch(Controler.COLOR.getCurrentColor()){
//
// case Color.GREEN:
// couleur = "GREEN";
// break;
//
// case Color.BLUE:
// couleur = "BLUE";
// break;
//
// case Color.RED:
// couleur = "RED";
// break;
//
// case Color.BLACK:
// couleur = "BLACK";
// break;
//
// case Color.WHITE:
// couleur = "WHITE";
// break;
//
// default:
// couleur = "inconnue";
// }
// }else{
// run = false;
// }
// }
// }
//
// public static void grabberTest(Controler c) {
//
// for(int i=0; i<2; i++){
// Controler.SCREEN.drawText("TEST",
// "Presser le capteur de pression",
// "Avec un palet",
// "pour continuer");
// if(!Controler.PRESSION.activePressWait())
// return;
// Controler.GRABER.close();
// while(Controler.GRABER.isRunning()){
// Controler.GRABER.checkState();
// //Sécurité d'échappement
// if(Button.ESCAPE.isDown())
// return;
// }
// Controler.GRABER.open();
// while(Controler.GRABER.isRunning()){
// Controler.GRABER.checkState();
// //Sécurité d'échappement
// if(Button.ESCAPE.isDown())
// return;
// }
//
// }
// }
//
// public static void sensorTest(Controler c) {
// Controler.SCREEN.drawText("TEST",
// "Presser le capteur de pression");
// Controler.SCREEN.clearDraw();
// boolean lastState = false;
// while(true){
// boolean state = Controler.PRESSION.isPressed();
// if(state != lastState){
// System.out.println("Pression "+state);
// System.out.println("Pression "+ Controler.PRESSION.raw()[0]);
// lastState = state;
// }
// if(Controler.INPUT.enterPressed())
// break;
// }
// Controler.SCREEN.clearPrintln();
// }
//
// public static void motorTest(Controler c) {
// Controler.SCREEN.drawText("Test", "test du moteur", "appuyez sur entrée");
// Controler.INPUT.waitAny();
// Controler.PROPULSION.runFor(1000, true);
// while(Controler.PROPULSION.isRunning()){
// Controler.PROPULSION.checkState();
// //Sécurité d'échappement
// if(Button.ESCAPE.isDown())
// return;
// }
//
// Controler.PROPULSION.volteFace(true);
// while(Controler.PROPULSION.isRunning()){
// //Sécurité d'échappement
// if(Button.ESCAPE.isDown())
// return;
// }
//
// Controler.PROPULSION.runFor(1000, true);
// while(Controler.PROPULSION.isRunning()){
// Controler.PROPULSION.checkState();
// //Sécurité d'échappement
// if(Button.ESCAPE.isDown())
// return;
// }
//
// Controler.PROPULSION.volteFace(false);
// while(Controler.PROPULSION.isRunning()){
// Controler.PROPULSION.checkState();
// //Sécurité d'échappement
// if(Button.ESCAPE.isDown())
// return;
// }
//
// }
//
// }
