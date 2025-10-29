package StrategyDesign.Vehicle;

public class NormalVehicle implements VehicleStrategy {
    @Override
    public void drive() {
        System.out.println("Driving Normal Vehicle");
    }
}
