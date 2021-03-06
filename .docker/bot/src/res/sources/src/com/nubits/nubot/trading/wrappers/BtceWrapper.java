/*
 * Copyright (C) 2014 desrever <desrever at nubits.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.nubits.nubot.trading.wrappers;

import com.nubits.nubot.exchanges.Exchange;
import com.nubits.nubot.global.Constant;
import com.nubits.nubot.global.Global;
import com.nubits.nubot.models.Amount;
import com.nubits.nubot.models.ApiError;
import com.nubits.nubot.models.ApiResponse;
import com.nubits.nubot.models.Balance;
import com.nubits.nubot.models.Currency;
import com.nubits.nubot.models.CurrencyPair;
import com.nubits.nubot.models.Order;
import com.nubits.nubot.trading.ServiceInterface;
import com.nubits.nubot.trading.Ticker;
import com.nubits.nubot.trading.TradeInterface;
import com.nubits.nubot.trading.keys.ApiKeys;
import com.nubits.nubot.utils.TradeUtils;
import com.nubits.nubot.utils.Utils;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author desrever <desrever at nubits.com>
 */
public class BtceWrapper implements TradeInterface {

    private static final Logger LOG = Logger.getLogger(BtceWrapper.class.getName());
    //Class fields
    private ApiKeys keys;
    private Exchange exchange;
    private String checkConnectionUrl = "http://btc-e.com";
    private final String SIGN_HASH_FUNCTION = "HmacSHA512";
    private final String ENCODING = "UTF-8";
    private final String API_BASE_URL = "https://btc-e.com/tapi/";
    private final String API_GET_INFO = "getInfo";
    private final String API_TRADE = "Trade";
    private final String API_ACTIVE_ORDERS = "ActiveOrders";
    private final String API_CANCEL_ORDER = "CancelOrder";
    private final String API_GET_FEE = "https://btc-e.com/exchange/";
    private final String API_TICKER_USD = "https://btc-e.com/api/2/btc_usd/ticker";
    // Errors
    private ArrayList<ApiError> errors;
    private final String TOKEN_ERR = "error";
    private final int ERROR_UNKNOWN = 4560;
    private final int ERROR_NO_CONNECTION = 4561;
    private final int ERROR_GENERIC = 4562;
    private final int ERROR_PARSING = 4563;
    private final int ERROR_ORDER_NOT_FOUND = 4564;
    private final int ERROR_SCRAPING_HTML = 4565;
    private final int ERRROR_API = 4566;

    public BtceWrapper() {
        setupErrors();

    }

    public BtceWrapper(ApiKeys keys, Exchange exchange) {
        this.keys = keys;
        this.exchange = exchange;
        setupErrors();

    }

    private void setupErrors() {
        errors = new ArrayList<ApiError>();
        errors.add(new ApiError(ERROR_NO_CONNECTION, "Failed to connect to the exchange entrypoint. Verify your connection"));

    }

    protected static String createNonce() {
        long toRet = Math.round(System.currentTimeMillis() / 1000);
        return Long.toString(toRet);
    }

