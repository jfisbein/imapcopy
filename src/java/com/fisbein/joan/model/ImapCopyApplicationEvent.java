package com.fisbein.joan.model;

public class ImapCopyApplicationEvent extends ImapCopyEvent {
    private static final long serialVersionUID = 1816693715825292307L;

    public final static int START = 1;

    public final static int END = 2;

    private final int type;

    public ImapCopyApplicationEvent(int type) {
        super();
        this.type = type;
    }

    public int getType() {
        return type;
    }
}