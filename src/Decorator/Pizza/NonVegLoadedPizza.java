package Decorator.Pizza;

public class NonVegLoadedPizza implements PizzaInterface{
    private int price;

    public NonVegLoadedPizza(int price) {
        this.price = price;
    }

    @Override
    public int price() {
        return price;
    }
}