    @Override
    public ApiResponse getAvailableBalances(CurrencyPair pair) {
        ApiResponse apiResponse = new ApiResponse();
        Balance balance = new Balance();

        String path = API_GET_INFO;
        HashMap<String, String> query_args = new HashMap<>();
        /*Params
         *
         */

        String queryResult = query(API_BASE_URL, path, query_args, false);
        if (queryResult.startsWith(TOKEN_ERR)) {
            apiResponse.setError(getErrorByCode(ERROR_NO_CONNECTION));
            return apiResponse;
        }


        /*Sample result
         *{
         *"success":1,
         *"return":{
         *	"funds":{
         *		"usd":325,
         *		"btc":23.998,
         *		"sc":121.998,
         *		"ltc":0,
         *		"ruc":0,
         *		"nmc":0
         *	},
         *	"rights":{
         *		"info":1,
         *		"trade":1
         *	},
         *	"transaction_count":80,
         *	"open_orders":1,
         *	"server_time":1342123547
         *      }
         *}
         */


        JSONParser parser = new JSONParser();
        try {
            JSONObject httpAnswerJson = (JSONObject) (parser.parse(queryResult));
            long success = (long) httpAnswerJson.get("success");
            if (success == 0) {
                //error
                String errorMessage = (String) httpAnswerJson.get("error");
                ApiError apiErr = new ApiError(ERROR_GENERIC, errorMessage);

                LOG.severe("Btce returned an error: " + errorMessage);

                apiResponse.setError(apiErr);
                return apiResponse;
            } else {
                //correct
                JSONObject dataJson = (JSONObject) httpAnswerJson.get("return");
                JSONObject funds = (JSONObject) dataJson.get("funds");

                String pegCode = pair.getPaymentCurrency().getCode().toLowerCase();
                String nbtCode = pair.getOrderCurrency().getCode().toLowerCase();

                Amount PEGTotal = new Amount((Double) funds.get(pegCode), Constant.USD);
                Amount NBTTotal = new Amount((Double) funds.get(nbtCode), Constant.NBT);

                balance = new Balance(NBTTotal, PEGTotal);


                //Pack it into the ApiResponse
                apiResponse.setResponseObject(balance);

            }
        } catch (ParseException ex) {
            LOG.severe("httpresponse: " + queryResult + " \n" + ex.getMessage());
            apiResponse.setError(new ApiError(ERROR_PARSING, "Error while parsing the balance response"));
            return apiResponse;
        }

        return apiResponse;


    }

    @Override
    public ApiResponse getAvailableBalance(Currency currency) {
        ApiResponse apiResponse = new ApiResponse();
        Balance balance = new Balance();

        String path = API_GET_INFO;
        HashMap<String, String> query_args = new HashMap<>();
        /*Params
         *
         */

        String queryResult = query(API_BASE_URL, path, query_args, false);
        if (queryResult.startsWith(TOKEN_ERR)) {
            apiResponse.setError(getErrorByCode(ERROR_NO_CONNECTION));
            return apiResponse;
        }


        /*Sample result
         *{
         *"success":1,
         *"return":{
         *	"funds":{
         *		"usd":325,
         *		"btc":23.998,
         *		"sc":121.998,
         *		"ltc":0,
         *		"ruc":0,
         *		"nmc":0
         *	},
         *	"rights":{
         *		"info":1,
         *		"trade":1
         *	},
         *	"transaction_count":80,
         *	"open_orders":1,
         *	"server_time":1342123547
         *      }
         *}
         */


        JSONParser parser = new JSONParser();
        try {
            JSONObject httpAnswerJson = (JSONObject) (parser.parse(queryResult));
            long success = (long) httpAnswerJson.get("success");
            if (success == 0) {
                //error
                String errorMessage = (String) httpAnswerJson.get("error");
                ApiError apiErr = new ApiError(ERROR_GENERIC, errorMessage);

                LOG.severe("Btce returned an error: " + errorMessage);

                apiResponse.setError(apiErr);
                return apiResponse;
            } else {
                //correct
                JSONObject dataJson = (JSONObject) httpAnswerJson.get("return");
                JSONObject funds = (JSONObject) dataJson.get("funds");

                Amount amount = new Amount((Double) funds.get(currency.getCode().toLowerCase()), currency);

                //Pack it into the ApiResponse
                apiResponse.setResponseObject(amount);

            }
        } catch (ParseException ex) {
            LOG.severe("httpresponse: " + queryResult + " \n" + ex.getMessage());
            apiResponse.setError(new ApiError(ERROR_PARSING, "Error while parsing the balance response"));
            return apiResponse;
        }

        return apiResponse;


    }

    @Override
    public ApiResponse getLastPrice(CurrencyPair pair) {
        Ticker ticker = new Ticker();
        ApiResponse apiResponse = new ApiResponse();

        String path = API_TICKER_USD;

        double last = -1;
        double ask = -1;
        double bid = -1;
        HashMap<String, String> query_args = new HashMap<>();
        /*Params
         *
         */

        String queryResult = query(path, query_args, false);

        /*Sample result
         * {"ticker":{"high":103.6,"low":100.14944,"avg":101.87472,"vol":3571.71545,"vol_cur":35.02405,"last":102,"buy":103.26,"sell":101.77,"server_time":1369650996}}
         */
        JSONParser parser = new JSONParser();
        try {
            JSONObject httpAnswerJson = (JSONObject) (parser.parse(queryResult));
            JSONObject tickerObject = (JSONObject) httpAnswerJson.get("ticker");

            last = Utils.getDouble(tickerObject.get("last"));
            bid = Utils.getDouble(tickerObject.get("sell"));
            ask = Utils.getDouble(tickerObject.get("buy"));

        } catch (ParseException ex) {
            LOG.severe("httpresponse: " + queryResult + " \n" + ex.getMessage());
            apiResponse.setError(new ApiError(ERROR_PARSING, "Error while parsing the balance response"));
            return apiResponse;
        }

        ticker.setAsk(ask);
        ticker.setBid(bid);
        ticker.setLast(last);
        apiResponse.setResponseObject(ticker);
        return apiResponse;
    }

