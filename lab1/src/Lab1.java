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
  boolean isGoingForward;
  final int UP = 0;
  final int DOWN = 1;

  public TrainThread(int id, int speed, Semaphore[] sem){
    this.id = id;
    this.speed = speed;
    this.critSections = sem;
    this.isGoingForward = true;
  }

  @Override
  public void run() {
    System.out.println("Hello, I'm train " + id);

    try {
      tsi.setDebug(false);
      tsi.setSpeed(id,speed);

      if (id == 1) {            // Train 1
        sectionIndex = 0;

        //critical section 0
        moveWhenFree(critSections[sectionIndex],6,3);
        releaseOnSensor(critSections[sectionIndex],9,7);

        //critical section 1
        moveWhenFree(critSections[sectionIndex],13,7);
        tsi.setSwitch(17,7,UP);

        //critical section 2
        skipSensorsUntil(19,9,false);
        if (critSections[sectionIndex+1].tryAcquire()) {
          // UPPER TRACK
          handleParallelSection(14,9,10,9,UP);
        } else {
          // LOWER TRACK
          handleParallelSection(15,10,10,10,DOWN);
        }
        releaseOnSensor(critSections[sectionIndex], 3,9);
        tsi.setSwitch(3,11,DOWN);

        sectionIndex++;
        //
      }
      // Train 2
      else {
        sectionIndex = 2;
        //Foward
      }
    }
      catch (CommandException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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

  private void moveWhenFree(Semaphore sem, int x, int y) throws CommandException, InterruptedException {
    skipSensorsUntil(x,y,false);
    while (!sem.tryAcquire())
      tsi.setSpeed(id,0);
    tsi.setSpeed(id, speed);
  }

  private void releaseOnSensor(Semaphore sem, int x,int y) throws CommandException, InterruptedException {
    skipSensorsUntil(x,y,true);
    sem.release();
    sectionIndex++;
  }

  private void handleParallelSection(int x1, int y1, int x2, int y2, int switchDir) throws CommandException, InterruptedException {
    tsi.setSwitch(15,9,switchDir);
    releaseOnSensor(critSections[sectionIndex],x1,y1);
    moveWhenFree(critSections[sectionIndex+1],x2,y2);
    tsi.setSwitch(4,9, switchDir);
  }
}
