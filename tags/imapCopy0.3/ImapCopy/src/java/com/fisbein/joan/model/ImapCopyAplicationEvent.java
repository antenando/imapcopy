package com.fisbein.joan.model;

public class ImapCopyAplicationEvent extends ImapCopyEvent {
	public final static int START = 1;

	public final static int END = 2;

	private int type;

	public ImapCopyAplicationEvent(int type) {
		super();
		this.type = type;
	}

	public int getType() {
		return type;
	}
}