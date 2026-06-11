package Problems.ManagingStates.VendingMachine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VendingMachine {
    enum Denomination{
        TWENTY_FIVE_CENT(25),
        FIFTY_CENT(50),
        ONE_DOLLAR(100),
        FIVE_DOLLAR(500),
        TEN_DOLLAR(1000);
        private final int value;

        Denomination(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    static class Item{
        private final String itemId;
        private final String itemName;
        private int price;

        public Item(String itemId, String itemName, int price) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.price = price;
        }

        public String getItemId() {
            return itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public int getPrice() {
            return price;
        }

        public boolean setPrice(int price) {
            this.price = price;
            return true;
        }
    }

    static class Bucket{
        private final Item item;
        private int noOfItems;

        public Bucket(Item item, int noOfItems) {
            this.item = item;
            this.noOfItems = noOfItems;
        }

        public Item getItem() {
            return item;
        }

        public int getNoOfItems() {
            return noOfItems;
        }

        public void setNoOfItems(int noOfItems) {
            this.noOfItems = noOfItems;
        }
    }
    static class Shelf{
        private final Bucket[][] items;
        private final int row, col;
        Map<String, Map.Entry<Integer, Integer>> location;
        private Map.Entry<Item, Integer> selectedItem;
        public Shelf(Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items, int row, int col) {
            this.items = new Bucket[row][col];
            location = new HashMap<>();

            this.row = row;
            this.col = col;
            for(var each:items.entrySet()){
                int x = each.getKey().getKey(), y = each.getKey().getValue();
                assert x >= 0 && x < row && y >= 0 && y < col;
                Item item = each.getValue().getKey();
                int quantity = each.getValue().getValue();
                assert quantity > 0;
                this.items[x][y] = new Bucket(item, quantity);
                location.put(item.getItemId(), Map.entry(x, y));
            }
        }
        public boolean updateItemsQuantity(VendingMachineState state, Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items){
            assert state instanceof AdminState;
            for(var each:items.entrySet()){
                int x = each.getKey().getKey(), y = each.getKey().getValue();
                assert x >= 0 && x < this.row && y >= 0 && y < this.col;
                Item item = each.getValue().getKey();
                int quantity = each.getValue().getValue();
                assert quantity > 0;
                this.items[x][y] = new Bucket(item, quantity);
                location.put(item.getItemId(), Map.entry(x, y));
            }
            return true;
        }
        public boolean updateItemPrice(VendingMachineState state, Item item){
            assert state instanceof AdminState;
            String itemId = item.getItemId();
            assert location.containsKey(itemId);
            Integer row = location.get(itemId).getKey();
            Integer col = location.get(itemId).getValue();
            return items[row][col].getItem().setPrice(item.getPrice());
        }

        public boolean isItemPresent(String itemId, int qty){
            assert location.containsKey(itemId);
            Integer row = location.get(itemId).getKey();
            Integer col = location.get(itemId).getValue();

            return row != null && col != null && items[row][col].getNoOfItems() >= qty;
        }

        public Bucket dispenseItem(){
            assert selectedItem != null;
            Item item = selectedItem.getKey();
            int qty = selectedItem.getValue();
            Integer row = location.get(item.getItemId()).getKey();
            Integer col = location.get(item.getItemId()).getValue();

            Bucket bucket = items[row][col];
            bucket.setNoOfItems(bucket.getNoOfItems() - qty);
            selectedItem = null;
            return new Bucket(item, qty);
        }

        public void takeDispensedItem(){
            selectedItem = null;
        }

        public void selectItem(String itemId, int qty){
            assert isItemPresent(itemId, qty);
            Integer row = location.get(itemId).getKey();
            Integer col = location.get(itemId).getValue();

            selectedItem = Map.entry(items[row][col].getItem(), qty);
        }

        public int getTotalAmount(){
            assert selectedItem != null;
            return selectedItem.getKey().getPrice() * selectedItem.getValue();
        }
    }
    interface VendingMachineState{
        void selectItem(Shelf shelf, String itemId, int noOfItems);
        boolean putMoney(Shelf shelf, Denomination denomination, int prevAmount);
        Map.Entry<Integer, Bucket> dispense(Shelf shelf, int amount);
        boolean updateShelf(Shelf shelf, Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items);
        boolean updateItemPrice(Shelf shelf, Item item);
        VendingMachineState cancel();

    }

    static class IdleState implements VendingMachineState{
        private static volatile VendingMachineState instance = null;
        private static final Object lock = new Object();
        private IdleState(){
        }

        public static VendingMachineState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new IdleState();
                    }
                }
            }
            return instance;
        }

        @Override
        public void selectItem(Shelf shelf, String itemId, int noOfItems) {
            shelf.selectItem(itemId, noOfItems);
        }

        @Override
        public boolean putMoney(Shelf shelf, Denomination denomination, int prevAmount) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public Map.Entry<Integer, Bucket> dispense(Shelf shelf, int amount){
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean updateShelf(Shelf shelf, Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean updateItemPrice(Shelf shelf, Item item) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public VendingMachineState cancel() {
            return instance;
        }
    }

    static class ItemSelectedState implements VendingMachineState{
        private static volatile VendingMachineState instance = null;
        private static final Object lock = new Object();
        public ItemSelectedState(){
        }
        public static VendingMachineState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new ItemSelectedState();
                    }
                }
            }
            return instance;
        }

        @Override
        public void selectItem(Shelf shelf, String itemId, int noOfItems) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean putMoney(Shelf shelf, Denomination denomination, int prevAmount) {
            prevAmount += denomination.getValue();
            return prevAmount >= shelf.getTotalAmount();
        }

        @Override
        public Map.Entry<Integer, Bucket> dispense(Shelf shelf, int amount) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean updateShelf(Shelf shelf, Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean updateItemPrice(Shelf shelf, Item item) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public VendingMachineState cancel() {
            return IdleState.getInstance();
        }
    }
    static class DispenseState implements VendingMachineState{
        private static volatile VendingMachineState instance = null;
        private static final Object lock = new Object();

        public DispenseState(){
        }
        public static VendingMachineState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new DispenseState();
                    }
                }
            }
            return instance;
        }

        @Override
        public void selectItem(Shelf shelf, String itemId, int noOfItems) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean putMoney(Shelf shelf, Denomination denomination, int prevAmount) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public Map.Entry<Integer, Bucket> dispense(Shelf shelf, int amount) {
            Bucket bucket = shelf.dispenseItem();
            return Map.entry(amount - bucket.getItem().getPrice() * bucket.getNoOfItems(), bucket);
        }

        @Override
        public boolean updateShelf(Shelf shelf, Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean updateItemPrice(Shelf shelf, Item item) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public VendingMachineState cancel() {
            return IdleState.getInstance();
        }
    }

    static class AdminState implements VendingMachineState{
        private static volatile VendingMachineState instance = null;
        private static final Object lock = new Object();
        public AdminState(){
        }
        public static VendingMachineState getInstance(){
            if (instance == null){
                synchronized (lock){
                    if (instance == null){
                        instance = new AdminState();
                    }
                }
            }
            return instance;
        }

        @Override
        public void selectItem(Shelf shelf, String itemId, int noOfItems) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean putMoney(Shelf shelf, Denomination denomination, int prevAmount) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public Map.Entry<Integer, Bucket> dispense(Shelf shelf, int amount) {
            throw new IllegalStateException("illegal state");
        }

        @Override
        public boolean updateShelf(Shelf shelf, Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items){
            return shelf.updateItemsQuantity(this, items);
        }

        @Override
        public boolean updateItemPrice(Shelf shelf, Item item){
            return shelf.updateItemPrice(this, item);
        }

        @Override
        public VendingMachineState cancel() {
            return IdleState.getInstance();
        }
    }
    static abstract class User{
        private final String userId;

        public User(String userId) {
            this.userId = userId;
        }
    }

    static class Admin extends User{
        public Admin(String userId) {
            super(userId);
        }
    }

    static class VendingMachineFacade{
        private VendingMachineState machineState;
        private final Shelf shelf;
        private final int PRECISION = 100;
        public VendingMachineFacade(Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items, int row, int col) {
            this.machineState = IdleState.getInstance();
            this.shelf = new Shelf(items, row, col);
        }

        public void selectItem(String itemId, int qty){
            machineState.selectItem(shelf, itemId, qty);
            machineState = ItemSelectedState.getInstance();
        }

        public void enterMoney(List<Denomination> amounts){
            int totAmt = 0;
            for(Denomination amount:amounts){
                if(machineState instanceof IdleState) break;
                if (machineState.putMoney(shelf, amount, totAmt)) {
                    machineState = DispenseState.getInstance();
                    System.out.println(dispenseItem(totAmt));
                    break;
                }
                else totAmt += amount.getValue();
            }
            machineState = IdleState.getInstance();
        }

        private Map.Entry<Double, Bucket> dispenseItem(int amount) {
            Map.Entry<Integer, Bucket> dispense = machineState.dispense(shelf, amount);
            return Map.entry(dispense.getKey() / (double)PRECISION, dispense.getValue());
        }

        public void cancel(){
            machineState = machineState.cancel();
        }

        public void updateShelfItems(User user, Map<Map.Entry<Integer, Integer>, Map.Entry<Item, Integer>> items){
            if (!(user instanceof Admin)) {
                throw new IllegalArgumentException("Only admin can perform this operation");
            }
            machineState = AdminState.getInstance();
            if (machineState.updateShelf(shelf, items)){
                machineState = IdleState.getInstance();
            }
        }


        public void updateItemsPrice(User user, List<Item> items){
            if (!(user instanceof Admin)) {
                throw new IllegalArgumentException("Only admin can perform this operation");
            }
            machineState = AdminState.getInstance();
            for(Item item:items) {
                if (!machineState.updateItemPrice(shelf, item)) {
                    throw new RuntimeException("Failure while updating item price");
                }
            }
            machineState = IdleState.getInstance();
        }
    }
}
