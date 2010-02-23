package vx.apps.email;

import java.io.*;
import javax.servlet.http.*;
import java.util.Properties;

import vx.util.Base64;


public class EmailSetupServlet extends HttpServlet
{
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        Properties props = new Properties();
        String appdir = getServletContext().getRealPath("..");
        File propspath = new File(new File(appdir), "conf/emailsetup.conf");
        if (propspath.exists()) { 
            FileInputStream propsFile = new FileInputStream(propspath);
            props.load(propsFile);
            propsFile.close();
        }
    
        String msg = "";

        String act = request.getParameter("action");
        if ("save".equals(act)) {
            String newpassword = request.getParameter("password");
            if (newpassword != null && newpassword.length() > 0) {
                String passwd = Base64.encode(newpassword);
                props.setProperty("readpassword", passwd);
            }
            else {
                String encpasswd = request.getParameter("encpassword");
                if (encpasswd != null && encpasswd.length() > 0)
                    props.setProperty("readpassword", encpasswd);
            }

            String readproto = request.getParameter("readproto");
            if (readproto != null)
                readproto = readproto.toLowerCase();
            props.setProperty("readproto", readproto); 
            
            props.setProperty("readuser", getValue("readuser", request));
            props.setProperty("readhost", getValue("readhost", request));
            props.setProperty("readport", getValue("readport", request));
            props.setProperty("readusessl", getValue("readusessl", request));
            props.setProperty("sendhost", getValue("sendhost", request));
            props.setProperty("sendport", getValue("sendport", request));
            props.setProperty("senduseauth", getValue("senduseauth", request));
            props.setProperty("sendusessl", getValue("sendusessl", request));
            props.setProperty("aphone", getValue("aphone", request));
            props.setProperty("polltime", getValue("polltime", request));
            props.setProperty("sockshost", getValue("sockshost", request));
            props.setProperty("socksport", getValue("socksport", request));
            props.setProperty("socksnoproxyhosts", getValue("socksnoproxyhosts", request));
            props.setProperty("alertfrom", getValue("alertfrom", request));
            props.setProperty("alertto", getValue("alertto", request));

            // save
            FileOutputStream fos = new FileOutputStream(propspath);
            props.save(fos, "emailtophone setup");
            fos.close();
            msg = "Setting saved. Please restart Voicent Gateway.";
        }

        String readproto = props.getProperty("readproto");
        if (readproto == null)
            readproto = "pop3";
        else {
            readproto = readproto.toLowerCase();
            if (! readproto.equals("imap"))
                readproto = "pop3";
        }
        
        String readuser = props.getProperty("readuser", "");
        String encpasswd = props.getProperty("readpassword");
        String readhost = props.getProperty("readhost", "");
        String readport = props.getProperty("readport", "110");
        String readusessl = props.getProperty("readusessl", "");
        String sendhost = props.getProperty("sendhost", "");
        String sendport = props.getProperty("sendport", "25");
        String senduseauth = props.getProperty("senduseauth");
        String sendusessl = props.getProperty("sendusessl");
        String aphone = props.getProperty("aphone", "");
        String polltime = props.getProperty("polltime", "");
        String alertfrom = props.getProperty("alertfrom", "");
        String alertto = props.getProperty("alertto", "");
        String sockshost = props.getProperty("sockshost", "");
        String socksport = props.getProperty("socksport", "");
        String socksnoproxyhosts = props.getProperty("socksnoproxyhosts", "");
        
        PrintWriter w = response.getWriter();

