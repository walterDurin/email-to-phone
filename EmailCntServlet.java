package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;

import javax.mail.*;


public class EmailCntServlet extends HttpServlet
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
        int index = Integer.parseInt(indexstr);

        Message msg = email_agent.read(index);
        try {
            emailReader.setMessage(msg);
        }
        catch (MessagingException e) {
            pw.println("<block>Failed to set message</block>");
            pw.println("</vxml>");
            return;
        }

        String newContent = emailReader.getNewContent();
        String[] newCts = EmailMessageReader.breakup(newContent);
        String oldContent = emailReader.getOldContent();
        String[] oldCts = EmailMessageReader.breakup(oldContent);

        int numIncludes = emailReader.numIncludeFiles();
        
        pw.println("<form id=\"content\">");
        pw.println("<var name=\"index\" expr=\"" + indexstr + "\"/>");
        pw.println("<field name=\"readContent\">");
        pw.println("  <prompt timeout=\"1s\">");
        if (newContent == null) {
            pw.println("<block>");
            pw.println("  <audio src=\"audio/${GW_RECORDED_VOICE}/email_no_content.wav\"/>");
            pw.println("</block>");
        }
        else {
            for (int i = 0; i < newCts.length; i++) {
                pw.println("<block>");
                pw.println(newCts[i]);
                pw.println("</block>");
            }
        }
        pw.println("  </prompt>");

        pw.println("  <dtmf>#</dtmf>");
        
        pw.println("  <noinput>");
        pw.println("    <assign name=\"readContent\" expr=\"'done'\"/>");
        pw.println("  </noinput>");
        pw.println("</field>");
  
        if (oldContent != null) {
            pw.println("  <field name=\"wantOldContent\" type=\"boolean\">");
            pw.println("    <prompt>");
            pw.println("      <audio src=\"audio/${GW_RECORDED_VOICE}/email_prev_msg.wav\"/>");
            pw.println("    </prompt>");
            pw.println("    <filled>");
            pw.println("      <if cond=\"!wantOldContent\">");
            pw.println("        <assign name=\"readOldContent\" expr=\"'skip'\"/>");
            pw.println("      </if>");
            pw.println("    </filled>");
            pw.println("  </field>");
  
            pw.println("  <field name=\"readOldContent\">");
            pw.println("    <prompt timeout=\"1s\">");
            for (int i = 0; i < oldCts.length; i++) {
                pw.println("      <block>");
                pw.println(oldCts[i]);
                pw.println("      </block>");
            }
            pw.println("    </prompt>");
            pw.println("    <dtmf>#</dtmf>");
            pw.println("    <noinput>");
            pw.println("      <assign name=\"readOldContent\" expr=\"'done'\"/>");
            pw.println("    </noinput>");
            pw.println("  </field>");
        }
  
        if (numIncludes > 0) {
            pw.println("    <block>");
            pw.println("      <audio src=\"audio/${GW_RECORDED_VOICE}/email_has.wav\"/>");
            pw.println(Integer.toString(numIncludes));
            pw.println("      <audio src=\"audio/${GW_RECORDED_VOICE}/attached_files.wav\"/>");
            pw.println("      <submit next=\"email_inc\" namelist=\"index\"/>");
            pw.println("    </block>");
        }
        else {
            pw.println("      <block>");
            pw.println("        <submit next=\"email_cnt_end\" namelist=\"index\"/>");
            pw.println("      </block>");
        }
        
        pw.println("</form>");
        pw.println("</vxml>");
    }
}
