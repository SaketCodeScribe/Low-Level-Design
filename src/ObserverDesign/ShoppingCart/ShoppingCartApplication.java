package ObserverDesign.ShoppingCart;

public class ShoppingCartApplication {
    public static void main(String[] args) {
        Observable product = new Product("iphone", 0);
        product.addObserver(new User("saket"));
        product.addObserver(new User("aditya"));
        product.addObserver(new User("kevin"));

        product.setData(10);
    }
}
