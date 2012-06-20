package fr.lip6.mg4j.extensions;

public class Pointer {
	/**
	 * Position of the pointer
	 */
	protected int position;
	
	public Pointer(int position) {
		this.position = position;
	}

	public int getPosition() {
		return this.position;
	}

}