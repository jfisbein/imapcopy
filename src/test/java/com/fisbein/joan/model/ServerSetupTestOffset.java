package com.fisbein.joan.model;

import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;

public class ServerSetupTestOffset extends ServerSetupTest {
    public static int portOffset = 4000;

    public static final ServerSetup SMTP = new ServerSetup(25 + portOffset, null, ServerSetup.PROTOCOL_SMTP);
    public static final ServerSetup SMTPS = new ServerSetup(465 + portOffset, null, ServerSetup.PROTOCOL_SMTPS);
    public static final ServerSetup POP3 = new ServerSetup(110 + portOffset, null, ServerSetup.PROTOCOL_POP3);
    public static final ServerSetup POP3S = new ServerSetup(995 + portOffset, null, ServerSetup.PROTOCOL_POP3S);
    public static final ServerSetup IMAP = new ServerSetup(143 + portOffset, null, ServerSetup.PROTOCOL_IMAP);
    public static final ServerSetup IMAPS = new ServerSetup(993 + portOffset, null, ServerSetup.PROTOCOL_IMAPS);

    public static final ServerSetup[] SMTP_POP3 = new ServerSetup[]{SMTP, POP3};
    public static final ServerSetup[] SMTP_IMAP = new ServerSetup[]{SMTP, IMAP};
    public static final ServerSetup[] SMTP_POP3_IMAP = new ServerSetup[]{SMTP, POP3, IMAP};

    public static final ServerSetup[] SMTPS_POP3S = new ServerSetup[]{SMTPS, POP3S};
    public static final ServerSetup[] SMTPS_POP3S_IMAPS = new ServerSetup[]{SMTPS, POP3S, IMAPS};
    public static final ServerSetup[] SMTPS_IMAPS = new ServerSetup[]{SMTPS, IMAPS};

    public static final ServerSetup[] ALL = new ServerSetup[]{SMTP, SMTPS, POP3, POP3S, IMAP, IMAPS};
}
