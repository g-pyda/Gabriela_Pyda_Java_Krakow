package dataHandler.dataSet;

import java.util.HashMap;
import dataHandler.dataType.PayMeth;

public class PaymentMethods extends DataSet<PayMeth>{

    public PayMeth[] getPaymentMethods() {
        return super.set;
    }

    public HashMap<String, PayMeth> getHashedPaymentMethods() {
        HashMap<String, PayMeth> result = new HashMap<String, PayMeth>();
        for (PayMeth p : super.set) {
            result.put(p.getId(), p);
        }
        return result;
    }

    public PaymentMethods(PayMeth[] paymentMethods) {
        super(paymentMethods);
    }
}
