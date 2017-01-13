package org.bhavaya.util;

/**
 * Implementation of this interface can be passed as a parameter to the play method of
 * {@link SoundSample}, {@link org.bhavaya.ui.SoundHandler} and {@link ExternalPlayerManager}.
 * Its methods will be called before and after playing sound.
 * With its help we can at least log messages or even implement some self diagnostic code
 * that could work arround the problem.
 *
 * @author Vladimir Hrmo
 * @version $Revision: 1.1 $
 */
public interface SoundPlayListener {

    /**
     * Invoked before playing sound. Sound cannot be discarded after this method has been invoked.
     */
    void beforePlayingSound();

    /**
     * Invoked after playing sound.
     */
    void afterPlayingSound();

    /**
     * Sound might be discarded withou being played in case there are too many requests to play sound
     */
    void discarded();
}
