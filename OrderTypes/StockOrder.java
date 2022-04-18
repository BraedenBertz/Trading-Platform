package OrderTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

public class StockOrder implements Serializable {
  //order size
  private int size;
  //symbol
  private String symbol;
  //DateTime
  private LocalDateTime orderSet;
  //DateTime
  private LocalDateTime orderFilled;
  //quantity
  private int quantity;
  //cost
  private double commission;

  //constructor
  //default constructor
  public StockOrder() {
    this.size = 0;
    this.symbol = "";
    this.orderSet = null;
    this.orderFilled = null;
    this.quantity = 0;
    this.commission = 0;
  }

  /**
   * @param
   * @param symbol The traded stock symbol
   * @param quantity The number of shares traded
   * @param cost The cost of the order type
   * @param orderSet The time the order was placed
   * @param orderFilled The time the order was filled
   */
  StockOrder(String symbol, int quantity, double cost, LocalDateTime orderSet, LocalDateTime orderFilled) {
    this.symbol = symbol;
    this.quantity = quantity;
    this.commission = cost;
    this.orderSet = orderSet;
    this.orderFilled = orderFilled;
  }

  //getters
  public int getSize() {
    return size;
  }

  public String getSymbol() {
    return symbol;
  }

  public LocalDateTime getOrderSet() {
    return orderSet;
  }

  public LocalDateTime getOrderFilled() {
    return orderFilled;
  }

  public int getQuantity() {
    return quantity;
  }

  public double getCommission() {
    return commission;
  }

  //setters

  public void setSize(int size) {
    this.size = size;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

 //set orderSet
  public void setOrderSet(LocalDateTime orderSet) {
    this.orderSet = orderSet;
  }

  //set orderFilled
  public void setOrderFilled(LocalDateTime orderFilled) {
    this.orderFilled = orderFilled;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public void setCommission(double commission) {
    this.commission = commission;
  }

  //toString
  public String toString() {
    return "StockOrder{" +
        "size=" + size +
        ", symbol='" + symbol + '\'' +
        ", orderSet=" + orderSet +
        ", orderFilled=" + orderFilled +
        ", quantity=" + quantity +
        ", cost=" + commission +
        '}';
  }

  //implement serializable methods
  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    out.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    in.defaultReadObject();
  }

}
