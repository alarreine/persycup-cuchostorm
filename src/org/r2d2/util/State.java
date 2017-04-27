package org.r2d2.util;

public enum State {
	firstMove, playStart, isCatching, needToReleaseBall, 
	needToSeek, isSeeking, isSeekingEnd, isSeekingLost, 
	needToGrab, isGrabing, 
	needToScoreGoal, wallInFront, wallInFrontWithPallet
}
