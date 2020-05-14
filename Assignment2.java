

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import java.util.Objects;

public class Assignment2 {

    public final static List<Workers> workers = new ArrayList<>();
    public final static List<Tables> tables = new ArrayList<>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        //process.loadTestData(process);
        
        Stream<String> commands = Stream.concat(readFile("setup.dat"),readFile("commands.dat"));
        bbm101 process = new bbm101();
        commands.forEach(f -> call(f, process));
        
        
    }

    public void add_item(String name, double cost, int amount) {
        Stocks.items.add(new Items(name, cost, amount));
    }

    public void add_employer(String name, double salary) {
        if (workers.stream().filter(f -> f.getAuthorization() == TypeOfWorkers.EMPLOYER).count() < CONSTANTS.MAX_EMPLOYER) {
            workers.add(new Workers(name, salary, TypeOfWorkers.EMPLOYER));
        } else {

        }
    }

    public void add_waiter(String name, double salary) {
        if (workers.stream().filter(f -> f.getAuthorization() == TypeOfWorkers.WAITER).count() < CONSTANTS.MAX_WAITER) {
            workers.add(new Workers(name, salary, TypeOfWorkers.WAITER));
        } else {

        }
    }

    /*
    i)      where non-existing employer attemps to create a new table.
    ii)     where more than allowed maxximum number of tables (MAX_TABLES)
    iii)    where given employer attemps to create more than maximum number of tables allowed to create (ALLOWED_MAX_TABLES)
     */
    public void create_table(String name, int capasity) {
        System.out.println("PROGRESSONG COMMAND: create_table");
        //i)
        Workers w = getWorkers(name, TypeOfWorkers.EMPLOYER);
        if (w == null) {
            System.out.println("There is no Employer named " + name);
            return;
        }
        //ii)
        if (!Tables.isTableSizeEnough()) {
            System.out.println("Not allowed to exceed max. number of tables, MAX_TABLES");
            return;
        }
        //iii)
        if (!w.isNotExceedAllowedTables()) {
            System.out.println(name + " has already created ALLOWED_MAX_TABLES tables!");
        }
        tables.add(Tables.newInstance(w, capasity));
        System.out.println("A new table has succesfully been added");
    }

    /*
    i)      no appropriate table exist
    ii)     Where no specified waiter exist 
    ii)     where a non existing/unknown item is speficied in the order  
     */
    public void new_order(String name, int customerSize, HashMap<String, Integer> items) {
        System.out.println("PROGRESSONG COMMAND: new_order");
        Workers w = getWorkers(name, TypeOfWorkers.WAITER);
        if (w == null) {
            System.out.println("There is no waiter named " + name);
            return;
        }

        if (!w.isNotExceedAllowedService()) {
            System.out.println("Not allowed the service max. number of tables, MAX_TABLE_SERVICES");
            return;
        }

        Tables t = getFirstSuitableTable(customerSize);
        if (t == null) {
            System.out.println("There is no appropriate table for this order!");
            return;
        }
        System.out.println("Table (= ID " + t.getID() + " ) has been taken into service");
        Orders order = new Orders();
        for (String itemName : items.keySet()) {
            int itemCount = items.get(itemName);

            Items it = getItem(itemName);
            if (it == null) {
                System.out.println("Unknown item " + itemName);
                return;
            }
            int i;
            for (i = 0; i < itemCount; i++) {

                if (it.getAmount() <= 0) {
                    System.out.println("Sorry! No " + itemName + " int the stock!");

                } else {
                    it.setAmount(it.getAmount() - 1);
                    order.setTotalItemCount(i + 1);
                    System.out.println("Item " + itemName + " added into order");
                }
            } // end of the for loop

            order.getItems().add(new Items(it.getName(), it.getCost(), i));
        }

        w.create_order();
        t.setWaiter(w);
        t.getOrders().add(order);
    }

    public void add_order(String waiterName, int tableID, HashMap<String, Integer> items) {
        System.out.println("PROGRESSING COMMAND: add_order");
        Tables t = getTableByID(tableID, waiterName);
        if (t == null) {
            System.out.println("This table is either not in service nor or " + waiterName + " cannot be assigned this table!");
            return;
        }
        if (t.getOrders().size() >= CONSTANTS.MAX_ORDERS) {
            System.out.println("Not allowed to exceed max number of orders!");
            return;
        }

        Orders order = new Orders();
        for (String itemName : items.keySet()) {
            int itemCount = items.get(itemName);

            Items it = getItem(itemName);
            if (it == null) {
                System.out.println("Unknown item " + itemName);
                return;
            }
            int i;
            for (i = 0; i < itemCount; i++) {

                if (it.getAmount() <= 0) {
                    System.out.println("Sorry! No " + itemName + " int the stock!");
                    break;
                }
                it.setAmount(it.getAmount() - 1);
                order.setTotalItemCount(i + 1);
                System.out.println("Item " + itemName + " added into order");
            } // end of the for loop

            order.getItems().add(new Items(it.getName(), it.getCost(), i));
        }

        t.getOrders().add(order);
        t.getWaiter().create_order();
    }

    public void chech_out(String waiterName, int tableID) {
        System.out.println("PROGRESSING COMMAND: check_out");
        Workers w = getWorkers(waiterName, TypeOfWorkers.WAITER);
        if (w == null) {
            System.out.println("There is no waiter named " + waiterName);
            return;
        }

        Tables t = getTableByID(tableID, waiterName);
        if (t == null) {
            System.out.println("This table is either not in service now or " + waiterName + " cannot be assigned this table!");
            return;
        }
        double total = 0D;
        for (Orders order : t.getOrders()) {
            for (Items item : order.getItems()) {
                System.out.println(item.getName() + ": " + item.getCost() + " (x " + item.getAmount() + ") " + item.getCost() * item.getAmount() + " $");
                total += (item.getAmount() * item.getCost());
            }
        }
        System.out.println("Total:  " + total + " $");
    }

    public void stock_status() {
        System.out.println("PROGRESSING COMMAND: stock_status");
        for (Items item : Stocks.items) {
            System.out.println(item.getName() + ": " + item.getAmount());
        }
    }

    public void get_table_status() {
        System.out.println("PROGRESSING COMMAND: get_table_status");
        for (Tables table : tables) {
                System.out.println("Table " + table.getID() + " : " + (table.getCapacity()!=table.getOrders().size() ? "free" : "reserved" + "(" + table.getWaiter().getName() + ")"));
        }
    }

    public void get_order_status() {
        System.out.println("PROGRESSING COMMAND: get_order_status");
        for (Tables table : tables) {
            System.out.println("Table : " + table.getID());
            System.out.println("            " + table.getOrders().size() + " order(s)");
            for (Orders order : table.getOrders()) {
                System.out.println("                        " + order.getTotalItemCount() + " item(s)");
            }
        }
    }

    public void get_employer_salary() {
        System.out.println("PROGRESSING COMMAND: get_employer_salary");
        workers.stream().filter(f -> f.getAuthorization() == TypeOfWorkers.EMPLOYER).forEach(f -> {
            System.out.println("Salary for " + f.getName() + ": " + (f.getSalary() + (f.getCreatedTableSizeByEmploeye() * f.getSalary() * 0.1)));
        });
    }

    public void get_waiter_salary() {
        System.out.println("PROGRESSING COMMAND: get_waiter_salary");
        workers.stream().filter(f -> f.getAuthorization() == TypeOfWorkers.WAITER).forEach(f -> {
            System.out.println("Salary for " + f.getName() + ": " + (f.getSalary() + (f.getCreatedTableServicesByWaiter() * f.getSalary() * 0.05)));
        });
    }

    private Workers getWorkers(String name, TypeOfWorkers type) {
        return workers.stream().filter(f -> f.getName().equals(name) & f.getAuthorization() == type).findFirst().orElse(null);
    }

    private Tables getFirstSuitableTable(int customerSize) {
        return tables.stream().filter(f -> f.getIsFree() && f.getCapacity() >= customerSize && f.getWaiter() == null).findFirst().orElse(null);
    }

    private Items getItem(String name) {
        return Stocks.items.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
    }

    private Tables getTableByID(Integer id, String waiterName) {

        return tables.stream().filter(f -> f.getID().equals(id) && f.getWaiter() != null).filter(f -> f.getWaiter().getName().equals(waiterName) && f.getIsFree()).findFirst().orElse(null);

    }

    static Stream<String> readFile(String fileName) throws IOException {
        return Files.lines(Paths.get(fileName));
    }

    static void call(String line, bbm101 process) {
        String command[] = line.split(" ");
        String data[] = null;
        String subData[] = null;
        HashMap<String, Integer> map = null;

        switch (command[0]) {
            case "add_item":
                data = command[1].split(";");
                process.add_item(data[0], Double.valueOf(data[1]), Integer.valueOf(data[2]));
                break;
            case "add_employer":
                data = command[1].split(";");
                process.add_employer(data[0], Integer.valueOf(data[1]));
                break;
            case "add_waiter":
                data = command[1].split(";");
                process.add_waiter(data[0], Integer.valueOf(data[1]));
                break;
            case "create_table":
                data = command[1].split(";");
                process.create_table(data[0], Integer.valueOf(data[1]));
                break;
            case "new_order":
                data = command[1].split(";");
                subData = data[2].split(":");
                map = new HashMap<>();
                for (int i = 0; i < subData.length; i++) {
                    String name = subData[i].split("-")[0];
                    Integer size = Integer.valueOf(subData[i].split("-")[1]);
                    map.put(name, size);
                }
                process.new_order(data[0], Integer.valueOf(data[1]), map);
                break;
            case "add_order":
                data = command[1].split(";");
                subData = data[2].split(":");
                map = new HashMap<>();
                for (int i = 0; i < subData.length; i++) {
                    String name = subData[i].split("-")[0];
                    Integer size = Integer.valueOf(subData[i].split("-")[1]);
                    map.put(name, size);
                }
                process.add_order(data[0], Integer.valueOf(data[1]), map);
                break;
            case "check_out":
                data=command[1].split(";");
                process.chech_out(data[0], Integer.valueOf(data[1]));
                break;
            case "stock_status":
                process.stock_status();
                break;
            case "get_order_status":
                process.get_order_status();
                break;
            case "get_table_status":
                process.get_table_status();
                break;
            case "get_employer_salary":
                process.get_employer_salary();
                break;
            case "get_waiter_salary":
                process.get_waiter_salary();
                break;
        }
        switch (command[0]){
            case "add_item":
            case "add_employer":
            case "add_waiter":
                break;
            default :
                System.out.println("*************************************************");
                break;
        }
        
    }

    public void loadTestData(bbm101 process){
        
        process.add_item("Pizza", 3, 5);
        process.add_item("Hamburger", 1.5, 5);
        process.add_item("Water", 0.5, 4);
        process.add_item("Coke", 1.5, 7);
        process.add_item("Coffee", 0.75, 3);
        process.add_item("Tea", 0.2, 5);
        process.add_item("Donut", 1.25, 4);
        process.add_item("Doner", 2.5, 6);
        process.add_employer("Ahmet", 2500);
        process.add_employer("Zeynep", 2500);
        process.add_employer("Kamil", 3000);
        process.add_waiter("Kemal", 1200);
        process.add_waiter("Ayse", 1500);
        process.add_waiter("Ziya", 1500);
        process.stock_status();
        process.create_table("Ahmet", 4);
        System.out.println("********************************");
        process.create_table("Zeynep", 2);
        System.out.println("********************************");
        process.create_table("Elif", 2);
        System.out.println("********************************");
        HashMap<String, Integer> map = new HashMap<>();
        map.put("Pizza", 2);
        map.put("Coke", 1);
        process.new_order("Kemal", 3, map);
        System.out.println("********************************");
        process.new_order("Kemal", 6, map);
        System.out.println("********************************");
        map.clear();
        map.put("Pizza", 1);
        map.put("Coke", 1);
        process.new_order("Ziya", 1, map);
        System.out.println("********************************");
        map.clear();
        map.put("Water", 1);
        map.put("Coffee", 1);
        process.add_order("Kemal", 0, map);
        System.out.println("********************************");
        process.chech_out("Kemal", 1);
        System.out.println("********************************");
        process.chech_out("Ziya", 1);
        System.out.println("********************************");
        process.create_table("Zeynep", 6);
        System.out.println("********************************");
        map.clear();
        map.put("Pizza", 8);
        map.put("Coke", 1);
        process.new_order("Kemal", 4, map);
        System.out.println("********************************");
        process.stock_status();
        System.out.println("********************************");
        map.clear();
        map.put("Donut", 2);
        map.put("Tea", 3);
        process.add_order("Kemal", 0, map);
        System.out.println("********************************");
        process.get_order_status();
        System.out.println("********************************");
        process.chech_out("Kemal", 0);
        System.out.println("********************************");
        process.chech_out("Kemal", 1);
        System.out.println("********************************");
        process.chech_out("Ismet", 0);
        System.out.println("********************************");
        process.get_table_status();
        System.out.println("********************************");
        process.get_employer_salary();
        System.out.println("********************************");
        process.get_waiter_salary();
        System.out.println("********************************");
        process.get_order_status();
        System.out.println("********************************");


    }
}


