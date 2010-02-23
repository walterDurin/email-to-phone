package vx.apps.email;

import java.io.*;
import javax.servlet.http.*;


public class LogFileServlet extends HttpServlet
{
    public void service(HttpServletRequest request, HttpServletResponse response)
        throws IOException
    {
        PrintWriter w = response.getWriter();
        
        w.println("<html>");
        w.println("<head>");
        w.println("<meta http-equiv=\"Content-Language\" content=\"en-us\">");
        w.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=windows-1252\">");
        w.println("<title>EmailToPhone - Log File</title>");
        w.println("</head>");
        w.println("<body>");
        w.println("<pre>");
        
        String appdir = getServletContext().getRealPath("..");
        File f = new File(appdir + "/email.log");
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        String line;
        while ((line = br.readLine()) != null)
            w.println(line);
        br.close();
        fr.close(); 

        w.println("</pre>");
        w.println("</body>");
        w.println("</html>");
    }
}
