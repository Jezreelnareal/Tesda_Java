public class Task3 {
    public static void main(String[] args) {
        String a = "Wow";
        String b = "Wow";
        String c = b + "!"; //Wow!
        String d = a + "!"; //Wow!

        boolean b1 = a == b; //true
        boolean b2 = d.equals(b + "!"); //true
        boolean b3 = !c.equals(a);//true

        if (b1 && b2 && b3) {
            System.out.println("Success!");
        }
    }
}