package vx.apps.email;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;



public class EmailLoaderServlet extends HttpServlet
{
    public void init(ServletConfig config)
        throws ServletException
    {
        super.init(config);

        ServletContext context = getServletContext();
        try {
            String appdir = context.getRealPath("../conf");
            context.log("EmailToPhone application dir: " + appdir);
            
            File dir = new File(appdir);
            String dirpath = dir.getAbsolutePath();
            
            EmailManager mgr = new EmailManager();
            mgr.init(dirpath);
            
            context.log("EmailLoaderServlet succeeded");
        }
        catch (Exception e) {
            context.log("EmailLoaderServlet failed: " + e.toString());
            throw new ServletException();
        }
    }
}

