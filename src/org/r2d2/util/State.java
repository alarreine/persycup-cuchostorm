package org.r2d2.util;

public enum State {
	firstMove, step2, step22, playStart, isCatching, needToReleaseBall, 
	isReleasing, needToSeek, isSeeking, isSeekingEnd, isSeekingLost, 
	needToGrab, isGrabing, isGrabing2ndAttempt, isGrabing3rdAttempt, needToRotateEast, 
	isRotatingToEast, needToRotateWest, isRotatingToWest, needToScoreGoal, isRunningBackHome, 
	needToResetInitialSeekOrientation, isResetingInitialSeekOrientation, needToTurnBackToGoBackHome, 
	isTurningBackToGoBackHome, needToOrientateNorthToRelease, isOrientatingNorthToRealease, 
	isAjustingBackHome, isGoingToOrientateN, wallInFront, wallInFrontWithPallet
}