final class  CONSTANTS {
 public static final Integer MAX_EMPLOYER=5;
 public static final Integer MAX_WAITER=5;
 public static final Integer ALLOWED_MAX_TABLES=2;
 public static final Integer MAX_TABLE_SERVICES=3;
 public static final Integer MAX_TABLES=5;
 public static final Integer MAX_ORDERS=5;
 public static final Integer MAX_ITEMS=10;
}
class Items {

    private String name;
    private double cost;
    private int amount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Items(String name, double cost, int amount) {
        this.name = name;
        this.cost = cost;
        this.amount = amount;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Items other = (Items) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    public Items(String name) {
        this.name = name;
    }

    

    
}
class Orders {

    private int totalItemCount;
    private final List<Items> items;

    public Orders() {
        items = new ArrayList<>();
    }

    public List<Items> getItems() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public int getTotalItemCount() {
        return totalItemCount;
    }

    public void setTotalItemCount(int totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    
    
}
class Stocks {
    
    public final static List<Items> items=new ArrayList<>();
    
}
class Tables {

    public static Integer createdTableSize = 0;
    private Integer ID;
    private int capacity;
    private int totalOrderCount;
    private Boolean isFree = true;
    private Workers creator;
    private Workers waiter;
    private List<Orders> orders;

    
    public static Tables newInstance(Workers creator, int capacity){
        if (isTableSizeEnough()) {
            Tables t=new Tables();
            t.ID=t.createdTableSize++;
            t.creator=creator;
            t.creator.create_table();
            t.capacity=capacity;
            t.orders = new ArrayList<>();
            
            return t;
        }
        return null;
    }
    
