package ObserverDesign.ShoppingCart;

public interface Observable {
    public void setData(int count);
    public void addObserver(Observer user);
    public void notifyObserver(String name);
}
