package com.fisbein.joan.model;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.SentDateTerm;
import java.io.Closeable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static java.time.temporal.ChronoUnit.DAYS;

@Command(usageHelpWidth = 120)
public class ImapCopier implements Runnable, Closeable {
    private static final Logger log = Logger.getLogger(ImapCopier.class);

    private Store sourceStore = null;

    private Store targetStore = null;

    @Option(names = {"-s", "--source"}, description = "IMAP source url", required = true)
    private String imapSource;

    @Option(names = {"-t", "--target"}, description = "IMAP target url", required = true)
    private String imapTarget;

    @Option(names = "--fromDate", description = "Minimum message sent date to synchronize from", required = true)
    private LocalDate fromDate;

    @Option(names = "--toDate", description = "Maximum message sent date to synchronize to")
    private LocalDate toDate;

    @Option(names = {"-e", "--excluded"}, arity = "*", description = "Folders to exclude.")
    private List<String> filteredFolders = new ArrayList<>();

    public static void main(String[] args) {
        log.info("Starting");
        CommandLine.run(new ImapCopier(), args);
        log.info("Done :-)");
    }

    private void init() throws MessagingException {
        openSourceConnection(imapSource);
        openTargetConnection(imapTarget);
        if (toDate != null) {
            toDate = toDate.plus(1, DAYS);
        } else {
            toDate = LocalDate.now().plus(1, DAYS);
        }
    }

    /**
     * Add folder to filtered list. Folder is this list will be skipped from synchronization.
     *
     * @param folderName Folder name
     */
    public void addFilteredFolder(String folderName) {
        log.debug("Adding '" + folderName + "' to filtered folders");
        filteredFolders.add(folderName);
    }

    /**
     * Open a connection to the source Store from where the messages will be copied when <code>copy</code> method will
     * be executed
     *
     * @param storeType Type of Store (imap, imaps, pop3, pop3s)
     * @param host      Server host name
     * @param user      User
     * @param password  Password
     * @throws MessagingException Messaging Exception
     */
    public void openSourceConnection(String storeType, String host, String user, String password)
            throws MessagingException {
        sourceStore = openConnection(storeType, host, user, password);
    }

    /**
     * Open a connection to the source Store from where the messages will be copied when <code>copy</code> method will
     * be executed
     *
     * @param url URL in the format </code>protocol://user[:password]@server[:port]</code>
     * @throws MessagingException Messaging Exception
     */
    public void openSourceConnection(String url) throws MessagingException {
        sourceStore = openConnection(url);
    }

    /**
     * Open a connection to the target Store where the messages will be copied when <code>copy</code> method will be
     * executed
     *
     * @param storeType Type of Store (imap, imaps, pop3, pop3s)
     * @param host      Server host name
     * @param user      User
     * @param password  Password
     * @throws MessagingException Messaging Exception
     */
    public void openTargetConnection(String storeType, String host, String user, String password)
            throws MessagingException {
        targetStore = openConnection(storeType, host, user, password);
    }

    /**
     * Open a connection to the target Store where the messages will be copied when <code>copy</code> method will be
     * executed
     *
     * @param url URL in the format </code>protocol://user[:password]@server[:port]</code>
     * @throws MessagingException Messaging Exception
     */
    public void openTargetConnection(String url) throws MessagingException {
        targetStore = openConnection(url);
    }

    /**
     * Opens a connection to a mail server
     *
     * @param storeType Type of Store (imap, imaps, pop3, pop3s)
     * @param host      Server host name
     * @param user      User
     * @param password  Password
     * @return Mail Store for the connection
     * @throws MessagingException Messaging Exception
     */
    private Store openConnection(String storeType, String host, String user, String password) throws MessagingException {
        log.debug("opening " + storeType + " connection to " + host);
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props);
        Store store = session.getStore(storeType);
        store.connect(host, user, password);
        log.debug(store.getURLName().toString());

