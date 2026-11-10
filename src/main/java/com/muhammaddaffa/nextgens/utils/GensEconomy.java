package com.muhammaddaffa.nextgens.utils;

import com.muhammaddaffa.mdlib.hooks.VaultEconomy;
import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.nextgens.NextGens;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * Money access for NextGens. Uses Vault by default. When {@code economy.excellent-currency} is set,
 * all generator money (shop, upgrades, repairs, sell, cashback) goes through that ExcellentEconomy
 * currency instead, resolved by reflection so there is no compile dependency.
 */
public final class GensEconomy {

    private static final String API_CLASS = "su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI";

    private static volatile boolean resolved;
    private static volatile Object api;
    private static volatile String currency;
    private static volatile Method balanceMethod;
    private static volatile Method withdrawMethod;
    private static volatile Method depositMethod;

    private GensEconomy() {
    }

    /** Re-reads the config on the next call (used on enable and reload). */
    public static synchronized void reset() {
        resolved = false;
        api = null;
        currency = null;
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        String configured = NextGens.DEFAULT_CONFIG.getConfig().getString("economy.excellent-currency", "");
        if (configured == null || configured.isBlank()) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(apiClass);
            Object provider = rsp == null ? null : rsp.getProvider();
            if (provider == null || !(Boolean) apiClass.getMethod("hasCurrency", String.class).invoke(provider, configured)) {
                Logger.warning("ExcellentEconomy currency '" + configured + "' was not found, using Vault instead.");
                return;
            }
            balanceMethod = apiClass.getMethod("getBalance", Player.class, String.class);
            withdrawMethod = apiClass.getMethod("withdraw", Player.class, String.class, double.class);
            depositMethod = apiClass.getMethod("deposit", Player.class, String.class, double.class);
            api = provider;
            currency = configured;
            Logger.info("Economy: using ExcellentEconomy currency '" + configured + "'.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            Logger.warning("Could not hook into ExcellentEconomy (" + ex.getClass().getSimpleName() + ": "
                    + ex.getMessage() + "), using Vault instead.");
        }
    }

    private static boolean excellent() {
        resolve();
        return api != null;
    }

    public static double getBalance(Player player) {
        if (!excellent()) {
            return VaultEconomy.getBalance(player);
        }
        Player online = player.getPlayer();
        if (online == null) {
            return 0.0;
        }
        try {
            return ((Number) balanceMethod.invoke(api, online, currency)).doubleValue();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Logger.warning("ExcellentEconomy balance lookup failed: " + ex.getMessage());
            return 0.0;
        }
    }

    public static EconomyResponse withdraw(Player player, double amount) {
        if (!excellent()) {
            return VaultEconomy.withdraw(player, amount);
        }
        return transaction(player, amount, withdrawMethod, true);
    }

    public static EconomyResponse deposit(Player player, double amount) {
        if (!excellent()) {
            return VaultEconomy.deposit(player, amount);
        }
        return transaction(player, amount, depositMethod, false);
    }

    private static EconomyResponse transaction(Player player, double amount, Method method, boolean withdraw) {
        Player online = player.getPlayer();
        if (online == null) {
            return failure(amount, 0.0, "Player is offline");
        }
        if (amount < 0.0) {
            return failure(amount, getBalance(player), "Negative amount");
        }
        if (amount == 0.0) {
            return new EconomyResponse(0.0, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
        }
        if (withdraw && getBalance(player) < amount) {
            return failure(amount, getBalance(player), "Insufficient funds");
        }
        try {
            boolean ok = (Boolean) method.invoke(api, online, currency, amount);
            return ok
                    ? new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null)
                    : failure(amount, getBalance(player), "Transaction refused");
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Logger.warning("ExcellentEconomy " + (withdraw ? "withdraw" : "deposit") + " failed: " + ex.getMessage());
            return failure(amount, 0.0, ex.getMessage());
        }
    }

    private static EconomyResponse failure(double amount, double balance, String error) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.FAILURE, error);
    }

}
