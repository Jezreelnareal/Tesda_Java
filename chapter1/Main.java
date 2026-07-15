package chapter1; // This is the package name, it is used to group related classes together
import java.util.*; //importing the ArrayList class from the java.util package

public class Main{
    public static void main(String[] args){ //psvm is for entry point of the program
    Toy toy1 = new Toy();
    toy1.name = "Rage Pink";
    toy1.brand = "Lab Vuvu";
    toy1.price = 4500;
    toy1.quantity = 12;
    System.out.println("Hello, Toy!");
    Random r = new Random();
    System.out.println(r.nextInt(100));
    ArrayList al = new ArrayList();
    System.out.println(al);
    Date d = new Date();
    System.out.println(d);
    }
}
