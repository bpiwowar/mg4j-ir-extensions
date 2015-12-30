/**
 *
 */
package net.bpiwowar.mg4j.extensions.utils;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author bpiwowar
 * @date 09/01/2007
 */
final public class DefaultMap<K extends Object, V extends Object> implements Map<K, V>, Serializable {

    private static final long serialVersionUID = -6783599054805824583L;
    final private Map<K, V> map;
    final private Constructor<V> valueConstructor;

    public DefaultMap(final AbstractMap<K, V> map, final Constructor<V> valueConstructor) {
        this.map = map;
        this.valueConstructor = valueConstructor;
    }

    /**
     * @param mapClass   The map class
     * @param valueClass The value class
     */
    @SuppressWarnings("unchecked")
    public DefaultMap(Class<? extends Map> mapClass, Class<V> valueClass) {
        try {
            map = mapClass.newInstance();
            valueConstructor = valueClass.getConstructor();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        map.clear();
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(final Object arg0) {
        return map.containsValue(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(javaObject.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public V get(final Object arg0) {
        final K key = (K) arg0;
        V v = map.get(arg0);
        if (v != null) return v;
        try {
            v = valueConstructor.newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        map.put(key, v);
        return v;
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set<K> keySet() {
        return map.keySet();
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public V put(final K arg0, final V arg1) {
        return map.put(arg0, arg1);
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(final Map<? extends K, ? extends V> arg0) {
        map.putAll(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#remove(java.lang.Object)
     */
    public V remove(final Object arg0) {
        return map.remove(arg0);
    }

    /* (non-Javadoc)
     * @see java.util.Map#size()
     */
    public int size() {
        return map.size();
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection<V> values() {
        return map.values();
    }

}
