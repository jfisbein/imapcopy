imapcopy
========

Copy recursively all email messages and folders from one imap account to another

Usage Instructions
-------------------------

#### Run as a docker container: **(recommended)**

        docker run -it --name imapcopy jfisbein/imapcopy --source imap://peter:mypassword@foo.com --target imaps://peter%40gmail.com:myotherpasword@imap.gmail.com --fromDate 2018-02-01 --toDate 2019-01-01 --excluded Spam INBOX [Gmail] Spam Drafts Bin "Bart Simpson"        

#### Run as a command line tool:
* Download the application from the repository
* build de jar:

        mvn clean package
        
* Run the application:

        java -jar target/imapcopy-x.y.z.jar --source sourceImapAccount --target targetImapAccount --fromDate filterFromDate --toDate filterToDate --excluded [list of exlcuded folders]    
        (ex: java -jar target/imapcopy-1.1.0.jar --source imap://peter:mypassword@foo.com --target imaps://peter%40gmail.com:myotherpasword@imap.gmail.com --fromDate 2018-02-01 --toDate 2019-01-01 --excluded Spam INBOX [Gmail])

        
### Imap accounts url format

        {protocol}://[user:password@]{host}[:port]
        
Where 
* `protocol` can be `imap` or `imaps`
* `user` and `password` are optional and must be url escaped (ex: `peter@gmail.com` becomes `peter%40gmail.com`)
* `host`: host of the imap server
* `port`: port of the imap server
