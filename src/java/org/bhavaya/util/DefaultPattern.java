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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DefaultPattern extends Pattern {
    private Pattern[] states = new Pattern[0];
    private Boolean recursive;
    private int lowestCharacter; // This will be initialised to the first entry.
    private boolean matcherForEmptyString;

    public DefaultPattern(boolean matcherForEmptyString) {
        this.matcherForEmptyString = matcherForEmptyString;
    }

    public boolean matchesEmptyString() {
        return matcherForEmptyString;
    }

    public void setMatcherForEmptyString(boolean matcherForEmptyString) {
        this.matcherForEmptyString = matcherForEmptyString;
    }

    public boolean isRecursive() {
        if (recursive == null) {
            for (int i = 0; i < states.length; i++) {
                Pattern state = states[i];
                if (state != null && ((DefaultPattern) state).contains(this)) {
                    recursive = Boolean.TRUE;
                    return true;
                }
            }
            recursive = Boolean.FALSE;
        }
        return recursive.booleanValue();
    }

    private boolean contains(Pattern p, Set visited) {
        if (this == p) {
            return true;
        }
        if (visited.contains(this)) {
            return false;
        }
        visited.add(this);
        for (int i = 0; i < states.length; i++) {
            Pattern state = states[i];
            if (state != null && ((DefaultPattern) state).contains(p, visited)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Pattern p) {
        return contains(p, new HashSet());
    }

    public void addTransition(char startChar, char endChar, Pattern toState) {
        // Do both ends first so that to avoid N^2
        // characteristics due to array copying.
        addTransition(endChar, toState);
        for (char i = startChar; i < endChar; i++) {
            addTransition(i, toState);
        }
    }

    // Beware of using System.out.println in these routines,
    // strange things happen.
    public void addTransition(char character, Pattern toState) {
        if (toState == null) {
            return;
        }
        int n = states.length;
        if (n == 0) { // First addition
            lowestCharacter = character;
        }
        int c = character - lowestCharacter;
        if (c < 0) {
            Pattern[] newStates = new Pattern[n - c];
            System.arraycopy(states, 0, newStates, -c, n);
            states = newStates;
            lowestCharacter = character;
            c = 0;
        } else if (c >= n) {
            Pattern[] newStates = new Pattern[c + 1];
            System.arraycopy(states, 0, newStates, 0, n);
            states = newStates;
        }
        states[c] = toState;
        recursive = null;
    }

    public Pattern nextState(char character) {
        int c = character - lowestCharacter;
        return (c >= 0 && c < states.length) ? states[c] : null;
    }

    // For debugging - with Builder.
    public Map getTransitions() {
        Map result = new LinkedHashMap();
        for (int i = 0; i < states.length; i++) {
            Pattern p = states[i];
            if (p != null) {
                result.put(new Character((char) (lowestCharacter + i)), p);
            }
        }
        return result;
    }
}
