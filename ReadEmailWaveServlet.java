package vx.apps.email;

import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.*;
import javax.mail.*;


public class ReadEmailWaveServlet extends HttpServlet
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
        
        PrintWriter pw = response.getWriter();
        
        pw.println("<?xml version=\"1.0\"?>");
        pw.println("<vxml version=\"1.0\">");
        
        EmailManager email_agent = EmailManager.getInstance(request);
        if (email_agent == null) {
            pw.println("</vxml>");
            return;
        }
        
        String indexstr = request.getParameter("index");

        String includeIndexStr = request.getParameter("includeidx");
        if (includeIndexStr == null)
            includeIndexStr = "0";
        int includeIndex = Integer.parseInt(includeIndexStr);

        Part include = emailReader.getIncludeFile(includeIndex);
        String fname = null;
        String userFname = null;
        if (include != null) {
            try {
                fname = include.getFileName();
                userFname = "../work/" + fname;

                String dir = getServletContext().getRealPath("");
                File workdir = new File(dir, "work");
                workdir.mkdirs();

                File savedFile = new File(workdir, fname);
                EmailMessageReader.readWaveFile(include, savedFile);
            }
            catch (MessagingException e) {
                include = null;
            }
        }

        pw.println("<var name=\"index\" expr=\"" + indexstr + "\"/>");
        pw.println("<var name=\"includeidx\" expr=\"" + includeIndexStr + "+1\"/>");
        pw.println("  <block>");
        if (include == null) {
            pw.println("Sorry, there is trouble getting the audio file");
        }
        else {
            pw.println("<audio src=\"" + userFname + "\"/>");
        }
        
        pw.println("    <submit next=\"email_inc\" namelist=\"index includeidx\"/>");
        pw.println("  </block>");
        pw.println("</form>");
        pw.println("</vxml>");
    }
}
