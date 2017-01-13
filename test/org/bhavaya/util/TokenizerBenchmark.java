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

import org.bhavaya.util.Tokenizer;
import org.bhavaya.util.Tokenizer2;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public class TokenizerBenchmark {
    public static void main(String[] args) {
        String irrelevantSQL = "begin transaction\nexecute rs_update_threads @rs_seq = 341, @rs_id = 6\nupdate bookingservice..BCG_Msg set MsgStatus='SENT', MsgDeliveredAt='20030501 13:40:17:316' where MsgServiceId=1000 and MsgTypeId=1000 and MsgQueuedAt='20030501 13:40:16:060' and MsgStatus='PENDING' and MsgDeliveredAt='20030501 13:40:16:846' and MsgFailedText=NULL  and MsgParam1='ZBI_TRADE_HOSPITAL_STAGE_1' and MsgParam2='transaction_number: 168045' and MsgParam3='transaction_type: 2' and MsgParam4='pricing_number: 36' and MsgParam5='error_code: 300' and MsgParam6='comment:  PP5K3LQ10' and MsgParam7='reported_at: May  1 2003  1:40:16:056PM' and MsgParam8='resolved: 0' and MsgParam9=NULL  and MsgParam10=NULL  and MsgId=894697 and MsgSpid=NULL\ninsert into bookingservice..ZBI_TRADE_HOSPITAL_STAGE_1 (transaction_number, transaction_type, pricing_number, error_code, comment, reported_at, resolved) values (169368, 202, 36, 300, ' PP5K3LQ10', '20030501 13:40:17:163', 0)\nupdate bookingservice..BCG_Msg_LastTS set LAST_TIME='20030501 13:40:17:166' where LAST_TIME='20030501 13:40:16:566'\nset identity_insert dbo.BCG_Msg on insert into dbo.BCG_Msg (MsgServiceId, MsgTypeId, MsgQueuedAt, MsgStatus, MsgDeliveredAt, MsgFailedText, MsgParam1, MsgParam2, MsgParam3, MsgParam4, MsgParam5, MsgParam6, MsgParam7, MsgParam8, MsgParam9, MsgParam10, MsgId, MsgSpid) values (1000, 1000, '20030501 13:40:17:166', 'NEW', NULL , NULL , 'ZBI_TRADE_HOSPITAL_STAGE_1', 'transaction_number: 169368', 'transaction_type: 202', 'pricing_number: 36', 'error_code: 300', 'comment:  PP5K3LQ10', 'reported_at: May  1 2003  1:40:17:163PM', 'resolved: 0', NULL , NULL , 894699, NULL )set identity_insert dbo.BCG_Msg off\nupdate bookingservice..BCG_Msg set MsgStatus='PENDING', MsgDeliveredAt='20030501 13:40:17:460' where MsgServiceId=1000 and MsgTypeId=1000 and MsgQueuedAt='20030501 13:40:16:566' and MsgStatus='NEW' and MsgDeliveredAt=NULL  and MsgFailedText=NULL  and MsgParam1='ZBI_TRADE_HOSPITAL_STAGE_1' and MsgParam2='transaction_number: 169367' and MsgParam3='transaction_type: 202' and MsgParam4='pricing_number: 36' and MsgParam5='error_code: 300' and MsgParam6='comment:  PP5K3LQ10' and MsgParam7='reported_at: May  1 2003  1:40:16:563PM' and MsgParam8='resolved: 0' and MsgParam9=NULL  and MsgParam10=NULL  and MsgId=894698 and MsgSpid=NULL\nupdate bookingservice..BCG_Msg set MsgStatus='SENT', MsgDeliveredAt='20030501 13:40:17:463' where MsgServiceId=400 and MsgTypeId=400 and MsgQueuedAt='20030501 13:39:42:400' and MsgStatus='PENDING' and MsgDeliveredAt='20030501 13:40:16:943' and MsgFailedText=NULL  and MsgParam1='169368' and MsgParam2='202' and MsgParam3='36' and MsgParam4='0' and MsgParam5='1' and MsgParam6='0' and MsgParam7=NULL  and MsgParam8=NULL  and MsgParam9=NULL  and MsgParam10=NULL  and MsgId=894608 and MsgSpid=NULL\nupdate bookingservice..BCG_Msg set MsgStatus='PENDING', MsgDeliveredAt='20030501 13:40:17:470' where MsgServiceId=400 and MsgTypeId=400 and MsgQueuedAt='20030501 13:39:42:403' and MsgStatus='NEW' and MsgDeliveredAt=NULL  and MsgFailedText=NULL  and MsgParam1='169371' and MsgParam2='202' and MsgParam3='36' and MsgParam4='0' and MsgParam5='1' and MsgParam6='0' and MsgParam7=NULL  and MsgParam8=NULL  and MsgParam9=NULL  and MsgParam10=NULL  and MsgId=894609 and MsgSpid=NULL\nselect seq from rs_threads where id = 6\nexecute rs_update_lastcommit @origin = 161,		@origin_qid = 0x000000011074a6de000b03fc000b000b03fc00030000936c00e194e70000000000000001,		@secondary_qid = 0x000000000000000000000000000000000000000000000000000000000000000000000000,		@origin_time = '20030501 13:41:19:063'\ncommit transaction\n";
        testTokenizer(irrelevantSQL);
        testTokenizer2(irrelevantSQL);
    }

    private static void testTokenizer(String irrelevantSQL) {
        long currentTime = System.currentTimeMillis();
        System.out.println("starting");
        int n = 1000;
        for (int i = 0; i < n; i++) {
            Tokenizer tokenizer = new Tokenizer(irrelevantSQL);
            for (String token = tokenizer.nextToken(); token.length() != 0; token = tokenizer.nextToken()) {
            }
        }
        long timeTaken = System.currentTimeMillis() - currentTime;
        System.out.println((double) timeTaken / n + " millis per iteration for Tokenizer");
    }

    private static void testTokenizer2(String irrelevantSQL) {
        long currentTime = System.currentTimeMillis();
        System.out.println("starting");
        int n = 1000;
        for (int i = 0; i < n; i++) {
            Tokenizer2 tokenizer = new Tokenizer2(irrelevantSQL);
            for (String token = tokenizer.readToken(); token.length() != 0; token = tokenizer.readToken()) {
            }
        }
        long timeTaken = System.currentTimeMillis() - currentTime;
        System.out.println((double) timeTaken / n + " millis per iteration for Tokenizer2");
    }

}