    @Override
    public ApiResponse sell(CurrencyPair pair, double amount, double rate) {
        return enterOrder(Constant.SELL, pair, amount, rate);
    }

    @Override
    public ApiResponse buy(CurrencyPair pair, double amount, double rate) {
        return enterOrder(Constant.BUY, pair, amount, rate);
    }

    @Override
    public ApiResponse getActiveOrders() {
        return getActiveOrdersImpl(null);
    }

    @Override
    public ApiResponse getActiveOrders(CurrencyPair pair) {
        return getActiveOrdersImpl(pair);
    }

    @Override
    public ApiResponse getOrderDetail(String orderID) {
        ApiResponse apiResp = new ApiResponse();
        Order order = null;

        ApiResponse listApiResp = getActiveOrders();
        if (listApiResp.isPositive()) {
            ArrayList<Order> orderList = (ArrayList<Order>) listApiResp.getResponseObject();
            boolean found = false;
            for (int i = 0; i < orderList.size(); i++) {
                Order tempOrder = orderList.get(i);
                if (orderID.equals(tempOrder.getId())) {
                    found = true;
                    apiResp.setResponseObject(tempOrder);
                    return apiResp;
                }
            }
            if (!found) {
                apiResp.setError(new ApiError(ERROR_ORDER_NOT_FOUND, "Cannot find the order with id " + orderID));
                return apiResp;

            }
        } else {
            return listApiResp;
        }


        return apiResp;
    }

    @Override
    public ApiResponse cancelOrder(String orderID) {
        ApiResponse apiResponse = new ApiResponse();

        String path = API_CANCEL_ORDER;
        HashMap<String, String> query_args = new HashMap<>();
        /*Params
         *  order_id
         */

        query_args.put("order_id", orderID);

        String queryResult = query(API_BASE_URL, path, query_args, false);
        if (queryResult.startsWith(TOKEN_ERR)) {
            apiResponse.setError(getErrorByCode(ERROR_NO_CONNECTION));
            return apiResponse;
        }


        /*Sample result
         *{
         "success":1,
         "return":{
         "order_id":343154,
         "funds":{
         "usd":325,
         "btc":24.998,
         "sc":121.998,
         "ltc":0,
         "ruc":0,
         "nmc":0
         }
         }
         }
         */


        JSONParser parser = new JSONParser();
        try {
            JSONObject httpAnswerJson = (JSONObject) (parser.parse(queryResult));
            long success = (long) httpAnswerJson.get("success");
            if (success == 0) {
                //error
                String errorMessage = (String) httpAnswerJson.get("error");
                apiResponse.setResponseObject(false);

                LOG.severe("Btce returned an error: " + errorMessage);

                return apiResponse;
            } else {
                //correct
                //Pack it into the ApiResponse
                apiResponse.setResponseObject(true);

            }
        } catch (ParseException ex) {
            LOG.severe("httpresponse: " + queryResult + " \n" + ex.getMessage());
            apiResponse.setError(new ApiError(ERROR_PARSING, "Error while parsing the response"));
            return apiResponse;
        }

        return apiResponse;
    }

