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

package org.bhavaya.ui.series;

import org.bhavaya.ui.table.DateSeries;
import org.bhavaya.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Description
 *
 * @author Brendon McLean
 * @version $Revision: 1.1 $
 */
public class TemporarySeriesMigrationStrategy {
    private static final Log log = Log.getCategory(TemporarySeriesMigrationStrategy.class);

    private static Map oldToNewMap = new HashMap();

    public static synchronized DateFunction convert(DateSeries.DateFunction oldDateFunction) {
        if (oldDateFunction instanceof DateSeries.FixedDateFunction) {
            DateFunction newDateFunction = convert((DateSeries.FixedDateFunction) oldDateFunction);
            oldToNewMap.put(oldDateFunction, newDateFunction);
            return newDateFunction;
        } else if (oldDateFunction instanceof DateSeries.RelativeDateFunction) {
            return convert((DateSeries.RelativeDateFunction) oldDateFunction);
        } else if (oldDateFunction instanceof DateSeries.SymbolicDateFunction) {
            DateFunction newDateFunction = convert((DateSeries.SymbolicDateFunction) oldDateFunction);
            oldToNewMap.put(oldDateFunction, newDateFunction);
            return newDateFunction;
        } else {
            log.error("Unable to map between DateSeries functions");
            return null;
        }
    }

    private static DateFunction convert(DateSeries.FixedDateFunction oldF) {
        FixedDateFunction newF = new FixedDateFunction(oldF.getDate());
        newF.setAlias(oldF.getAlias());
        return newF;
    }

    private static DateFunction convert(DateSeries.RelativeDateFunction oldF) {
        DateFunction referenceDate = (DateFunction) oldToNewMap.get(oldF.getReferenceDate());
        if (referenceDate == null) {
            referenceDate = convert(oldF.getReferenceDate());
            oldToNewMap.put(oldF.getReferenceDate(), referenceDate);
        }
        RelativeDateFunction newF = new RelativeDateFunction((DateFunction) oldToNewMap.get(oldF.getReferenceDate()),
                oldF.getCalendarOffsetType().toLowerCase().substring(0, 1),
                oldF.getPreposition().intern() == DateSeries.RelativeDateFunction.AFTER ? RelativeDateFunction.PREPOSITION_AFTER.getCode() : RelativeDateFunction.PREPOSITION_BEFORE.getCode(),
                oldF.getOffset());
        return newF;
    }

    private static DateFunction convert(DateSeries.SymbolicDateFunction oldF) {
        return SymbolicDateFunction.getInstance(oldF.getSymbolName().toUpperCase().substring(0, 1));
    }

}
