package org.bhavaya.util;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Apr 2, 2006
 * Time: 5:26:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class Switch extends Latch {
    public void waitFor() throws InterruptedException {
        acquire();
    }

    public synchronized void setOn(boolean latched) {
        if (latched) {
            release();
        } else {
            latched_ = false;
        }
    }

    public boolean isOn() {
        return latched_;
    }
}