    @Override
    public ApiResponse getTxFee() {
        if (Global.options != null) {
            return new ApiResponse(true, Global.options.getTxFee(), null);
        } else {
            ApiResponse apiResponse = new ApiResponse();

            String strDelimiterStart = "the fee for transactions is ";
            String strDelimterStop = "%.</p>";
            String content = "ERROR";
            try {
                content = Utils.getHTML(API_GET_FEE);
            } catch (IOException ex) {
                LOG.severe(ex.getMessage());
            }
            if (!content.equals("ERROR")) {
                int startIndex = content.lastIndexOf(strDelimiterStart) + strDelimiterStart.length();
                int stopIndex = content.lastIndexOf(strDelimterStop);
                String feeString = content.substring(startIndex, stopIndex);
                double fee = Double.parseDouble(feeString);
                apiResponse.setResponseObject(fee);
            } else {
                apiResponse.setError(new ApiError(ERROR_SCRAPING_HTML, "Error scraping HTML"
                        + " while opening " + API_GET_FEE));
            }

            return apiResponse;
        }
    }

    @Override
    public ApiResponse getTxFee(CurrencyPair pair) {
        LOG.severe("Btc-e uses global TX fee, currency pair not supprted. \n"
                + "now calling getTxFee()");
        return getTxFee();
    }

    /*
     public ApiResponse getPermissions() {
     ApiResponse apiResponse = new ApiResponse();
     String path = API_GET_INFO;
     HashMap<String, String> query_args = new HashMap<>();

     ApiPermissions permissions = new ApiPermissions(false, false, false, false, false, false);
     String queryResult = query(API_BASE_URL, API_GET_INFO, query_args, false);
     if (queryResult.startsWith(TOKEN_ERR)) {
     apiResponse.setError(new ApiError(ERROR_GENERIC, "Generic error with btce service call"));
     return apiResponse;
     }

     if (queryResult.equals(ERROR_NO_CONNECTION)) {
     apiResponse.setError(getErrorByCode(ERROR_NO_CONNECTION));
     return apiResponse;
     }
     /*Sample result
     *{
     *"success":1,
     *"return":{
     *	"funds":{
     *		"usd":325,
     *		"btc":23.998,
     *		"sc":121.998,
     *		"ltc":0,
     *		"ruc":0,
     *		"nmc":0
     *	},
     *	"rights":{
     *		"info":1,
     *		"trade":1
     *	},
     *	"transaction_count":80,
     *	"open_orders":1,
     *	"server_time":1342123547
     *      }
     *}
     */
    /*
     JSONParser parser = new JSONParser();
     try {
     JSONObject httpAnswerJson = (JSONObject) (parser.parse(queryResult));
     long success = (long) httpAnswerJson.get("success");
     if (success == 0) {
     //error
     String error = (String) httpAnswerJson.get("error");
     apiResponse.setError(new ApiError(ERRROR_API, error));
     LOG.severe("Btce returned an error: " + error);
     return apiResponse;
     } else {
     //correct
     JSONObject dataJson = (JSONObject) httpAnswerJson.get("return");
     JSONObject rightsJson = (JSONObject) dataJson.get("rights");

     long info = (long) rightsJson.get("info");
     long trade = (long) rightsJson.get("trade");
     long withdraw = (long) rightsJson.get("withdraw");

     if (info == 1) {
     permissions.setGet_info(true);
     }
     if (trade == 1) {
     permissions.setTrade(true);
     }
     if (withdraw == 1) {
     permissions.setWithdraw(true);
     }

     permissions.setValid_keys(true);
     }
     } catch (ParseException ex) {
     LOG.severe(ex.getMessage());
     apiResponse.setError(new ApiError(ERROR_PARSING, "Error while parsing api permissions for btce"));
     }
     apiResponse.setResponseObject(permissions);
     return apiResponse;

     }
     */
    private ApiResponse getActiveOrdersImpl(CurrencyPair pair) {
        ApiResponse apiResponse = new ApiResponse();
        ArrayList<Order> orderList = new ArrayList<Order>();

        String path = API_ACTIVE_ORDERS;
        HashMap<String, String> query_args = new HashMap<>();


        /*Params
         * pair, default all pairs
         */
        if (pair != null) {
            query_args.put("pair", pair.toString("_"));
        }

        String queryResult = query(API_BASE_URL, path, query_args, false);
        if (queryResult.startsWith(TOKEN_ERR)) {
            apiResponse.setError(getErrorByCode(ERROR_NO_CONNECTION));
            return apiResponse;
        }

        /*
         * {
         "success":1,
         "return":{
         "343152":{
         "pair":"btc_usd",
         "type":"sell",
         "amount":1.45000000,
         "rate":3.00000000,
         "timestamp_created":1342448420,
         "status":0
         },
         "343153":{
         "pair":"btc_usd",
         "type":"sell",
         "amount":1.33000000,
         "rate":3.00000000,
         "timestamp_created":1342448420,
         "status":1
         }
         }
         }
         */

        try {
            org.json.JSONObject httpAnswerJson = new org.json.JSONObject(queryResult);
            Integer success = (Integer) httpAnswerJson.get("success");
            if (success == 0) {
                String errorMessage = (String) httpAnswerJson.get("error");

                if (errorMessage.equals("no orders")) {
                    //No orders
                    apiResponse.setResponseObject(new ArrayList<Order>());
                } else {
                    //error
                    ApiError apiErr = new ApiError(ERROR_GENERIC, errorMessage);
                    LOG.severe("Btce returned an error: " + errorMessage);
                    apiResponse.setError(apiErr);
                }

                return apiResponse;
            } else {
                //correct
                org.json.JSONObject dataJson = (org.json.JSONObject) httpAnswerJson.get("return");

                //Iterate on orders
                String names[] = org.json.JSONObject.getNames(dataJson);
                for (int i = 0; i < names.length; i++) {
                    org.json.JSONObject tempJson = dataJson.getJSONObject(names[i]);
                    Order temp = new Order();

                    temp.setId(names[i]);

                    //Create a CurrencyPair object
                    CurrencyPair cp = CurrencyPair.getCurrencyPairFromString((String) tempJson.get("pair"), "_");

                    //Parse the status to encapsulate into Order object
                    boolean executed = false;
                    int status = tempJson.getInt("status");


                    switch (status) {
                        case 0: {
                            executed = false;
                            break;
                        }
                        case 1: {
                            executed = true;
                            break;
                        }
                        default: {
                            apiResponse.setError(new ApiError(231445, "Order status unknown : " + status));
                            break;
                        }
                    }

                    temp.setPair(cp);
                    temp.setType((String) tempJson.get("type"));
                    temp.setAmount(new Amount(tempJson.getDouble("amount"), cp.getOrderCurrency()));
                    temp.setPrice(new Amount(tempJson.getDouble("rate"), cp.getPaymentCurrency()));

                    temp.setInsertedDate(new Date(tempJson.getLong("timestamp_created")));

                    temp.setCompleted(executed);

                    if (!executed) //Do not return orders that are already executed
                    {
                        orderList.add(temp);
                    }

                }

            }
        } catch (JSONException ex) {
            LOG.severe(ex.getMessage());
            apiResponse.setError(new ApiError(ERROR_PARSING, "Error while parsing the balance response"));
            return apiResponse;
        }
        apiResponse.setResponseObject(orderList);
        return apiResponse;
    }

