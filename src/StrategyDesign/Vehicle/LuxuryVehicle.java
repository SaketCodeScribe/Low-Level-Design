package StrategyDesign.Vehicle;

public class LuxuryVehicle implements VehicleStrategy {
    @Override
    public void drive() {
        System.out.println("Driving Luxury Vehicle");
    }
}
