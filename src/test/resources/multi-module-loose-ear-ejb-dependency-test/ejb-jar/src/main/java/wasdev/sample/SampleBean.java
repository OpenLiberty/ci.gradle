package wasdev.sample;

import javax.ejb.Stateless;

@Stateless
public class SampleBean {
    
    public DataBean getData() {
        return new DataBean("test");
    }
}
