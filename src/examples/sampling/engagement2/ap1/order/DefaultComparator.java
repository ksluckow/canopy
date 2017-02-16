package sampling.engagement2.ap1.order;

import java.util.*;

public class DefaultComparator<T extends Comparable<? super T>> implements Comparator<T>
{
    public static final DefaultComparator<String> STRING;
    
    @Override
    public int compare(final T object1, final T object2) {
        return object1.compareTo(object2);
    }
    
    static {
        STRING = new DefaultComparator<String>();
    }
}