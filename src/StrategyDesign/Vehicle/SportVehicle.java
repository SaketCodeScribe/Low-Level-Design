package StrategyDesign.Vehicle;

public class SportVehicle implements VehicleStrategy {
    @Override
    public void drive() {
        System.out.println("Driving Sport Vehicle");
    }
}
