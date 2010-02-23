package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;


public class EmailServlet extends HttpServlet
{
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        EmailManager email_agent = EmailManager.getInstance(request);

        // Voicent Gateway sends this request when session ends
        String vxstate = request.getParameter("vxstate");    
        if ("end".equals(vxstate)) {
            if (email_agent != null)
                email_agent.endSession();
            return;
        }
        
        String failedReason = null;
        if (email_agent == null) {
            failedReason = "Invalid Session or email failed to start";
        }
        else {
            boolean ok = false;    
            if (email_agent.startSession())
                ok = email_agent.download(false);
            if (! ok)
                failedReason = email_agent.getFailedReason();
        }
    
        PrintWriter pw = response.getWriter();
        
        pw.println("<?xml version=\"1.0\"?>");
        pw.println("<vxml version=\"1.0\">");

        pw.println("<form id=\"email\">");
        pw.println("<var name=\"fromapp\" expr=\"'email'\"/>");
        pw.println("  <block>");
        
        if (failedReason != null) {
            failedReason = "The cause of the problem is " + failedReason;
            pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/no_serv_try_later.wav\"/>");
            pw.println(failedReason);
        }
        else {
            int total = email_agent.totalMessages();
            if (total == 0) {
                pw.println("<audio src=\"/audio/${GW_RECORDED_VOICE}/u_have_no_msg.wav\"/>");
            }
            else {
                pw.println("<audio src=\"/audio/${GW_RECORDED_VOICE}/u_have.wav\"/>");
                pw.println(Integer.toString(total));
                pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/msgs.wav\"/>");
                pw.println("<audio src=\"audio/${GW_RECORDED_VOICE}/first_msg.wav\"/>");
                pw.println("<var name=\"index\" expr=\"0\"/>");
                pw.println("<submit next=\"email_msg\" namelist=\"index\"/>");
            }
        }
        
        pw.println("  </block>");
        pw.println("</form>");

        pw.println("</vxml>");
    }
}
