package Decorator.Pizza;

public class Margarita implements PizzaInterface{
    private int price;

    public Margarita(int price) {
        this.price = price;
    }

    @Override
    public int price() {
        return price;
    }
}
