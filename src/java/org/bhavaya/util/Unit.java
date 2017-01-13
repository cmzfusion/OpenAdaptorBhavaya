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

import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Parwinder Sekhon
 * @version $Revision: 1.8 $
 */
public class Unit {
    private static final Log log = Log.getCategory(Unit.class);

    private static final String UNIT_TYPE_CURRENCY = "currency";
    private static final String UNIT_TYPE_OTHER = "other";
    private static final int UNIT_TYPE_DEFAULT_PRECISION = 0;

    private static Map instances = new HashMap();
    private static Map unitConversionRateServices = new HashMap();
    private static volatile boolean initialised;

    private String name;
    private String type;
    private int precision;
    private boolean conversionUnit;
    private Unit defaultConversionUnit;

    public synchronized static Unit getInstance(String unitName) {
        init();
        Unit unit = (Unit) instances.get(unitName);

        if (unit == null) {
            try {
                Currency currency = Currency.getInstance(unitName);
                // if unitName is not a currency code, then an exception is thrown
                unit = new Unit(unitName, UNIT_TYPE_CURRENCY, currency.getDefaultFractionDigits(), false);
            } catch (Exception e) {
                unit = new Unit(unitName, UNIT_TYPE_OTHER, UNIT_TYPE_DEFAULT_PRECISION, false);
            }
            instances.put(unitName, unit);
        }

        return unit;
    }

    private Unit(String name, String type, int precision, boolean conversionUnit) {
        this.name = name;
        this.type = type;
        this.precision = precision;
        this.conversionUnit = conversionUnit;
    }

    private Unit createDefaultConversionUnit() {
        final UnitConversionRateService unitConversionRateService = (UnitConversionRateService) unitConversionRateServices.get(type);
        return unitConversionRateService != null ? new Unit(unitConversionRateService.getDefaultConversionUnit(), type, precision, true) : null;
    }

    public String getType() {
        return type;
    }

    public int getPrecision() {
        return precision;
    }

    public boolean isConversionUnit() {
        return conversionUnit;
    }

    public double conversionRate(String toUnitName, Date rateDate) {
        if (Utilities.equals(name, toUnitName)) return 1.0;

        Unit toUnit = Unit.getInstance(toUnitName);
        String toUnitType = toUnit.getType();
        // units have to be of same type
        if (!Utilities.equals(type, toUnitType)) {
            return Double.NaN;
        }

        UnitConversionRateService unitConversionRateService = (UnitConversionRateService) unitConversionRateServices.get(type);

        // if no unitConversionRateService could either return Double.NaN, 1.0 or fail, decide to return Double.NaN
        if (unitConversionRateService == null) {
            return Double.NaN;
        }

        return unitConversionRateService.conversionRate(name, toUnitName, rateDate);
    }

    /**
     * In currency terms, this is the triangulation currency that other currencies are converted to by default for
     * calculation purposes.
     *
     * @return a unit used for normalisation or null if one does not exist.
     */
    public Unit getDefaultConversionUnit() {
        if (defaultConversionUnit == null) defaultConversionUnit = createDefaultConversionUnit();
        return defaultConversionUnit;
    }

    public String getName() {
        return name;
    }

    private static void init() {
        // Only run once
        synchronized (Unit.class) {
            if (initialised) return;
            initialised = true;

            PropertyGroup servicesPropertyGroupParent = ApplicationProperties.getApplicationProperties().getGroup("unitConversionRateServices");
            if (servicesPropertyGroupParent == null) {
                log.warn("No unit conversion rate services");
                return;
            }

            PropertyGroup[] servicesPropertyGroups = servicesPropertyGroupParent.getGroups();
            if (servicesPropertyGroups == null || servicesPropertyGroups.length == 0) {
                log.warn("No unit conversion rate services");
                return;
            }

            for (int i = 0; i < servicesPropertyGroups.length; i++) {
                PropertyGroup servicesPropertyGroup = servicesPropertyGroups[i];
                String unitType = servicesPropertyGroup.getMandatoryProperty("unitType");
                String unitConversionRateServiceClassName = servicesPropertyGroup.getMandatoryProperty("serviceClass");

                if (log.isDebug()) log.debug("Adding unit conversion rate service: " + unitConversionRateServiceClassName + " for unit type: " + unitType);
                // Currently assume that unit conversion rate services have a static "getInstance" method,
                // this is to avoid having factories for each service and a singleton to access factories
                try {
                    Class unitConversionRateServiceClass = ClassUtilities.getClass(unitConversionRateServiceClassName);
                    Object unitConversionRateService = unitConversionRateServiceClass.getDeclaredMethod("getInstance").invoke(unitConversionRateServiceClass);
                    unitConversionRateServices.put(unitType, unitConversionRateService);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Unit)) return false;

        final Unit unit = (Unit) o;

        return Utilities.equals(name, unit.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return name;
    }
}
