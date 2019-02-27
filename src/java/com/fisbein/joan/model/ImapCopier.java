package com.fisbein.joan.model;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.log4j.Logger;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class ImapCopier implements Runnable {
    private final static Logger log = Logger.getLogger(ImapCopier.class);

    private Store sourceStore = null;

    private Store targetStore = null;

    private List<ImapCopyListenerInterface> listeners = new ArrayList<ImapCopyListenerInterface>(0);

    private List<String> filteredFolders = new ArrayList<String>();

    public static void main(String[] args) throws MessagingException {
        log.info("Starting");
        log.debug("Parameters length:" + args.length);
        if (args.length >= 2) {
            ImapCopier imapCopier = new ImapCopier();

            try {
                log.debug("opening conections");
                imapCopier.openSourceConnection(args[0].trim());
                imapCopier.openTargetConnection(args[1].trim());
                if (args.length > 2) {
                    for (int i = 2; i < args.length; i++) {
                        imapCopier.addFilteredFolder(args[i]);
                    }
                }
                imapCopier.copy();
            } finally {
                imapCopier.close();
            }
        } else {
            String usage = "usage: imapCopy source target [excludedFolder]\n";
            usage += "source & target format: protocol://user[:password]@server[:port]\n";
            usage += "protocols: imap or imaps";
            System.out.println(usage);
        }
    }

    public void addFilteredFolder(String folderName) {
        log.debug("Adding '" + folderName + "' to filtered folders");
        filteredFolders.add(folderName);
    }

    /**
     * Open a connection to the source Store from where the messages will be copied when <code>copy</code> method will
     * be executed
     *
     * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
     * @param host      Server host name
     * @param user      User
     * @param password  Password
     * @throws MessagingException
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
     * @throws MessagingException
     */
    public void openSourceConnection(String url) throws MessagingException {
        sourceStore = openConnection(url);
    }

    /**
     * Open a connection to the target Store where the messages will be copied when <code>copy</code> method will be
     * executed
     *
     * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
     * @param host      Server host name
     * @param user      User
     * @param password  Password
     * @throws MessagingException
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
     * @throws MessagingException
     */
    public void openTargetConnection(String url) throws MessagingException {
        targetStore = openConnection(url);
    }

    /**
     * Opens a connection to a mail server
     *
     * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
     * @param host      Server host name
     * @param user      User
     * @param password  Password
     * @return Mail Store for the connection
     * @throws MessagingException
     */
    private Store openConnection(String storeType, String host, String user, String password) throws MessagingException {
        log.debug("opening " + storeType + " conection to " + host);
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
     * @throws MessagingException
     */
    private Store openConnection(String url) throws MessagingException {
        URLName urlName = new URLName(url);
        log.debug("opening " + urlName.getProtocol() + " conection to " + urlName.getHost() + " with " + urlName.getUsername());
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
     * @throws MessagingException
     */
    public void copy() throws MessagingException {
        ImapCopyAplicationEvent evt = new ImapCopyAplicationEvent(ImapCopyAplicationEvent.START);
        for (ImapCopyListenerInterface listener : listeners) {
            listener.eventNotification(evt);
        }

        Folder defaultSourceFolder = sourceStore.getDefaultFolder();
        Folder defaultTargetFolder = targetStore.getDefaultFolder();
        copyFolderAndMessages(defaultSourceFolder, defaultTargetFolder, true);

        evt = new ImapCopyAplicationEvent(ImapCopyAplicationEvent.END);
        for (ImapCopyListenerInterface listener : listeners) {
            listener.eventNotification(evt);
        }
    }

    /**
     * Copy recusively the soruce folder and his contents to the target folder
     *
     * @param sourceFolder    Source Folder
     * @param targetFolder    Target Folder
     * @param isDefaultFolder Flag if the folder are the defualt folder of thre store
     * @throws MessagingException
     */
    private void copyFolderAndMessages(Folder sourceFolder, Folder targetFolder, boolean isDefaultFolder)
            throws MessagingException {

        if (sourceFolder.exists() && !filteredFolders.contains(sourceFolder.getFullName())) {
            if (!isDefaultFolder) {
                openfolderIfNeeded(sourceFolder, Folder.READ_ONLY);
                openfolderIfNeeded(targetFolder, Folder.READ_ONLY);

                Message[] notCopiedMessages = getNotCopiedMessages(sourceFolder, targetFolder);

                log.debug("Copying " + notCopiedMessages.length + " messages from " + sourceFolder.getFullName()
                        + " Folder");
                if (notCopiedMessages.length > 0) {
                    openfolderIfNeeded(targetFolder, Folder.READ_WRITE);
                    try {
                        targetFolder.appendMessages(notCopiedMessages);
                    } catch (MessagingException e) {
                        log.error("Error copying messages from " + sourceFolder.getFullName() + " Folder", e);
                        copyMessagesOneByOne(targetFolder, notCopiedMessages);
                    }
                    closeFolderIfNeeded(targetFolder);
                }
                closeFolderIfNeeded(sourceFolder);
            }

            Folder[] sourceFolders = sourceFolder.list();
            logFoldersList("Source Folders", sourceFolders);
            for (Folder sourceSubFolder : sourceFolders) {
                Folder targetSubFolder = targetFolder.getFolder(sourceSubFolder.getName());
                if (!targetSubFolder.exists()) {
                    log.debug("Creating target Folder: " + targetSubFolder.getFullName());
                    targetSubFolder.create(sourceSubFolder.getType());
                }
                notifyToListeners(targetSubFolder);
                copyFolderAndMessages(sourceSubFolder, targetSubFolder, false);
            }
        } else {
            log.info("Skipping folder " + sourceFolder.getFullName());
        }
    }

    private Message[] getNotCopiedMessages(Folder sourceFolder, Folder targetFolder) throws MessagingException {
        log.info("Looking for non synced messages from folder " + sourceFolder.getFullName());
        List<Message> sourceMessages = Arrays.asList(sourceFolder.getMessages());
        List<Message> res = ListUtils.select(sourceMessages, new MessageFilterPredicate(targetFolder.getMessages()));

        return res.toArray(new Message[0]);
    }

    /**
     * Copy the list of messages one by one to the target Folder
     *
     * @param targetFolder Folder to store messages
     * @param messages     List of messages
     */
    private void copyMessagesOneByOne(Folder targetFolder, Message[] messages) {
        int counter = 0;
        for (Message message : messages) {
            counter++;
            Message[] aux = new Message[1];
            aux[0] = message;
            try {
                openfolderIfNeeded(targetFolder, Folder.READ_WRITE);
                targetFolder.appendMessages(aux);
            } catch (MessagingException e) {
                log.error("Error copying 1 message to " + targetFolder.getFullName() + " Folder", e);
            }
            if (counter % 100 == 0) {
                log.debug("Copied " + counter + " messages to " + targetFolder.getFullName());
            }
        }
    }

    private void openfolderIfNeeded(Folder folder, int mode) throws MessagingException {
        if (!folder.isOpen() || folder.getMode() != mode) {
            closeFolderIfNeeded(folder);
            folder.open(mode);
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

    private void notifyToListeners(Folder folder) {
        ImapCopyFolderEvent evt = new ImapCopyFolderEvent(folder.getFullName());
        for (ImapCopyListenerInterface listener : listeners) {
            listener.eventNotification(evt);
        }
    }

    public void addImapCopyListener(ImapCopyListenerInterface listener) {
        listeners.add(listener);
    }

    /**
     * Closes the open resources
     *
     * @throws MessagingException
     */
    public void close() throws MessagingException {
        if (sourceStore != null) {
            sourceStore.close();
        }

        if (targetStore != null) {
            targetStore.close();
        }
    }

    private void logFoldersList(String message, Folder[] folders) {
        String txt = message + ": ";
        for (Folder folder : folders) {
            txt += folder.getFullName() + "(" + getFolderMessageCount(folder) + "), ";
        }
        if (folders.length > 0) {
            log.debug(txt);
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

    public void run() {
        try {
            copy();
        } catch (MessagingException e) {
            e.printStackTrace();
        } finally {
            try {
                close();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    class MessageFilterPredicate implements Predicate<Message> {
        Set<String> messagesId = new HashSet<String>();

        public MessageFilterPredicate(Message[] filterMessages) throws MessagingException {
            for (Message message : filterMessages) {
                messagesId.add(message.getHeader("Message-ID")[0]);
            }
        }

        public boolean evaluate(Message message) {
            boolean res = true;
            try {
                res = messagesId.isEmpty() || !messagesId.contains(message.getHeader("Message-ID")[0]);
            } catch (MessagingException e) {
            }

            return res;
        }
    }
}