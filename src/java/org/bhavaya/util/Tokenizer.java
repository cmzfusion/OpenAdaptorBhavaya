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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description.
 *
 * @author Philip Milne
 * @version $Revision: 1.2 $
 */
public class Tokenizer {
    private static final Log log = Log.getCategory(Tokenizer.class);

    private static final boolean DEBUG = false;

//    public static final Pattern EOF = Pattern.compile("\uFFFF");
    public static final Pattern TOKEN = Pattern.compile("[a-zA-Z_]\\w*");
    public static final Pattern STRING = Pattern.compile("\"([^\"]|\\\")*\"");
    public static final Pattern WHITE_SPACE = Pattern.compile("\\s+");
    public static final Pattern SEPARATOR = Pattern.compile("[^\\w\\s]");
    public static final Pattern INTEGER = Pattern.compile("[\\+\\-]?[0-9]+");
//    public static final Pattern NUMBER = Pattern.compile("[\\+\\-]?[0-9]*" + "(\\.[0-9]*)?" + "(e" + INTEGER.pattern() + ")?");
    // This will match 123.234.345 which the expression above does not.
    // The difficulty is making sure the expression doesn't match the empty string.
    public static final Pattern NUMBER = Pattern.compile("[\\+\\-]?[0-9\\.]+" + "(e" + INTEGER.pattern() + ")?");
    public static final Pattern FLOAT = Pattern.compile(NUMBER.pattern() + "f");
    public static final Pattern DOUBLE = Pattern.compile(NUMBER.pattern() + "d");

    public static CharSequence toCharSequence(InputStream s) throws IOException {
        byte[] in = new byte[s.available()];
        s.read(in);
        return new String(in);
    }

    private CharSequence shiftedInput;
    private CharSequence input;
    private int start;
    private List matchers;
    private Pattern lastPattern;
    private Matcher union = null;
    private int[] groupToPattern;
    private boolean awaitingFlush;

    private void init() {
        this.matchers = new ArrayList();
//        add(EOF);
        add(TOKEN);
        add(STRING);
        add(WHITE_SPACE);
        add(SEPARATOR);
        add(INTEGER);
        add(NUMBER);
        add(FLOAT);
        add(DOUBLE);
    }

    public Tokenizer() {
        this.shiftedInput = new CharSequence() {
            public int length() {
                return Tokenizer.this.input.length() - start;
            }

            public char charAt(int index) {
                return Tokenizer.this.input.charAt(index + start);
            }

            public CharSequence subSequence(int start, int end) {
                int s = Tokenizer.this.start;
                return Tokenizer.this.input.subSequence(start + s, end + s);
            }
        };
        init();
    }

    public Tokenizer(final CharSequence input) {
        this();
        setInput(input);
    }

    public void setInput(CharSequence input) {
        this.input = input;
        this.start = 0;
    }

    public Tokenizer(InputStream s) throws IOException {
        this(toCharSequence(s));
    }

    public void add(Pattern pattern) {
        add(matchers.size(), pattern);
    }

    public void add(int i, Pattern pattern) {
        Matcher m = pattern.matcher(shiftedInput);
        matchers.add(i, m);
        reset();
    }

    public void remove(Pattern pattern) {
        for (int i = 0; i < matchers.size(); i++) {
            Matcher m = (Matcher) matchers.get(i);
            if (m.pattern().equals(pattern)) {
                matchers.remove(m);
            }
        }
        reset();
    }

    public void removeAllPatterns() {
        matchers = new ArrayList();
        reset();
    }

    public Pattern getPatternForPreviousMatch() {
        if (lastPattern == null) {
            int bestGroup = -1;
            int bestLength = -1;
            for (int i = 1; i < union.groupCount() + 1; i++) {
                try {
                    String s = union.group(i);
                    if (DEBUG) System.out.println("group[" + i + "] matched {" + s + "}");
                    if (s != null && s.length() > bestLength) {
                        bestLength = s.length();
                        bestGroup = i;
                    }
                } catch (IllegalStateException e) {
                    // No match found.
                }

            }
            lastPattern = ((Matcher) matchers.get(patternToGroup(bestGroup))).pattern();
        }
        return lastPattern;
    }

