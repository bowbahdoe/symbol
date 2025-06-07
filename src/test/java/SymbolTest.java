import dev.mccue.symbol.Symbol;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class SymbolTest {
    @Test
    public void testSymbolCreation() {
        var s1 = Symbol.of("ABC");
        var s2 = Symbol.of(new String(new char[]{'A', 'B', 'C'}));
        assertSame(s1, s2);
        assertEquals(s1, s2);
    }

    @Test
    public void testSymbolCreationMultiThreaded() throws InterruptedException {
        var m = Collections.synchronizedSet(
                Collections.newSetFromMap(new IdentityHashMap<>())
        );

        var threads = new ArrayList<Thread>();

        for (int i = 0; i < 100; i++) {
            var t = new Thread(() -> {
                try {
                    Thread.sleep(100);
                    var s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                    s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                    s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                    s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                    s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                    s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                    s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                    s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                    s = Symbol.of(new String(new char[]{'A', 'B'}));
                    m.add(s);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            threads.add(t);
            t.start();
        }

        for (var t : threads) {
            t.join();
        }

        assertEquals(1, m.size());
    }
}
