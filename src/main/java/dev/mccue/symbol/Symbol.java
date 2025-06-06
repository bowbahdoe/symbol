package dev.mccue.symbol;

import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/// Clone of the Scala "[Symbol](https://github.com/scala/scala/blob/v2.13.6/src/library/scala/Symbol.scala)" class.
///
/// The original Scala docs are as follows
///
/// > This class provides a simple way to get unique objects for equal strings.
/// > Since symbols are interned, they can be compared using reference equality.
/// > Instances of `Symbol` can be created easily with Scala's built-in quote
/// > mechanism.
///
/// The differences here are:
///
/// 1. This class makes no guarantees about reference equality. In fact, assume
/// that instances of Symbol are value-based and will be converted to value
/// classes in a future version.
/// 2. No equivalent of "unapply" is provided. When Java has member patterns
/// a `Symbol.of(String name)` pattern will be added.
public final class Symbol implements Serializable {
    private final String name;

    Symbol(String name) {
        this.name = name;
    }

    /// Returns the symbol for the given name.
    ///
    /// The value returned from this method will be equivalent
    /// for all strings that contain the same contents.
    ///
    /// (Absent ClassLoader shenanigans).
    ///
    /// @param name The name of the symbol.
    /// @return The {@link Symbol} for that name
    public static Symbol of(String name) {
        return SymbolCache.INSTANCE.apply(name);
    }

    /// Returns the name of the symbol.
    /// @return The name of the symbol.
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "Symbol[" + name + "]";
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Serial
    private Object readResolve() throws ObjectStreamException  {
        return Symbol.of(name);
    }
}

/// This absolutely batshit implementation with the abstract class
/// is translated from the Scala one.
///
/// I probably could have just translated it to some static final fields
/// and a meatier `Symbol.of` implementation, but this is more fun.
final class SymbolCache extends UniquenessCache<String, Symbol> {
    static final SymbolCache INSTANCE = new SymbolCache();

    @Override
    protected Symbol valueFromKey(String s) {
        return new Symbol(s);
    }
}

abstract class UniquenessCache<K, V> {
    ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    ReentrantReadWriteLock.ReadLock rlock = rwl.readLock();
    ReentrantReadWriteLock.WriteLock wlock = rwl.writeLock();
    WeakHashMap<K, WeakReference<V>> map = new WeakHashMap<>();

    protected abstract V valueFromKey(K k);

    V apply(K name) {
        Supplier<V> cached = () -> {
            rlock.lock();
            try {
                var reference = map.get(name);
                if (reference == null) {
                    return null;
                }
                else {
                    return reference.get();
                }
            }
            finally {
                rlock.unlock();;
            }
        };
        Supplier<V> updateCache = () -> {
            wlock.lock();
            try {
                var res = cached.get();
                if (res != null) {
                    return res;
                }
                else {
                    // If we don't remove the old String key from the map, we can
                    // wind up with one String as the key and a different String as
                    // the name field in the Symbol, which can lead to surprising GC
                    // behavior and duplicate Symbols. See scala/bug#6706.
                    map.remove(name);
                    var sym = valueFromKey(name);
                    map.put(name, new WeakReference<>(sym));
                    return sym;
                }
            } finally {
                wlock.unlock();
            }
        };

        var res = cached.get();
        if (res == null) {
            return updateCache.get();
        }
        else {
            return res;
        }
    }

}

