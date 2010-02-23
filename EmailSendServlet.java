package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;


public class EmailSendServlet extends HttpServlet
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
        
        String grammar = "";
        for (int i = 0; i < email_agent.totalContacts(); i++) {
            String name = email_agent.getContactName(i);
            if (i != 0) 
                grammar += " | ";
            grammar += name;
        }

        if (email_agent == null) {
            pw.println("  <block>");
            pw.println("    <audio src=\"audio/${GW_RECORDED_VOICE}/no_serv_try_later.wav\"/>");
            pw.println("  </block>");
        }
        else {
            pw.println("  <field name=\"name\">");
            pw.println("    <prompt>");
            pw.println("      <audio src=\"audio/${GW_RECORDED_VOICE}/email_to_who.wav\"/>");
            pw.println("    </prompt>");
            pw.println("    <grammar>");
            pw.println(grammar);
            pw.println("    </grammar>");
            pw.println("  </field>");
  
            pw.println("    <block>");
            pw.println("      <audio src=\"audio/${GW_RECORDED_VOICE}/email_to_confirm.wav\"/>");
            pw.println("      <value expr=\"name\"/>");
            pw.println("      <submit next=\"recordEmail\" namelist=\"name\"/>");
            pw.println("    </block>");
        }

        pw.println("</form>");
        pw.println("</vxml>");
    }
}
