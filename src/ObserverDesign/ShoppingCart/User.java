package ObserverDesign.ShoppingCart;

public class User implements Observer{
    String userName;

    public User(String userName) {
        this.userName = userName;
    }

    @Override
    public void notification(String productName) {
        System.out.println(userName+" "+productName+" is now avaiable");
    }
}
