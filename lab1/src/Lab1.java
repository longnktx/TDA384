import TSim.*;

public class Lab1 {

  public Lab1(Integer speed1, Integer speed2) {

    TrainThread t1 = new TrainThread(1, speed1);
    TrainThread t2 = new TrainThread(2, speed2);
    t1.start();
    t2.start();
  }
}

class TrainThread extends Thread {
  TSimInterface tsi = TSimInterface.getInstance();
  int id;
  int speed;

  public TrainThread(int id, int speed){
    this.id = id;
    this.speed = speed;
  }

  @Override
  public void run() {
    System.out.println("Hello, I'm train " + id);

    try {
      tsi.setSpeed(id,speed);
    }
      catch (CommandException e) {
      e.printStackTrace();    // or only e.getMessage() for the error
      System.exit(1);
    }
  }
}
