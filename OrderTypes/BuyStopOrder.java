package OrderTypes;

import java.time.LocalDateTime;

public class BuyStopOrder extends LimitOrder {

  // MarketOrder
  private MarketOrder convertedOrder;
  private boolean converted = false;

  public BuyStopOrder(String symbol, int quantity, double cost, LocalDateTime orderSet, LocalDateTime orderFilled, double limitPrice) {
    super(symbol, quantity, cost, orderSet, orderFilled, limitPrice);
  }

  //convert the order to a market order
  public void convert(double priceOfSecurity) {
    convertedOrder = new MarketOrder(getSymbol(), getQuantity(), getCommission(), getOrderSet(), getOrderFilled(), priceOfSecurity);
  }

  //check to see if converted
  public boolean isConverted() {
    return converted;
  }

  //get the converted order
  public MarketOrder getConvertedOrder() {
    return convertedOrder;
  }

  //toString
  public String toString() {
    return "BuyStopOrder: " + super.toString() + (converted ? " (converted)" + convertedOrder.toString() : "");
  }
}