    private void reset() {
        union = null;
        groupToPattern = null;
    }

    private Matcher buildMatcher() {
        if (union == null) {
            String matchString = "";
            for (int i = 0; i < matchers.size(); i++) {
                Matcher m = (Matcher) matchers.get(i);
                Pattern p = m.pattern();
                matchString = matchString + "(" + p.pattern() + ")";
                if (i != matchers.size() - 1) {
                    matchString = matchString + "|";
                }
            }
            Pattern p2 = Pattern.compile(matchString, 0);
            if (DEBUG) System.out.println("union = " + matchString);
            union = p2.matcher(shiftedInput);
        }
        return union;
    }

    private int patternToGroup(int groupIndex) {
        if (groupToPattern == null) {
            List l = new ArrayList();
            for (int i = 0; i < matchers.size(); i++) {
                Matcher m = (Matcher) matchers.get(i);
                for (int j = 0; j < m.groupCount() + 1; j++) {
                    l.add(new Integer(i));
                }
            }
            if (DEBUG) System.out.println("group counts: " + l);
            groupToPattern = new int[l.size()];
            for (int i = 0; i < l.size(); i++) {
                groupToPattern[i] = ((Integer) l.get(i)).intValue();
            }
        }
        return groupToPattern[groupIndex - 1];
    }

    public String nextToken() {
        Matcher union = buildMatcher();
        if (awaitingFlush) {
            start = start + union.end();
            awaitingFlush = false;
        }
        // For some reason the pattern, "[^\\w\\s]", seems to match this
        // strange character when the stream is empty.
        if (shiftedInput.length() == 0) {
            return "";
        }
//        union.reset();
        union.lookingAt();
        if (DEBUG) System.out.println("");
        if (DEBUG) System.out.println("Matching: " + shiftedInput.subSequence(0, Math.min(100, shiftedInput.length() - 1)));
        if (DEBUG) System.out.println("union.end() = " + union.end());

        lastPattern = null;
        String result = shiftedInput.subSequence(0, union.end()).toString();

        if (union.end() < 1) {
            String errorLog = "No patterns matched the input after: " + start + "." + '\n' + input;
            log.error(errorLog);
            throw new RuntimeException(errorLog);
        }

        awaitingFlush = true;
        return result;
    }

