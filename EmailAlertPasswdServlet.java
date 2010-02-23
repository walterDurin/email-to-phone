package vx.apps.email;

import java.io.PrintWriter;
import java.io.IOException;
import javax.servlet.http.*;


public class EmailAlertPasswdServlet extends HttpServlet
{
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        PrintWriter pw = response.getWriter();
        
        pw.println("<?xml version=\"1.0\"?>");
        pw.println("<vxml version=\"1.0\">");

        pw.println("<form id=\"emailalertpasswd\">");
        pw.println("  <dtmf>");
        pw.println("    passwd = $digits");
        pw.println("  </dtmf>");
        
        pw.println("<initial name=\"sysprompt\">");
        pw.println("  <prompt timeout=\"0s\">");
        pw.println("  </prompt>");
        pw.println("  <noinput>");
        pw.println("    <assign name=\"sysprompt\" expr=\"'toproc'\"/>");
        pw.println("  </noinput>");
        pw.println("</initial>");
        
        pw.println("<block>");
        pw.println("  <submit next=\"/password.jsp\"/>");
        pw.println("</block>");
        
        pw.println("<filled namelist=\"passwd\">");
        pw.println("  <submit next=\"/authenticate.jsp\" namelist=\"passwd\"/>");
        pw.println("</filled>");

        pw.println("</form>");
        pw.println("</vxml>");
    }
}
