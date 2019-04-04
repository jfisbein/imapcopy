package com.fisbein.joan.model;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.IMAPStore;
import org.junit.jupiter.api.*;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ImapCopierIT {

    private GreenMail greenMailSource;
    private GreenMail greenMailTarget;

    private final static String SOURCE_USER = "source@gmail.com";
    private final static String SOURCE_PASS = "sourcesourcesource";

    private final static String TARGET_USER = "target@gmail.com";
    private final static String TARGET_PASS = "targettargettarget";
    private GreenMailUser sourceUser;

    @BeforeEach
    void setUp() {
        greenMailSource = new GreenMail(ServerSetupTest.ALL);
        greenMailSource.start();
        sourceUser = greenMailSource.setUser(SOURCE_USER, SOURCE_PASS);

        greenMailTarget = new GreenMail(ServerSetupTestOffset.ALL);
        greenMailTarget.start();
        greenMailTarget.setUser(TARGET_USER, TARGET_PASS);
    }

    @Test
    void main() throws MessagingException {
        MimeMessage inboxMessage = createMimeMessage(GreenMailUtil.random(), GreenMailUtil.random(), SOURCE_USER, greenMailSource); // Construct message
        MimeMessage testMessage1 = createMimeMessage(GreenMailUtil.random(), GreenMailUtil.random(), SOURCE_USER, greenMailSource); // Construct message
        MimeMessage testMessage2 = createMimeMessage(GreenMailUtil.random(), GreenMailUtil.random(), SOURCE_USER, greenMailSource); // Construct message
        sourceUser.deliver(inboxMessage);
        IMAPStore store = greenMailSource.getImap().createStore();
        store.connect("localhost", greenMailSource.getImap().getPort(), SOURCE_USER, SOURCE_PASS);
        Folder testFolder = store.getDefaultFolder().getFolder("test");
        testFolder.create(Folder.HOLDS_MESSAGES);
        Message[] sourceTestMessages = {testMessage1, testMessage2};
        testFolder.appendMessages(sourceTestMessages);

        String[] args = new String[8];
        args[0] = "--source";
        args[1] = generateImapUrl(SOURCE_USER, SOURCE_PASS, "localhost", greenMailSource.getImap().getPort());
        args[2] = "--target";
        args[3] = generateImapUrl(TARGET_USER, TARGET_PASS, "localhost", greenMailTarget.getImap().getPort());
        args[4] = "--fromDate";
        args[5] = LocalDate.now().toString();
        args[6] = "--toDate";
        args[7] = LocalDate.now().toString();

        ImapCopier.main(args);

        IMAPStore targetStore = greenMailTarget.getImap().createStore();
        targetStore.connect("localhost", greenMailTarget.getImap().getPort(), TARGET_USER, TARGET_PASS);

        Folder targetInboxFolder = targetStore.getDefaultFolder().getFolder("INBOX");
        targetInboxFolder.open(Folder.READ_ONLY);
        assertEquals(1, targetInboxFolder.getMessageCount());
        Message[] targetInboxMessages = targetInboxFolder.getMessages();
        assertEquals(targetInboxMessages[0].getHeader("Message-ID")[0], inboxMessage.getHeader("Message-ID")[0]);

        Folder targetTestFolder = targetStore.getDefaultFolder().getFolder("test");
        targetTestFolder.open(Folder.READ_ONLY);
        Message[] targetTestMessages = targetTestFolder.getMessages();
        assertMessagesArrayEqualsById(sourceTestMessages, targetTestMessages);
    }

    private void assertMessagesArrayEqualsById(Message[] expected, Message[] actual) {
        assertEquals(expected.length, actual.length, "Message arrays not same size");
        Set<String> expectedMsgIds = Arrays.stream(expected).map(msg -> {
            String msgId = null;
            try {
                msgId = msg.getHeader("Message-ID")[0];
            } catch (MessagingException ignored) {
            }
            return msgId;
        }).collect(Collectors.toSet());

        Set<String> actualMsgIds = Arrays.stream(actual).map(msg -> {
            String msgId = null;
            try {
                msgId = msg.getHeader("Message-ID")[0];
            } catch (MessagingException ignored) {
            }
            return msgId;
        }).collect(Collectors.toSet());

        assertEquals(expectedMsgIds, actualMsgIds);
    }

    private MimeMessage createMimeMessage(String subject, String body, String to, GreenMail greenMail) throws MessagingException {
        MimeMessage msg = new MimeMessage(greenMail.getSmtp().createSession());
        msg.setFrom(new InternetAddress("foo@example.com"));
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject);
        msg.setText(body);

        return msg;
    }

    private String generateImapUrl(String user, String pass, String host, int port) {
        return "imap://" + user.replace("@", "%40") + ":" + pass + "@" + host + ":" + port;
    }
}