package io.openliberty.test.ejb;

import io.openliberty.test.lib.LibraryService;
import javax.ejb.Stateless;

@Stateless
public class TransitiveTestBean {
    public void testMethod() {
        LibraryService service = new LibraryService();
        service.processEntity();
    }
}
