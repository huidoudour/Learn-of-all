package com.zms.combination;

import java.util.ArrayList;
import java.util.List;

// 菜单类：树枝节点
public class Menu extends MenuComponent {
    // 菜单可以有多个菜单或者菜单项
    private List<MenuComponent> menuComponents = new ArrayList<MenuComponent>();

    public Menu(String name, int level) {
        this.name = name;
        this.level = level;
    }

    @Override
    public void add(MenuComponent component) {
        menuComponents.add(component);
    }

    @Override
    public void remove(MenuComponent component) {
        menuComponents.remove(component);
    }

    @Override
    public MenuComponent getChild(int index) {
        return menuComponents.get(index);
    }

    @Override
    public void print() {
        for(int i = 0; i < level; i++) {
            System.out.print("--");
        }
        System.out.println(name);
        for (MenuComponent menuComponent : menuComponents) {
            menuComponent.print();
        }
    }
}
