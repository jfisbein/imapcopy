package com.fisbein.joan.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sun.mail.imap.IMAPStore;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    MimeMessage inboxMessage = createMimeMessage(GreenMailUtil.random(), GreenMailUtil.random(), SOURCE_USER,
      greenMailSource); // Construct message
    MimeMessage testMessage1 = createMimeMessage(GreenMailUtil.random(), GreenMailUtil.random(), SOURCE_USER,
      greenMailSource); // Construct message
    MimeMessage testMessage2 = createMimeMessage(GreenMailUtil.random(), GreenMailUtil.random(), SOURCE_USER,
      greenMailSource); // Construct message
    MimeMessage excludedMessage = createMimeMessage(GreenMailUtil.random(), GreenMailUtil.random(), SOURCE_USER,
      greenMailSource); // Construct message

    sourceUser.deliver(inboxMessage);
    IMAPStore sourceStore = greenMailSource.getImap().createStore();
    sourceStore.connect("localhost", greenMailSource.getImap().getPort(), SOURCE_USER, SOURCE_PASS);

    Folder testFolder = sourceStore.getDefaultFolder().getFolder("test");
    testFolder.create(Folder.HOLDS_MESSAGES);
    Message[] sourceTestMessages = {testMessage1, testMessage2};
    testFolder.appendMessages(sourceTestMessages);

    Folder excludedFolder = sourceStore.getDefaultFolder().getFolder("excluded");
    excludedFolder.create(Folder.HOLDS_MESSAGES);
    excludedFolder.appendMessages(new Message[]{excludedMessage});

    String[] args = createArgs(generateImapUrl(SOURCE_USER, SOURCE_PASS, "localhost", greenMailSource.getImap().getPort()),
      generateImapUrl(TARGET_USER, TARGET_PASS, "localhost", greenMailTarget.getImap().getPort()), LocalDate.now(), LocalDate.now(),
      "excluded");

    ImapCopier.main(args);

    // Asserts
    IMAPStore targetStore = greenMailTarget.getImap().createStore();
    targetStore.connect("localhost", greenMailTarget.getImap().getPort(), TARGET_USER, TARGET_PASS);

    // Inbox messages
    Folder targetInboxFolder = targetStore.getDefaultFolder().getFolder("INBOX");
    targetInboxFolder.open(Folder.READ_ONLY);
    assertThat(targetInboxFolder.getMessageCount()).isOne();
    Message[] targetInboxMessages = targetInboxFolder.getMessages();
    assertThat(inboxMessage.getHeader("Message-ID")[0]).isEqualTo(targetInboxMessages[0].getHeader("Message-ID")[0]);

    // Test folder messages
    Folder targetTestFolder = targetStore.getDefaultFolder().getFolder("test");
    targetTestFolder.open(Folder.READ_ONLY);
    Message[] targetTestMessages = targetTestFolder.getMessages();
    assertMessagesArrayEqualsById(sourceTestMessages, targetTestMessages);

    // Excluded folder
    assertThat(targetStore.getDefaultFolder().getFolder("excluded").exists()).isFalse();
  }

  private void assertMessagesArrayEqualsById(Message[] expected, Message[] actual) {
    assertThat(actual).hasSameSizeAs(expected);
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

    assertThat(actualMsgIds).isEqualTo(expectedMsgIds);
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

  private String[] createArgs(String sourceUrl, String targetUrl, LocalDate fromDate, LocalDate toDate, String... excludedFolders) {
    List<String> args = new ArrayList<>();
    if (sourceUrl != null) {
      args.add("--source");
      args.add(sourceUrl);
    }

    if (targetUrl != null) {
      args.add("--target");
      args.add(targetUrl);
    }

    if (fromDate != null) {
      args.add("--fromDate");
      args.add(fromDate.toString());
    }

    if (toDate != null) {
      args.add("--toDate");
      args.add(toDate.toString());
    }

    if (excludedFolders != null) {
      args.add("--excluded");
      args.addAll(Arrays.asList(excludedFolders));
    }

    return args.toArray(new String[0]);
  }
}