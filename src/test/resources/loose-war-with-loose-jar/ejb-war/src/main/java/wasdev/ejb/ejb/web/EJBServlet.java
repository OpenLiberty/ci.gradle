package wasdev.ejb.ejb.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import wasdev.ejb.ejb.SampleStatelessBean;

/**
 * A servlet which injects a stateless EJB
 */
@WebServlet({"/", "/ejbServlet"})
public class EJBServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    SampleStatelessBean statelessBean;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();

        // Call hello method on a stateless session bean
        String message = statelessBean.hello();

        writer.println(message);
    }
}
