package io.github.tortaje.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.tortaje.compare.engine.Format;
import org.junit.jupiter.api.Test;

class CellTextTest {
    @Test void preUnwrapped() {   // &amp; decoded last: "&amp;lt;" becomes the literal "&lt;"
        assertEquals("<a x=\"1\">&lt;</a>",
                CellText.normalize(" <pre>&lt;a x=&quot;1&quot;&gt;&amp;lt;&lt;/a&gt;</pre> ", Format.XML));
        assertEquals("{\"a\":\"<b>\"}",
                CellText.normalize("<pre>{&quot;a&quot;:&quot;&lt;b&gt;&quot;}</pre>", Format.JSON));
    }
    @Test void plainUnchanged() { assertEquals("{\"a\":1}", CellText.normalize("{\"a\":1}", Format.JSON)); }
    @Test void nullStaysNull() { assertNull(CellText.normalize(null, Format.JSON)); }
    @Test void xmlWhosePreRootHoldsMarkupIsKept() {
        assertEquals("<pre><b>x</b></pre>", CellText.normalize("<pre><b>x</b></pre>", Format.XML));
    }
    @Test void xmlWhosePreRootHoldsTextIsKept() {
        assertEquals("<pre>a &amp; b</pre>", CellText.normalize("<pre>a &amp; b</pre>", Format.XML));
    }
}
