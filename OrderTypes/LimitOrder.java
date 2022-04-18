package OrderTypes;

import java.time.LocalDateTime;

public class LimitOrder extends StockOrder{
  private double limitPrice;
  public LimitOrder(String symbol, int quantity, double cost, LocalDateTime orderSet, LocalDateTime orderFilled, double limitPrice) {
    super(symbol, quantity, cost, orderSet, orderFilled);
    this.limitPrice = limitPrice;
  }
  public double getLimitPrice() {
    return limitPrice;
  }

  public void setLimitPrice(double limitPrice) {
    this.limitPrice = limitPrice;
  }

  public String toString() {
    return "LimitOrder: " + super.toString() + " Limit Price: " + limitPrice;
  }
}
