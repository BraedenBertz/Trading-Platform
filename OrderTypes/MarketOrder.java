package OrderTypes;

import java.time.LocalDateTime;

public class MarketOrder extends StockOrder{

  double priceOfSecurity;
  //constructor
  public MarketOrder(String symbol, int quantity, double commission, LocalDateTime orderSet, LocalDateTime orderFilled, double priceOfSecurity) {
    super(symbol, quantity, commission, orderSet, orderFilled);
    this.priceOfSecurity = priceOfSecurity;
  }

  //default constructor
  public MarketOrder() {
    super();
    this.priceOfSecurity = 0;
  }

  //getters and setters
  public double getPriceOfSecurity() {
    return priceOfSecurity;
  }

  public void setPriceOfSecurity(double priceOfSecurity) {
    this.priceOfSecurity = priceOfSecurity;
  }

  //toString
  public String toString() {
    return "MarketOrder [priceOfSecurity=" + priceOfSecurity + super.toString() + "]";
  }

  //override serializable methods
  public void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    out.defaultWriteObject();
  }
  public void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    in.defaultReadObject();
  }

}
