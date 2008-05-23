package com.fisbein.joan;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.apache.log4j.Logger;

public class ImapCopier {
	private final static Logger log = Logger.getLogger(ImapCopier.class);

	private Store sourceStore = null;

	private Store targetStore = null;

	public static void main(String[] args) throws MessagingException {
		if (args.length == 2) {
			ImapCopier imapCopier = new ImapCopier();

			try {
				imapCopier.openSourceConnection(args[0].trim());
				imapCopier.openTargetConnection(args[1].trim());
				imapCopier.copy();
			} finally {
				imapCopier.close();
			}
		} else {
			String usage = "usage: imapCopy source target\n";
			usage += "source & target format: protocol://user[:password]@server[:port]\n";
			usage += "protocol: imap or imaps";
			System.out.println(usage);
		}
	}

	/**
	 * Open a connection to the source Store from where the messages will be copied when
	 * <code>copy</code> method will be executed
	 * 
	 * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
	 * @param host Server host name
	 * @param user User
	 * @param password Password
	 * @throws MessagingException
	 */
	public void openSourceConnection(String storeType, String host, String user, String password)
			throws MessagingException {
		this.sourceStore = this.openConnection(storeType, host, user, password);
	}

	/**
	 * Open a connection to the source Store from where the messages will be copied when
	 * <code>copy</code> method will be executed
	 * 
	 * @param url URL in the format </code>protocol://user[:password]@server[:port]</code>
	 * @throws MessagingException
	 */
	public void openSourceConnection(String url) throws MessagingException {
		this.sourceStore = this.openConnection(url);
	}

	/**
	 * Open a connection to the target Store where the messages will be copied when
	 * <code>copy</code> method will be executed
	 * 
	 * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
	 * @param host Server host name
	 * @param user User
	 * @param password Password
	 * @throws MessagingException
	 */
	public void openTargetConnection(String storeType, String host, String user, String password)
			throws MessagingException {
		this.targetStore = this.openConnection(storeType, host, user, password);
	}

	/**
	 * Open a connection to the target Store where the messages will be copied when
	 * <code>copy</code> method will be executed
	 * 
	 * @param url URL in the format </code>protocol://user[:password]@server[:port]</code>
	 * @throws MessagingException
	 */
	public void openTargetConnection(String url) throws MessagingException {
		this.targetStore = this.openConnection(url);
	}

	/**
	 * Opens a connection to a mail server
	 * 
	 * @param storeType Type of Store (imap, aimaps, pop3, pop3s)
	 * @param host Server host name
	 * @param user User
	 * @param password Password
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
		log.debug("opening " + urlName.getProtocol() + " conection to " + urlName.getHost());
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props);
		Store store = session.getStore(urlName);
		store.connect();

		return store;
	}

	/**
	 * Copy the folders and messages defined with the methods <code>openSourceConnection</code>
	 * and <code>openTargetConnection</code>
	 * 
	 * @throws MessagingException
	 */
	public void copy() throws MessagingException {
		Folder defaultSourceFolder = this.sourceStore.getDefaultFolder();
		Folder defaultTargetFolder = this.targetStore.getDefaultFolder();
		copyFolderAndMessages(defaultSourceFolder, defaultTargetFolder, true);
	}

	/**
	 * Copy recusively the soruce folder and his contents to the target folder
	 * 
	 * @param sourceFolder Source Folder
	 * @param targetFolder Target Folder
	 * @param isDefaultFolder Flag if the folder are the defualt folder of thre store
	 * @throws MessagingException
	 */
	private void copyFolderAndMessages(Folder sourceFolder, Folder targetFolder, boolean isDefaultFolder)
			throws MessagingException {
		if (!isDefaultFolder) {
			sourceFolder.open(Folder.READ_ONLY);
			Message[] sourceMessages = sourceFolder.getMessages();
			log.debug("Copying " + sourceMessages.length + " messages from " + sourceFolder.getFullName() + " Folder");
			if (sourceMessages.length > 0) {
				targetFolder.open(Folder.READ_WRITE);
				try {
					targetFolder.appendMessages(sourceMessages);
				} catch (MessagingException e) {
					log.error("Error copying messages from " + sourceFolder.getFullName() + " Folder", e);
					copyMessagesOneByOne(targetFolder, sourceMessages);
				}
			}
		}

		Folder[] sourceFolders = sourceFolder.list();
		for (Folder sourceSubFolder : sourceFolders) {
			Folder targetSubFolder = targetFolder.getFolder(sourceSubFolder.getName());
			if (!targetSubFolder.exists()) {
				log.debug("Creating target Folder: " + targetSubFolder.getFullName());
				targetSubFolder.create(sourceSubFolder.getType());
			}
			copyFolderAndMessages(sourceSubFolder, targetSubFolder, false);
		}
	}

	/**
	 * Copy the list of messages one by one to the target Folder
	 * 
	 * @param targetFolder Folder to store messages
	 * @param messages List of messages
	 */
	private void copyMessagesOneByOne(Folder targetFolder, Message[] messages) {
		for (Message message : messages) {
			Message[] aux = new Message[1];
			aux[0] = message;
			try {
				targetFolder.appendMessages(aux);
			} catch (MessagingException e) {
				log.error("Error copying 1 message to " + targetFolder.getFullName() + " Folder", e);
			}
		}
	}

	/**
	 * Closes the open resources
	 * 
	 * @throws MessagingException
	 */
	public void close() throws MessagingException {
		if (this.sourceStore != null)
			this.sourceStore.close();

		if (this.targetStore != null)
			this.targetStore.close();
	}
}