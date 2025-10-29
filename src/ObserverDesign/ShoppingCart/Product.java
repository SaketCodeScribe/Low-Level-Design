package ObserverDesign.ShoppingCart;

import java.util.ArrayList;
import java.util.List;

public class Product implements Observable{
    private String name;
    private int count;
    private List<Observer> observers;

    public Product(String name, int count) {
        this.name = name;
        this.count = count;
        observers = new ArrayList<>();
    }

    @Override
    public void setData(int count) {
        this.count = count;
        notifyObserver(this.name);

    }

    @Override
    public void addObserver(Observer user) {
        observers.add(user);
    }

    @Override
    public void notifyObserver(String name) {
        for(Observer user:observers){
            user.notification(name);
        }
    }
}