    private Tables() {
        
    }

    private boolean create_order(Orders order) {
        if (totalOrderCount < CONSTANTS.MAX_ORDERS) {
            totalOrderCount++;
            orders.add(order);
            return true;
        }
        return false;
    }

    public static boolean isTableSizeEnough(){
        return createdTableSize<CONSTANTS.MAX_TABLES;
    }    

    public static Integer getCreatedTableSize() {
        return createdTableSize;
    }

    public static void setCreatedTableSize(Integer createdTableSize) {
        Tables.createdTableSize = createdTableSize;
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getTotalOrderCount() {
        return totalOrderCount;
    }

    public void setTotalOrderCount(int totalOrderCount) {
        this.totalOrderCount = totalOrderCount;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
    }

    public Workers getCreator() {
        return creator;
    }

    public void setCreator(Workers creator) {
        this.creator = creator;
    }

    public Workers getWaiter() {
        return waiter;
    }

    public void setWaiter(Workers waiter) {
        this.waiter = waiter;
    }

    public List<Orders> getOrders() {
        return orders;
    }

    public void setOrders(List<Orders> orders) {
        this.orders = orders;
    }
    
}
enum TypeOfWorkers {
    EMPLOYER,
    WAITER
}
class Workers {

    private String name;
    private Double salary;
    private TypeOfWorkers authorization;
    private Integer createdTableSizeByEmploeye = 0;
    private Integer createdTableServicesByWaiter = 0;

