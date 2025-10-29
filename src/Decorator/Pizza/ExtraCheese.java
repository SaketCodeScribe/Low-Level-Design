package Decorator.Pizza;

public class ExtraCheese implements PizzaInterface{
    int price;
    PizzaInterface pizza;

    public ExtraCheese(int price, PizzaInterface pizza) {
        this.price = price;
        this.pizza = pizza;
    }

    @Override
    public int price() {
        return pizza.price()+price;
    }
}
