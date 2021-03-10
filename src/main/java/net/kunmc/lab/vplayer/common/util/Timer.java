package net.kunmc.lab.vplayer.common.util;

public class Timer {
    // Record the timer resolution on classload
    public static final double resolution = 1e+9;

    // Globally keeps track of time for all instances of Timer
    private static long currentTime;

    // When the timer was started
    private long startTime;

    // The last time recorded by getTime()
    private long lastTime;

    // Whether the timer is paused
    private boolean paused;

    static {
        tick();
    }

    /**
     * Constructs a timer. The timer will be reset to 0.0 and resumed immediately.
     */
    private Timer() {
        reset();
        resume();
    }

    /**
     * Constructs a timer. The timer will be reset to 0.0 and resumed immediately.
     */
    public static Timer createStarted() {
        return new Timer();
    }

    /**
     * Constructs a timer. The timer will be reset to 0.0 and paused.
     */
    public static Timer createUnstarted() {
        Timer timer = new Timer();
        timer.pause();
        return timer;
    }

    /**
     * @return the time in seconds, as a float
     */
    public float getTime() {
        if (!this.paused)
            this.lastTime = currentTime - this.startTime;

        return (float) (this.lastTime / resolution);
    }

    /**
     * @return whether this timer is paused
     */
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * Set paused
     */
    public void setPaused(boolean paused) {
        if (paused)
            pause();
        else
            resume();
    }

    /**
     * Pause the timer. Whilst paused the time will not change for this timer
     * when tick() is called.
     *
     * @see #resume()
     */
    public void pause() {
        this.paused = true;
        this.lastTime = currentTime - this.startTime;
    }

    /**
     * Reset the timer. Equivalent to set(0.0f);
     *
     * @see #set(float)
     */
    public void reset() {
        set(0.0f);
    }

    /**
     * Resume the timer.
     *
     * @see #pause()
     */
    public void resume() {
        this.paused = false;
        this.startTime = currentTime - this.lastTime;
    }

    /**
     * Set the time of this timer
     *
     * @param newTime the new time, in seconds
     */
    public void set(final float newTime) {
        final long newTimeInTicks = (long) (newTime * resolution);
        this.startTime = currentTime - newTimeInTicks;
        this.lastTime = newTimeInTicks;
    }

    /**
     * Get the next time update from the system's hires timer. This method should
     * be called once per main loop iteration; all timers are updated simultaneously
     * from it.
     */
    public static void tick() {
        currentTime = System.nanoTime();
    }

    /**
     * Debug output.
     */
    @Override
    public String toString() {
        return "Timer[Time=" + getTime() + ", Paused=" + this.paused + "]";
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Timer timer = (Timer) o;
		long delta = this.paused ? this.lastTime : currentTime - this.startTime;
		long deltaOther = timer.paused ? timer.lastTime : currentTime - timer.startTime;
		return delta == deltaOther &&
				paused == timer.paused;
	}
}