    private ApiResponse enterOrder(String type, CurrencyPair pair, double amount, double rate) {
        ApiResponse apiResponse = new ApiResponse();
        String order_id = "";

        HashMap<String, String> query_args = new HashMap<>();
        query_args.put("pair", pair.toString("_"));
        query_args.put("type", type);
        query_args.put("rate", Double.toString(rate));
        query_args.put("amount", Double.toString(amount));

        String queryResult = query(API_BASE_URL, API_TRADE, query_args, false);

        /* Sample Answer
         * {
         "success":1,
         "return":{
         "received":0.1,
         "remains":0,
         "order_id":0,
         "funds":{
         "usd":325,
         "btc":2.498,
         "sc":121.998,
         "ltc":0,
         "ruc":0,
         "nmc":0
         }
         }
         }
         */

        JSONParser parser = new JSONParser();
        try {
            JSONObject httpAnswerJson = (JSONObject) (parser.parse(queryResult));
            long success = (long) httpAnswerJson.get("success");
            if (success == 0) {
                //error
                String error = (String) httpAnswerJson.get("error");
                LOG.severe("Btce returned an error: " + error);
                apiResponse.setError(new ApiError(ERROR_GENERIC, error));

            } else {
                //correct
                JSONObject dataJson = (JSONObject) httpAnswerJson.get("return");
                order_id = "" + (long) dataJson.get("order_id");
                apiResponse.setResponseObject(order_id);
            }
        } catch (ParseException ex) {
            LOG.severe("httpresponse: " + queryResult + " \n" + ex.getMessage());
            apiResponse.setError(new ApiError(ERROR_PARSING, "Error while parsing the balance response"));
        }
        return apiResponse;
    }

