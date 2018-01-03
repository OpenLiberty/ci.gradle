package application.servlet;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

/**
 * A servlet which uses JPA to persist data.
 */
@WebServlet(urlPatterns="/")
public class JPAServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    /**
     * The JNDI name for the persistence context is the one defined in web.xml
     */
    private static final String JNDI_NAME = "java:comp/env/jpasample/entitymanager";

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println("Hello JPA World");

        try {
            // First create a Thing in the database, then retrieve it
            createThing(writer);
            retrieveThing(writer);
        } catch (Exception e) {
            writer.println("Something went wrong. Caught exception " + e);
        }

    }

    public void createThing(PrintWriter writer) throws NamingException, NotSupportedException, SystemException, IllegalStateException, SecurityException, HeuristicMixedException, HeuristicRollbackException, RollbackException {
    	Context ctx = new InitialContext();
        // Before getting an EntityManager, start a global transaction
        UserTransaction tran = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
        tran.begin();

        // Now get the EntityManager from JNDI
        EntityManager em = (EntityManager) ctx.lookup(JNDI_NAME);
        writer.println("Creating a brand new Thing with " + em.getDelegate().getClass());

        // Create a Thing object and persist it to the database
        Thing thing = new Thing();
        em.persist(thing);

        // Commit the transaction 
        tran.commit();
        int id = thing.getId();
        writer.println("Created Thing: " + thing);
    }

    @SuppressWarnings("unchecked")
    public void retrieveThing(PrintWriter writer) throws SystemException, NamingException {
        // Look up the EntityManager in JNDI
        Context ctx = new InitialContext();
        EntityManager em = (EntityManager) ctx.lookup(JNDI_NAME);
        // Compose a JPQL query
        String query = "SELECT t FROM Thing t";
        Query q = em.createQuery(query);

        // Execute the query
        List<Thing> things = q.getResultList();
        writer.println("Query returned " + things.size() + " things");

        // Let's see what we got back!
        for (Thing thing : things) {
            writer.println("Thing in list " + thing);
        }
    }
}
