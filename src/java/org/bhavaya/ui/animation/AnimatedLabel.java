package org.bhavaya.ui.animation;

import org.bhavaya.ui.animation.IconComponentAnimator;

import javax.swing.*;


/**
 * nb. shamelessly half-inched from http://www.swingwiki.org/howto:animated_label
 *
 * A label that takes care of background animations
 */
public class AnimatedLabel extends JLabel implements IconComponentAnimator.IconComponent {
    private IconComponentAnimator iconComponentAnimator;


    public AnimatedLabel(
            String imageResourcePrefix, String imageResourceSuffix,
            int numImages, int startIndex,
            int delay, int pauseBetweenAnimations, boolean runOnce) {

        iconComponentAnimator = new IconComponentAnimator(
                this,
                imageResourcePrefix,
                imageResourceSuffix,
                numImages,
                startIndex,
                delay,
                pauseBetweenAnimations,
                runOnce);
    }

     /**
     * To guarantee cleanup client class can call this dispose method, if paranoid
     */
    public void disposeResources() {
        iconComponentAnimator.disposeResources();
    }

    public void runAnimations() {
        iconComponentAnimator.runAnimation();
    }

    public void stopAnimations() {
        iconComponentAnimator.stopAnimation();
    }
    
    public void joinAnimationThread() throws InterruptedException {
        iconComponentAnimator.joinThread();
    }
}