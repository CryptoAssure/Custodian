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
package com.nubits.nubot.pricefeed;

import com.nubits.nubot.models.Amount;
import com.nubits.nubot.models.CurrencyPair;
import com.nubits.nubot.models.LastPrice;
import com.nubits.nubot.utils.Utils;
import java.io.IOException;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author desrever <desrever at nubits.com>
 */
public class CoinmarketcapnexuistPriceFeed extends AbstractPriceFeed {

    private static final Logger LOG = Logger.getLogger(BitcoinaveragePriceFeed.class.getName());

    public CoinmarketcapnexuistPriceFeed() {
        name = PriceFeedManager.COINMARKETCAP_NE;
        refreshMinTime = 120 * 1000; //Two minutes
        lastRequest = 0L;
    }

    @Override
    public LastPrice getLastPrice(CurrencyPair pair) {

        long now = System.currentTimeMillis();
        long diff = now - lastRequest;
        if (diff >= refreshMinTime) {
            String htmlString;
            try {
                htmlString = Utils.getHTML(getUrl(pair));
            } catch (IOException ex) {
                LOG.severe(ex.getMessage());
                return new LastPrice(true, name, pair.getOrderCurrency(), null);
            }
            JSONParser parser = new JSONParser();
            try {
                JSONObject httpAnswerJson = (JSONObject) (parser.parse(htmlString));
                JSONObject price = (JSONObject) httpAnswerJson.get("price");
                double last = Utils.getDouble(price.get("usd"));
                lastRequest = System.currentTimeMillis();
                lastPrice = new LastPrice(false, name, pair.getOrderCurrency(), new Amount(last, pair.getPaymentCurrency()));
                return lastPrice;
            } catch (Exception ex) {
                LOG.severe(ex.getMessage());
                lastRequest = System.currentTimeMillis();
                return new LastPrice(true, name, pair.getOrderCurrency(), null);
            }
        } else {
            LOG.fine("Wait " + (refreshMinTime - (System.currentTimeMillis() - lastRequest)) + " ms "
                    + "before making a new request. Now returning the last saved price\n\n");
            return lastPrice;
        }


    }

    private String getUrl(CurrencyPair pair) {
        //controls
        String currency = pair.getOrderCurrency().getCode().toLowerCase();
        return "http://coinmarketcap-nexuist.rhcloud.com/api/" + currency;
    }
}
