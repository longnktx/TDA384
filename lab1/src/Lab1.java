import TSim.*;

public class Lab1 {

  public Lab1(Integer speed1, Integer speed2) {
    TSimInterface tsi = TSimInterface.getInstance();

    try {
      System.out.println("Hello");
      tsi.setSpeed(1,speed1);
    }
    catch (CommandException e) {
      e.printStackTrace();    // or only e.getMessage() for the error
      System.exit(1);
    }
  }
}
