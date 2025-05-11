package algorithm;

import dataHandler.dataSet.Orders;
import dataHandler.dataSet.PaymentMethods;
import dataHandler.dataType.Order;
import dataHandler.dataType.PayMeth;

import java.util.*;

public class PaymentOptimizer {
    private Orders orders;
    private PaymentMethods paymentMethods;

    // -------- FULL PAYMENTS
    // stores the orders assigned to the payment method suitable for them
    private HashMap<String, ArrayList<String>> orders_in_payment = new HashMap<>();
    // stores the values assigned to the payment methods suitable for the order at the same index
    private HashMap<String, ArrayList<Double>> values_in_payment = new HashMap<>();

    // stores the orders fully covered by the points
    private HashSet<String> orders_full_points = new HashSet<>();
    // stores the orders fully covered by the cards
    private HashSet<String> orders_full_cards = new HashSet<>();

    // ---------- PARTIAL PAYMENTS
    // stores the orders already divided between the points and cards with the value stored in points
    private HashMap<String, Double> orders_partial_points = new HashMap<>();
    // stores the orders already divided between the points and cards with the order stored in cards
    private HashMap<String, ArrayList<String>> orders_partial_cards = new HashMap<>();
    // stores the orders already divided between the points and cards with the value stored in cards
    private HashMap<String, ArrayList<Double>> values_partial_cards = new HashMap<>();


    // stores all the names of payments in descending order in terms of the discount provided
    private ArrayList<String> paymentsSorted = new ArrayList<>();

    private boolean withinTheLimit(PayMeth payMeth) {
        String payment_name = payMeth.getId();
        ArrayList<Double> values_checked = values_in_payment.get(payment_name);
        double sum = valueSum(values_checked);
        sum *= 1.0 - payMeth.getDiscount()*0.01;
        return (sum <= payMeth.getLimit());
    }

    private boolean additionWithinTheLimit(PayMeth payMeth, Order order) {
        String payment_name = payMeth.getId();
        ArrayList<Double> values_checked = values_in_payment.get(payment_name);
        double sum = valueSum(values_checked);
        sum *= 1.0 - payMeth.getDiscount()*0.01;
        sum += order.getValue();
        for (String s : orders_partial_points.keySet()) {
            if (payment_name.equals("PUNKTY"))
                sum += orders_partial_points.get(s);
            else
                sum += valueSum(values_partial_cards.get(payment_name));
        }
        return (sum <= payMeth.getLimit());
    }

    private double remaningSpace(PayMeth payMeth) {
        String payment_name = payMeth.getId();
        ArrayList<Double> values_checked = values_in_payment.get(payment_name);
        double sum = valueSum(values_checked);
        sum *= 1.0 - payMeth.getDiscount()*0.01;
        for (String s : orders_partial_points.keySet()) {
            if (payment_name.equals("PUNKTY"))
                sum += orders_partial_points.get(s);
            else
                sum += valueSum(values_partial_cards.get(payment_name));
        }
        return payMeth.getLimit() - sum;
    }

    private double valueSum(ArrayList<Double> values) {
        double sum = 0;
        for (double value : values)
            sum += value;
        return sum;
    }

    public PaymentOptimizer(Orders orders, PaymentMethods paymentMethods) {
        this.orders = orders;
        this.paymentMethods = paymentMethods;

        // initialization of hashmaps storing the orders and values in payments
        PayMeth[] pay_meths_arr = paymentMethods.getPaymentMethods();
        for (PayMeth payMeth : pay_meths_arr) {
            orders_in_payment.put(payMeth.getId(), new ArrayList<>());
            values_in_payment.put(payMeth.getId(), new ArrayList<>());
            paymentsSorted.add(payMeth.getId());
        }

        // distribution of every order to its the best payment method
        Order[] orders_ar = orders.getOrders();
        HashMap<String, PayMeth> hashedPaymentMethods = paymentMethods.getHashedPaymentMethods();
        double points_discount = hashedPaymentMethods.get("PUNKTY").getDiscount(),
                points_limit = hashedPaymentMethods.get("PUNKTY").getLimit();
        for (Order order : orders_ar) {
            String payment_name = "";
            // no payment methods specified - whole order goes to the points
            if (Objects.isNull(order.getPromotions()))
                payment_name = "PUNKTY";
            else {
                // the order is assigned to the best payment method
                HashMap<Double, ArrayList<String>> pay_in_order = order.getPromotionsRate();
                ArrayList<String> best_payments = pay_in_order.get(order.getPromotionsRateSorted().getFirst());
                payment_name = best_payments.getFirst();
                double pay_discount = hashedPaymentMethods.get(payment_name).getDiscount();
                if (pay_discount <= points_discount
                && (100.0 - points_discount)*0.01*order.getValue() <= points_limit)
                    payment_name = "PUNKTY";
            }

            ArrayList<String> ord = orders_in_payment.get(payment_name);
            ord.add(order.getId());
            orders_in_payment.put(payment_name, ord);

            ArrayList<Double> vals = values_in_payment.get(payment_name);
            vals.add(order.getValue()*0.01*(100.0 - hashedPaymentMethods.get(payment_name).getDiscount()));
            values_in_payment.put(payment_name, vals);

            if (payment_name.equals("PUNKTY"))
                orders_full_points.add(order.getId());
            else
                orders_full_cards.add(order.getId());
        }

        // sorting the payments
        for (int i = 0; i < paymentsSorted.size(); i++) {
            for (int j = i + 1; j < paymentsSorted.size(); j++) {
                if (hashedPaymentMethods.get(paymentsSorted.get(i)).getDiscount() < hashedPaymentMethods.get(paymentsSorted.get(j)).getDiscount()) {
                    String temp = paymentsSorted.get(i);
                    paymentsSorted.set(i, paymentsSorted.get(j));
                    paymentsSorted.set(j, temp);
                }
            }
        }

        // at the end, all the orders are assigned to the best payment for them based on the discount rate
        // nevertheless, this doesn't take care about the limit on the cards - this will be optimized further
    }

