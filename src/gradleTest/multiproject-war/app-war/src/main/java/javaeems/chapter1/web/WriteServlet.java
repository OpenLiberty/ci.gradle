package javaeems.chapter1.web;

import java.io.IOException;
import javaeems.chapter1.model.MessageException;
import javaeems.chapter1.model.ModelEJB;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet(name = "WriteServlet", urlPatterns = {"/WriteServlet"})
public class WriteServlet extends HttpServlet {
    @EJB
    private ModelEJB helloEJB;
    private static String PUT_MESSAGE = "put_message";

    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String message = request.getParameter(PUT_MESSAGE);
        if ("".equals(message)) {
            helloEJB.deleteMessage();
        } else {
            try {
                helloEJB.putUserMessage(message);
            } catch (MessageException nme) {
                throw new ServletException(nme);
            }    
        } 
        response.sendRedirect("./DisplayServlet"); 
    }
    
}