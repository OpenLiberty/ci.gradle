package io.openliberty.test.ejb;

import io.openliberty.test.lib.LibraryService;
import org.apache.commons.lang3.StringUtils;
import javax.ejb.Stateless;

@Stateless
public class MixedDependencyBean {
    public void testMethod() {
        LibraryService service = new LibraryService();
        String message = service.getMessage();
        // Use commons-lang3 to demonstrate external JAR dependency
        String capitalized = StringUtils.capitalize(message);
    }
}