    public void printResults() {
        for (String p : paymentsSorted) {
            System.out.print(p + " ");
            System.out.print(valueSum(values_in_payment.get(p)));
            System.out.print(" ");
            System.out.println(orders_in_payment.get(p));
        }
    }

    // reduces the orders paid through the cards if the value for card exceeds the limit
    public void cardsReduce() {
        HashMap<String, PayMeth> payments_hashed = paymentMethods.getHashedPaymentMethods();
        HashMap<String, Order> orders_hashed = orders.getHashedOrders();

        for (String c : paymentsSorted) {
            if (c.equals("PUNKTY"))
                continue;
            System.out.println(c);
            if (withinTheLimit(payments_hashed.get(c)))
                continue;
            ArrayList<String> verif_orders = orders_in_payment.get(c);
            ArrayList<Double> verif_values = values_in_payment.get(c);
            // moving every the lowest payment for the lower one as long as the card exceeds the limit
            for (int i = 0; i < verif_orders.size(); i++) {
                if (withinTheLimit(payments_hashed.get(c)))
                    break;
                // looking for the lowest order
                String lowest = "";
                double value = 1000000.0;
                for (int j = 0; j < verif_orders.size(); j++) {
                    if (value > verif_values.get(j)) {
                        lowest = verif_orders.get(j);
                        value = verif_values.get(j);
                    }
                }
                Order lowest_order = orders_hashed.get(lowest);
                System.out.println(lowest_order.getId());
                // moving the lowest found order to the worse payment as long as it's possible
                String newPayment = lowest_order.moveToLowerDiscount(payments_hashed.get(c));
                while (!Objects.isNull(newPayment)) {
                    System.out.println(newPayment);
                    if(additionWithinTheLimit(payments_hashed.get(newPayment), lowest_order)) {
                        // deleting the order from the overloaded payment
                        int ind = verif_orders.indexOf(lowest_order.getId());
                        verif_orders.remove(ind);
                        verif_values.remove(ind);
                        orders_in_payment.put(c, verif_orders);
                        values_in_payment.put(c, verif_values);
                        // adding the order to the new payment
                        ArrayList<Double> vals = values_in_payment.get(newPayment);
                        vals.add(lowest_order.getValue());
                        values_in_payment.put(newPayment, vals);
                        ArrayList<String> ords = orders_in_payment.get(newPayment);
                        ords.add(lowest_order.getId());
                        orders_in_payment.put(newPayment, ords);
                        break;
                    }
                    newPayment = lowest_order.moveToLowerDiscount(payments_hashed.get(newPayment));
                }
                if (!Objects.isNull(newPayment)) {
                    System.out.println("Order moved from " + c + " to " + newPayment);
                    continue;
                }
                // new payment is null - can't be assigned to the discount one
                // is assigned to the non-discount ones
                for (String newPay : paymentsSorted) {
                    if(newPay.equals("PUNKTY"))
                        continue;
                    newPayment = newPay;
                    if (withinTheLimit(payments_hashed.get(c))) {
                        // deleting the order from the overloaded payment
                        int ind = verif_orders.indexOf(lowest_order.getId());
                        verif_orders.remove(ind);
                        verif_values.remove(ind);
                        orders_in_payment.put(c, verif_orders);
                        values_in_payment.put(c, verif_values);
                        // adding the order to the new payment
                        ArrayList<Double> vals = values_in_payment.get(newPayment);
                        vals.add(lowest_order.getValue());
                        values_in_payment.put(newPayment, vals);
                        ArrayList<String> ords = orders_in_payment.get(newPayment);
                        ords.add(lowest_order.getId());
                        orders_in_payment.put(newPayment, ords);
                        break;
                    }
                }
                if (!Objects.isNull(newPayment)){
                    System.out.println("Order moved from " + c + " to " + newPayment);
                    continue;
                }
                // new payment is still null
            }
        }
    }

    // reduces the orders paid through the points if the value for points exceeds the limit
    // !!! shifts only the part of the order
    public void pointsReduce() {
        HashMap<String, PayMeth> payments_hashed = paymentMethods.getHashedPaymentMethods();
        HashMap<String, Order> orders_hashed = orders.getHashedOrders();

        PayMeth points = payments_hashed.get("PUNKTY");
        while (!withinTheLimit(points)) {
            double limit_exceed = valueSum(values_in_payment.get(points.getId())) - points.getLimit();
            // looking for the least worth order in points
            String least_worths_order = "";
            double worth = points.getLimit();
            for (String or : orders_full_points) {
                Order temp_order = orders_hashed.get(or);
                if (temp_order.getValue() < worth) {
                    least_worths_order = or;
                    worth = temp_order.getValue();
                }
            }
            // calculating the minimal amount of points to remove
            double transfer_rate = 0.0;
            if (worth*(1.0 - (double)points.getDiscount()*0.01) < limit_exceed)
                transfer_rate = 1.0;
            else if (worth*0.1 < limit_exceed)
                transfer_rate = 0.9;
            else
                transfer_rate = limit_exceed/worth;

            // looking fot the bank to move the part of points
            for (String c : paymentsSorted) {
                if (c.equals("PUNKTY"))
                    continue;
                // checking the amount of money that can be transfered
                double rem = remaningSpace(payments_hashed.get(c));
                if (rem < worth*transfer_rate) {
                    double partial_transfer = rem/(worth*transfer_rate);

                }


            }

        }

    }

    // reduces the orders paid through the points if the value for points exceeds the limit
    public void pointsSplit() {}
}
