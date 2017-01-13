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

package org.bhavayademo.ui;

import org.bhavaya.beans.BeanFactory;
import org.bhavaya.collection.BeanCollection;
import org.bhavaya.coms.NotificationPublisher;
import org.bhavaya.db.Persister;
import org.bhavaya.util.DateUtilities;
import org.bhavaya.util.Log;
import org.bhavaya.util.Quantity;
import org.bhavaya.util.Utilities;
import org.bhavayademo.beans.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.7 $
 */
public class RandomDataGenerator {
    private static final Log log = Log.getCategory(RandomDataGenerator.class);
    private static final String[] RANDOM_TICKERS = {"BGB", "RXP", "MNP", "TSV", "GNC", "XSE", "RLF", "RSE", "KLD", "QAL", "EAL"};
    private static final String[] RANDOM_COUNTRIES = {"DE", "GB", "FR"};
    private static final DateFormat TICKER_DATE_FORMAT = new SimpleDateFormat("ddMMMyyyy");

    private Persister tradeAndPositionPersister;
    private Persister instrumentPersister;
    private Thread instrumentThread;
    private Thread tradeThread;

    public RandomDataGenerator(final long numberOfUnits, final long waitUnit) throws Exception {
        NotificationPublisher instrumentSqlPublisher = NotificationPublisher.getInstance("instrumentSql");
        instrumentPersister = new Persister("demoDatabaseInstrumentsTx", instrumentSqlPublisher);

        NotificationPublisher tradeAndPositionSqlPublisher = NotificationPublisher.getInstance("tradeAndPositionSql");
        tradeAndPositionPersister = new Persister("demoDatabaseTradesTx", tradeAndPositionSqlPublisher);

        Runnable tradeRunnable = new Runnable() {
            public void run() {
                for (int i = 0; i < numberOfUnits; i++) {
                    createRandomTrade();
                    sleep(waitUnit * 2);
                }
            }
        };
        tradeThread = Utilities.newThread(tradeRunnable, "RandomTradeGenerator", true);

        Runnable instrumentRunnable = new Runnable() {
            public void run() {
                createRandomInstrument();
                createRandomInstrument();
                sleep(waitUnit);
                tradeThread.start();

                for (int i = 0; i < numberOfUnits; i++) {
                    if (i % 15 == 0) {
                        createRandomInstrument();
                    }

                    updateInstrumentPrice();

                    sleep(waitUnit);
                }
            }
        };
        instrumentThread = Utilities.newThread(instrumentRunnable, "RandomInstrumentGenerator", true);
    }

    public void start() {
        instrumentThread.start();
    }

    private void sleep(long ONE_MINUTE) {
        try {
            Thread.sleep(ONE_MINUTE);
        } catch (InterruptedException e) {
            log.error(e);
        }
    }

    private void updateInstrumentPrice() {
        Instrument instrument = (Instrument) getRandomBean(Instrument.class);
        if (instrument != null) instrument.setPrice(Utilities.getRandomDouble(90, 110));
    }

