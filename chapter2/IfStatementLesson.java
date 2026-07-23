package chapter2;

public class IfStatementLesson {
    public static void main(String[] args) {
        int hourOfDay = 10;
        if (hourOfDay < 11) {
            System.out.println("Good morning");
            System.out.println("Kumain ka na ba?");
        }
        else if (hourOfDay < 15) {
            System.out.println("Good afternoon");
        }
        else {
            System.out.println("Good evening");
        }
    }
}
