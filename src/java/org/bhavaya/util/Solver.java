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

/**
 * Description
 *
 * @author
 * @version $Revision: 1.2 $
 */
public class Solver {
    private static final double PRECISION = 1e-8;
    private static final double DERIVATIVE_STEP = 1e-10;

    public static interface Function {
        double value(double x);
    };

    public static Function derivative(final Function f, final double dx) {
        return new Function() {
            public double value(double x) {
                return (f.value(x + dx) - f.value(x)) / dx;
            }
        };
    }

    public static Function derivative(final Function f) {
        return derivative(f, DERIVATIVE_STEP);
    }

    // Newton-Raphson
    public static double solve(Function f, double targetY, double x) {
        for (int tries = 0; tries < 25; tries++) {
            double fx = f.value(x) - targetY;
            x = x - fx / ((f.value(x + DERIVATIVE_STEP) - targetY - fx) / DERIVATIVE_STEP);
            if (Math.abs(fx / targetY) < PRECISION) {
                return x;
            }
        }
        return 0.0 / 0;
    }

    public static double solve(Function f, Function fp, double targetY, double x) {
        for (int tries = 0; tries < 25; tries++) {
            double fx = f.value(x) - targetY;
            x = x - fx / fp.value(x);
            if (Math.abs(fx / targetY) < PRECISION) {
                return x;
            }
        }
        return 0.0 / 0;
    }

    public static double solve(Function f, double targetY) {
        return solve(f, targetY, 0);
    }

}
