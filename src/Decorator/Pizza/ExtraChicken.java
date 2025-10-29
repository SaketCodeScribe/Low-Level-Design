package Decorator.Pizza;

public class ExtraChicken implements PizzaDecorator{
    int price;
    PizzaInterface pizza;

    public ExtraChicken(int price, PizzaInterface pizza) {
        this.price = price;
        this.pizza = pizza;
    }

    @Override
    public int price() {
        return pizza.price()+price;
    }

}
