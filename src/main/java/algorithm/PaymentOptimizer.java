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
    // stores all the names of payments in descending order in terms of the remaining space in them
    private ArrayList<String> paymentsSortedRemainingSpace = new ArrayList<>();

    private boolean withinTheLimit(PayMeth payMeth) {
        return (remainingSpace(payMeth) >= 0.0);
    }

    private boolean additionWithinTheLimit(PayMeth payMeth, Order order) {
        String payment_name = payMeth.getId();
        ArrayList<Double> values_checked = values_in_payment.get(payment_name);
        double sum = valueSum(values_checked);
        sum *= 1.0 - (double)payMeth.getDiscount()*0.01;
        sum += order.getValue();
        if (payment_name.equals("PUNKTY")) {
            for (String s : orders_partial_points.keySet())
                sum += orders_partial_points.get(s);
        }
        else
            sum += valueSum(values_partial_cards.get(payment_name));

        return (sum <= payMeth.getLimit());
    }

    private double remainingSpace(PayMeth payMeth) {
        String payment_name = payMeth.getId();
        double sum = valueSum(values_in_payment.get(payment_name));
        sum *= 1.0 - (double)payMeth.getDiscount()*0.01;
        for (String s : orders_partial_points.keySet()) {
            if (payment_name.equals("PUNKTY"))
                sum += orders_partial_points.get(s);
            else
                sum += valueSum(values_partial_cards.get(payment_name));
        }
        return payMeth.getLimit() - sum;
    }

    private double valueSum(ArrayList<Double> values) {
        if (Objects.isNull(values))
            return 0;
        double sum = 0;
        for (double value : values)
            sum += value;
        return sum;
    }

    public PaymentOptimizer(Orders orders, PaymentMethods paymentMethods) {
        this.orders = orders;
        this.paymentMethods = paymentMethods;
        this.orders.sortPromotions(this.paymentMethods);

        // initialization of hashmaps storing the orders and values in payments
        PayMeth[] pay_meths_arr = paymentMethods.getPaymentMethods();
        for (PayMeth payMeth : pay_meths_arr) {
            orders_in_payment.put(payMeth.getId(), new ArrayList<>());
            values_in_payment.put(payMeth.getId(), new ArrayList<>());
            paymentsSorted.add(payMeth.getId());
            paymentsSortedRemainingSpace.add(payMeth.getId());
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
            vals.add(order.getValue());
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
        HashMap<String, PayMeth> hashedPaymentMethods = paymentMethods.getHashedPaymentMethods();
        for (String p : paymentsSorted) {
            System.out.print(p + " ");
            System.out.println(hashedPaymentMethods.get(p).getLimit() - remainingSpace(hashedPaymentMethods.get(p)));
        }
    }

    // reduces the orders paid through the cards if the value for card exceeds the limit
    public void cardsReduce() {
        HashMap<String, PayMeth> payments_hashed = paymentMethods.getHashedPaymentMethods();
        HashMap<String, Order> orders_hashed = orders.getHashedOrders();

        for (String c : paymentsSorted) {
            if (c.equals("PUNKTY"))
                continue;
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
                // moving the lowest found order to the worse payment as long as it's possible
                String newPayment = lowest_order.moveToLowerDiscount(payments_hashed.get(c));
                while (!Objects.isNull(newPayment)) {
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
                if (!Objects.isNull(newPayment))
                    continue;
            }
        }
    }

//    // reduces the orders paid through the points if the value for points exceeds the limit
//    public void pointsReduce() {
//        HashMap<String, PayMeth> payments_hashed = paymentMethods.getHashedPaymentMethods();
//        HashMap<String, Order> orders_hashed = orders.getHashedOrders();
//
//        PayMeth points = payments_hashed.get("PUNKTY");
//        ArrayList<String> verif_orders = orders_in_payment.get("PUNKTY");
//        ArrayList<Double> verif_values = values_in_payment.get("PUNKTY");
//
//        // moving every the lowest payment for the another card payment as long as the points exceeds the limit
//        while (!withinTheLimit(points)) {
//            // looking for the lowest order
//            String lowest = "";
//            double value = 1000000.0;
//            for (int j = 0; j < verif_orders.size(); j++) {
//                if (value > verif_values.get(j)) {
//                    lowest = verif_orders.get(j);
//                    value = verif_values.get(j);
//                }
//            }
//            if (lowest.isEmpty())
//                break;
//            Order lowest_order = orders_hashed.get(lowest);
//            System.out.println(lowest);
//
//            // moving the lowest found order to the worse payment as long as it's possible
//            String newPayment = lowest_order.moveToLowerDiscount(payments_hashed.get("PUNKTY"));
//            while (!Objects.isNull(newPayment)) {
//                System.out.println(newPayment);
//                if (additionWithinTheLimit(payments_hashed.get(newPayment), lowest_order)) {
//                    // deleting the order from the overloaded payment
//                    int ind = verif_orders.indexOf(lowest_order.getId());
//                    verif_orders.remove(ind);
//                    verif_values.remove(ind);
//                    orders_in_payment.put("PUNKTY", verif_orders);
//                    values_in_payment.put("PUNKTY", verif_values);
//                    // adding the order to the new payment
//                    ArrayList<Double> vals = values_in_payment.get(newPayment);
//                    vals.add(lowest_order.getValue());
//                    values_in_payment.put(newPayment, vals);
//                    ArrayList<String> ords = orders_in_payment.get(newPayment);
//                    ords.add(lowest_order.getId());
//                    orders_in_payment.put(newPayment, ords);
//                    break;
//                }
//                newPayment = lowest_order.moveToLowerDiscount(payments_hashed.get(newPayment));
//            }
//            if (!Objects.isNull(newPayment)) {
//                continue;
//            }
//            // new payment is null - can't be assigned to the discount one
//            // is assigned to the non-discount ones
//            for (String newPay : paymentsSorted) {
//                if (newPay.equals("PUNKTY"))
//                    continue;
//                newPayment = newPay;
//                if (withinTheLimit(payments_hashed.get("PUNKTY"))) {
//                    // deleting the order from the overloaded payment
//                    int ind = verif_orders.indexOf(lowest_order.getId());
//                    verif_orders.remove(ind);
//                    verif_values.remove(ind);
//                    orders_in_payment.put("PUNKTY", verif_orders);
//                    values_in_payment.put("PUNKTY", verif_values);
//                    // adding the order to the new payment
//                    ArrayList<Double> vals = values_in_payment.get(newPayment);
//                    vals.add(lowest_order.getValue());
//                    values_in_payment.put(newPayment, vals);
//                    ArrayList<String> ords = orders_in_payment.get(newPayment);
//                    ords.add(lowest_order.getId());
//                    orders_in_payment.put(newPayment, ords);
//                    break;
//                }
//            }
//            if (!Objects.isNull(newPayment)) {
//                continue;
//            }
//
//        }
//    }

    // splits the orders paid through the points if the value for points exceeds the limit
    public void pointsSplit() {
        HashMap<String, PayMeth> payments_hashed = paymentMethods.getHashedPaymentMethods();
        HashMap<String, Order> orders_hashed = orders.getHashedOrders();

        PayMeth points = payments_hashed.get("PUNKTY");
        while (!withinTheLimit(points)) {
            // sorting the rest of payments in terms of remaining space
            for (int i = 0; i < paymentsSortedRemainingSpace.size(); i++) {
                for (int j = i + 1; j < paymentsSortedRemainingSpace.size(); j++) {
                    if (remainingSpace(payments_hashed.get(paymentsSortedRemainingSpace.get(i))) < remainingSpace(payments_hashed.get(paymentsSorted.get(j)))) {
                        String temp = paymentsSortedRemainingSpace.get(i);
                        paymentsSortedRemainingSpace.set(i, paymentsSortedRemainingSpace.get(j));
                        paymentsSortedRemainingSpace.set(j, temp);
                    }
                }
            }

            double limit_exceed = remainingSpace(points)*-1.0;
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
            if (least_worths_order.isEmpty())
                break;
            // calculating the minimal amount of points to remove
            double transfer_rate;
            transfer_rate = points.getDiscount()*0.01 + limit_exceed/worth;
            if (transfer_rate > 1.0)
                transfer_rate = 0.9;
            boolean discount10 = true;
            if (transfer_rate >= 0.9)
                discount10 = false;
            orders_partial_points.put(least_worths_order, (1.0 - transfer_rate)*worth);
            orders_full_points.remove(least_worths_order);
            ArrayList<String> ords_d = orders_in_payment.get("PUNKTY");
            ords_d.remove(least_worths_order);
            orders_in_payment.put("PUNKTY", ords_d);
            ArrayList<Double> vals_d = values_in_payment.get(points.getId());
            vals_d.remove(worth);
            values_in_payment.put(points.getId(), vals_d);
            double discount = discount10 ? 0.1 : 0.0;
            transfer_rate -= discount;

            // looking for the bank to move the part of points
            for (String c : paymentsSortedRemainingSpace) {
                if (transfer_rate <= 0.0)
                    break;
                if (c.equals("PUNKTY"))
                    continue;
                // checking the amount of money that can be transfered
                double rem = remainingSpace(payments_hashed.get(c)),
                        partial_transfer;
                if (rem < worth*transfer_rate)
                    partial_transfer = rem/(worth*transfer_rate);
                else
                    partial_transfer = transfer_rate;
                if(orders_partial_cards.containsKey(c)) {
                    ArrayList<String> ords = orders_partial_cards.get(c);
                    ords.add(least_worths_order);
                    orders_partial_cards.put(c, ords);
                    ArrayList<Double> vals = values_partial_cards.get(c);
                    vals.add(partial_transfer*worth);
                    values_partial_cards.put(c, vals);
                }
                else {
                    ArrayList<Double> vals = new ArrayList<>();
                    ArrayList<String> ords = new ArrayList<>();
                    vals.add(partial_transfer*worth);
                    ords.add(least_worths_order);
                    orders_partial_cards.put(c, ords);
                    values_partial_cards.put(c, vals);
                }
                transfer_rate -= partial_transfer;
            }
        }
    }

    // splits the orders paid through the cards if the value for points exceeds the limit
    public void cardsSplit() {
        HashMap<String, PayMeth> payments_hashed = paymentMethods.getHashedPaymentMethods();
        HashMap<String, Order> orders_hashed = orders.getHashedOrders();

        for (String s : paymentsSorted) {
            PayMeth pay = payments_hashed.get(s);
            if (s.equals("PUNKTY"))
                continue;
            if (withinTheLimit(payments_hashed.get(s)))
                continue;
            ArrayList<String> verif_orders = orders_in_payment.get(s);
            ArrayList<Double> verif_values = values_in_payment.get(s);
            // moving every the lowest payment to the another one with remaining space
            while (!withinTheLimit(pay)) {
                // sorting the rest of payments in terms of remaining space
                for (int i = 0; i < paymentsSortedRemainingSpace.size(); i++) {
                    for (int j = i + 1; j < paymentsSortedRemainingSpace.size(); j++) {
                        if (remainingSpace(payments_hashed.get(paymentsSortedRemainingSpace.get(i))) < remainingSpace(payments_hashed.get(paymentsSorted.get(j)))) {
                            String temp = paymentsSortedRemainingSpace.get(i);
                            paymentsSortedRemainingSpace.set(i, paymentsSortedRemainingSpace.get(j));
                            paymentsSortedRemainingSpace.set(j, temp);
                        }
                    }
                }
                double limit_exceed = remainingSpace(pay)*-1.0;
                // looking for the least worth order in points
                String least_worths_order = "";
                double worth = pay.getLimit();
                for (String or : orders_full_points) {
                    Order temp_order = orders_hashed.get(or);
                    if (temp_order.getValue() < worth) {
                        least_worths_order = or;
                        worth = temp_order.getValue();
                    }
                }
                if (least_worths_order.isEmpty())
                    break;
                // calculating the minimal amount of points to remove
                double transfer_rate;
                transfer_rate = pay.getDiscount()*0.01 + limit_exceed/worth;
                if (transfer_rate > 1.0)
                    transfer_rate = 1.0;
                if (orders_partial_cards.containsKey(s)) {
                    ArrayList<String> ords = orders_partial_cards.get(s);
                    ords.add(least_worths_order);
                    orders_partial_cards.put(s, ords);
                    ArrayList<Double> vals = values_partial_cards.get(s);
                    vals.add(transfer_rate*worth);
                    values_partial_cards.put(s, vals);
                }
                orders_full_points.remove(least_worths_order);
                ArrayList<String> ords_d = orders_in_payment.get(s);
                ords_d.remove(least_worths_order);
                orders_in_payment.put(s, ords_d);
                ArrayList<Double> vals_d = values_in_payment.get(pay.getId());
                vals_d.remove(worth);
                values_in_payment.put(pay.getId(), vals_d);

                // looking for the bank to move the part of points
                for (String c : paymentsSortedRemainingSpace) {
                    if (transfer_rate <= 0.0)
                        break;
                    if (c.equals(s) || c.equals("PUNKTY"))
                        continue;
                    // checking the amount of money that can be transfered
                    double rem = remainingSpace(payments_hashed.get(c)),
                            partial_transfer;
                    if (rem < worth * transfer_rate)
                        partial_transfer = rem / (worth * transfer_rate);
                    else
                        partial_transfer = transfer_rate;
                    if (orders_partial_cards.containsKey(c)) {
                        ArrayList<String> ords = orders_partial_cards.get(c);
                        ords.add(least_worths_order);
                        orders_partial_cards.put(c, ords);
                        ArrayList<Double> vals = values_partial_cards.get(c);
                        vals.add(partial_transfer * worth);
                        values_partial_cards.put(c, vals);
                    } else {
                        ArrayList<Double> vals = new ArrayList<>();
                        ArrayList<String> ords = new ArrayList<>();
                        vals.add(partial_transfer * worth);
                        ords.add(least_worths_order);
                        orders_partial_cards.put(c, ords);
                        values_partial_cards.put(c, vals);
                    }
                    transfer_rate -= partial_transfer;
                }
            }
        }

    }

    // checks if all the payments don't exceed the limit
    public boolean allPaymentsOk() {
        HashMap<String, PayMeth> hashedPayments = paymentMethods.getHashedPaymentMethods();
        for (String p : hashedPayments.keySet()) {
            if (!withinTheLimit(hashedPayments.get(p))) {
                System.out.println(p);
                System.out.println(remainingSpace(hashedPayments.get(p)));
                System.out.println(orders_in_payment.get(p));
                System.out.println(values_in_payment.get(p));
                return false;
            }
        }
        return true;
    }
}
