package utilities;

import technicalAnalysis.overlays.ExponentialMovingAverage;
import technicalAnalysis.overlays.SimpleMovingAverage;
import technicalAnalysis.overlays.VolumeWeightedAveragePrice;
import yahoofinance.Stock;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Class that reduces / simplifies naively a contextual mathematic condition, using +-=< > >= <=
 * using a naive implementation of a recursive descent parser
 * @link{https://en.wikipedia.org/wiki/Recursive_descent_parser}
 *
 */
public class Evaluator {
    //Whether the provided argument to trigonometric functions are to be in degree mode or radian mode
    private boolean isDegree = false;
    //The active stock, the stock that takes place of '?' in functions
    private Stock activeStock;

    /**
     * The constructor for the evaluator
     */
    public Evaluator() {}

    /**
     * @param degree Whether the trigonometric functions will have their arguments interpreted as degrees
     */
    public void setDegree(boolean degree) {
        this.isDegree = degree;
    }

    /**
     *
     * @return True if the trigonometric functions arguments are interpreted as degrees, false if otherwise
     */
    public boolean isDegree() {
        return isDegree;
    }

    /**
     * Set the active stock that would take place of '?' in the evaluation
     *
     * @param activeStock The currently active stock to be used in evaluation
     */
    public void setActiveStock(Stock activeStock){this.activeStock = activeStock;}
    
    /**
     * NOTE: Does not take x(y) format
     * NOTE: When using a function that takes an exponentiation,
     * i.e., vwap(x,y)^sin(u) the first function must be surrounded by parenthesis.
     * Like so: (vwap(x,y))^sin(u)
     * uses recursive descent parsing
     */
    public double evaluate(final String str) {
        try {
            return new Object() {
                int pos = -1, ch;
    
                void nextChar() {
                    ch = (++pos < str.length()) ? str.charAt(pos) : -1;
                }
    
                boolean eat(int charToEat) {
                    while (ch == ' ') nextChar();
                    if (ch == charToEat) {
                        nextChar();
                        return true;
                    }
                    return false;
                }
    
                double parse() throws IOException, RuntimeException {
                    nextChar();
                    double x = parseExpression();
                    if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                    return x;
                }
    
                // Grammar:
                // expression = term | expression `+` term | expression `-` term
                // term = factor | term `*` factor | term `/` factor
                // factor = `+` factor | `-` factor | `(` expression `)`
                //        | number | functionName factor | factor `^` factor
    
                double parseExpression() throws IOException {
                    double x = parseTerm();
                    for (; ; ) {
                        if (eat('+')) x += parseTerm(); // addition
                        else if (eat('-')) x -= parseTerm(); // subtraction
                        else return x;
                    }
                }
    
                double parseTerm() throws IOException {
                    double x = parseFactor();
                    for (; ; ) {
                        if (eat('*')) x *= parseFactor(); // multiplication
                        else if (eat('/')) x /= parseFactor(); // division
                        else return x;
                    }
                }
    
                double parseFactor() throws IOException, RuntimeException {
                    if (eat('+')) return parseFactor(); // unary plus
                    if (eat('-')) return -parseFactor(); // unary minus
        
                    double x;
                    int startPos = this.pos;
                    if (eat('(')) { // parentheses
                        x = parseExpression();
                        eat(')');
                    } else if (eat(',')) {
                        x = parseExpression();
                        if (!eat(')')) {
                            eat(',');
                        }
                    } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(str.substring(startPos, this.pos));
                    } else if (ch >= 'a' && ch <= 'z' || ch == '%') { // functions
                        while (ch >= 'a' && ch <= 'z' || ch == '%') {
                            nextChar();
                        }
                        String func = str.substring(startPos, this.pos);
                        x = evaluateFunction(func);
                        eat('(');
                        eat('?');
                        eat(')');
    
                    } else {
                        throw new RuntimeException("Unexpected: " + (char) ch);
                    }
        
                    if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation
        
                    return x;
                }
    
                /**
                 * @param func the function we are trying to find
                 * @throws IOException when YahooFinance.get() times out or has invalid argument
                 * @throws RuntimeException when the function is not predefined
                 * Go through the list of predefined functions and evaluate for their arguments
                 *             return the evaluated expression for the function
                 * Example: sin(90) | the function is sin and the argument is 90, so it solves sin(90)
                 * */
                double evaluateFunction(String func) throws IOException, RuntimeException {
                    double x = 0;
                    switch (func) {
                        case "sqrt":
                            x = Math.sqrt(parseFactor());
                            break;
                        case "sin":
                            x = isDegree ? Math.sin(Math.toRadians(parseFactor())) : Math.sin(parseFactor());
                            break;
                        case "invsin":
                            x = isDegree ? Math.asin(parseFactor()) : Math.asin(Math.toRadians(parseFactor()));
                            break;
                        case "cos":
                            x = isDegree ? Math.cos(Math.toRadians(parseFactor())) : Math.cos(parseFactor());
                            break;
                        case "invcos":
                            x = isDegree ? Math.acos(parseFactor()) : Math.acos(Math.toRadians(parseFactor()));
                            break;
                        case "tan":
                            x = isDegree ? Math.tan(Math.toRadians(parseFactor())) : Math.tan(parseFactor());
                            break;
                        case "invtan":
                            x = isDegree ? Math.atan(parseFactor()) : Math.atan(Math.toRadians(parseFactor()));
                            break;
                        case "vwap":
                            //Get the window, and the timePeriod (separated with a comma)
                            VolumeWeightedAveragePrice vwap = new VolumeWeightedAveragePrice((int) parseFactor());
                            x = vwap.lastVWAP((int) parseFactor());
                            break;
                        case "sma":
                            //Get the window, and the timePeriod (separated with a comma)
                            SimpleMovingAverage sma = new SimpleMovingAverage((int) parseFactor());
                            x = sma.lastSMA((int) parseFactor());
                            break;
                        case "ema":
                            //Get the window, and the timePeriod (separated with a comma)
                            ExponentialMovingAverage ema = new ExponentialMovingAverage((int) parseFactor());
                            x = ema.lastEMA((int) parseFactor());
                            break;
                        case "lr":
                            break;
                        case "price":
                            x = activeStock.getQuote().getPrice() == null ? 0 : activeStock.getQuote().getPrice().doubleValue();
                            break;
                        case "vol":
                            x = activeStock.getQuote().getVolume() == null ? 0 : activeStock.getQuote().getVolume().doubleValue();
                            break;
                        case "bid":
                            x = activeStock.getQuote().getBid() == null ? 0 : activeStock.getQuote().getBid().doubleValue();
                            break;
                        case "bidvol":
                            x = activeStock.getQuote().getBidSize() == null ? 0 : activeStock.getQuote().getBidSize().doubleValue();
                            break;
                        case "ask":
                            x = activeStock.getQuote().getAsk() == null ? 0 : activeStock.getQuote().getAsk().doubleValue();
                            break;
                        case "askvol":
                            x = activeStock.getQuote().getAskSize() == null ? 0 : activeStock.getQuote().getAskSize().doubleValue();
                            break;
                        case "close":
                            x = activeStock.getQuote().getPreviousClose() == null ? 0 : activeStock.getQuote().getPreviousClose().doubleValue();
                            break;
                        case "open":
                            x = activeStock.getQuote().getOpen() == null ? 0 : activeStock.getQuote().getOpen().doubleValue();
                            break;
                        case "open%":
                            x = activeStock.getQuote().getChangeInPercent() == null ? 0 : activeStock.getQuote().getChangeInPercent().doubleValue();
                            break;
                        case "openc":
                            x = activeStock.getQuote().getChange() == null ? 0 : activeStock.getQuote().getChange().doubleValue();
                            break;
                        case "highd":
                            x = activeStock.getQuote().getDayHigh() == null ? 0 : activeStock.getQuote().getDayHigh().doubleValue();
                            break;
                        case "highy":
                            x = activeStock.getQuote().getYearHigh() == null ? 0 : activeStock.getQuote().getYearHigh().doubleValue();
                            break;
                        case "highy%":
                            x = activeStock.getQuote().getChangeFromYearHighInPercent() == null ? 0 : activeStock.getQuote().getChangeFromYearHighInPercent().doubleValue();
                            break;
                        case "lowd":
                            x = activeStock.getQuote().getDayLow() == null ? 0 : activeStock.getQuote().getDayLow().doubleValue();
                            break;
                        case "lowy":
                            x = activeStock.getQuote().getYearLow() == null ? 0 : activeStock.getQuote().getYearLow().doubleValue();
                            break;
                        case "lowy%":
                            x = activeStock.getQuote().getChangeFromYearLowInPercent() == null ? 0 : activeStock.getQuote().getChangeFromYearLowInPercent().doubleValue();
                            break;
                        case "div":
                            x = activeStock.getDividend().getAnnualYield() == null ? 0 : activeStock.getDividend().getAnnualYield().doubleValue();
                            break;
                        case "divy":
                            x = activeStock.getDividend().getAnnualYieldPercent() == null ? 0 : activeStock.getDividend().getAnnualYieldPercent().doubleValue();
                            break;
                        case "bv":
                            x = activeStock.getStats().getBookValuePerShare() == null ? 0 : activeStock.getStats().getBookValuePerShare().doubleValue();
                            break;
                        case "sharefloat":
                            x = activeStock.getStats().getSharesFloat() == null ? 0 : activeStock.getStats().getSharesFloat().doubleValue();
                            break;
                        case "shareout":
                            x = activeStock.getStats().getSharesOutstanding() == null ? 0 : activeStock.getStats().getSharesOutstanding().doubleValue();
                            break;
                        case "ebitda":
                            x = activeStock.getStats().getEBITDA() == null ? 0 : activeStock.getStats().getEBITDA().doubleValue();
                            break;
                        case "eps":
                            x = activeStock.getStats().getEps() == null ? 0 : activeStock.getStats().getEps().doubleValue();
                            break;
                        case "peg":
                            x = activeStock.getStats().getPeg() == null ? 0 : activeStock.getStats().getPeg().doubleValue();
                            break;
                        case "rev":
                            x = activeStock.getStats().getRevenue() == null ? 0 : activeStock.getStats().getRevenue().doubleValue();
                            break;
                        case "shortratio":
                            x = activeStock.getStats().getShortRatio() == null ? 0 : activeStock.getStats().getShortRatio().doubleValue();
                            break;
                        case "pe":
                            x = activeStock.getStats().getPe() == null ? 0 : activeStock.getStats().getPe().doubleValue();
                            break;
                        case "targetprice":
                            x = activeStock.getStats().getOneYearTargetPrice() == null ? 0 : activeStock.getStats().getOneYearTargetPrice().doubleValue();
                            break;
                        default:
                            throw new RuntimeException("Unknown evaluateFunction: " + func);
                    }
                    return x;
                }
    
            }.parse();
        } catch (IOException | RuntimeException e) {
            System.out.println(e.getMessage());
        }
        return 0d;
    }
    
    /**
     * Check to see if the conditional or the discriminator have a constant value that does not need to be constantly
     * reevaluated, for example: conditional = "price(goog)" is a constant for the scan so we can just set the
     * function to take the new conditional of the double / int value.
     * <p></p>
     * @param argument The argument that we are evaluating for constant values, a constant value is defined as a
     *                 mathematical expression that will yield the same value if done at the same absolute time.
     *                 For example: 10/4 is a constant value as it will always yield 2.5; price(goog) is a
     *                 constant value as it will always give the price of goog at the specified time of execution
     *                 which, by virtue of being in the past, is unchangeable and thus constant; price(?) is a
     *                 dynamic/non-constant value as the ? denotes a differing symbol. It could be the price of
     *                 goog or the price of aapl which means that it is not a constant value and cannot be reduced.
     *                 Example: argument = price(?) * ((19/4) + price(goog)) will reduce to price(?) * somenumber
     * @return the reduced mathematical expression
     */
    public String reduce(String argument) {
    
        //Defensive checking
        //Check to see if the given string is empty, if it is, return the empty string as no operations can be
        // productively done on it
        if (argument.isEmpty()) return argument;
        //Clear all spaces as these make the evaluator sometimes screw up
        argument = argument.replaceAll("\\s", "");
        //Check to see if the argument even contains a dynamic value, if it does not, then it can be reduced all the
        // way to its base value by evaluator.evaluate(argument). Return the fully reduced argument
        if (!argument.contains("?")) {
            //The argument has no dynamic values, calculate the evaluation and set argument equal to it
            return "" + this.evaluate(argument);
        }
    
        //The argument contains at least 1 dynamic value, try and isolate the dynamic value and compute the other
        // static values, set the dynamic argument with the static arguments for easier computation later
        ArrayList<String> values = new ArrayList<>();//this will hold all of the values and the operations that will
        // be applied to them in chronological order, but it will be reduced using the order of operations do not worry
        int counter = 0;//This is to count the number of parentheses that occur when trying to isolate a block
        // statement, which is a statement between a set of parentheses; i.e., 4/(9*8) where 9*8 is the block statement
    
        //Iterate through the argument
        for (int i = 0; i < argument.length(); i++) {
            char ch = argument.charAt(i);
            //Check to see if the given character is a lowercase alphabetical character, this denotes a function
            // query starter which has the form: funcName(argument)
            if (ch >= 'a' && ch <= 'z') {
                //Check for opening parenthesis so that we can evaluate the argument
                int startPos = i;
                i = findCharacter(i, argument, '(');
                //Check to see if this function is dynamic or static, denoted by whether or not the function takes
                // the dynamic value ?
                int endPos = findCharacter(i, argument, ')');
                if (argument.substring(i, endPos).contains("?")) {
                    //It was dynamic, gather the argument and then put it into values as we cannot reduce it, the end
                    // of the argument is denoted by a closing parenthesis
                    i = findCharacter(i, argument, ')');
                    values.add(argument.substring(startPos, i+1));
                } else {
                    //static function found, find closing parenthesis
                    i = findCharacter(i, argument, ')');
                    values.add(argument.substring(startPos, i+1));
                }
            }
            //We did not find a function query starter, see if the character is a number that can be evaluated
            else if (ch >= '0' && ch <= '9' || ch == '.') {
                //It was a number, find the end of the number and then add that entire number to values
                int startPos = i;
                while (ch >= '0' && ch <= '9' || ch == '.') ch = argument.charAt(i++);
                values.add(argument.substring(startPos, i));
            }
            //The character was not a number, see if it is the start of a block statement
            else if (ch == '(') {
                //start of a block statement, find the closing parenthesis and then evaluate the block
                for (int j = i + 1; j < argument.length(); j++) {
                    if (argument.charAt(j) == '(') {
                        //another block statement, will be resolved in the recursive call once we find the bigger
                        // block statement and send that to be evaluated
                        counter++;
                    }
                    if (argument.charAt(j) == ')') {
                        if (counter == 0) {
                            //Found the end of the block comments, evaluate the new statement
                            values.add(reduce(argument.substring(i, j+1)));
                            i = j;
                            break;
                        } else counter--;//We did not find the end of the block statement, but we did find the end to
                        // one of them, decrement counter
                    }
                }
            }
            //The character was not the start of a block statement, treat it as a operator like + / * - and put it into
            // values for later operations
            else {
                values.add(ch + "");
            }
        }
    
        //We have finished breaking down the argument into its separate values, now try to evaluate all static
        // functions / values
        StringBuilder reducedArgument = new StringBuilder();//This will be the reduced argument
    
        //Since the order of the values needs to be chronological, the same order as they were put in, it must be a
        // traditional for loop rather than a for-each loop, IDE!
        for (int i = 0; i < values.size(); i++) {
            //See if whatever is in the index is a operand, and then put that into reducedArgument, otherwise, see if
            // it has a ? denoting a dynamic value, if no, evaluate it, if yes, put it into reducedArgument
            switch (values.get(i)) {
                case "(":
                case ")":
                case "*":
                case "/":
                case "-":
                case "+":
                    reducedArgument.append(values.get(i));
                    break;
            
                default:
                    if (values.get(i).contains("?")) {
                        reducedArgument.append(values.get(i));
                    } else {
                        reducedArgument.append(this.evaluate(values.get(i)));
                    }
            }
        }
        return reducedArgument.toString();
    }
    
    /**
     * Find the desired character's first occurrence after the given index from an argument string and return that index
     * <p></p>
     * @param argument The string that we will be polling for a character match
     * @param charToFind The character that we will use for the matching criteria
     * @param index the starting index of the search in argument
     * */
    private int findCharacter(int index, String argument, char charToFind) {
        while (argument.charAt(index) != charToFind) {
            index++;
        }
        return index;
    }
}
