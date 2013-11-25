imapcopy
========

Copy recursively all email messages and folders from 1 imap account to another

Installation Instructions
-------------------------

### Linux and other Unixes
* Download the application from the repository
* build de jar

        ant build

* go to target directory and get imapCopy.zip 
* move it to your desired directory and decompress it
* Change imapCopy.sh file mode

        chmod +x imapCopy.sh

#### Run as command line tool
* Run as a command line tool:

        ./imapCopy.sh sourceImapAccount targetImapAccount (ex: ./imapCopy.sh imap://peter:mypassword@foo.com imaps://peter%40gmail.com:myotherpasword@imap.gmail.com)
      
* Run using (very basic) Gui

        ./imapCopyGui.sh
        
        
### Format of the imap accounts url

        {protocol}://[user:password@]{host}
        
Where 
* protocol can be imap or imaps
* user and password are optional and must be url scaped (ex: peter@gmail.com becomes peter%40gmail.com)
* host: host of the imap server
