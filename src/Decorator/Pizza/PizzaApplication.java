package Decorator.Pizza;

public class PizzaApplication {
    public static void main(String[] args) {
        PizzaInterface nvpizza = new ExtraChicken(40, new ExtraCheese(20, new NonVegLoadedPizza(120)));
        PizzaInterface mpizza = new ExtraChicken(40,new ExtraChicken(40, new ExtraCheese(20, new ExtraCheese(20, new Margarita(100)))));

        System.out.println(nvpizza.price());
        System.out.println(mpizza.price());
    }
}
