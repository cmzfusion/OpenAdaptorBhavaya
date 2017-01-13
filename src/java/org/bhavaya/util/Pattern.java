/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.util;

import java.util.*;

/**
 * @author Philip Milne
 */

/**
 *  The pattern class is analogous to the Pattern class which predates this
 *  class and is in the java.util.regex package of the SDK. It is used in the
 *  Bhavaya tool kit for performing lexicographical analysis of character streams.
 *  Advantages and disadvantages of the two implementation techniques are
 *  discussed below.
 *  <p/>
 *  A pattern is best thought of as defining a (potentially infinite)
 *  set of strings. A string belongs to the set if and only if the pattern matches
 *  it. A set of strings may or may not contain the empty string and the
 *  matchesEmptyString() method may be used to distinguish
 *  these two cases. This distinction is critical to the "append" operation
 *  which may be defined as a replacement of all by sub-patterns of a given
 *  pattern which match the empty string and unioning them with a second,
 *  given, pattern.
 *  <p/>
 *  The static EMPTY_STRING pattern describes a set with one element: the
 *  empty string. It can be unioned with patterns that do not accept the
 *  empty string to make them optional. For example, an integer
 *  sometimes starts with a "+" or a "-" but needn't have either character
 *  as a prefix. The pattern union(union('+', '-'), EMPTY_STRING) is a
 *  suitable pattern to match the start of an integer.
 *  <p/>
 *  Advantages over regex patterns. The main advantage of this scheme over
 *  traditional regular expression automata is its speed (in our application
 *  tokenisation of a string inputs is 5x - 10x faster than an older
 *  technique using (grouped) regular expressions). As of,
 *  1.4.1 the regex package also had a number of show-stopping bugs.
 *  <p/>
 *  The speed of the lexicographic analysis is due to the fact that the
 *  resulting automata are essentially just arrays of arrays. Each character
 *  taken from an input sequence is handled with what is essentially a
 *  single array lookup. By
 *  contrast, the engine used by the regex package has a collection of nodes
 *  of different types which embody the original rules with which it was built.
 *  Patterns interpreted with traditional engines become slower as more rules
 *  are supplied to them - where these automata take a constant time
 *  to handle each input character in all cases.
 *  <p/>
 *  Disadvantages over regex patterns. There is no way to find out any information
 *  on which subpatterns were involved in a particular match - in the way that
 *  groups may be used with the traditional automata.
 *  There are no facilites to distuinguish greedy and reluctant qualifiers.
 *  All patterns are greedy in this scheme though the need for
 *  qualifiers to control branching is somewhat obviated by the
 *  precise treatment the empty string - which is handled correctly by
 *  all of the set-theoretical operators on Patterns.
 *  The current implementation works only on bytes and does not support
 *  unicode characters correctly.
 **/
public abstract class Pattern {
    /** A pattern matching empty string. */
    public static final Pattern EMPTY_STRING = new DefaultPattern(true);
    public static final Pattern ANY_CHARACTER = range((char) 0, (char) 255);

    public static final Pattern JAVA_STRING = string('"', '\\', '"');
    public static final Pattern SQL_STRING = string('\'', '\'', '\'');
    public static final Pattern STRING = union(SQL_STRING, JAVA_STRING);

    public static final Pattern SIGN = union(pattern('-'), pattern('+'));
    public static final Pattern DIGIT = range('0', '9');
    public static final Pattern HEX_DIGIT = union(DIGIT, union(range('a', 'f'), range('A', 'F')));
    public static final Pattern HEXADECIMAL = append(pattern("0x"), repeat(HEX_DIGIT));
    public static final Pattern UNSIGNED_INTEGER = repeat(DIGIT);
    public static final Pattern SIGNED_INTEGER = append(SIGN, UNSIGNED_INTEGER);
    public static final Pattern INTEGER = union(UNSIGNED_INTEGER, SIGNED_INTEGER);
    public static final Pattern DECIMAL = append(INTEGER, append('.', UNSIGNED_INTEGER));
    public static final Pattern DECIMAL_WITH_EXPONENT = append(DECIMAL, append('e', INTEGER));
    public static final Pattern FLOAT = append(DECIMAL_WITH_EXPONENT, pattern('f'));
    public static final Pattern DOUBLE = append(DECIMAL_WITH_EXPONENT, pattern('d'));