        w.println("<html>");
        w.println("<head>");
        w.println("<meta http-equiv=\"Content-Language\" content=\"en-us\">");
        w.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">");
        w.println("<title>EmailToPhone - Setup</title>");
        w.println("</head>");

        w.println("<body>");
        
        w.println("<p>");
        w.println("<font color=red>" + msg + "</font>");

        w.println("<h3 align=\"center\"><u><font color=\"#FF0000\">Email to Phone - Setup</font></u></h3>");
        w.println("<form action=\"emailsetup\" method=\"post\">");
        w.println("<input type=hidden name=encpassword value=\"" + encpasswd + "\">");
        w.println("<table border=\"0\" cellpadding=\"0\" cellspacing=\"1\" bordercolor=\"#111111\" width=\"80%\" bgcolor=\"#000080\">");
        w.println("  <tr>");
        w.println("    <td>");
        w.println("    <table border=\"1\" cellpadding=\"0\" cellspacing=\"8\" bordercolor=\"#111111\" width=\"100%\" bgcolor=\"#FFFFD7\" style=\"border-collapse: collapse\">");
        w.println("      <tr>");
        w.println("        <td width=\"100%\" height=\"22\" bgcolor=\"#808000\" colspan=\"2\">");
        w.println("        <font color=\"#FFFFFF\">Setup Email Account</font></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Email Address</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"readuser\" value=\"" + readuser + "\" size=\"36\" tabindex=\"1\"></td>");
        w.println("      </tr>");        
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Password</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"password\" name=\"password\" size=\"24\" tabindex=\"2\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Incoming mail server</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"readhost\"  value=\"" + readhost + "\" size=\"36\" tabindex=\"3\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Incoming mail protocol (POP3)</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"readproto\" value=\"" + readproto + "\" size=\"36\" tabindex=\"3\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Incoming mail port (default 110)</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"readport\"  value=\"" + readport + "\" size=\"10\" tabindex=\"4\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Incoming Use SSL</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.print("        <input type=\"checkbox\" name=\"readusessl\" value=\"1\"");
        if ("1".equals(readusessl))
            w.print(" checked");
        w.println("></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Outgoing mail server (SMTP)</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"sendhost\"  value=\"" + sendhost + "\" size=\"36\" tabindex=\"5\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">SMTP port (default 25)</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"sendport\"  value=\"" + sendport + "\" size=\"10\" tabindex=\"6\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Send Use Authentication</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.print("        <input type=\"checkbox\" name=\"senduseauth\" value=\"1\"");
        if ("1".equals(senduseauth))
            w.print(" checked");
        w.println("></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Send Use SSL</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.print("        <input type=\"checkbox\" name=\"sendusessl\" value=\"1\"");
        if ("1".equals(sendusessl))
            w.print(" checked");
        w.println("></td>");
        w.println("      </tr>");
        w.println("      </table>");
        w.println("    </td>");
        w.println("  </tr>");
        w.println("</table>");
        w.println("<p></p>");
        w.println("<table border=\"0\" cellpadding=\"0\" cellspacing=\"1\" bordercolor=\"#111111\" width=\"80%\" bgcolor=\"#000080\">");
        w.println("  <tr>");
        w.println("    <td>");
        w.println("    <table border=\"1\" cellpadding=\"0\" cellspacing=\"8\" bordercolor=\"#111111\" width=\"100%\" bgcolor=\"#FFFFD7\" style=\"border-collapse: collapse\">");
        w.println("      <tr>");
        w.println("        <td width=\"100%\" height=\"22\" bgcolor=\"#808000\" colspan=\"2\">");
        w.println("        <font color=\"#FFFFFF\">Setup email alert over the phone (not available in shareware version)");
        w.println("        </font></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Alert Phone Number</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"aphone\"  value=\"" + aphone + "\" size=\"36\" tabindex=\"10\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Check email every</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"polltime\"  value=\"" + polltime + "\" size=\"3\" tabindex=\"11\"> Minutes</td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Alert me if email is sent FROM<p>(One email address per line)</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <textarea rows=\"8\" name=\"alertfrom\" cols=\"36\">" + alertfrom + "</textarea></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">Alert me if email is sent TO<p>(One email address per line)</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <textarea rows=\"8\" name=\"alertto\" cols=\"36\">" + alertto + "</textarea></td>");
        w.println("      </tr>");
        w.println("    </table>");
        w.println("    </td>");
        w.println("  </tr>");
        w.println("</table>");
        w.println("<br>");
        w.println("<table border=\"0\" cellpadding=\"0\" cellspacing=\"1\" bordercolor=\"#111111\" width=\"80%\" bgcolor=\"#000080\">");
        w.println("  <tr>");
        w.println("    <td>");
        w.println("    <table border=\"1\" cellpadding=\"0\" cellspacing=\"8\" bordercolor=\"#111111\" width=\"100%\" bgcolor=\"#FFFFD7\" style=\"border-collapse: collapse\">");
        w.println("      <tr>");
        w.println("        <td width=\"100%\" height=\"22\" bgcolor=\"#808000\" colspan=\"2\">");
        w.println("        <font color=\"#FFFFFF\">Other Options (leave it blank if you don't understand what they are)</font></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">SocksProxyHost</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"sockshost\" value=\"" + sockshost + "\" size=\"24\" tabindex=\"12\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">SocksProxyPort</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"socksport\" value=\"" + socksport + "\" size=\"10\" tabindex=\"13\"></td>");
        w.println("      </tr>");
        w.println("      <tr>");
        w.println("        <td width=\"44%\" height=\"22\" bgcolor=\"#FFFFD7\">SocksNoProxyHosts</td>");
        w.println("        <td width=\"56%\" height=\"22\" bgcolor=\"#FFFFD7\">");
        w.println("        <input type=\"text\" name=\"socksnoproxyhosts\" value=\"" + socksnoproxyhosts + "\" size=\"36\" tabindex=\"14\"></td>");
        w.println("      </tr>");
        w.println("    </table>");
        w.println("   </td>");
        w.println("  </tr>");
        w.println("</table>");
        w.println("<br>");
        w.println("<input type=hidden name=action value=\"save\">");
        w.println("<input type=submit name=submit value=\"Save Settings\">");
        w.println("</form>");

        w.println("<p><a href=\"emailtophone\">Go back</a></p>");

        w.println("</body>");
        w.println("</html>");
    }
    
    private String getValue(String name, HttpServletRequest request)
    {
        String v = request.getParameter(name);
        if (v == null)
            return "";
        return v;
    }
}
