package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;


public class EmailDelServlet extends HttpServlet
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
        int index = Integer.parseInt(indexstr);
        email_agent.delete(index);

        pw.println("<form id=\"reademail\">");
        pw.println("  <block>");
        pw.println("    <audio src=\"audio/${GW_RECORDED_VOICE}/msg_deleted.wav\"/>");
        pw.println("    <var name=\"index\" expr=\"" + indexstr + "\"/>");
        pw.println("    <submit next=\"email_msg\" namelist=\"index\"/>");
        pw.println("  </block>");
        pw.println("</form>");
        pw.println("</vxml>");
    }
}
