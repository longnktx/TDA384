import TSim.*;

import java.util.concurrent.Semaphore;

public class Lab1 {
    public Lab1(Integer speed1, Integer speed2) {
        Semaphore[] sem = new Semaphore[4];
        for (int i = 0; i < sem.length; i++) {
            sem[i] = new Semaphore(1);
        }
        TrainThread t1 = new TrainThread(1, speed1, sem);
        TrainThread t2 = new TrainThread(2, speed2, sem);
        t1.start();
        t2.start();
    }
}


class TrainThread extends Thread {
    TSimInterface tsi = TSimInterface.getInstance();
    int id;
    int speed;
    int sectionIndex;
    Semaphore[] critSections;
    final int UP = 0;
    final int DOWN = 1;

    public TrainThread(int id, int speed, Semaphore[] sem) {
        this.id = id;
        this.speed = speed;
        this.critSections = sem;
    }

    @Override
    public void run() {
        System.out.println("Hello, I'm train " + id);

        try {
            tsi.setDebug(false);
            speed = speed * (-1);
            if (id == 1) {          // Train 1
                while (true) {
                    topToBottomRoute();
                    bottomToTopRoute();
                }
            } else {                  // Train 2
                while (true) {
                    bottomToTopRoute();
                    topToBottomRoute();
                }
            }
        } catch (CommandException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * The behaviour of the train when moving from top to bottom
     * Depends on the ID, the train will choose different tracks to reach the station
     */
    private void topToBottomRoute() throws CommandException, InterruptedException {
        speed = speed * (-1);
        sectionIndex = 0;
        tsi.setSpeed(id, speed);
        //critical section 0
        if (id == 1) {                              // Train 1 specific
          skipSensorsUntil(6, 6, false);
            moveWhenFree(sectionIndex);
            releaseOnSensor(13, 7, 1);
            //critical section 1
            moveWhenFree(sectionIndex);
            tsi.setSwitch(17, 7, UP);
        } else {                                    // Train 2 specific
            skipSensorsUntil(9, 5, false);
            moveWhenFree(sectionIndex);
            releaseOnSensor(13, 8, 1);
            //critical section 1
            moveWhenFree(sectionIndex);
            tsi.setSwitch(17, 7, DOWN);
        }

        //Parallel section
        skipSensorsUntil(19, 9, false);

        //critical section 2
        if (critSections[sectionIndex + 1].tryAcquire()) {
            // UPPER TRACK
            handleParallelSection(12, 9, 7, 9, UP, true);
            // Release parallel section
            releaseOnSensor(1, 10, 1);
        } else {
            // LOWER TRACK
            handleParallelSection(12, 10, 7, 10, DOWN, true);
            sectionIndex++;
            skipSensorsUntil(1, 10, false);
        }
        //critical section 3
        if (id == 1) {                                // Train 1 specific
            tsi.setSwitch(3, 11, UP);
            releaseOnSensor(4, 13, 1);
            skipSensorsUntil(14, 13, false);
        } else {                                        // Train 2 specific
            tsi.setSwitch(3, 11, DOWN);
            releaseOnSensor(6, 11, 1);
            skipSensorsUntil(14, 11, false);
        }
        tsi.setSpeed(id, 0);
        sleep(1000 + (20 * Math.abs(speed))); //Make sure we stand still before we go back
    }

    /**
     * The behaviour of the train when moving from bottom to top
     * Depends on the ID, the train will choose different tracks to reach the station
     */
    private void bottomToTopRoute() throws CommandException, InterruptedException {
        speed = speed * (-1);
        sectionIndex = 3;
        tsi.setSpeed(id, speed);
        //Similar to the topToBottomRoute but the opposite
        if (id == 1) {
            skipSensorsUntil(4, 13, false);
            moveWhenFree(sectionIndex);
            tsi.setSwitch(3, 11, UP);
        } else {
            skipSensorsUntil(6, 11, false);
            moveWhenFree(sectionIndex);
            tsi.setSwitch(3, 11, DOWN);
        }
        //Parallel section
        skipSensorsUntil(1, 10, false);
        if (critSections[sectionIndex - 1].tryAcquire()) {
            // UPPER TRACK
            handleParallelSection(7, 9, 12, 9, UP, false);
            // Release parallel section
            releaseOnSensor(19, 9, -1);
        } else {
            // LOWER TRACK
            handleParallelSection(7, 10, 12, 10, DOWN, false);
            sectionIndex--;
            skipSensorsUntil(19, 9, false);
        }

        if (id == 1) {
            tsi.setSwitch(17, 7, UP);
            releaseOnSensor(13, 7, -1);
            moveWhenFree(sectionIndex);
            releaseOnSensor(6, 6, -1);
            skipSensorsUntil(14, 3, false);
        } else {
            tsi.setSwitch(17, 7, DOWN);
            releaseOnSensor(13, 8, -1);
            moveWhenFree(sectionIndex);
            releaseOnSensor(9, 5, -1);
            skipSensorsUntil(14, 5, false);
        }
        tsi.setSpeed(id, 0);
        sleep(1000 + (40 * Math.abs(speed))); //Make sure we stand still before we go back
    }

    /**
     * Skip all the sensors before the train reach the sensor at position x and y
     * Using boolean untilPass to decide if it should inactive the sensor
     */
    private void skipSensorsUntil(int x, int y, boolean untilPass) throws CommandException, InterruptedException {
        int status = untilPass ? SensorEvent.ACTIVE : SensorEvent.INACTIVE;
        SensorEvent se;
        do {
            se = tsi.getSensor(id);
        } while ((se.getXpos() != x || se.getYpos() != y) || se.getStatus() == status);
    }

    /**
     * The train will wait until it acquires the permission from the semaphore at position x and y
     */
    private void moveWhenFree(int index) throws CommandException, InterruptedException {
        tsi.setSpeed(id, 0);
        critSections[index].acquireUninterruptibly();
        tsi.setSpeed(id, speed);
    }

    /**
     * After passing the sensor at position x and y the current semaphore will be released
     */
    private void releaseOnSensor(int x, int y, int sectionDiff) throws CommandException, InterruptedException {
        skipSensorsUntil(x, y, false);
        critSections[sectionIndex].release();
        sectionIndex += sectionDiff;
    }

    /**
     * Switch to most suitable track and release the semaphore
     * after passing sensor at position x1, y1 then wait to acquire the next semaphore
     */
    private void handleParallelSection(int x1, int y1, int x2, int y2, int switchDir, boolean fromTopToBottom) throws CommandException, InterruptedException {
        int sectionDiff = 1;
        if (fromTopToBottom) tsi.setSwitch(15, 9, switchDir);
        else {
            tsi.setSwitch(4, 9, (switchDir + 1) % 2);
            sectionDiff = -1;
        }
        releaseOnSensor(x1, y1, sectionDiff);
        skipSensorsUntil(x2, y2, false);
        moveWhenFree(sectionIndex + sectionDiff);
        if (!fromTopToBottom) tsi.setSwitch(15, 9, switchDir);
        else tsi.setSwitch(4, 9, (switchDir + 1) % 2);
    }
}
