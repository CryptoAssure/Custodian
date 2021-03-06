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
package com.nubits.nubot.tests;

import com.nubits.nubot.RPC.NuRPCClient;
import com.nubits.nubot.exchanges.Exchange;
import com.nubits.nubot.exchanges.ExchangeLiveData;
import com.nubits.nubot.global.Constant;
import com.nubits.nubot.global.Global;
import com.nubits.nubot.global.Passwords;
import com.nubits.nubot.models.OptionsJSON;
import com.nubits.nubot.tasks.TaskManager;
import com.nubits.nubot.trading.keys.ApiKeys;
import com.nubits.nubot.trading.wrappers.PeatioWrapper;
import com.nubits.nubot.utils.Utils;
import com.nubits.nubot.utils.logging.NuLogger;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author desrever <desrever at nubits.com>
 */
public class TestOrderTask {

    private static final Logger LOG = Logger.getLogger(TestOrderTask.class.getName());
    private static Exchange exchange;
    private static String nudip = "127.0.0.1";
    private static int nudport = 9091;

    public static void main(String[] args) {
        setup();
        Global.taskManager.getCheckOrdersTask().start();
    }

    private static void setup() {

        String folderName = "tests_"+System.currentTimeMillis()+"/";
        String logsFolder = Global.settings.getProperty("log_path")+folderName;
         
       
        try {
            NuLogger.setup(true,logsFolder);
        } catch (IOException ex) {
            LOG.severe(ex.getMessage());
        }
        LOG.setLevel(Level.FINE);

        System.setProperty("javax.net.ssl.trustStore", Global.settings.getProperty("keystore_path"));
        System.setProperty("javax.net.ssl.trustStorePassword", Global.settings.getProperty("keystore_pass"));


        Global.options = new OptionsJSON(true, nudip, "", "", "", "",
                nudip, nudport, nudport, nudport, true, "", true, true, null,
                60, 30, false, false, "", -1, 0,false, null);


        //Check local filesystem for API keys
        LOG.fine("Checking existance of API keys on local filesystem");

        ApiKeys keys;

        String secret = Passwords.PEATIO_SECRET;
        String apikey = Passwords.PEATIO_KEY;

        //Wrap the keys into a new ApiKeys object
        keys = new ApiKeys(secret, apikey);

        //Create a new Exchange


        exchange = new Exchange(Constant.PEATIO_BTCCNY);

        //Create e ExchangeLiveData object to accomodate liveData from the exchange
        ExchangeLiveData liveData = new ExchangeLiveData();
        exchange.setLiveData(liveData);


        //Create a new TradeInterface object using the BtceWrapper implementation
        //Assign the TradeInterface to the btceExchange
        exchange.setTrade(new PeatioWrapper(keys, exchange, Constant.PEATIO_BTCCNY_API_BASE));
        exchange.getLiveData().setUrlConnectionCheck(exchange.getTrade().getUrlConnectionCheck());

        //Create a TaskManager and
        Global.taskManager = new TaskManager();
        //Start checking for connection
        Global.taskManager.getCheckConnectionTask().start();


        Global.publicAddress = Passwords.CUSTODIAN_PUBLIC_ADDRESS;

        LOG.fine("Setting up (verbose) RPC client on " + nudip + ":" + nudport);
        Global.rpcClient = new NuRPCClient(nudip, nudport, Passwords.NUD_RPC_USER,
                Passwords.NUD_RPC_PASS, true, true, Global.publicAddress, Constant.NBT_PPC, "testid");

        Utils.printSeparator();


        Global.taskManager.getCheckNudTask().start();
        //Wait a couple of seconds for the connectionThread to get live
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            LOG.severe(ex.getMessage());
        }


        LOG.fine("Check connection with nud");
        if (Global.rpcClient.isConnected()) {
            LOG.fine("OK!");
        } else {
            LOG.severe("Problem while connecting with nud");
            System.exit(0);
        }





        /* Setup (end) ------------------------------------------------------------------------------------------------------ */

    }
}
