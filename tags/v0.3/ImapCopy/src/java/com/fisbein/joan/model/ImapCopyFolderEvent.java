package com.fisbein.joan.model;

public class ImapCopyFolderEvent extends ImapCopyEvent {
	private String folderName;

	public String getFolderName() {
		return folderName;
	}

	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}

}
