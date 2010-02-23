package vx.apps.email;

import java.io.*;
import javax.servlet.http.*;
import javax.mail.*;
import HTTPClient.*;


public class SendEmailServlet extends HttpServlet
{
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        HttpSession session = request.getSession(true);
        EmailMessageReader emailReader = (EmailMessageReader) session.getAttribute("emailReader");
        if (emailReader == null) {
            emailReader = new EmailMessageReader();
            session.setAttribute("emailReader", emailReader);
        }
        
        EmailManager email_agent = EmailManager.getInstance(request);
        if (email_agent == null)
            return;
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream ins = request.getInputStream();
        byte[] chunk = new byte [256];
        int len;
        while ((len = ins.read(chunk)) != -1)
            bout.write(chunk, 0, len);

        byte[] body = bout.toByteArray();
        String dir = email_agent.getRecordDir();
        String name = null;
        String indexstr = null;
        String filename = null;
        
        try {
            NVPair[] opts = Codecs.mpFormDataDecode(bout.toByteArray(),
                                                    request.getContentType(),
                                                    dir);

            for (int i = 0; i < opts.length; i++) {
                if (opts[i].getName().equals("name"))
                    name = opts[i].getValue();
                else if (opts[i].getName().equals("index"))
                    indexstr = opts[i].getValue();
                else
                    filename = opts[i].getValue();
            }
        }
        catch (ParseException e) {
            return;
        }

        File oldf = new File(dir, filename);
        File f = new File(dir, "recording.wav");
        f.delete();
        oldf.renameTo(f);
    
        if (indexstr != null && indexstr.length() != 0) { // reply
            int index = Integer.parseInt(indexstr);
            Message msg = email_agent.read(index);
            if (msg != null) {
                try {
                    email_agent.reply("Hi, this message is recorded using EmailToPhone Software (http://www.emailtophone.com).",
                                      f.getAbsolutePath(),
                                      msg);
                }
                catch (MessagingException e) {
                }
            }

            response.sendRedirect("email_cnt_end?index=" + indexstr);
            return;
        }
    
        String addr = email_agent.getContactEmail(name);
        if (addr != null) {
            try {
                email_agent.send(addr,
                                 "Recorded voice message", 
                                 "Hi, this message is recorded using Voicent EmailToPhone Software (http://www.emailtophone.com).", 
                                 f.getAbsolutePath());
            }
            catch (MessagingException e) {
            }
        }

        response.sendRedirect("/sindex.jsp?fromapp=email");
    }
}