    public static final Pattern LETTER = union(range('a', 'z'), range('A', 'Z'));
    public static final Pattern WORD = repeat(LETTER);

    // note sql can contain _ % are valid identifier chars
    // note @ as the first char may be considered seperate from identifier as it is used to denote variables, but we dont
    // note sql can contain . * # $ & are valid identifier chars (not first char)
    // note . and * are used in compound table/column name definitions e.g tableA.* means all columns in tableA, you could consider these as seperate but we dont
    public static final Pattern IDENTIFIER = append(union(union("_@%"), LETTER), repeat(union(new Pattern[]{union("_@%.*#$&"), LETTER, DIGIT})));

    public static final Pattern WHITE_SPACE_CHARACTER = union(" \t\f\13"); // \13 is 0x13 (char 11) is vertical tab.
    public static final Pattern WHITE_SPACE = repeat(WHITE_SPACE_CHARACTER);

    public static final Pattern NEW_LINE = union(new Pattern[]{pattern('\n'), // newline char
                                                                       pattern("\r\n"), //carriage-return char followed immediately by a newline char
                                                                       pattern('\r'), // carriage-return char
                                                                       pattern('\u0085'), // next-line char
                                                                       pattern('\u2028'), // line-separator char
                                                                       pattern('\u2029'), // paragraph-separator char
    });


    public static final Pattern COMPARATOR = union(new Pattern[]{pattern("=="), pattern("<>"), pattern("<="), pattern(">="), pattern("!=")});
    public static final Pattern SQL_OPERATOR = union(new Pattern[]{COMPARATOR, pattern('='), pattern("*="), pattern("=*")});

    public static final Pattern TOKEN = union(new Pattern[]{ANY_CHARACTER, IDENTIFIER, COMPARATOR, SQL_OPERATOR, STRING, WHITE_SPACE, FLOAT, DOUBLE, HEXADECIMAL, DECIMAL_WITH_EXPONENT});

    private static Pattern string(char quote, char escape1, char escape2) {
        Pattern escapeSequence = append(escape1, pattern(escape2));
        return append(quote, append(repeat(union(setdiff(ANY_CHARACTER, pattern(escape1)), escapeSequence)), pattern(quote)));
    }

    public static Pattern pattern(char c) {
        return append(c, EMPTY_STRING);
    }

    public static Pattern range(char c1, char c2) {
        DefaultPattern result = new DefaultPattern(false);
        result.addTransition(c1, c2, EMPTY_STRING);
        return result;
    }

    private static Pattern pattern(String s, int start) {
        if (start == s.length()) {
            return EMPTY_STRING;
        } else {
            return append(s.charAt(start), pattern(s, start + 1));
        }
    }

    public static Pattern pattern(String s) {
        return pattern(s, 0);
    }

    public static Pattern append(char c, Pattern rest) {
        DefaultPattern result = new DefaultPattern(false);
        result.addTransition(c, rest);
        return result;
    }

    public static Pattern append(Pattern p1, Pattern p2, Map visited) {
        DefaultPattern prev = (DefaultPattern) visited.get(p1);
        if (prev != null) {
            return prev;
        }
        boolean isMatcherForEmptyString = p1.matchesEmptyString();
        DefaultPattern result = new DefaultPattern(isMatcherForEmptyString && p2.matchesEmptyString());
        visited.put(p1, result);
        for (char c = 0; c < 256; c++) {
            Pattern n1 = p1.nextState(c);
            if (n1 == null) {
                if (isMatcherForEmptyString) {
                    Pattern n2 = p2.nextState(c);
                    result.addTransition(c, n2);
                }
            } else {
                result.addTransition(c, append(n1, p2, visited));
            }
        }
        return result;
    }

