package org.bhavaya.util;

/**
 * Support for dynamic properties. Use {@link Generic#getType(Object)} to get an instance of {@link DynamicObjectType}.
 * Call {@link DynamicObjectType#addAttribute}/{@link DynamicObjectType#removeAttribute} methods to add/remove attributes to the type.
 * They will immediately become available in table views.
 * <pre>
 * Example:
 *   DynamicObjectType type = (DynamicObjectType) Generic.getType(MyTypeWhichImplementsDynamicObservable.class);
 *   type.addAttribute(new DynamicAttribute("myProperty", String.class));
 * </pre>
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.2 $
 */
public interface DynamicObservable extends Observable {

    public void set(String propertyName, Object value);

    public Object get(String property);

}
