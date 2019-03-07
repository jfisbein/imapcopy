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

    private List<ImapCopyListenerInterface> listeners = new ArrayList<>(0);

    private List<String> filteredFolders = new ArrayList<>();

    public static void main(String[] args) throws MessagingException {
        log.info("Starting");
        log.debug("Parameters length:" + args.length);
        if (args.length >= 2) {
            ImapCopier imapCopier = new ImapCopier();

            try {
                log.debug("opening connections");
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
     * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
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
     * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
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
     * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
     * @param host      Server host name
     * @param user      User
     * @param password  Password
     * @return Mail Store for the connection
     * @throws MessagingException Messaging Exception
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
     * @throws MessagingException Messaging Exception
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
     * @throws MessagingException Messaging Exception
     */
    public void copy() throws MessagingException {
        ImapCopyApplicationEvent evt = new ImapCopyApplicationEvent(ImapCopyApplicationEvent.START);
        for (ImapCopyListenerInterface listener : listeners) {
            listener.eventNotification(evt);
        }

        Folder defaultSourceFolder = sourceStore.getDefaultFolder();
        Folder defaultTargetFolder = targetStore.getDefaultFolder();
        copyFolderAndMessages(defaultSourceFolder, defaultTargetFolder, true);

        evt = new ImapCopyApplicationEvent(ImapCopyApplicationEvent.END);
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
     * @throws MessagingException Messaging Exception
     */
    private void copyFolderAndMessages(Folder sourceFolder, Folder targetFolder, boolean isDefaultFolder)
            throws MessagingException {

        if (sourceFolder.exists() && !filteredFolders.contains(sourceFolder.getFullName())) {
            if (!isDefaultFolder) {
                openFolderIfNeeded(sourceFolder, Folder.READ_ONLY);
                openFolderIfNeeded(targetFolder, Folder.READ_ONLY);

                Message[] notCopiedMessages = getNotCopiedMessages(sourceFolder, targetFolder);

                log.debug("Copying " + notCopiedMessages.length + " messages from " + sourceFolder.getFullName()
                        + " Folder");
                if (notCopiedMessages.length > 0) {
                    Message[][] messages = chunkArray(notCopiedMessages, 500);

                    for (Message[] messagesChunk : messages) {
                        openFolderIfNeeded(sourceFolder, Folder.READ_ONLY);
                        openFolderIfNeeded(targetFolder, Folder.READ_WRITE);
                        try {
                            log.info("Copying chunk of " + messagesChunk.length + " messages");
                            targetFolder.appendMessages(messagesChunk);
                        } catch (MessagingException e) {
                            log.error("Error copying messages from " + sourceFolder.getFullName() + " Folder", e);
                            log.info("Copying messages from chunk one by one");
                            copyMessagesOneByOne(targetFolder, messagesChunk);
                        }
                        closeFolderIfNeeded(targetFolder);
                    }
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

    private Message[][] chunkArray(Message[] array, int chunkSize) {
        int chunkedSize = (int) Math.ceil((double) array.length / chunkSize); // chunked array size
        Message[][] chunked = new Message[chunkedSize][chunkSize];
        for (int index = 0; index < chunkedSize; index++) {
            Message[] chunk = new Message[chunkSize]; // small array
            System.arraycopy(array, index * chunkSize, chunk, 0, Math.min(chunkSize, array.length - index * chunkSize));
            chunked[index] = chunk;
        }
        return chunked;
    }

    private Message[] getNotCopiedMessages(Folder sourceFolder, Folder targetFolder) throws MessagingException {
        log.info("Looking for non synced messages from folder " + sourceFolder.getFullName());
        List<Message> sourceMessages = Arrays.asList(sourceFolder.getMessages());
        log.debug("Got " + sourceMessages.size() + " messages from source folder");
        openFolderIfNeeded(targetFolder, Folder.READ_ONLY);
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
        if (!folder.isOpen() || folder.getMode() != mode) {
            closeFolderIfNeeded(folder);
            folder.open(mode);
        }
    }

    private void reconnectStoreIfNeeded(Store store) throws MessagingException {
        if (!store.isConnected()) {
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
     */
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

    @Override
    public void run() {
        try {
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