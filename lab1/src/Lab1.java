import TSim.*;
import java.util.concurrent.Semaphore;

public class Lab1 {
	public Lab1(Integer speed1, Integer speed2) {
		Semaphore[] sem = new Semaphore[4];
		for (int i=0; i<sem.length; i++) {
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

	public TrainThread(int id, int speed, Semaphore[] sem){
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
			}
			else {                  // Train 2
			  while(true) {
          bottomToTopRoute();
          topToBottomRoute();
        }
			}
		}
			catch (CommandException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void topToBottomRoute() throws CommandException, InterruptedException {
    speed = speed * (-1);
    sectionIndex = 0;
    tsi.setSpeed(id, speed);
    //critical section 0
    if (id == 1) {                              // Train 1 specific
      moveWhenFree(sectionIndex, 6, 3);
      releaseOnSensor(9, 7, 1);
      //critical section 1
      moveWhenFree(sectionIndex, 13, 7);
      tsi.setSwitch(17, 7, UP);
    }
    else {                                      // Train 2 specific
      moveWhenFree(sectionIndex, 9, 5);
      releaseOnSensor(8, 8, 1);
      //critical section 1
      moveWhenFree(sectionIndex, 13, 8);
      tsi.setSwitch(17, 7, DOWN);
    }

    //Parallel section
    skipSensorsUntil(19, 9, false);

    if (critSections[sectionIndex + 1].tryAcquire()) {
      // UPPER TRACK
      handleParallelSection(14, 9, 10, 9, UP, true);
      // Release parallel section
      releaseOnSensor(3, 9, 1);
    } else {
      // LOWER TRACK
      handleParallelSection(15, 10, 10, 10, DOWN, true);
      sectionIndex++;
      skipSensorsUntil(3,9, true);
    }
    if (id == 1) {                                // Train 1 specific
      tsi.setSwitch(3, 11, UP);
      releaseOnSensor(3, 12, 1);
      skipSensorsUntil(14, 13, false);
    }
    else {                                        // Train 2 specific
      tsi.setSwitch(3, 11, DOWN);
      releaseOnSensor(4, 11, 1);
      skipSensorsUntil(14, 11, false);
    }
    tsi.setSpeed(id, 0);
    sleep(1000 + (40 * Math.abs(speed))); //Make sure we stand still before we go back
  }

	private void bottomToTopRoute() throws CommandException, InterruptedException {
		speed = speed * (-1);
		sectionIndex = 3;
		tsi.setSpeed(id, speed);
		if (id == 1) {
			moveWhenFree(sectionIndex, 6, 13);
			tsi.setSwitch(3, 11, UP);
		}
		else {
			moveWhenFree(sectionIndex, 6, 11);
			tsi.setSwitch(3, 11, DOWN);
		}
    //Parallel section
		skipSensorsUntil(2,11,true);
    if (critSections[sectionIndex - 1].tryAcquire()) {
			// UPPER TRACK
			handleParallelSection(5, 9, 10, 9, UP, false);
      // Release parallel section
      releaseOnSensor(16, 9, -1);
		} else {
			// LOWER TRACK
			handleParallelSection(4, 10, 10, 10, DOWN, false);
			sectionIndex--;
			skipSensorsUntil(16,9, true);
		}

		if (id == 1) {
			tsi.setSwitch(17, 7, UP);
			releaseOnSensor(16, 7, -1);
			moveWhenFree(sectionIndex, 13, 7);
			releaseOnSensor(7, 7, -1);
			//skipSensorsUntil(14, 3, false);
		} else {
			tsi.setSwitch(17, 7, DOWN);
			releaseOnSensor(17, 8, -1);
			moveWhenFree(sectionIndex, 13, 8);
			releaseOnSensor(8, 6, -1);
			skipSensorsUntil(14, 5, false);
		}
		tsi.setSpeed(id, 0);
		sleep(1000 + (40 * Math.abs(speed))); //Make sure we stand still before we go back
	}

	/**
	 * skipSensorsUntil skips all sensors till given position. It takes a boolean if it should pass the selected sensor or just activate it.
	 * @param posX describes the selected sensors x position.
	 * @param posY describes the selected sensors y position.
	 * @param untilPass describes if the train should pass the sensor or if it should activate it.
	 * @throws CommandException A TSim exception that could be thrown.
	 * @throws InterruptedException Thread interruption exception.
	 */
	private void skipSensorsUntil(int posX, int posY, boolean untilPass) throws CommandException, InterruptedException {
		int status = untilPass ? SensorEvent.ACTIVE : SensorEvent.INACTIVE;
		SensorEvent se;
		do {
			se = tsi.getSensor(id);
		} while ((se.getXpos() != posX || se.getYpos() != posY) || se.getStatus() == status);
	}

	private void moveWhenFree(int index, int x, int y) throws CommandException, InterruptedException {
		skipSensorsUntil(x,y,false);
		while (!critSections[index].tryAcquire())
			tsi.setSpeed(id,0);
		tsi.setSpeed(id, speed);
	}

	private void releaseOnSensor(int x,int y, int sectionDiff) throws CommandException, InterruptedException {
		skipSensorsUntil(x,y,true);
		critSections[sectionIndex].release();
		sectionIndex+=sectionDiff;
	}

	private void handleParallelSection(int x1, int y1, int x2, int y2, int switchDir, boolean fromTopToBottom) throws CommandException, InterruptedException {
		int sectionDiff = 1;
		if (fromTopToBottom) tsi.setSwitch(15,9,switchDir);
		else {
			tsi.setSwitch(4,9,(switchDir+1) % 2);
			sectionDiff = -1;
		}
		releaseOnSensor(x1, y1, sectionDiff);
		moveWhenFree(sectionIndex+sectionDiff,x2,y2);
		if (!fromTopToBottom) tsi.setSwitch(15,9,switchDir);
		else tsi.setSwitch(4,9,(switchDir+1) % 2);
	}
}
