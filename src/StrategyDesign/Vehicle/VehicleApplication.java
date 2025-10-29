package StrategyDesign.Vehicle;

public class VehicleApplication {
    public static void main(String[] args) {
        Vehicle hyundai = new Hyundai();
        Vehicle ferrari = new Ferrari();
        Vehicle mercedez = new Mercedez();

        hyundai.drive();
        ferrari.drive();
        mercedez.drive();
    }
}
