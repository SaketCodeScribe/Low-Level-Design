package Problems.ManagementStates.InventoryManagement;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Functional requirements:
 * * track multiple warehouse
 * * audit log
 * 1. multiple type of products configurable
 * 2. actions like restocking, editing price or buying supported
 * 3. overall view of products having stocks less than critical, and out of stock
 * 4. Monitoring change in qty/price of product. who are the observers?
 * 5. Thread safe actions on point 2.
 *
 * Non-functional requirements:
 * 1. code should be modular for easier testing.
 * 2. code should follow ood for maintainibility and scalability
 */
public class InventoryManagementSystem {
    enum Category{
        Electronics,
        Clothing,
        Food,
        Home,
        Sports;
    }
    static class Product{
        String name;
        Category category;
        volatile double price;
        AtomicInteger quantity;

        public Product(String name, Category category, double price, int quantity) {
            this.name = name;
            this.category = category;
            this.price = price;
            this.quantity = new AtomicInteger(quantity);
        }

        public void updatePrice(double price){
            this.price = price;
        }

        public boolean updateQuantity(int changeInQty){
            while(true){
                int oldQty = this.quantity.get();
                int newQty = oldQty + changeInQty;
                if (oldQty < 0 || newQty < 0) return false;
                if (quantity.compareAndSet(oldQty, newQty)){
                    break;
                }
            }
            return true;
        }

        public int getQuantity() {
            return quantity.get();
        }

        public double getPrice(){
            return this.price;
        }
    }

    static interface UserAction{
        boolean update(Product product);
        void deleteProductFromWarehouse(Warehouse warehouse);
        void addProductInWareHouse(Warehouse warehouse);
        String getWareHouseId();
    }
    static class PriceAction implements UserAction{
        double price;
        String productId;
        String wareHouseId;

        public PriceAction(double price, String productId, String wareHouseId) {
            this.price = price;
            this.productId = productId;
            this.wareHouseId = wareHouseId;
        }

        public boolean update(Product product) {
            product.updatePrice(price);
            return true;
        }

        @Override
        public String getWareHouseId() {
            return wareHouseId;
        }

        @Override
        public void deleteProductFromWarehouse(Warehouse warehouse) {
            throw new RuntimeException("Action not supported");
        }

        @Override
        public void addProductInWareHouse(Warehouse warehouse) {
            throw new RuntimeException("Action not supported");
        }

        @Override
        public String toString() {
            return "PriceAction{" +
                    "price=" + price +
                    ", productId='" + productId + '\'' +
                    ", wareHouseId='" + wareHouseId + '\'' +
                    '}';
        }
    }

    static class ChangeQuantityAction implements UserAction{
        int quantity;
        String productId;
        String wareHouseId;

        public ChangeQuantityAction(int quantity, String productId, String wareHouseId) {
            this.quantity = quantity;
            this.productId = productId;
            this.wareHouseId = wareHouseId;
        }

        public boolean update(Product product) {
            return product.updateQuantity(quantity);
        }
        @Override
        public String getWareHouseId() {
            return wareHouseId;
        }

        @Override
        public void deleteProductFromWarehouse(Warehouse warehouse) {
            throw new RuntimeException("Action not supported");
        }

        @Override
        public void addProductInWareHouse(Warehouse warehouse) {
            throw new RuntimeException("Action not supported");
        }

        @Override
        public String toString() {
            return "ChangeQuantityAction{" +
                    "quantity=" + quantity +
                    ", productId='" + productId + '\'' +
                    ", wareHouseId='" + wareHouseId + '\'' +
                    '}';
        }
    }
    static class DeleteProductAction implements UserAction{
        String productId;
        String wareHouseId;

        public DeleteProductAction(String productId, String wareHouseId) {
            this.productId = productId;
            this.wareHouseId = wareHouseId;
        }

        @Override
        public String getWareHouseId() {
            return wareHouseId;
        }
        public boolean update(Product product) {
            throw new RuntimeException("Action not supported");
        }

        @Override
        public void deleteProductFromWarehouse(Warehouse warehouse) {
            warehouse.removeProduct(productId);
        }

        @Override
        public void addProductInWareHouse(Warehouse warehouse) {
            throw new RuntimeException("Action not supported");
        }

        @Override
        public String toString() {
            return "DeleteProductAction{" +
                    "productId='" + productId + '\'' +
                    ", wareHouseId='" + wareHouseId + '\'' +
                    '}';
        }
    }
    static class AddProductAction implements UserAction{
        Product product;
        String wareHouseId;

        public AddProductAction(Product product, String wareHouseId) {
            this.product = product;
            this.wareHouseId = wareHouseId;
        }

        @Override
        public String getWareHouseId() {
            return wareHouseId;
        }
        public boolean update(Product product) {
            throw new RuntimeException("Action not supported");
        }

        @Override
        public void deleteProductFromWarehouse(Warehouse warehouse) {
            throw new RuntimeException("Action not supported");
        }

        @Override
        public void addProductInWareHouse(Warehouse warehouse) {
            warehouse.addProduct(List.of(product));
        }

