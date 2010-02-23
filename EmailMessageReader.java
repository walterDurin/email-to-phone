package vx.apps.email;

import javax.mail.*;
import java.io.*;
import java.util.ArrayList;
import java.text.SimpleDateFormat;


public class EmailMessageReader
{
    public EmailMessageReader()
    {
        msg_ = null;
    }
    
    public EmailMessageReader(Message msg)
        throws IOException, MessagingException
    {
        msg_ = msg;
        setMessage(msg);
    }
    
    public void setMessage(Message msg)
        throws IOException, MessagingException
    {
        msg_ = msg;
        
        // parse message exception included files
        String content = getContent();
        if (content == null) {
            newContent_ = null;
            oldContent_ = null;
        }
        else {
            int idx = content.indexOf("----- Original Message -----");
            newContent_ = content;
            oldContent_ = null;
            if (idx != -1) {
                newContent_ = content.substring(0, idx);
                oldContent_ = content.substring(idx);
            }
        }
        
        ArrayList list = collectFiles();
        if (list.size() == 0)
            includes_ = null;
        else {
            includes_ = new Part[list.size()];
            list.toArray(includes_);
        }
    }
    
    public String getFrom()
        throws MessagingException
    {
        return getFrom(msg_);
    }
    
    public static String getFrom(Message msg)
        throws MessagingException
    {
        javax.mail.Address[] addrs = msg.getFrom();
        if (addrs == null || addrs.length == 0)
            return null;
            
        String str = "";
        for (int i = 0; i < addrs.length; i++) {
            if (i != 0)
                str += ", ";
            String text = addrs[i].toString();
            str += removeChars(text);
        }
        
        return str;
    }
    
    public String getRecipients()
        throws MessagingException
    {
        return getRecipients(msg_);
    }
    
    public static String getRecipients(Message msg)
        throws MessagingException
    {
        javax.mail.Address[] addrs = msg.getRecipients(Message.RecipientType.TO);
        if (addrs == null || addrs.length == 0)
            return null;
            
        String str = "";
        for (int i = 0; i < addrs.length; i++) {
            if (i != 0)
                str += ", ";
            String text = addrs[i].toString();
            str += removeChars(text);
        }
        
        return str;
    }
    
    public String getDate()
        throws MessagingException
    {
        return getDate(msg_);
    }
    
    public static String getDate(Message msg)
        throws MessagingException
    {
        java.util.Date d = msg.getSentDate();
        if (d != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("MMMM d hh:mm aa");
            return formatter.format(d);
        }
        return null;
    }
    
    public static boolean isMultipart(Part p)
        throws MessagingException
    {
        return p.isMimeType("multipart/*");
    }
    
    public static boolean isWave(Part p)
        throws MessagingException
    {
        return p.isMimeType("audio/x-wav") || p.isMimeType("audio/wav");
    }
    
    public static boolean isText(Part p)
        throws MessagingException
    {
        return p.isMimeType("text/plain");
    }
    
    // return original filename
    public static String readWaveFile(Part p, File savefile)
        throws IOException, MessagingException
    {
	    InputStream is = p.getInputStream();
	    FileOutputStream fs = new FileOutputStream(savefile);
	    byte[] wavbuf = new byte[256];
	    int len;
	    while ((len = is.read(wavbuf)) > 0)
	        fs.write(wavbuf, 0, len);
	    fs.close();
	    
	    return p.getFileName();
    }
    
    public ArrayList collectFiles()
        throws IOException, MessagingException
    {
        return collectFiles(msg_);
    }
    
    public ArrayList collectFiles(Part p)
        throws IOException, MessagingException
    {
        ArrayList list = new ArrayList();
        
        if (p.getFileName() != null) {
            list.add(p);
            return list;
        }
        
        if (isMultipart(p)) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++)
                list.addAll(collectFiles(mp.getBodyPart(i)));
        }
        
        return list;
    }
    
    public String includeContent()
        throws IOException, MessagingException
    {
        return includeContent(msg_);
    }
    
    public static String includeContent(Part p)
        throws IOException, MessagingException
    {
        StringBuffer buf = new StringBuffer();
        
        if (isMultipart(p)) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                buf.append(includeContent(mp.getBodyPart(i)));
                buf.append("\n");
            }
        }
        else if (isText(p) && p.getFileName()==null) {
            BufferedReader br = new BufferedReader(
                                    new StringReader((String)p.getContent()));
            String line;
            while ((line = br.readLine()) != null) {
                buf.append(">");
                buf.append(line);
                buf.append("\n");
            }
        }
        
        return buf.toString();
    }
        
    public String getContent()
        throws IOException, MessagingException
    {
        return getContent(msg_);
    }
    
    public static String getContent(Part p)
        throws IOException, MessagingException
    {
        StringBuffer buf = new StringBuffer();
        
	    if (isText(p)) {
		    buf.append((String)p.getContent());
	    }
	    else if (isMultipart(p)) {
	        Multipart mp = (Multipart)p.getContent();
	        int count = mp.getCount();
	        for (int i = 0; i < count; i++) {
	            Part pp = mp.getBodyPart(i);
	            if (pp.getFileName() != null)
	                continue;
	            String ppcontent = getContent(pp);
	            if (ppcontent != null) {
		            buf.append(ppcontent);
		            buf.append("\n");
		        }
		    }
	    }
	    else if (p.isMimeType("message/rfc822")) {
	        String pcontent = getContent((Part)p.getContent());
	        if (pcontent != null)
	            buf.append(pcontent);
	    }
	    
        String m = removeChars(buf.toString());
        if (m.trim().length() == 0)
            return null;
            
        return m;
    }
    
    public static String[] breakup(String content)
    {
        if (content == null)
            return null;
            
        ArrayList list = new ArrayList();
        
        while (content.length() > 0) {
            if (content.length() < 400) {
                list.add(content);
                break;
            }
            
            int i = 320;
            for (; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '\n' ||
                    i == content.length() - 1 || 
                    (c == '.' && content.charAt(i+1) == ' ')) 
                {
                    list.add(content.substring(0, i+1));
                    content = content.substring(i+1);
                    break;
                }
            }
        }
        
        String[] cts = new String[list.size()];
        list.toArray(cts);
        return cts;
    }
    
    public String getNewContent()
    {
        return newContent_;
    }
    
    public String getOldContent()
    {
        return oldContent_;
    }
    
    public Part getIncludeFile(int index)
    {
        if (includes_ == null)
            return null;
            
        if (index >= includes_.length || index < 0)
            return null;
        
        return includes_[index];
    }
    
    public int numIncludeFiles()
    {
        if (includes_ == null)
            return 0;
        return includes_.length;
    }
    
    public static String filterChars(String text)
    {
        if (text == null)
            return null;
            
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<')
                buf.append(" less than ");
            else if (c == '>')
                buf.append(" greater than ");
            else if (c == '\'' || c == '\"')
                buf.append(" quote ");
            else if (c == '&')
                buf.append(" and ");
            else
                buf.append(c);
        }
        
        return buf.toString();
    }
    
    public static String removeChars(String text)
    {
        if (text == null)
            return null;
            
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<' || c == '>' || c == '\'' || c == '\"' || c == '&')
                buf.append(' ');
            else
                buf.append(c);
        }
        
        return buf.toString();
    }
        
    
    protected Message msg_ = null;
    protected String newContent_ = null;
    protected String oldContent_ = null;
    protected Part[] includes_ = null;
}
