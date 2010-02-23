package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;


public class EmailCntEndServlet extends HttpServlet
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

        String indexstr = request.getParameter("index");
        
        pw.println("<form>");
        pw.println("  <var name=\"index\" expr=\"'" + indexstr + "'\"/>");
        pw.println("  <var name=\"fromapp\" expr=\"'email'\"/>");

        pw.println("  <field name=\"action\">");
        pw.println("    <prompt>");
        pw.println("      <audio src=\"audio/${GW_RECORDED_VOICE}/listen_next_reply_del.wav\"/>");
        pw.println("    </prompt>");
        pw.println("    <grammar>");
        pw.println("	  listen | next | reply | delete this email");
        pw.println("    </grammar>");
        pw.println("    <filled>");
        pw.println("      <if cond=\"action == 'listen'\">");
        pw.println("        <submit next=\"email_cnt\" namelist=\"index\"/>");
        pw.println("      <elseif cond=\"action == 'next'\"/>");
        pw.println("        <assign name=\"index\" expr=\"" + indexstr + "+1\"/>");
        pw.println("        <submit next=\"email_msg\" namelist=\"index\"/>");
        pw.println("      <elseif cond=\"action == 'delete this email'\"/>");
        pw.println("        <submit next=\"email_del\" namelist=\"index\"/>");
        pw.println("      <elseif cond =\"action == 'reply'\"/>");
        pw.println("        <submit next=\"recordEmail\" namelist=\"index\"/>");
        pw.println("      </if>");
        pw.println("    </filled>");
        pw.println("  </field>");

        pw.println("</form>");
        pw.println("</vxml>");
    }
}
