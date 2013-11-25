package com.fisbein.joan.model;

public class ImapCopyFolderEvent extends ImapCopyEvent {
	private static final long serialVersionUID = 7149783270907588909L;

	private final String folderName;

	public ImapCopyFolderEvent(String folderName) {
		super();
		this.folderName = folderName;
	}

	public String getFolderName() {
		return folderName;
	}
}
