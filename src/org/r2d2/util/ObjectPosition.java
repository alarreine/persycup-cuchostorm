package org.r2d2.util;

public class ObjectPosition {

	private int x;
	private int y;

	private Boolean isRobot;
	private Boolean amI;

	public ObjectPosition(int x, int y) {

		this.x = x;
		this.y = y;

	}

	public ObjectPosition() {

		this.x = 0;
		this.y = 0;

	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Boolean IsRobot() {
		return isRobot;
	}

	public void isRobot(Boolean isRobot) {
		this.isRobot = isRobot;
	}

	public Boolean amI() {
		return amI;
	}

	public void amI(Boolean amI) {
		this.amI = amI;
	}

	public boolean amI(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectPosition other = (ObjectPosition) obj;

		if ((y + 10) > other.y && y > (other.y + 10)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((amI == null) ? 0 : amI.hashCode());
		result = prime * result + ((isRobot == null) ? 0 : isRobot.hashCode());
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectPosition other = (ObjectPosition) obj;

		if ((x + 5) > other.x && x > (other.x + 5) && (y + 5) > other.y && y > (other.y + 5)) {
			return true;
		} else {
			return false;
		}

	}

}
