package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;
import javax.mail.*;


public class EmailMsgServlet extends HttpServlet
{
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        PrintWriter pw = response.getWriter();
        
        pw.println("<?xml version=\"1.0\"?>");
        pw.println("<vxml version=\"1.0\">");
        
        EmailManager email_agent = EmailManager.getInstance(request);
        if (email_agent == null) {
            pw.println("</vxml>");
            return;
        }
        
        Message msg = null;
        String msgfrom = null;
        String date = null;
        String subject = null;

        String indexstr = request.getParameter("index");
        if (indexstr == null)
            indexstr = "0";
        int index = Integer.parseInt(indexstr);

        if (index < email_agent.totalMessages()) {
            msg = email_agent.read(index);

            try {
                msgfrom = EmailMessageReader.getFrom(msg);
                if (msgfrom == null)
                    msgfrom = "an anonymous sender";

                date = EmailMessageReader.getDate(msg);
                if (date == null)
                    date = "date not available";
                else
                    date = "on " + date;

                subject = msg.getSubject();
            }
            catch (MessagingException e) {
                msg = null;
            }
        }
        
        if (msg == null) {
            pw.println("<form id=\"nomoremail\">");
            pw.println("<block>");
            pw.println("  <audio src=\"audio/${GW_RECORDED_VOICE}/no_more_msg.wav\"/>");
            pw.println("</block>");
            pw.println("</form>");
        }
        else {
            pw.println("<form id=\"reademail\">");
            pw.println("<var name=\"index\" expr=\"" + indexstr + "\"/>");
            pw.println("  <block>");
            pw.println("    <audio src=\"audio/${GW_RECORDED_VOICE}/email_from.wav\"/>");
            pw.println(msgfrom);
            pw.println(date);
            if (subject == null) {
                pw.println("  <audio src=\"audio/${GW_RECORDED_VOICE}/email_no_sub.wav\"/>");
            }
            else {
                pw.println("  <audio src=\"audio/${GW_RECORDED_VOICE}/email_subject_is.wav\"/>");
                pw.println(subject);
            }
            pw.println("</block>");

            pw.println("  <field name=\"action\">");
            pw.println("    <prompt>");
            pw.println("      <audio src=\"audio/${GW_RECORDED_VOICE}/listen_next_del.wav\"/>");
            pw.println("    </prompt>");
            pw.println("    <grammar>");
            pw.println("	  listen | next | delete this email");
            pw.println("    </grammar>");
            pw.println("    <filled>");
            pw.println("      <if cond=\"action == 'listen'\">");
            pw.println("        <submit next=\"email_cnt\" namelist=\"index\"/>");
            pw.println("      <elseif cond = \"action == 'next'\"/>");
            pw.println("        <assign name=\"index\" expr=\"" + indexstr + "+1\"/>");
            pw.println("        <submit next=\"email_msg\" namelist=\"index\"/>");
            pw.println("      <elseif cond = \"action == 'delete this email'\"/>");
            pw.println("        <submit next=\"email_del\" namelist=\"index\"/>");
            pw.println("      </if>");
            pw.println("    </filled>");
            pw.println("  </field>");
            pw.println("</form>");
        }

        pw.println("</vxml>");
    }
}