    /**
     * Testing -- sanity check.
     */
    public static void main(String[] args) throws Exception {
        Pattern SQL_STRING = Pattern.compile("'([^']|'')*'");
        Pattern SQL_TOKEN = Pattern.compile("[a-zA-Z_][a-zA-Z_\\.\\*]*");
        Pattern SQL_OPERATOR = Pattern.compile("(<>)|(<=)|(>=)|(!=)|(\\*=)|(=\\*)");
        int iterations = 1;
        long start = System.currentTimeMillis();
        String input = ApplicationProperties.substituteApplicationProperties("SELECT 'FROM A, WHERE' AS 'dodgy column', %instrumentDatabaseName%.Instrument.instrument <= 100000 AS capacity, %instrumentDatabaseName%.Bond.*, %instrumentDatabaseName%.BondAmountIssued.amountIssued, %instrumentDatabaseName%.BondAmountOutstanding.amtOutstanding, %instrumentDatabaseName%.BondIssuenceDetails.*, %instrumentDatabaseName%.BondDescriptionNotes.descriptionNotes FROM %instrumentDatabaseName%.Instrument, %instrumentDatabaseName%.Bond, %instrumentDatabaseName%.BondAmountIssued BondAmountIssued, %instrumentDatabaseName%.BondAmountOutstanding, %instrumentDatabaseName%.BondIssuenceDetails, %instrumentDatabaseName%.BondDescriptionNotes, %bookingDatabaseName%.POSITION WHERE (%instrumentDatabaseName%.Instrument.instrumentId = %instrumentDatabaseName%.Bond.instrumentId AND %instrumentDatabaseName%.Bond.instrumentId *= %instrumentDatabaseName%.BondAmountIssued.instrumentId AND %instrumentDatabaseName%.Bond.instrumentId *= %instrumentDatabaseName%.BondAmountOutstanding.instrumentId AND %instrumentDatabaseName%.Bond.instrumentId = %instrumentDatabaseName%.BondIssuenceDetails.instrumentId AND %instrumentDatabaseName%.Bond.instrumentId = %instrumentDatabaseName%.BondDescriptionNotes.instrumentId AND %instrumentDatabaseName%.BondAmountIssued.validPeriodEnd IS NULL AND %instrumentDatabaseName%.BondAmountOutstanding.validPeriodEnd IS NULL AND %instrumentDatabaseName%.BondDescriptionNotes.validPeriodEnd IS NULL) and (%bookingDatabaseName%.POSITION.instrument_id = %instrumentDatabaseName%.Instrument.instrumentId) GROUP By snum, odate HAVING MAX(snum) >= 3 AND MIN(odate) <= .2 ORDER BY instrument_id, portfolio_id");
//            String input = "begin transaction\nexecute rs_update_threads @rs_seq = 341, @rs_id = 6\nupdate bookingservice..BCG_Msg set MsgStatus='SENT', MsgDeliveredAt='20030501 13:40:17:316' where MsgServiceId=1000 and MsgTypeId=1000 and MsgQueuedAt='20030501 13:40:16:060' and MsgStatus='PENDING' and MsgDeliveredAt='20030501 13:40:16:846' and MsgFailedText=NULL  and MsgParam1='ZBI_TRADE_HOSPITAL_STAGE_1' and MsgParam2='transaction_number: 168045' and MsgParam3='transaction_type: 2' and MsgParam4='pricing_number: 36' and MsgParam5='error_code: 300' and MsgParam6='comment:  PP5K3LQ10' and MsgParam7='reported_at: May  1 2003  1:40:16:056PM' and MsgParam8='resolved: 0' and MsgParam9=NULL  and MsgParam10=NULL  and MsgId=894697 and MsgSpid=NULL\ninsert into bookingservice..ZBI_TRADE_HOSPITAL_STAGE_1 (transaction_number, transaction_type, pricing_number, error_code, comment, reported_at, resolved) values (169368, 202, 36, 300, ' PP5K3LQ10', '20030501 13:40:17:163', 0)\nupdate bookingservice..BCG_Msg_LastTS set LAST_TIME='20030501 13:40:17:166' where LAST_TIME='20030501 13:40:16:566'\nset identity_insert dbo.BCG_Msg on insert into dbo.BCG_Msg (MsgServiceId, MsgTypeId, MsgQueuedAt, MsgStatus, MsgDeliveredAt, MsgFailedText, MsgParam1, MsgParam2, MsgParam3, MsgParam4, MsgParam5, MsgParam6, MsgParam7, MsgParam8, MsgParam9, MsgParam10, MsgId, MsgSpid) values (1000, 1000, '20030501 13:40:17:166', 'NEW', NULL , NULL , 'ZBI_TRADE_HOSPITAL_STAGE_1', 'transaction_number: 169368', 'transaction_type: 202', 'pricing_number: 36', 'error_code: 300', 'comment:  PP5K3LQ10', 'reported_at: May  1 2003  1:40:17:163PM', 'resolved: 0', NULL , NULL , 894699, NULL )set identity_insert dbo.BCG_Msg off\nupdate bookingservice..BCG_Msg set MsgStatus='PENDING', MsgDeliveredAt='20030501 13:40:17:460' where MsgServiceId=1000 and MsgTypeId=1000 and MsgQueuedAt='20030501 13:40:16:566' and MsgStatus='NEW' and MsgDeliveredAt=NULL  and MsgFailedText=NULL  and MsgParam1='ZBI_TRADE_HOSPITAL_STAGE_1' and MsgParam2='transaction_number: 169367' and MsgParam3='transaction_type: 202' and MsgParam4='pricing_number: 36' and MsgParam5='error_code: 300' and MsgParam6='comment:  PP5K3LQ10' and MsgParam7='reported_at: May  1 2003  1:40:16:563PM' and MsgParam8='resolved: 0' and MsgParam9=NULL  and MsgParam10=NULL  and MsgId=894698 and MsgSpid=NULL\nupdate bookingservice..BCG_Msg set MsgStatus='SENT', MsgDeliveredAt='20030501 13:40:17:463' where MsgServiceId=400 and MsgTypeId=400 and MsgQueuedAt='20030501 13:39:42:400' and MsgStatus='PENDING' and MsgDeliveredAt='20030501 13:40:16:943' and MsgFailedText=NULL  and MsgParam1='169368' and MsgParam2='202' and MsgParam3='36' and MsgParam4='0' and MsgParam5='1' and MsgParam6='0' and MsgParam7=NULL  and MsgParam8=NULL  and MsgParam9=NULL  and MsgParam10=NULL  and MsgId=894608 and MsgSpid=NULL\nupdate bookingservice..BCG_Msg set MsgStatus='PENDING', MsgDeliveredAt='20030501 13:40:17:470' where MsgServiceId=400 and MsgTypeId=400 and MsgQueuedAt='20030501 13:39:42:403' and MsgStatus='NEW' and MsgDeliveredAt=NULL  and MsgFailedText=NULL  and MsgParam1='169371' and MsgParam2='202' and MsgParam3='36' and MsgParam4='0' and MsgParam5='1' and MsgParam6='0' and MsgParam7=NULL  and MsgParam8=NULL  and MsgParam9=NULL  and MsgParam10=NULL  and MsgId=894609 and MsgSpid=NULL\nselect seq from rs_threads where id = 6\nexecute rs_update_lastcommit @origin = 161,		@origin_qid = 0x000000011074a6de000b03fc000b000b03fc00030000936c00e194e70000000000000001,		@secondary_qid = 0x000000000000000000000000000000000000000000000000000000000000000000000000,		@origin_time = '20030501 13:41:19:063'\ncommit transaction\n";
        Tokenizer tokenizer = new Tokenizer();
//            Tokenizer tokenizer = new Tokenizer("'foo'   b bar  'baz'    asdasd affaff");
        tokenizer.remove(TOKEN);
        tokenizer.remove(STRING);
        tokenizer.remove(SEPARATOR);
        tokenizer.add(SQL_TOKEN);
        tokenizer.add(SQL_STRING);
        tokenizer.add(SQL_OPERATOR);
//            tokenizer.add(WHITE_SPACE);
        tokenizer.add(SEPARATOR);
//            tokenizer = SQL.TRANSACTION_TOKENIZER;
        for (int i = 0; i < iterations; i++) {
            tokenizer.setInput(input);
            for (String token = tokenizer.nextToken(); token.length() != 0; token = tokenizer.nextToken()) {
//                    System.out.println("{" + token + "}: " + tokenizer.getPatternForPreviousMatch().pattern());
                    System.out.print(token + "|");
//                    tokenizer.getPatternForPreviousMatch();
            }
//            System.out.println("tokenizer.buildMatcher().pattern() = " + tokenizer.buildMatcher().pattern().pattern());
//            String[] result = tokenizer.buildMatcher().pattern().split(input);
//            System.out.println(Arrays.asList(result));
//            System.out.println("");
        }
        System.out.println("Parsing time: " + (System.currentTimeMillis() - start) * 1.0 / iterations + "ms");
    }

}
