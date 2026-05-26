package com.zms.combination;

// 菜单组件：抽象根节点
public abstract class MenuComponent {

    // 菜单组件的名称
    protected String name;

    // 菜单组件的层级
    protected int level;

    // 添加子菜单
    public void add(MenuComponent component) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // 移除子菜单
    public void remove(MenuComponent component) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // 获取指定子菜单
    public MenuComponent getChild(int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // 获取菜单或者菜单项的名称
    public String getName() {
        return name;
    }

    // 打印菜单名称的方法（包含菜单和子菜单项）
    public abstract void print();
}
