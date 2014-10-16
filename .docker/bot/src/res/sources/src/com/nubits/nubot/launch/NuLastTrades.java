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

package com.nubits.nubot.launch;

import com.nubits.nubot.exchanges.Exchange;
import com.nubits.nubot.exchanges.ExchangeLiveData;
import com.nubits.nubot.global.Constant;
import com.nubits.nubot.global.Global;
import com.nubits.nubot.global.Passwords;
import com.nubits.nubot.models.ApiResponse;
import com.nubits.nubot.models.CurrencyPair;
import com.nubits.nubot.models.Trade;
import com.nubits.nubot.tasks.TaskManager;
import com.nubits.nubot.trading.keys.ApiKeys;
import com.nubits.nubot.trading.wrappers.BtceWrapper;
import com.nubits.nubot.trading.wrappers.BterWrapper;
import com.nubits.nubot.trading.wrappers.CcedkWrapper;
import com.nubits.nubot.trading.wrappers.PeatioWrapper;
import com.nubits.nubot.trading.wrappers.PoloniexWrapper;
import com.nubits.nubot.utils.FileSystem;
import com.nubits.nubot.utils.Utils;
import com.nubits.nubot.utils.logging.NuLogger;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class NuLastTrades {
    
    private static final Logger LOG = Logger.getLogger(NuLastTrades.class.getName());
    private final String USAGE_STRING = "java  -jar NuLastTrades <exchange-name> <apikey> <apisecret> <pair(xxx_yyy)>";
    private final String HEADER = "id,order_id,pair,type,price,amount,date";
    private String output ;
    
    private String api;
    private String secret;
    private String exchangename;
    private CurrencyPair pair;
    
    private ApiKeys keys;
     public static void main(String[] args) {
         //Load settings
         Utils.loadProperties("settings.properties");

         NuLastTrades app = new NuLastTrades();

         String folderName = "NuLastTrades_"+System.currentTimeMillis()+"/";
         String logsFolder = Global.settings.getProperty("log_path")+folderName;
         
         //Create log dir
         FileSystem.mkdir(logsFolder);
         if (app.readParams(args)) {
            try {
                NuLogger.setup(false,logsFolder);
            } catch (IOException ex) {
                LOG.severe(ex.getMessage());
            }

            LOG.info("Launching NuLastTrades ");
            app.prepareForExecution();
            app.execute();
            LOG.info("Done");
            System.exit(0);

        } else {
            System.exit(0);
        }
    }
     
     
     private void prepareForExecution() {
        //Wrap the keys into a new ApiKeys object
        keys = new ApiKeys(secret, api);

        Global.exchange = new Exchange(exchangename);

        //Create e ExchangeLiveData object to accomodate liveData from the exchange
        ExchangeLiveData liveData = new ExchangeLiveData();
        Global.exchange.setLiveData(liveData);

      
        if (exchangename.equals(Constant.BTCE)) {
            Global.exchange.setTrade(new BtceWrapper(keys, Global.exchange));
        } else if (exchangename.equals(Constant.PEATIO_BTCCNY)) {
           Global.exchange.setTrade(new PeatioWrapper(keys, Global.exchange, Constant.PEATIO_BTCCNY_API_BASE));
        } else if (exchangename.equals(Constant.CCEDK)) {
            Global.exchange.setTrade(new CcedkWrapper(keys, Global.exchange));
        } else if (exchangename.equals(Constant.BTER)) {
            Global.exchange.setTrade(new BterWrapper(keys, Global.exchange));
        } 
        else if (exchangename.equals(Constant.POLONIEX)) {
            //Wrap the keys into a new ApiKeys object
            keys = new ApiKeys(Passwords.POLONIEX_SECRET, Passwords.POLONIEX_KEY);
            //Create a new TradeInterface object using the custom implementation
            //Assign the TradeInterface to the exchange
            Global.exchange.setTrade(new PoloniexWrapper(keys, Global.exchange));
        }
        else {
            LOG.severe("Exchange " + exchangename + " not supported");
            System.exit(0);
        }
        
        Global.exchange.getLiveData().setUrlConnectionCheck(Global.exchange.getTrade().getUrlConnectionCheck());


        //Create a TaskManager and
        Global.taskManager = new TaskManager();
        //Start checking for connection
        Global.taskManager.getCheckConnectionTask().start();

        //Wait a couple of seconds for the connectionThread to get live
        LOG.fine("Exchange setup complete. Now checking connection ...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            LOG.severe(ex.getMessage());
        }

    }

    private boolean readParams(String[] args) {
        boolean ok = false;

        if (args.length != 4) {
            LOG.severe("wrong argument number : call it with \n" + USAGE_STRING);
            System.exit(0);
        }

        exchangename = args[0];
        api = args[1];
        secret = args[2];
        pair = CurrencyPair.getCurrencyPairFromString(args[3], "_");

        output = "last_trades_"+exchangename+"_"+pair.toString()+".csv";
        ok = true;
        return ok;
    }

    private void execute() {
        FileSystem.writeToFile(HEADER, output, false);
        ApiResponse activeOrdersResponse = Global.exchange.getTrade().getLastTrades(pair);
        if (activeOrdersResponse.isPositive()) {
            ArrayList<Trade> tradeList = (ArrayList<Trade>) activeOrdersResponse.getResponseObject();
            LOG.info("Last 24h trades : " + tradeList.size());
            for (int i = 0; i < tradeList.size(); i++) {
                Trade tempTrade = tradeList.get(i);
                LOG.info(tempTrade.toString());
                FileSystem.writeToFile(tempTrade.toCsvString(), output, true);
            }
        } else {
            LOG.severe(activeOrdersResponse.getError().toString());
        }
    
    }
         
}
