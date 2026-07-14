public class Toy{
/** sample of Java Documentation comment
 * Toy Object
 * has property of name, brand, price, quantity
 * with a method of setPrice() where you can change
 * the price of the toy object
 */

String name;
String brand;
double price;
int quantity; 

void setPrice(double price){
    //this is used to refer to the current object line 4 for the price and this.price for line 7
    this.price = price;
}

public static void main(String[] args){ //psvm is for entry point of the program
    //create and instance of the Toy class = Toy object

    /* this
    is
    a 
    multiline
    comment */

    Toy toy1 = new Toy();
    toy1.name = "Rage Pink";
    toy1.brand = "Lab Vuvu";
    toy1.price = 4500;
    toy1.quantity = 12;
    toy1.setPrice((toy1.price * 0.5)); //50% discount
    System.out.println(toy1.name);
    }
} 