    @Override
    public ApiError getErrorByCode(int code) {
        boolean found = false;
        ApiError toReturn = null;;
        for (int i = 0; i < errors.size(); i++) {
            ApiError temp = errors.get(i);
            if (code == temp.getCode()) {
                found = true;
                toReturn = temp;
                break;
            }
        }

        if (found) {
            return toReturn;
        } else {
            return new ApiError(ERROR_UNKNOWN, "Unknown API error");
        }
    }

    @Override
    public ApiResponse isOrderActive(String id) {
        ApiResponse existResponse = new ApiResponse();

        ApiResponse orderDetailResponse = getOrderDetail(id);
        if (orderDetailResponse.isPositive()) {
            Order order = (Order) orderDetailResponse.getResponseObject();
            existResponse.setResponseObject(true);
        } else {
            ApiError err = orderDetailResponse.getError();
            if (err.getCode() == 4564) {
                //Cannot find order
                existResponse.setResponseObject(false);
            } else {
                existResponse.setError(err);
                LOG.severe(existResponse.getError().toString());
            }
        }

        return existResponse;
    }

    @Override
    public String query(String url, HashMap<String, String> args, boolean isGet) {
        BtceService query = new BtceService(url, args);
        String queryResult = getErrorByCode(ERROR_NO_CONNECTION).getDescription();
        if (exchange.getLiveData().isConnected()) {
            queryResult = query.executeQuery(false, false);
        } else {
            LOG.severe("The bot will not execute the query, there is no connection to btce");
            queryResult = "error : no connection with btce";
        }
        return queryResult;
    }

    @Override
    public String query(String base, String method, HashMap<String, String> args, boolean isGet) {
        BtceService query = new BtceService(base, method, args, keys);
        String queryResult = getErrorByCode(ERROR_NO_CONNECTION).getDescription();
        if (exchange.getLiveData().isConnected()) {
            queryResult = query.executeQuery(true, false);
        } else {
            LOG.severe("The bot will not execute the query, there is no connection to btce");
            queryResult = "error : no connection with btce";
        }
        return queryResult;
    }

    @Override
    public String query(String url, TreeMap<String, String> args, boolean isGet) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String query(String base, String method, TreeMap<String, String> args, boolean isGet) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ApiResponse clearOrders() {
        //Since there is no API entry point for that, this call will iterate over actie
        ApiResponse toReturn = new ApiResponse();
        boolean ok = true;

        ApiResponse activeOrdersResponse = getActiveOrders();
        if (activeOrdersResponse.isPositive()) {
            ArrayList<Order> orderList = (ArrayList<Order>) activeOrdersResponse.getResponseObject();
            for (int i = 0; i < orderList.size(); i++) {
                Order tempOrder = orderList.get(i);

                ApiResponse deleteOrderResponse = cancelOrder(tempOrder.getId());
                if (deleteOrderResponse.isPositive()) {
                    boolean deleted = (boolean) deleteOrderResponse.getResponseObject();

                    if (deleted) {
                        LOG.warning("Order " + tempOrder.getId() + " deleted succesfully");
                    } else {
                        LOG.warning("Could not delete order " + tempOrder.getId() + "");
                        ok = false;
                    }

                } else {
                    LOG.severe(deleteOrderResponse.getError().toString());
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    LOG.severe(ex.getMessage());
                }

            }
            toReturn.setResponseObject(ok);
        } else {
            LOG.severe(activeOrdersResponse.getError().toString());
            toReturn.setError(activeOrdersResponse.getError());
            return toReturn;
        }

        return toReturn;
    }

    @Override
    public String getUrlConnectionCheck() {
        return checkConnectionUrl;
    }

    @Override
    public void setKeys(ApiKeys keys) {
        this.keys = keys;
    }

    @Override
    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void setApiBaseUrl(String apiBaseUrl) {
        throw new UnsupportedOperationException("Not supported yet."); //TODO change body of generated methods, choose Tools | Templates.

    }

    @Override
    public ApiResponse getLastTrades(CurrencyPair pair) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    /* Service implementation */
    private class BtceService implements ServiceInterface {

        protected String base;
        protected String method;
        protected HashMap args;
        protected ApiKeys keys;
        protected String url;

