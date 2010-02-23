package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;


public class RecordEmailServlet extends HttpServlet
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
        
        String name = request.getParameter("name");
        if (name == null)
            name = "";
        String indexstr = request.getParameter("index");
        if (indexstr == null)
            indexstr = "";
        
        pw.println("<form id=\"recordemail\">");

        pw.println("  <var name=\"name\" expr=\"'" + name + "'\"/>");
        pw.println("  <var name=\"index\" expr=\"'" + indexstr + "'\"/>");

        pw.println("  <dtmf>");
        pw.println("	textmsg = $digits");
        pw.println("  </dtmf>");

        pw.println("  <initial name=\"choice\">");      
        pw.println("    <prompt timeout=\"1s\">");
        pw.println("      <audio src=\"/audio/${GW_RECORDED_VOICE}/record_msg.wav\"/>");
        pw.println("      <audio src=\"/audio/${GW_RECORDED_VOICE}/finish_press_pound.wav\"/>");
        pw.println("    </prompt>");
        pw.println("    <noinput>");
        pw.println("      <assign name=\"choice\" expr=\"'recordmsg'\"/>");
        pw.println("    </noinput>");
        pw.println("  </initial>");       

        pw.println("  <record name=\"content\">");
        pw.println("    <filled>");
        pw.println("      <prompt>");
        pw.println("	<audio src=\"/audio/${GW_RECORDED_VOICE}/wait_while_conn.wav\"/>");
        pw.println("      </prompt>");
        pw.println("      <submit next=\"sendEmail\" namelist=\"name index content\"/>");
        pw.println("    </filled>");
        pw.println("  </record>");

        pw.println(" <filled namelist=\"textmsg\">");
        pw.println("   <if cond=\"textmsg.length == 0\">");
        pw.println("     empty message. please try again.");
        pw.println("     <clear/>");
        pw.println("   <else/>");    
        pw.println("     <prompt>");
        pw.println("       <audio src=\"/audio/${GW_RECORDED_VOICE}/wait_while_conn.wav\"/>");
        pw.println("     </prompt>");
        pw.println("     <submit next=\"sendTextEmail\" namelist=\"name index textmsg\"/>");
        pw.println("   </if>");
        pw.println(" </filled>");   

        pw.println("</form>");
        pw.println("</vxml>");
    }
}
