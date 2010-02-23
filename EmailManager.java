package vx.apps.email;

import vx.server.VxApp;
import vx.util.Base64;

import java.util.Date;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import java.io.*;
import java.net.Socket;
import HTTPClient.*;

import javax.mail.*;
import javax.mail.event.*;
import javax.mail.internet.*;
import javax.activation.*;
import com.sun.mail.pop3.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

import java.security.Security;


public class EmailManager
{
    public EmailManager()
    {
        if (instance_ != null)
            throw new RuntimeException();
    }
    
    public static String getVxsid(HttpServletRequest request)
    {
        String result = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals("vxsid")) {
                    result = cookies[i].getValue();
                    break;
                }
    	    }
        }

        return result;
    }
    
    public static EmailManager getInstance(HttpServletRequest request)
    {
        String sessionId = getVxsid(request);
                
        if (! VxApp.checkSession(sessionId))
            return null;
        
        return instance_;
    }
    
    public void init(String appdir)
        throws IOException
    {
        // fix dirs
        log_.println("Init: " + appdir);
        
        File dir = new File(appdir);
        if (! dir.exists())
            throw new IOException("application direction does not exist: " + appdir);
          
        log_ = new PrintStream(new FileOutputStream(new File(dir, "../email.log")));
        
        File workdir = new File(dir, "../work");
        if (! workdir.exists())
            workdir.mkdir();
        recordDir_ = workdir.getAbsolutePath();
        
        Properties props = new Properties();
        File propspath = new File(dir, "emailsetup.conf");
        if (propspath.exists()) {
            FileInputStream propsFile = new FileInputStream(propspath);
            props.load(propsFile);
            propsFile.close();
        }
        else {
            log_.println("ERROR: email account has not been set up yet.");
            failedReason_ = "Email account not set up yet";
            return;
        }
        
        readProto_ = props.getProperty("readproto");
        if (readProto_ == null)
            readProto_ = "pop3";
        readHost_ = props.getProperty("readhost");
        if (readHost_ == null || readHost_.length() == 0) {
            log_.println("ERROR: POP host not defined.");
            failedReason_ = "POP host not defined";
            return;
        }
        readPort_ = props.getProperty("readport");
        if (readPort_ == null)
            readPort_ = "110";
            
        readUser_ = props.getProperty("readuser");
        if (readUser_ == null || readUser_.length() == 0) {
            log_.println("ERROR: email name not defined.");
            failedReason_ = "email name not defined";
            return;
        }
        
        String passwd = props.getProperty("readpassword");
        if (passwd != null) {
            readPassword_ = new String(Base64.decode(passwd));
        }
        else {
            readPassword_ = "";
        }
        
        readUseSSL_ = "1".equals(props.getProperty("readusessl"));
        
        log_.println("Email Address: " + readUser_);
        log_.println("Incoming Email Server: " + readHost_);
        log_.print(  "                        Port: " + readPort_);
        if (readUseSSL_)
            log_.print(", Use SSL");
        log_.println();
        
        sendProto_ = props.getProperty("sendproto");
        if (sendProto_ == null)
            sendProto_ = "SMTP";
        sendHost_ = props.getProperty("sendhost");
        if (sendHost_ == null || sendHost_.length() == 0) {
            log_.println("ERROR: SMTP host not defined.");
            failedReason_ = "SMTP host not defined";
            return;
        }
        sendPort_ = props.getProperty("sendport");
        if (sendPort_ == null)
            sendPort_ = "25";
        sendUser_ = props.getProperty("senduser");
        sendPassword_ = props.getProperty("sendpassword");
        if (sendUser_ == null) {
            sendUser_ = readUser_;
            sendPassword_ = readPassword_;
        }
        sendUseAuth_ = "1".equals(props.getProperty("senduseauth"));
        
        sendUseSSL_ = "1".equals(props.getProperty("sendusessl"));
        
        log_.println("Outgoing Email Server: " + sendHost_);
        log_.print(  "                       Port: " + sendPort_);
        if (sendUseSSL_)
            log_.print(", Use SSL");
        if (sendUseAuth_)
            log_.append(", Use Authentication");
        log_.println();
        
        notifyPhone_ = props.getProperty("aphone");
        String pollstr = props.getProperty("polltime");
        if (pollstr != null && pollstr.length() > 0)
            pollTime_ = Long.parseLong(pollstr) * 60 * 1000;
        else
            pollTime_ = 600000;
            
        String fromtxt = props.getProperty("alertfrom");
        BufferedReader br = new BufferedReader(new StringReader(fromtxt));
        String addr;
        while ((addr = br.readLine()) != null) {
            if (addr.length() > 0)
                alertFroms_.add(addr);
        }
        String totxt = props.getProperty("alertto");
        br = new BufferedReader(new StringReader(totxt));
        while ((addr = br.readLine()) != null) {
            if (addr.length() > 0)
                alertTos_.add(addr);
        }
            
        socksHost_ = props.getProperty("socksProxyHost");
        socksPort_ = props.getProperty("socksProxyPort");
        
        noSocksHosts_ = new ArrayList();
        String hoststr = props.getProperty("noSocksProxyHosts");
        if (hoststr != null) {
            StringTokenizer tkz = new StringTokenizer(hoststr, ",");
            while (tkz.hasMoreTokens())
                noSocksHosts_.add(tkz.nextToken());
        }
        if (! noSocksHosts_.contains("localhost"))
            noSocksHosts_.add("localhost");
        if (socksHost_ != null && socksPort_ != null) {
            try {
                Socket sock = new Socket(socksHost_, Integer.parseInt(socksPort_));
                sock.close();
                useSocks_ = true;
            }
            catch (Exception e) {
                useSocks_ = false;
            }
        }
                        
        instance_ = this;
    }
        
    public boolean download(boolean isAlertSession)
    {
        try {
            openDefaultFolder(isAlertSession);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace(log_);
            failedReason_ = e.toString();
        }
        
        return false;
    }
    
    public void endSession()
    {
        try {
            closeDefaultFolder();
            closeStore();
            
            conn_ = null;
            transport_ = null;
            
            accessLock_.release();
        }
        catch (MessagingException e) {
            e.printStackTrace(log_);
        }
    }
    
    public boolean startSession()
    {
        try {
            accessLock_.lock();
            
            failedReason_ = null;
            switchOnSocksProxy(sendHost_);
            
            Properties props = System.getProperties();
            props.setProperty("mail.smtp.host", sendHost_);
            props.setProperty("mail.smtp.port", sendPort_);
            
            if (readUseSSL_ || sendUseSSL_) {
                Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
                final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
                
                if (readUseSSL_) {
                    props.setProperty("mail." + readProto_ + ".socketFactory.class", SSL_FACTORY);
                    props.setProperty("mail." + readProto_ + ".socketFactory.fallback", "false");
                    props.setProperty("mail." + readProto_ + ".socketFactory.port", readPort_);
                }
                if (sendUseSSL_) {
                    props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
                    props.setProperty("mail.smtp.socketFactory.fallback", "false");
                    props.setProperty("mail.smtp.socketFactory.port", sendPort_);
                }
            }
            
            if (sendUser_ == null || !sendUseAuth_)
                conn_ = Session.getDefaultInstance(props, null);
            else {
                props.setProperty("mail.smtp.auth", "true");
                conn_ = Session.getDefaultInstance(props, 
                                    new Authenticator() {
                                        protected PasswordAuthentication getPasswordAuthentication() {
                                            return new PasswordAuthentication(sendUser_, sendPassword_);
                                        }
                                    });
            }
                
            deliveredMessages_ = new ArrayList();
                        
            switchOffSocksProxy(sendHost_);
            
            openStore();                
            
            return true;
        }
        catch (AuthenticationFailedException ae) {
            ae.printStackTrace(log_);
            failedReason_ = "Authentication failed";
        }
        catch (NoSuchProviderException pe) {
            pe.printStackTrace(log_);
            failedReason_ = "Cannot connect to the server";
        }
        catch (Throwable t) {
            t.printStackTrace(log_);
            failedReason_ = t.toString();
        }
        return false;
    }
    
    protected void openStore()
        throws NoSuchProviderException, MessagingException
    {
        switchOnSocksProxy(readHost_);
                
        store_ = conn_.getStore(readProto_);
        int port = -1;
        if (readPort_ != null)
            port = Integer.parseInt(readPort_);
        log_.println("Connecting to incoming email server...");
        store_.connect(readHost_, port, readUser_, readPassword_);
        log_.println("connected");
            
        switchOffSocksProxy(readHost_);
    }
    
    protected void closeStore()
        throws MessagingException
    {
        if (store_ != null) {
            store_.close();
            store_ = null;
        }
    }
    
    protected void openDefaultFolder(boolean isAlertSession)
        throws NoSuchProviderException, MessagingException
    {        
        switchOnSocksProxy(readHost_);

        log_.println("Openning Inbox...");
        curFolder_ = store_.getFolder("INBOX");
        curFolder_.open(Folder.READ_WRITE);
        log_.println("Opened");
        initDefaultFolder(isAlertSession);
        
        switchOffSocksProxy(readHost_);
    }
    
    public void closeDefaultFolder()
        throws MessagingException
    {
        if (curFolder_ != null) {
            log_.println("close inbox");
            curFolder_.close(true);
            curFolder_ = null;
        }
    }
    
    protected void initDefaultFolder(boolean isAlertSession)
        throws MessagingException
    {
        Message[] msgs = curFolder_.getMessages();
        log_.println("Total messages: " + Integer.toString(msgs.length));
            
        // get flags first
        log_.println("Fetching email envelopes...");
        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        fp.add(FetchProfile.Item.FLAGS);
        curFolder_.fetch(msgs, fp);
        log_.println("fetched");
            
        if (readProto_.equals("pop3")) {
            log_.println("save message UIDs");
            POP3Folder pf = (POP3Folder)curFolder_;
            // save
            for (int i = 0; i < messages_.size(); i++) {
                String uid = pf.getUID((Message)messages_.get(i));
                oldUids_.add(uid);
            }
            // delete
            for (int i = oldUids_.size() - 1; i >= 0; i--) {
                boolean deleted = true;
                for (int j = 0; j < msgs.length; j++) {
                    String uid = pf.getUID(msgs[j]);
                    if (uid.equals(oldUids_.get(i))) {
                        deleted = false;
                        break;
                    }
                }
                if (deleted)
                    oldUids_.remove(i);
            }           
        }
        
        log_.println("Initialize message array");
        messages_ = new ArrayList();
        deletedMessages_ = new ArrayList();
                        
        for (int i = 0; i < msgs.length; i++) {
            Flags flags = msgs[i].getFlags();
            if (readProto_.equals("pop3")) {
                if (!isAlertSession || isNewPop3Message((POP3Folder)curFolder_, msgs[i]))
                    messages_.add(msgs[i]);
            }
            else {
                if (msgs[i].isSet(Flags.Flag.RECENT))
                    messages_.add(msgs[i]);
            }
        }
        log_.println("Initialization done");
    }
        
    public int totalMessages()
    {
        return messages_.size();
    }
    
    public Message read(int i) 
    {
        return (Message) messages_.get(i);
    }
    
    public boolean delete(int i)
    {      
        try {
            Message msg = read(i);            
            Message[] msgs = { msg };
            
            msg.setFlag(Flags.Flag.DELETED, true);
            
            deletedMessages_.add(msg);
            messages_.remove(i);
            return true;
        }
        catch (MessagingException e) {
            failedReason_ = e.toString();
            e.printStackTrace(log_);
            return false;
        }
        
    }
    
    public void delete(Message msg)
    {
        for (int i = 0; i < messages_.size(); i++) {
            if (msg == read(i)) {
                delete(i);
                break;
            }
        }
    }
    
    public String getRecordDir()
    {
        return recordDir_;
    }
    
    public String getFailedReason()
    {
        return failedReason_;
    }
    
    public String getUserAddress()
    {
        return readUser_; // + "@" + domain;
    }
    
    public int totalContacts()
    {
        return 0;
    }
    
    public String getContactName(int i)
    {
        return null;
    }
    
    public String getContactEmail(int i)
    {
        return null;
    }
    
    public String getContactEmail(String name)
    {
        return null;
    }
    
    public Message send(String to, String subject, String content, String filename)
        throws AddressException, MessagingException
    {
        return send(InternetAddress.parse(to, false), subject, content, filename);
    }
    
    public Message send(javax.mail.Address[] to, String subject, String content, String filename)
        throws AddressException, MessagingException
    {
        Message msg = new MimeMessage(conn_);
        msg.setFrom(new InternetAddress(getUserAddress()));
                    
        msg.setRecipients(Message.RecipientType.TO, to);
                
        msg.setSubject(subject);
                
        msg.setSentDate(new Date());
            
        if (filename == null)
            msg.setText(content);
        else {
	        Multipart mp = new MimeMultipart();
        	    
	        MimeBodyPart mbp1 = new MimeBodyPart();
	        mbp1.setText(content);
	        mp.addBodyPart(mbp1);
        	    
	        MimeBodyPart mbp2 = new MimeBodyPart();
   	        FileDataSource fds = new FileDataSource(filename);
	        mbp2.setDataHandler(new DataHandler(fds));
	        mbp2.setFileName(fds.getName());
		    mp.addBodyPart(mbp2);
        	    
	        msg.setContent(mp);
	    }

        send(msg);
            
        return msg;
    }
    
    public String includeMessage(Message msg)
        throws IOException, MessagingException
    {
        EmailMessageReader er = new EmailMessageReader(msg);
        
        StringBuffer buf = new StringBuffer();
        buf.append("----- Original Message -----\n");
        buf.append("From: " + er.getFrom() + "\n");
        buf.append("To: " + er.getRecipients() + "\n");
        buf.append("Sent: " + er.getDate() + "\n");
        buf.append("Subject: " + msg.getSubject() + "\n\n");
        buf.append(er.includeContent() + "\n");
        
        return buf.toString();
    }
    
    public Message reply(String content, String filename, Message em)
        throws MessagingException, IOException
    {
        return send(em.getFrom(),
                    "Re: " + em.getSubject(),
                    content + "\n\n" + includeMessage(em),
                    filename);
    }
    
    public boolean waitForDeliver(Message msg, long timeout)
    {
        long starttime = System.currentTimeMillis();
        
        while (true) {
            if (deliveredMessages_.contains(msg))
                return true;
            
            long timewaiting = System.currentTimeMillis() - starttime;
            if (timewaiting > timeout)
                return false;
                
            synchronized (synch_) {
                try {
                    synch_.wait(timeout);
                }
                catch (InterruptedException e) {
                    return false;
                }
            }
        }
    }
    
    public void messageDelivered(TransportEvent e)
    {
        deliveredMessages_.add(e.getMessage());
        synchronized (synch_) {
            synch_.notifyAll();
        }
    }
    
    public void messageNotDelivered(TransportEvent e)
    {
    }
    
    public void messagePartiallyDelivered(TransportEvent e)
    {
    }
    
    private void send(Message msg)
        throws MessagingException
    {
        switchOnSocksProxy(sendHost_);
        
        // transport_.send(msg);
        Transport.send(msg);
        
        switchOffSocksProxy(sendHost_);
    }
    
    private void switchOnSocksProxy(String host)
    {
        if (!useSocks_ || socksHost_ == null)
            return;
        if (noSocksHosts_.contains(host))
            return;
        
        System.out.println("Turn on socks for host : " + host);
        
        Properties props = System.getProperties();
        props.setProperty("socksProxyHost", socksHost_);
        props.setProperty("socksProxyPort", socksPort_);
        System.setProperties(props);
    }
    
    private void switchOffSocksProxy(String host)
    {
        if (!useSocks_ || socksHost_ == null)
            return;
        if (noSocksHosts_.contains(host))
            return;
        
        System.out.println("Turn off socks for host : " + host);

        Properties props = System.getProperties();
        props.remove("socksProxyHost");
        props.remove("socksProxyPort");
        System.setProperties(props);
    }
    
    class AccessLock
    {
        synchronized public void lock()
        {
            if (isLocked_) {
                try {
                    wait();
                }
                catch (InterruptedException e) {
                }
            }
            isLocked_ = true;
        }
        
        synchronized public void release()
        {
            isLocked_ = false;
            notifyAll();
        }
        
        private boolean isLocked_ = false;
    }
    
    private void makeAlertCall()
    {
        try {
            log_.println("Making alert calls. Sending request to Voicent gateway...");
            
            NVPair[] params = new NVPair [7];
            params[0] = new NVPair("info", "New Email Alert Call");
            params[1] = new NVPair("calltime", "0");
            params[2] = new NVPair("firstocc", "30");
            params[3] = new NVPair("phoneno", notifyPhone_);
            params[4] = new NVPair("attendee", "<My Self>");
            params[5] = new NVPair("selfdelete", "1");
            
            String starturl = "http://localhost:" + Integer.toString(VxApp.getPort());
            starturl += "/email/email_alert?newemail=" + newEmailCount_;
            params[6] = new NVPair("starturl", starturl);
            
            HTTPConnection conn = new HTTPConnection("localhost", VxApp.getPort());
            HTTPResponse res = conn.Post("/ocall/callreqHandler.jsp", params);
    	    
	        if (res == null || res.getStatusCode() != 200) {
	            log_.print("*** Failed: ");
	            if (res != null)
	                log_.println(res.getReasonLine());
	        }
	        else
	            log_.println("Request Sent");
	            
	        conn.stop();
	    }
	    catch (Throwable e) {
	        e.printStackTrace(log_);
	    }
    }
    
    public synchronized String startAlertThread()
    {
        if (notifyThread_ != null && !notifyThread_.isStopped())
            return "Alert has been turned on already.";
        
        if (notifyPhone_ == null || notifyPhone_.length() == 0) {
            return "Alert phone number not defined.";
        }
        
        if (alertFroms_.size() == 0 && alertTos_.size() == 0) {
            return "No address is defined for alerts.";
        }
            
        if (notifyThread_ == null) {
            notifyThread_ = new AlertThread();
            notifyThread_.start();
        }
        else {
            notifyThread_.setStop(false);
        }
        
        return "Email Alert started";
    }
    
    public synchronized String stopAlertThread()
    {
        if (notifyThread_ == null || notifyThread_.isStopped())
            return "Alert has been turned off already";
            
        notifyThread_.setStop(true);
        return "Alert is turned off";
    }
    
    public boolean isAlertStopped()
    {
        return notifyThread_ == null || notifyThread_.isStopped();
    }
    
    public synchronized void setAlertThread(AlertThread t)
    {
        notifyThread_ = t;
    }
    
    private boolean isNewPop3Message(POP3Folder pf, Message msg)
        throws MessagingException
    {
        String uid = pf.getUID(msg);
        if (oldUids_.contains(uid))
            return false;
        
        for (int i = 0; i < oldUids_.size(); i++) {
            if (uid.equals(oldUids_.get(i)))
                return false;
        }
        
        return true;
    }
            
    class AlertThread extends Thread
    {
        public void run()
        {
            boolean isFirstTime = true;
            while (true) {
                Calendar now = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM d hh:mm aa");
                log_.println(sdf.format(now.getTime()) + " Checking emails...");
                
                boolean hasAlertMessage = false;
                
                try {
                    startSession();
                    download(true);
                    newEmailCount_ = totalMessages();
                    
                    if (isFirstTime) {
                        isFirstTime = false;
                        log_.println("Total messages in INBOX: " + Integer.toString(newEmailCount_));
                    }
                    else {
                        
                        for (int i = 0; i < totalMessages(); i++) {
                            Message msg = read(i);
                            InternetAddress[] addrs = (InternetAddress[])msg.getFrom();
                            if (addrs == null)
                                continue;
                            for (int j = 0; j < addrs.length; j++) {
                                if (alertFroms_.contains(addrs[j].getAddress())) {
                                    hasAlertMessage = true;
                                    break;
                                }
                            }
                            if (hasAlertMessage)
                                break;
                            
                            addrs = (InternetAddress[])msg.getRecipients(Message.RecipientType.TO); 
                            if (addrs == null)
                                continue;
                            for (int j = 0; j < addrs.length; j++) {
                                if (alertTos_.contains(addrs[j].toString())) {
                                    hasAlertMessage = true;
                                    break;
                                }
                            }
                        } 
                    }
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
                
                try {
                    endSession();
                }
                catch (Throwable t) {
                    t.printStackTrace(log_);
                }
                
                // now make the call
                if (hasAlertMessage)
                    makeAlertCall();
                
                log_.println("Done checking email");
                try {
                    Thread.currentThread().sleep(pollTime_);
                    if (isStopped())
                        break;
                }
                catch (InterruptedException e) {
                    break;
                }
            }
            
            setAlertThread(null);
        }
        
        public synchronized void setStop(boolean tf)
        {
            isStop_ = tf;
        }
        
        public synchronized boolean isStopped()
        {
            return isStop_;
        }
        
        
        private boolean isStop_ = false;
    }
            
    public static void main(String[] args)
    {
        try {            
            EmailManager agent = new EmailManager();
            agent.init("C:\\download\\email\\conf");
            
            agent.startSession();
            agent.send("info@voicent.com", "hello 1", "world 1", null);
            agent.send("info@voicent.com", "hello 2", "world 2", null);
            agent.endSession();
            
            // stop here to have the msg delivered
            agent.startSession();
            agent.download(false);
            int total = agent.totalMessages();
            for (int i = 0; i < total; i++) {
                Message msg = agent.read(i);
                printMessage(msg, System.out);
                if (i == 0)
                    agent.reply("try reply", null, msg);
                else
                    agent.reply("try attachment reply", "c:/winnt/media/notify.wav", msg);
                agent.delete(msg);
            }         
            agent.endSession();
            
            agent.startAlertThread();
            
            Thread.currentThread().sleep(1000000000);
         }
        catch (Exception e) {
            System.out.println(e.toString());
        }
    }
    
    public static void printMessage(Message msg, PrintStream out)
        throws MessagingException, IOException
    {
        EmailMessageReader rd = new EmailMessageReader(msg);
        
        out.println(rd.getFrom());
        out.println(rd.getRecipients());
        out.println(rd.getDate());
        out.println(msg.getSubject());
        out.println(rd.getContent());
        out.println("Include files: " + Integer.toString(rd.numIncludeFiles()));
        
        out.println("****************** structure ****************");
        printPart(rd, msg, out, 0);
    }
    
    public static void printPart(EmailMessageReader rd, 
                                 Part p,
                                 PrintStream out, 
                                 int indent)
        throws MessagingException, IOException
    {
        if (p == null)
            return;
        out.println("Mime-Type: " + p.getContentType());
        
        if (EmailMessageReader.isMultipart(p)) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++)
                printPart(rd, mp.getBodyPart(i), out, indent+1);
        }
        else if (EmailMessageReader.isWave(p)) {
            printIndentation(out, indent);
            out.println("======== Attached Wave File ========");
            printIndentation(out, indent);
            out.println("File name: " + p.getFileName());
        }
        else if (EmailMessageReader.isText(p)) {
            if (p.getFileName() != null) {
                printIndentation(out, indent);
                out.println("-------- Attached Text File --------");
                printIndentation(out, indent);
                out.println("File name: " + p.getFileName());
            }
            
            BufferedReader br = new BufferedReader(
                                    new StringReader((String)p.getContent()));
            String line;
            while ((line = br.readLine()) != null) {
                printIndentation(out, indent);
                out.println(line);
            }
        }
    }
    
    public static void printIndentation(PrintStream out, int indent)
    {
        for (int i = 0; i < indent; i++)
            out.print(">");
    }
    
           
    private String readProto_ = null;
    private String readHost_ = null;
    private String readPort_ = null;
    private String readUser_ = null;
    private String readPassword_ = null;
    private boolean readUseSSL_ = false;

    private String sendProto_ = null;
    private String sendHost_ = null;
    private String sendPort_ = null;
    private String sendUser_ = null;
    private String sendPassword_ = null;
    private boolean sendUseAuth_ = false;
    private boolean sendUseSSL_ = false;
    
    private boolean useSocks_ = false;
    private String socksHost_ = null;
    private String socksPort_ = null;
    private ArrayList noSocksHosts_ = null;
    
    private String recordDir_ = null;    
    private String notifyPhone_ = null;
    private ArrayList alertFroms_ = new ArrayList();
    private ArrayList alertTos_ = new ArrayList();
    private int newEmailCount_ = 0;
    
    private AlertThread notifyThread_ = null;
    private long pollTime_ = 600000;
    private AccessLock accessLock_ = new AccessLock();
    
    private Session     conn_ = null;
    private Transport   transport_ = null;
    private Store       store_ = null;
    private Folder      curFolder_ = null;
    private ArrayList   messages_ = new ArrayList();
    private ArrayList   oldUids_ = new ArrayList();
    private ArrayList   deletedMessages_ = null;
    private ArrayList   deliveredMessages_ = null;
    private Object      synch_ = new Object();
    
    protected PrintStream log_ = System.out;
    protected String failedReason_ = null;
    
    static EmailManager instance_ = null;
}