        public BtceService(String base, String method, HashMap<String, String> args, ApiKeys keys) {
            this.base = base;
            this.method = method;
            this.args = args;
            this.keys = keys;
        }

        private BtceService(String url, HashMap<String, String> args) {
            //Used for ticker, does not require auth
            this.url = url;
            this.args = args;
            this.method = "";
        }

        @Override
        public String executeQuery(boolean needAuth, boolean isGet) {

            String answer = "";
            String signature = "";
            String post_data = "";
            boolean httpError = false;
            HttpsURLConnection connection = null;

            try {
                // add nonce and build arg list
                if (needAuth) {
                    args.put("nonce", createNonce());
                    args.put("method", method);

                    post_data = TradeUtils.buildQueryString(args, ENCODING);

                    // args signature with apache cryptografic tools
                    String toHash = post_data;

                    signature = signRequest(keys.getPrivateKey(), toHash);
                }
                // build URL

                URL queryUrl;
                if (needAuth) {
                    queryUrl = new URL(base);
                } else {
                    queryUrl = new URL(url);
                }


                connection = (HttpsURLConnection) queryUrl.openConnection();
                connection.setRequestMethod("POST");

                // create and setup a HTTP connection

                connection.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("User-Agent", Global.settings.getProperty("app_name"));

                if (needAuth) {
                    connection.setRequestProperty("Key", keys.getApiKey());
                    connection.setRequestProperty("Sign", signature);
                }

                connection.setDoOutput(true);
                connection.setDoInput(true);

                //Read the response

                DataOutputStream os = new DataOutputStream(connection.getOutputStream());
                os.writeBytes(post_data);
                os.close();

                BufferedReader br = null;
                boolean toLog = false;
                if (connection.getResponseCode() >= 400) {
                    httpError = true;
                    br = new BufferedReader(new InputStreamReader((connection.getErrorStream())));
                    toLog = true;
                } else {
                    br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
                }

                String output;

                if (httpError) {
                    LOG.severe("Post Data: " + post_data);
                }
                LOG.fine("Query to :" + base + "(method=" + method + ")" + " , HTTP response : \n"); //do not log unless is error > 400
                while ((output = br.readLine()) != null) {
                    LOG.fine(output);
                    answer += output;
                }

                if (httpError) {
                    JSONParser parser = new JSONParser();
                    try {
                        JSONObject obj2 = (JSONObject) (parser.parse(answer));
                        answer = (String) obj2.get(TOKEN_ERR);

                    } catch (ParseException ex) {
                        LOG.severe(ex.getMessage());

                    }
                }
            } //Capture Exceptions
            catch (IllegalStateException ex) {
                LOG.severe(ex.getMessage());

            } catch (NoRouteToHostException | UnknownHostException ex) {
                //Global.BtceExchange.setConnected(false);
                LOG.severe(ex.getMessage());

                answer = getErrorByCode(ERROR_NO_CONNECTION).getDescription();
            } catch (IOException ex) {
                LOG.severe(ex.getMessage());
            } finally {
                //close the connection, set all objects to null
                connection.disconnect();
                connection = null;
            }
            return answer;
        }

        @Override
        public String signRequest(String secret, String hash_data) {
            String signature = "";

            Mac mac;
            SecretKeySpec key = null;

            // Create a new secret key
            try {
                key = new SecretKeySpec(secret.getBytes(ENCODING), SIGN_HASH_FUNCTION);
            } catch (UnsupportedEncodingException uee) {
                LOG.severe("Unsupported encoding exception: " + uee.toString());
                return null;
            }

            // Create a new mac
            try {
                mac = Mac.getInstance(SIGN_HASH_FUNCTION);
            } catch (NoSuchAlgorithmException nsae) {
                LOG.severe("No such algorithm exception: " + nsae.toString());
                return null;
            }

            // Init mac with key.
            try {
                mac.init(key);
            } catch (InvalidKeyException ike) {
                LOG.severe("Invalid key exception: " + ike.toString());
                return null;
            }
            try {
                signature = Hex.encodeHexString(mac.doFinal(hash_data.getBytes(ENCODING)));

            } catch (UnsupportedEncodingException ex) {
                LOG.severe(ex.getMessage());
            }
            return signature;
        }
    }
}