        @Override
        public String toString() {
            return "AddProductAction{" +
                    "product=" + product +
                    ", wareHouseId='" + wareHouseId + '\'' +
                    '}';
        }
    }
//    todo: update userAction to below interface. The above mixes more than one responsibility because of which each implementation need to throw exception for unsupported operation. below is the command pattern
//    todo: or you can make this as a service class rather than following interface
//    interface InventoryCommand {
//        void execute(Warehouse warehouse);
//    }
//    class UpdatePriceCommand implements InventoryCommand {
//        private final String productId;
//        private final double newPrice;
//
//        public UpdatePriceCommand(String productId, double newPrice) {
//            this.productId = productId;
//            this.newPrice = newPrice;
//        }
//
//        @Override
//        public void execute(Warehouse warehouse) {
//            warehouse.updatePrice(productId, newPrice);
//        }
//    }class UpdateQuantityCommand implements InventoryCommand {
//        private final String productId;
//        private final int delta;
//
//        public UpdateQuantityCommand(String productId, int delta) {
//            this.productId = productId;
//            this.delta = delta;
//        }
//
//        @Override
//        public void execute(Warehouse warehouse) {
//            warehouse.updateQuantity(productId, delta);
//        }
//    }
    static interface Observer{
        void updateStateChange(UserAction action);
    }
    static class InventoryMonitor implements Observer{

        @Override
        public void updateStateChange(UserAction action) {
            action.toString();
        }
    }
    static class Warehouse{
        String location;
        String wareHouseId;
        ConcurrentMap<String, Product> products;
        ConcurrentLinkedQueue<Map.Entry<UserAction, Boolean>> audit;
        int threshold;
        Set<Observer> observers;
        public Warehouse(String location, String wareHouseId, int threshold) {
            this.location = location;
            this.wareHouseId = wareHouseId;
            this.products = new ConcurrentHashMap<>();
            this.threshold = threshold;
            audit = new ConcurrentLinkedQueue<>();
            observers = ConcurrentHashMap.newKeySet();
        }

        public boolean makeAction(UserAction action){
            boolean isDone = true;
            if (action instanceof PriceAction){
                isDone = action.update(products.get(((PriceAction) action).productId));
            } else if (action instanceof ChangeQuantityAction){
                isDone = action.update(products.get(((ChangeQuantityAction) action).productId));
            } else if (action instanceof AddProductAction){
                action.addProductInWareHouse(this);
            } else if (action instanceof DeleteProductAction){
                action.deleteProductFromWarehouse(this);
            }
            audit.offer(Map.entry(action, isDone));
            if (isDone) notifyObservers(action);
            return true;
        }

        public List<Product> getOutOfStockProducts(){
            return products.values().stream().filter(product -> product.getQuantity() == 0).collect(Collectors.toList());
        }

        public List<Product> getLowInStockProducts(){
            return products.values().stream().filter(product -> product.getQuantity() <= threshold).collect(Collectors.toList());
        }

        public void addProduct(List<Product> products){
            products.forEach(p -> this.products.putIfAbsent(p.name, p));
        }

        public void removeProduct(String productId){
            this.products.remove(productId);
        }

        public void addObserver(Observer observer){
            observers.add(observer);
        }
        public void removeObserver(Observer observer){
            observers.remove(observer);
        }
        public void notifyObservers(UserAction action){
            for(Observer ob:observers){
                ob.updateStateChange(action);
            }
        }
    }
    static class WarehouseController{
        private static volatile WarehouseController instance = null;
        private static final Object lock = new Object();
        ConcurrentHashMap<String, Warehouse> warehouses;

        private WarehouseController() {
            warehouses = new ConcurrentHashMap<>();
        }

        public static WarehouseController getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new WarehouseController();
                    }
                }
            }
            return instance;
        }

        public void registerWarehouse(Warehouse warehouse){
            warehouses.putIfAbsent(warehouse.wareHouseId, warehouse);
        }

        public void removeWarehouse(Warehouse warehouse){
            warehouses.remove(warehouse.wareHouseId);
        }

        public void action(UserAction action){
            boolean flag = true;
            warehouses.computeIfPresent(action.getWareHouseId(), (k,v) -> {
                if (!v.makeAction(action)) {
                    System.out.println(action.toString()+" X FAILED!!");
                }
                return v;
            });
        }

        public Enumeration<String> getWarehouses() {
            return warehouses.keys();
        }
    }
    WarehouseController controller;
    private static volatile InventoryManagementSystem instance = null;
    private static final Object lock = new Object();
    private InventoryManagementSystem(){}

    public static InventoryManagementSystem getInstance(){
        if (instance == null){
            synchronized (lock){
                if (instance == null){
                    instance = new InventoryManagementSystem();
                }
            }
        }
        return instance;
    }

    public void createWarehouse(String location, String warehouseId, int threshold){
        controller.registerWarehouse(new Warehouse(location, warehouseId, threshold));
    }

    public void addProduct(Map<String, Product> warehouseToProduct){
        UserAction action;
        for(Map.Entry<String, Product> entry:warehouseToProduct.entrySet()) {
            action = new AddProductAction(entry.getValue(), entry.getKey());
            controller.action(action);
        }
    }

    public void removeProduct(Map<String, String> warehouseToProduct){
        UserAction action;
        for(Map.Entry<String, String> entry:warehouseToProduct.entrySet()) {
            action = new DeleteProductAction(entry.getValue(), entry.getKey());
            controller.action(action);
        }
    }

    public void updatePriceOfProduct(String productId, double price){
        Iterator<String> iterator = controller.getWarehouses().asIterator();
        UserAction action;
        while(iterator.hasNext()){
            String warehouseId = iterator.next();
            action = new PriceAction(price, productId, warehouseId);
            controller.action(action);
        }
    }

    public void updateStock(int quantity, String productId, String wareHouseId){
        if (quantity < 0) throw new RuntimeException("quantity can't be in negative");
        UserAction action = new ChangeQuantityAction(quantity, productId, wareHouseId);
       controller.action(action);
    }

}