        return store;
    }

    /**
     * Opens a connection to a mail server
     *
     * @param url URL in the format </code>protocol://user[:password]@server[:port]</code>
     * @return Mail Store for the connection
     * @throws MessagingException Messaging Exception
     */
    private Store openConnection(String url) throws MessagingException {
        URLName urlName = new URLName(url);
        log.debug("opening " + urlName.getProtocol() + " connection to " + urlName.getHost() + " with " + urlName.getUsername());
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props);
        Store store = session.getStore(urlName);
        store.connect();

        return store;
    }

    /**
     * Copy the folders and messages defined with the methods <code>openSourceConnection</code> and
     * <code>openTargetConnection</code>
     *
     * @throws MessagingException Messaging Exception
     */
    public void copy() throws MessagingException {
        Folder defaultSourceFolder = sourceStore.getDefaultFolder();
        Folder defaultTargetFolder = targetStore.getDefaultFolder();
        copyFolderAndMessages(defaultSourceFolder, defaultTargetFolder, fromDate, toDate);
    }

    /**
     * Copy recursively the source folder and his contents to the target folder
     *
     * @param sourceFolder Source Folder
     * @param targetFolder Target Folder
     * @throws MessagingException Messaging Exception
     */
    private void copyFolderAndMessages(Folder sourceFolder, Folder targetFolder, LocalDate fromDate, LocalDate toDate)
            throws MessagingException {

        if (sourceFolder.exists() && !filteredFolders.contains(sourceFolder.getFullName())) {
            // Copying messages
            if (isType(sourceFolder, Folder.HOLDS_MESSAGES)) {
                log.info("Synchronizing " + sourceFolder.getFullName() + " folder");
                openFolderIfNeeded(sourceFolder, Folder.READ_ONLY);
                openFolderIfNeeded(targetFolder, Folder.READ_WRITE);

                if (sourceFolder.getMessageCount() != targetFolder.getMessageCount()) {
                    LocalDate messagesDate = fromDate;
                    while (messagesDate.isBefore(toDate)) {
                        log.info("Looking for messages for " + messagesDate);
                        Message[] notCopiedMessages = getNotCopiedMessages(sourceFolder, targetFolder, messagesDate);
                        log.debug("Copying " + notCopiedMessages.length + " messages from " + sourceFolder.getFullName() + " Folder for " + messagesDate);

                        copyMessages(sourceFolder, targetFolder, notCopiedMessages);
                        messagesDate = messagesDate.plusDays(1);
                    }

                    closeFolderIfNeeded(sourceFolder);
                } else {
                    log.info("Both folders have same amount of messages, skipping.");
                }
            }

            //Iterating subfolders
            Folder[] sourceFolders = sourceFolder.list();
            logFoldersList("Source Folders", sourceFolders);
            for (Folder sourceSubFolder : sourceFolders) {
                Folder targetSubFolder = targetFolder.getFolder(sourceSubFolder.getName());
                if (!targetSubFolder.exists()) {
                    log.debug("Creating target Folder: " + targetSubFolder.getFullName());
                    targetSubFolder.create(sourceSubFolder.getType());
                }

                copyFolderAndMessages(sourceSubFolder, targetSubFolder, fromDate, toDate);
            }
        } else {
            log.info("Skipping folder " + sourceFolder.getFullName());
        }
    }

    private void copyMessages(Folder sourceFolder, Folder targetFolder, Message[] notCopiedMessages) throws MessagingException {
        if (notCopiedMessages.length > 0) {
            openFolderIfNeeded(sourceFolder, Folder.READ_ONLY);
            openFolderIfNeeded(targetFolder, Folder.READ_WRITE);
            try {
                targetFolder.appendMessages(notCopiedMessages);
            } catch (MessagingException e) {
                log.warn("Error copying messages from " + sourceFolder.getFullName() + " Folder: " + e.getMessage());
                log.info("Copying messages from chunk one by one");
                copyMessagesOneByOne(targetFolder, sourceFolder, notCopiedMessages);
            }
            closeFolderIfNeeded(targetFolder);
        }
    }

    private Message[] getNotCopiedMessages(Folder sourceFolder, Folder targetFolder, LocalDate date) throws MessagingException {
        List<Message> res = Collections.emptyList();
        Date startDate = toDate(date);
        Date endDate = toDate(date.plus(1, DAYS));

        openFolderIfNeeded(sourceFolder, Folder.READ_ONLY);
        List<Message> sourceMessages = Arrays.asList(sourceFolder.search(new AndTerm(new SentDateTerm(ComparisonTerm.GE, startDate), new SentDateTerm(ComparisonTerm.LT, endDate))));

        if (!sourceMessages.isEmpty()) {

            openFolderIfNeeded(targetFolder, Folder.READ_ONLY);
            Message[] targetMessages = targetFolder.search(new AndTerm(new SentDateTerm(ComparisonTerm.GE, startDate), new SentDateTerm(ComparisonTerm.LT, endDate)));

            res = ListUtils.select(sourceMessages, new MessageFilterPredicate(targetMessages));
        }

        return res.toArray(new Message[0]);
    }

    private Date toDate(LocalDate localDate) {
        return java.sql.Date.valueOf(localDate);
    }

    /**
     * Copy the list of messages one by one to the target Folder
     *
     * @param targetFolder Folder to store messages
     * @param messages     List of messages
     */
    private void copyMessagesOneByOne(Folder targetFolder, Folder sourceFolder, Message[] messages) {
        int counter = 0;
        for (Message message : messages) {
            counter++;
            Message[] aux = new Message[1];
            aux[0] = message;
            try {
                openFolderIfNeeded(sourceFolder, Folder.READ_ONLY);
                openFolderIfNeeded(targetFolder, Folder.READ_WRITE);
                targetFolder.appendMessages(aux);
            } catch (MessagingException e) {
                log.error("Error copying 1 message to " + targetFolder.getFullName() + " Folder", e);
            }
            if (counter % 100 == 0) {
                log.debug("Copied " + counter + " messages to " + targetFolder.getFullName());
            }
        }
    }

    private void openFolderIfNeeded(Folder folder, int mode) throws MessagingException {
        reconnectStoreIfNeeded(folder.getStore());
        if (!folder.isOpen() || folder.getMode() != mode) {
            closeFolderIfNeeded(folder);
            folder.open(mode);
        }
    }

    private void reconnectStoreIfNeeded(Store store) throws MessagingException {
        if (!store.isConnected()) {
            log.debug("Reconnecting store - " + store);
            store.connect();
        }
    }

    private void closeFolderIfNeeded(Folder folder) {
        if (folder.isOpen()) {
            try {
                folder.close(false);
            } catch (MessagingException e) {
                log.warn("Problems closing folder", e);
            }
        }
    }

    /**
     * Closes the open resources
     */
    @Override
    public void close() {
        closeStore(sourceStore);
        closeStore(targetStore);
    }

    private void closeStore(Store store) {
        if (store != null) {
            try {
                store.close();
            } catch (MessagingException e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    private void logFoldersList(String message, Folder[] folders) {
        List<String> messageParts = new ArrayList<>();

        for (Folder folder : folders) {
            messageParts.add(folder.getFullName() + "(" + getFolderMessageCount(folder) + ")");
        }

        if (folders.length > 0) {
            log.debug(message + ": " + String.join(", ", messageParts));
        }
    }

    private int getFolderMessageCount(Folder folder) {
        int res = -1;
        try {
            res = folder.getMessageCount();
        } catch (MessagingException e) {
            log.warn("Problems getting folder size - " + folder.getFullName());
        }

        return res;
    }

    private boolean isType(Folder folder, int type) throws MessagingException {
        return ((folder.getType() & type) != 0);
    }

    @Override
    public void run() {
        try {
            init();
            copy();
        } catch (MessagingException e) {
            log.warn(e.getMessage(), e);
        } finally {
            close();
        }
    }

    class MessageFilterPredicate implements Predicate<Message> {
        Set<String> messagesId = new HashSet<>();

        MessageFilterPredicate(Message[] filterMessages) {
            for (Message message : filterMessages) {
                try {
                    messagesId.add(message.getHeader("Message-ID")[0]);
                } catch (MessagingException e) {
                    log.warn("Error getting Message-ID", e);
                }
            }
        }

        @Override
        public boolean evaluate(Message message) {
            boolean res = true;
            try {
                res = messagesId.isEmpty() || !messagesId.contains(message.getHeader("Message-ID")[0]);
            } catch (MessagingException ignored) {
            }

            return res;
        }
    }
}