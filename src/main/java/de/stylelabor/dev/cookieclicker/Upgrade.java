package de.stylelabor.dev.cookieclicker;

import org.bukkit.Material;

public class Upgrade {
    private final String name;
    private final Material item;
    private final int cost;
    private final int cookiesPerClick;

    public Upgrade(String name, Material item, int cost, int cookiesPerClick) {
        this.name = name;
        this.item = item;
        this.cost = cost;
        this.cookiesPerClick = cookiesPerClick;
    }

    public String getName() {
        return name;
    }

    public Material getItem() {
        return item;
    }

    public int getCost() {
        return cost;
    }

    public int getCookiesPerClick() {
        return cookiesPerClick;
    }
}