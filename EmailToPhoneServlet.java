package vx.apps.email;

import java.io.*;
import javax.servlet.http.*;


public class EmailToPhoneServlet extends HttpServlet
{
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        PrintWriter w = response.getWriter();
        
        w.println("<html>");
        w.println("<head>");
        w.println("<meta http-equiv=\"Content-Language\" content=\"en-us\">");
        w.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">");
        w.println("<title>EmailToPhone</title>");
        w.println("</head>");

        w.println("<h1 align=\"center\"><u><font color=\"#000080\">Email<font size=5>TO</font>Phone</font></u></h1>");

        w.println("<h3 align=center><i>");
        w.println("<font color=\"#B5B5FF\">Welcome to your phone gateway for your email.</font></i></h3>");
        
        EmailManager mgr = EmailManager.instance_;
        String op = request.getParameter("action");
        String opmsg = "";
        if (op != null && op.length() > 0) {
            if (op.equals("on"))
                opmsg = mgr.startAlertThread();
            else
                opmsg = mgr.stopAlertThread();
        }
    
        w.println("<hr>");
        w.println("<ul>");
        w.println("<li><p align=left>");
        
        if (mgr == null)
            w.println("EmailToPhone is not running. Please check your setup and log file");
        else {
            if (mgr.isAlertStopped())
                w.println("<font color = \"#0000FF\">Alert is off. Click here to " +
                "<a href=\"emailtophone?action=on\">Turn Alert On</a>.");
            else
                w.println("<font color = \"#0000FF\">Alert is on. Click here to " +
                "<a href=\"emailtophone?action=off\">Turn Alert Off</a>.");
        }

        w.println("<br>");
        w.println("<font color=red>" + opmsg + "</font>");

        w.println("</li>");
        w.println("<li>");
        w.println("<p align=left>");
        w.println("<a href=\"logfile\">Log File</a></li>");
        w.println("<li>");

        w.println("<p align=left><a href=\"emailsetup\">Setup</a></li>");
        w.println("</ul>");
        w.println("<hr>");
        w.println("<p>&nbsp;</p>");
        w.println("<p>&nbsp;</p>");

        w.println("</body>");
        w.println("</html>");
    }
}