    public Workers() {
        
    }

    public Workers(String name, Double salary, TypeOfWorkers authorization) {
        this.name = name;
        this.salary = salary;
        this.authorization = authorization;
    }
    

    /*
    table creation can only be performed by employer
    each employer can ony create table max CONSTANTS.ALLOWED_MAX_TABLES
    max created table size is 
     */
    public void create_table() {
        //only employer
        if (authorization != TypeOfWorkers.EMPLOYER) {
            return;
        }
        //max created table size per employer
        if (createdTableSizeByEmploeye >= CONSTANTS.ALLOWED_MAX_TABLES) {
            return;
        }
        //mazimum table size 
        if (Tables.createdTableSize < CONSTANTS.MAX_TABLES) {
            createdTableSizeByEmploeye++;
        }
    }

    //
    public boolean create_order() {
        if (authorization != TypeOfWorkers.WAITER) {
            return false;
        }

        if (createdTableServicesByWaiter >= CONSTANTS.MAX_TABLE_SERVICES) {
            return false;
        }
        createdTableServicesByWaiter++;
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public TypeOfWorkers getAuthorization() {
        return authorization;
    }

    public void setAuthorization(TypeOfWorkers authorization) {
        this.authorization = authorization;
    }

    public Integer getCreatedTableSizeByEmploeye() {
        return createdTableSizeByEmploeye;
    }

    public void setCreatedTableSizeByEmploeye(Integer createdTableSizeByEmploeye) {
        this.createdTableSizeByEmploeye = createdTableSizeByEmploeye;
    }

    public Integer getCreatedTableServicesByWaiter() {
        return createdTableServicesByWaiter;
    }

    public void setCreatedTableServicesByWaiter(Integer createdTableServicesByWaiter) {
        this.createdTableServicesByWaiter = createdTableServicesByWaiter;
    }
    /**
     * for Employer
     * @return boolean
     */
    public boolean isNotExceedAllowedTables(){
        return this.createdTableSizeByEmploeye<CONSTANTS.ALLOWED_MAX_TABLES;
    }
    
    /**
     * for Waiter
     * @return boolean
     */
    public boolean isNotExceedAllowedService(){
        return this.createdTableServicesByWaiter<CONSTANTS.MAX_TABLE_SERVICES;
    }

}




