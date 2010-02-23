package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;
import javax.mail.*;


public class EmailIncServlet extends HttpServlet
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

        String nextstr = ((includeIndex == 0)? "" : "next");
        
        pw.println("<form>");
        pw.println("<var name=\"index\" expr=\"" + indexstr + "\"/>");
        pw.println("<var name=\"includeidx\" expr=\"" + includeIndexStr + "\"/>");

        if (include != null) {
            pw.println("<field name=\"wantReadFile\" type=\"boolean\">");
            pw.println("  <prompt>");
            if (includeIndex == 0) {
                pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/the.wav\"/>");
                pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/first.wav\"/>");
            }
            else {
                pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/the.wav\"/>");
                pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/next.wav\"/>");
            }
            pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/attached_filename_is.wav\"/>");
            try {
                pw.println(include.getFileName());
            }
            catch (MessagingException e) {
            }
            pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/do_u_want_to_hear_it.wav\"/>");
            pw.println("</prompt>");
            pw.println("<filled>");
            pw.println("  <if cond=\"!wantReadFile\">");
            pw.println("    <assign name=\"readFile\" expr=\"'skip'\"/>");
            pw.println("  </if>");
            pw.println("</filled>");
            pw.println("</field>");
     
            try {
                if (EmailMessageReader.isText(include)) {
                    String includeText = (String) include.getContent();
                    String[] incTexts = EmailMessageReader.breakup(includeText);
                    if (includeText != null) {
                        pw.println("<field name=\"readFile\">");
                        pw.println("  <prompt>");
                        for (int i = 0; i < incTexts.length; i++) {
                            pw.println("<block>");
                            pw.println(incTexts[i]);
                            pw.println("</block>");
                        }
                        pw.println("  </prompt>");
                        pw.println("  <dtmf>#</dtmf>");
                        pw.println("  <noinput>");
                        pw.println("    <assign name=\"readFile\" expr=\"'done'\"/>");
                        pw.println("  </noinput>");
                        pw.println("</field>");
                    }
                }
                else if (EmailMessageReader.isWave(include)) {
                    pw.println("<block name=\"readFile\">");
                    pw.println("<submit next=\"readEmailWave\" namelist=\"index includeidx\"/>");
                    pw.println("</block>");
                }
            }
            catch (MessagingException e) {
            }

            pw.println("<block>");
            pw.println("  <assign name=\"includeidx\" expr=\"includeidx + 1\"/>");
            pw.println("  <submit next=\"email_inc\" namelist=\"index includeidx\"/>");
            pw.println("</block>");
        }
        else {
            pw.println("<block>");
            pw.println("  <submit next=\"email_cnt_end\" namelist=\"index\"/>");
            pw.println("</block>");
        }

        pw.println("</form>");
        pw.println("</vxml>");
    }
}
