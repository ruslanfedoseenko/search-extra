package org.wikimedia.search.extra.regex.expression;

import org.junit.Test;
import org.wikimedia.search.extra.regex.ngram.NGramExtractor;

import static org.junit.Assert.*;

import java.util.Locale;

import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.RegExp;

public class ExpressionTest {
    private final Leaf<String> foo = new Leaf<>("foo");
    private final Leaf<String> bar = new Leaf<>("bar");
    private final Leaf<String> baz = new Leaf<>("baz");

    @Test
    public void simple() {
        assertEquals(True.instance(), True.instance());
        assertEquals(True.instance().hashCode(), True.instance().hashCode());
        assertEquals(False.instance(), False.instance());
        assertEquals(False.instance().hashCode(), False.instance().hashCode());
        assertNotEquals(True.instance(), False.instance());
        assertNotEquals(False.instance(), True.instance());

        Leaf<String> leaf = new Leaf<>("foo");
        assertEquals(leaf, leaf);
        assertNotEquals(True.instance(), leaf);
        assertNotEquals(False.instance(), leaf);
    }

    @Test
    public void extract() {
        assertEquals(new And<>(foo, new Or<>(bar, baz)),
                new Or<>(
                        new And<>(foo, bar),
                        new And<>(foo, baz)
                ).simplify());
    }

    @Test
    public void extractToEmpty() {
        assertEquals(new And<>(foo, bar),
                new Or<>(
                        new And<>(foo, bar),
                        new And<>(foo, bar, baz)
                ).simplify());
    }

    @Test
    public void extractSingle() {
        assertEquals(foo,
                new Or<>(
                        new And<>(foo, bar),
                        foo
                ).simplify());
    }

    @Test
    public void extractDuplicates() {
        assertEquals(foo, new Or<>(foo, new And<>(foo)).simplify());
    }

    @Test
    public void testDegradedDisjunction() {
        String regex = "[ab]*a[cd]{50,80}";
        int maxExpand = 4;
        int maxStatesTraced = 10000;
        int maxDeterminizedStates = 20000;
        int maxNgramsExtracted = 100;

        Automaton automaton = new RegExp(regex.toLowerCase(Locale.ENGLISH), RegExp.ALL ^ RegExp.AUTOMATON).toAutomaton(maxDeterminizedStates);
        NGramExtractor extractor = new NGramExtractor(3, maxExpand, maxStatesTraced, maxNgramsExtracted);

        Expression<String> expression = extractor.extract(automaton);
        assertTrue(expression.countClauses() > 1024);
        expression = new ExpressionRewriter<>(expression).degradeAsDisjunction();
        assertTrue(expression.getClass() == Or.class);
        assertFalse(expression.alwaysFalse());
        assertFalse(expression.alwaysTrue());
        assertTrue(expression.countClauses() <= maxNgramsExtracted);
    }
}