    private void createRandomInstrument() {
        try {
            InstrumentType instrumentType = (InstrumentType) getRandomBean(InstrumentType.class);
            Instrument instrument = null;

            if (InstrumentType.BOND.equals(instrumentType)) {
                Bond bond = (Bond) BeanFactory.newBeanInstance(Bond.class);
                instrument = bond;
                java.util.Date maturityDate = DateUtilities.getRandomDate(2003, 2010);
                bond.setMaturityDate(maturityDate);
                bond.setParAmount(Utilities.getRandomInt(1000000, 1000000));
                bond.setCoupon(Utilities.getRandomDouble(0, 10));
                bond.setZSpread(Utilities.getRandomDouble(0, 3));
                instrument.setDescription(RANDOM_TICKERS[Utilities.getRandomInt(0, RANDOM_TICKERS.length - 1)] + " " + TICKER_DATE_FORMAT.format(maturityDate));
            } else if (InstrumentType.BOND_FUTURE.equals(instrumentType)) {
                BondFuture bondFuture = (BondFuture) BeanFactory.newBeanInstance(BondFuture.class);
                instrument = bondFuture;
                bondFuture.setContractSize(Utilities.getRandomInt(1, 50));
                java.util.Date firstDeliveryDate = DateUtilities.getRandomDate(2003, 2010);
                bondFuture.setFirstDeliveryDate(firstDeliveryDate);
                bondFuture.setLastDeliveryDate(DateUtilities.getRandomDate(2003, 2010));
                instrument.setDescription(RANDOM_TICKERS[Utilities.getRandomInt(0, RANDOM_TICKERS.length - 1)] + " " + TICKER_DATE_FORMAT.format(firstDeliveryDate));
            }

            instrument.setInstrumentType(instrumentType);
            instrument.setCurrency((Currency) getRandomBean(Currency.class));
            instrument.setIssuerCountry(RANDOM_COUNTRIES[Utilities.getRandomInt(0, RANDOM_COUNTRIES.length - 1)]);
            instrument.setValid("Y");
            instrumentPersister.insertObject(instrument);

            instrumentPersister.commit();
        } catch (Exception e) {
            log.error(e);
            instrumentPersister.rollback();
        }
    }

    private void createRandomTrade() {
        try {
            Trade trade = (Trade) BeanFactory.newBeanInstance(Trade.class);
            Instrument instrument = (Instrument) getRandomBean(Instrument.class);
            trade.setInstrument(instrument);
            trade.setPrice(Utilities.getRandomDouble(90, 110));
            double quantity = Utilities.getRandomDouble(100000, 1000000);
            trade.setQuantity(new Quantity(quantity, instrument.getCurrency().getCode()));
            java.util.Date tradeDate = DateUtilities.newDateTime();
            trade.setTradeDate(tradeDate);
            TradeType tradeType = (TradeType) getRandomBean(TradeType.class);
            trade.setTradeType(tradeType);
            trade.setVersion(1);
            trade.setVersionStatus((VersionStatus) getRandomBean(VersionStatus.class));
            trade.setCounterparty((Counterparty) getRandomBean(Counterparty.class));
            trade.setComments("Random generation");
            tradeAndPositionPersister.insertObject(trade);

            Position position = (Position) BeanFactory.getInstance(Position.class).get(new Integer(instrument.getInstrumentId()));
            if (position == null) {
                position = (Position) BeanFactory.newBeanInstance(Position.class);
                position.setInstrument(instrument);
                tradeAndPositionPersister.insertObject(position);
            }
            position.setTimestamp(tradeDate);

            Quantity positionQuantity = position.getQuantity();
            String currencyCode = instrument.getCurrency().getCode();
            if (positionQuantity == null) positionQuantity = new Quantity(0, currencyCode);
            if (TradeType.BUY.equals(tradeType)) {
                positionQuantity = positionQuantity.sum(new Quantity(quantity, currencyCode));
            } else if (TradeType.SELL.equals(tradeType)) {
                positionQuantity = positionQuantity.difference(new Quantity(quantity, currencyCode));
            }
            position.setQuantity(positionQuantity);
            tradeAndPositionPersister.updateObject(position, new String[]{"timestamp", "quantity"});

            tradeAndPositionPersister.commit();
        } catch (Exception e) {
            log.error(e);
            tradeAndPositionPersister.rollback();
        }
    }

    private Object getRandomBean(Class type) {
        BeanFactory beanFactory = BeanFactory.getInstance(type);
        int size = beanFactory.size();
        if (size == 0) {
            // force load
            BeanCollection beanCollection = beanFactory.getAllBeanCollection();
            beanCollection.size();
        }
        size = beanFactory.size();
        int index = Utilities.getRandomInt(0, size - 1);
        synchronized (beanFactory.getLock()) {
            return beanFactory.values().toArray(new Object[size])[index];
        }
    }

}
