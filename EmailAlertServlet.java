package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;


public class EmailAlertServlet extends HttpServlet
{
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        String newcount = request.getParameter("newemail");
        String ans = request.getParameter("ans");
    
        PrintWriter pw = response.getWriter();
        
        pw.println("<?xml version=\"1.0\"?>");
        pw.println("<vxml version=\"1.0\">");

        pw.println("<form id=\"emailalert\">");
        pw.println("  <block>");
        
        pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/alert_for.wav\"/>");
        pw.println("<audio><say-as interpret-as=\"myname\"/></audio>");
        pw.println("<audio src=\"/audio/${GW_RECORDED_VOICE}/u_have.wav\"/>");
        pw.println(newcount);
        pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/email.wav\"/>");
        pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/msgs.wav\"/>");
        
        if (! "t".equals(ans)) {
            // this is the default Voicent Gateway inbound call password
            // it can be set under gateway > setup > options > inbound
            // the password.jsp is provided by the gateway
            pw.println("<submit next=\"/password.jsp\"/>");
        }
        
        pw.println("  </block>");
        pw.println("</form>");

        pw.println("</vxml>");
    }
}