    /** Returns a pattern matching all strings s3 such that s3 = s1 + s2,
     * for all strings s1 matching p1 and s2 matching p2. */
    public static Pattern append(Pattern p1, Pattern p2) {
        return append(p1, p2, new IdentityHashMap());
    }

    private static Object key(Object k1, Object k2) {
        List key = new ArrayList(2);
        key.add(k1);
        key.add(k2);
        return key;
    }

    public static Pattern union(Pattern p1, Pattern p2, Map visited) {
        if (p1 == null) {
            return p2;
        }
        if (p2 == null) {
            return p1;
        }
        Object key = key(p1, p2);
        Pattern prev = (Pattern) visited.get(key);
        if (prev != null) {
            return prev;
        }
        DefaultPattern result = new DefaultPattern(p1.matchesEmptyString() || p2.matchesEmptyString());
        visited.put(key, result);
        for (char c = 0; c < 256; c++) {
            result.addTransition(c, union(p1.nextState(c), p2.nextState(c), visited));
        }
        return result;
    }

    /** Returns a pattern matching all strings that are matched either or both p1 and p2. */
    public static Pattern union(Pattern p1, Pattern p2) {
        return union(p1, p2, new HashMap());
    }

    public static Pattern union(char[] chars) {
        DefaultPattern result = new DefaultPattern(false);
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            result.addTransition(c, EMPTY_STRING);
        }
        return result;
    }

    public static Pattern union(Pattern[] patterns) {
        Pattern result = new DefaultPattern(false);
        for (int i = 0; i < patterns.length; i++) {
            Pattern p = patterns[i];
            result = union(result, p);
        }
        return result;
    }

    public static Pattern union(String chars) {
        return union(chars.toCharArray());
    }

    private static Pattern setdiff(Pattern p1, Pattern p2, Map visited) {
        if (p1 == null) {
            return null;
        }
        if (p2 == null) {
            return p1;
        }
        Object key = key(p1, p2);
        Pattern prev = (Pattern) visited.get(key);
        if (prev != null) {
            return prev;
        }
        DefaultPattern result = new DefaultPattern(p1.matchesEmptyString() && !p2.matchesEmptyString());
        visited.put(key, result);
        for (char c = 0; c < 256; c++) {
            result.addTransition(c, setdiff(p1.nextState(c), p2.nextState(c), visited));
        }
        return result;
    }

    /** Returns a pattern matching all strings that are matched by p1 and not by p2. */
    public static Pattern setdiff(Pattern p1, Pattern p2) {
        return setdiff(p1, p2, new HashMap());
    }

    /** Returns a pattern which matches all strings {"", s, s + s, s + s + s, ...}
     * for all strings s matching p. Note the result always matches the
     * empty string.
     * */
    public static Pattern repeat(Pattern p, DefaultPattern output, Map visited) {
        // This is unnecessary but provides more sharing in the resulting graph.
        if (p == EMPTY_STRING) {
            return output;
        }
        Pattern prev = (Pattern) visited.get(p);
        if (prev != null) {
            return prev;
        }
        DefaultPattern result = new DefaultPattern(true);
        visited.put(p, result);
        for (char c = 0; c < 256; c++) {
            Pattern n1 = p.nextState(c);
            if (n1 != null) {
                result.addTransition(c, repeat(n1, output == null ? result : output, visited));
            }
        }
        return result;
    }

    public static Pattern repeat(Pattern p1) {
        return repeat(p1, null, new IdentityHashMap());
    }

    // Instance methods.

    public abstract Pattern nextState(char character);

    public abstract boolean matchesEmptyString();

    public boolean matches(CharSequence s) {
        Pattern p = this;
        for (int i = 0; i < s.length(); i++) {
            p = p.nextState(s.charAt(i));
            if (p == null) {
                return false;
            }
        }
        return p.matchesEmptyString();
    }
}
