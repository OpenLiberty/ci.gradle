package javaeems.chapter1.model;

import static org.junit.Assert.*;

import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ModelEJBTest {

	private ModelEJB ejb;
	
	private EntityManagerFactory emf;
	
	@Before
	public void setUp() {
		ejb = new ModelEJB();
		emf = Persistence.createEntityManagerFactory("ejb-tests-pu");
		ejb.em = emf.createEntityManager();
	}
	
	@After
	public void tearDown() {
		if (ejb.em != null) {
			EntityTransaction tx = ejb.em.getTransaction();
			try {
				tx.begin();
				ejb.em.createQuery("delete from Message").executeUpdate();
			} finally {
				tx.commit();
			}
			ejb.em.close();
		}
		if (emf != null) {
			emf.close();
		}
	}

	@Test(expected=MessageException.class)
	public void testNothingInDB() throws MessageException {
		ejb.getStoredMessage();
	}

	@Test
	public void testSetAndGet() throws MessageException {
		EntityTransaction tx = ejb.em.getTransaction();
		try {
			tx.begin();
			ejb.putUserMessage("hello");
			ejb.putUserMessage("some statistically improbable phrase");
		} finally {
			tx.commit();
		}
		long numEntries = (long) ejb.em.createQuery("select count(m) from Message m").getSingleResult();
		assertEquals(1, numEntries);
		String message = ejb.getStoredMessage();
		assertTrue(message.contains("some statistically improbable phrase"));
	}

	@Test
	public void testSetAndDelete() throws MessageException {
		assertEquals(0, (long) ejb.em.createQuery("select count(m) from Message m").getSingleResult());

		EntityTransaction tx = ejb.em.getTransaction();
		try {
			tx.begin();
			ejb.putUserMessage("hello");
		} finally {
			tx.commit();
		}
		assertEquals(1, (long) ejb.em.createQuery("select count(m) from Message m").getSingleResult());
		
		try {
			tx.begin();
			ejb.deleteMessage();
		} finally {
			tx.commit();
		}
		assertEquals(0, (long) ejb.em.createQuery("select count(m) from Message m").getSingleResult());
	}
}
