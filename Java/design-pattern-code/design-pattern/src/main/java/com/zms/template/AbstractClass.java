package com.zms.template;

// 抽象类
public abstract class AbstractClass {

    // 模版方法定义
    public final void process() {
        pourOil();
        heatOil();
        pourVegetable();
        pourSauce();
        fry();
    }

    // 倒油
    public void pourOil() {
        System.out.println("倒油");
    }

    // 热油
    public void heatOil() {
        System.out.println("热油");
    }

    // 倒蔬菜
    public abstract void pourVegetable();

    // 倒调料
    public abstract void pourSauce();

    // 翻炒，都是一样，所以不需要抽象
    public void fry() {
        System.out.println("炒啊炒");
    }
}
