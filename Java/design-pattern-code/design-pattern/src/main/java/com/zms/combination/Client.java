package com.zms.combination;

public class Client {
    public static void main(String[] args) {
        // 二级菜单和菜单项
        Menu menu1 = new Menu("菜单管理", 2);
        menu1.add(new MenuItem("页面访问", 3));
        menu1.add(new MenuItem("展示菜单", 3));
        menu1.add(new MenuItem("编辑菜单", 3));
        menu1.add(new MenuItem("删除菜单", 3));
        menu1.add(new MenuItem("新增菜单", 3));

        Menu menu2 = new Menu("权限配置", 2);
        menu2.add(new MenuItem("页面访问", 3));
        menu2.add(new MenuItem("提交保存", 3));

        Menu menu3 = new Menu("角色管理", 2);
        menu3.add(new MenuItem("页面访问", 3));
        menu3.add(new MenuItem("修改角色", 3));

        // 创建一级菜单
        // 将二级菜单添加到一级菜单
        Menu menu = new Menu("系统管理", 1);
        menu.add(menu1);
        menu.add(menu2);
        menu.add(menu3);

        // 打印系统菜单
        menu.print();
    }
}
